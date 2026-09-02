package com.example.utils

import com.example.data.repository.GameStateRepository
import com.example.models.AssistantCoachNotification
import com.example.models.FacilityType
import com.example.models.Finance
import com.example.models.FinanceAdvanced
import com.example.models.HistoryManager
import com.example.models.MatchBoxScore
import com.example.models.NbaTeam
import com.example.models.Player
import com.example.models.Season
import com.example.models.StaffMember
import com.example.models.TeamFacilities
import com.example.models.TeamStaff
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/**
 * Review-driven import invariants that must be checked before Room or save-slot metadata is
 * mutated. This adapter is import/export-codec-only and deliberately leaves gameplay rules
 * untouched.
 */
class ImportSnapshotReviewValidationFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (type.rawType != GameStateRepository.GameStateSnapshot::class.java) return null
        val delegate = gson.getDelegateAdapter(this, type)
        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T) = delegate.write(out, value)

            override fun read(input: JsonReader): T {
                val tree = JsonParser.parseReader(input)
                if (!tree.isJsonObject) throw JsonParseException("Save snapshot must be a JSON object")
                validateRawSnapshot(tree.asJsonObject)
                val value = delegate.fromJsonTree(tree)
                @Suppress("UNCHECKED_CAST")
                validateDecodedSnapshot(value as GameStateRepository.GameStateSnapshot, gson)
                return value
            }
        }.nullSafe()
    }

    private fun validateRawSnapshot(root: JsonObject) {
        val schema = root.get("schemaVersion")
        if (schema == null || !schema.isJsonPrimitive || !schema.asJsonPrimitive.isNumber || schema.asInt != SUPPORTED_SCHEMA_VERSION) {
            throw JsonParseException("Unsupported or missing snapshot schemaVersion")
        }
        validatePlayerFieldsRecursively(root)
    }

    private fun validatePlayerFieldsRecursively(element: JsonElement) {
        when {
            element.isJsonArray -> element.asJsonArray.forEach(::validatePlayerFieldsRecursively)
            element.isJsonObject -> {
                val obj = element.asJsonObject
                val looksLikePlayer = PLAYER_REQUIRED_FIELDS.all(obj::has)
                if (looksLikePlayer) {
                    val position = obj.get("position")
                    if (position == null || !position.isJsonPrimitive || !position.asJsonPrimitive.isString || position.asString !in SUPPORTED_POSITIONS) {
                        throw JsonParseException("Imported player contains an unsupported position")
                    }
                    listOf("careerGames", "seasonGames").forEach { field ->
                        val value = obj.get(field)
                        if (value != null && value.isJsonPrimitive && value.asJsonPrimitive.isNumber && value.asLong >= Int.MAX_VALUE.toLong()) {
                            throw JsonParseException("Imported player leaves no headroom for $field")
                        }
                    }
                    val age = obj.get("age")
                    if (age == null || !age.isJsonPrimitive || !age.asJsonPrimitive.isNumber || age.asLong !in 1 until Int.MAX_VALUE.toLong()) {
                        throw JsonParseException("Imported player age leaves no headroom for offseason progression")
                    }
                }
                obj.entrySet().forEach { (_, child) ->
                    if (child.isJsonPrimitive && child.asJsonPrimitive.isString) {
                        val parsed = runCatching { JsonParser.parseString(child.asString) }.getOrNull()
                        if (parsed != null && (parsed.isJsonObject || parsed.isJsonArray)) {
                            validatePlayerFieldsRecursively(parsed)
                        }
                    } else {
                        validatePlayerFieldsRecursively(child)
                    }
                }
            }
        }
    }

    private fun validateDecodedSnapshot(snapshot: GameStateRepository.GameStateSnapshot, gson: Gson) {
        if (snapshot.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            throw JsonParseException("Unsupported snapshot schemaVersion")
        }
        val season = snapshot.seasonJson?.let { gson.fromJson(it, Season::class.java) }
            ?: throw JsonParseException("seasonJson is required")
        validateSeasonProgress(season)
        validateCanonicalGameParticipants(season)
        validateSeasonNumberHeadroom(season)
        validatePlayoffCompletionState(snapshot, season, gson)
        validateFinance(snapshot, season, gson)
        validateAdvancedFinance(snapshot, gson)
        validateNotifications(snapshot, gson)
        validateFacilities(snapshot, gson)
        validateBoxScore(snapshot, gson)
        validateStaffIdentityAndSalary(snapshot, gson)
        validateStartingFive(snapshot, season, gson)
    }

    private fun validateSeasonProgress(season: Season) {
        if (season.currentDay !in 0..MAX_REGULAR_SEASON_GAMES) {
            throw JsonParseException("season.currentDay exceeds the supported regular-season range")
        }
        season.standings.forEach { (teamName, record) ->
            if (record.gamesPlayed != season.currentDay) {
                throw JsonParseException("Standings progress for $teamName does not match season.currentDay")
            }
        }
    }

    private fun validateCanonicalGameParticipants(season: Season) {
        val canonicalTeams = season.teams.associateBy(::persistenceTeamId)
        val canonicalPlayers = season.teams.flatMap { it.players }.associateBy(Player::id)
        season.history.forEach { result ->
            if (canonicalTeams[persistenceTeamId(result.homeTeam)] == null) {
                throw JsonParseException("Historical home team is not canonical")
            }
            if (canonicalTeams[persistenceTeamId(result.awayTeam)] == null) {
                throw JsonParseException("Historical away team is not canonical")
            }

            val recordedHomePlayers = result.homeTeam.players.associateBy(Player::id)
            val recordedAwayPlayers = result.awayTeam.players.associateBy(Player::id)
            result.homeStats.keys.forEach { historical ->
                val recorded = recordedHomePlayers[historical.id]
                    ?: throw JsonParseException("Historical home stats contain a player outside the recorded home roster")
                val canonical = canonicalPlayers[historical.id]
                    ?: throw JsonParseException("Historical home stats reference a player outside persisted player state")
                if (historical != recorded || historical != canonical) {
                    throw JsonParseException("Historical home stats conflict with persisted player state")
                }
            }
            result.awayStats.keys.forEach { historical ->
                val recorded = recordedAwayPlayers[historical.id]
                    ?: throw JsonParseException("Historical away stats contain a player outside the recorded away roster")
                val canonical = canonicalPlayers[historical.id]
                    ?: throw JsonParseException("Historical away stats reference a player outside persisted player state")
                if (historical != recorded || historical != canonical) {
                    throw JsonParseException("Historical away stats conflict with persisted player state")
                }
            }

            val recordedParticipants = recordedHomePlayers + recordedAwayPlayers
            result.injuries.forEach { injury ->
                val recorded = recordedParticipants[injury.player.id]
                    ?: throw JsonParseException("Historical injury references a player outside recorded participants")
                val canonical = canonicalPlayers[injury.player.id]
                    ?: throw JsonParseException("Historical injury references a player outside persisted player state")
                if (injury.player != recorded || injury.player != canonical) {
                    throw JsonParseException("Historical injury player conflicts with persisted player state")
                }
            }
        }
    }

    private fun validateSeasonNumberHeadroom(season: Season) {
        if (season.seasonNumber >= Int.MAX_VALUE) {
            throw JsonParseException("seasonNumber leaves no headroom for the next season")
        }
    }

    private fun validatePlayoffCompletionState(
        snapshot: GameStateRepository.GameStateSnapshot,
        season: Season,
        gson: Gson
    ) {
        val history = snapshot.historyJson?.let { gson.fromJson(it, HistoryManager::class.java) }
            ?: throw JsonParseException("historyJson is required")
        val currentSeasonCompleted = history.seasons.any { it.seasonNumber == season.seasonNumber }
        val hasPlayoffResult = snapshot.playoffResultJson != null
        if (currentSeasonCompleted != hasPlayoffResult) {
            throw JsonParseException("Current-season completion and playoffResultJson must agree")
        }
    }

    private fun validateFinance(snapshot: GameStateRepository.GameStateSnapshot, season: Season, gson: Gson) {
        val raw = snapshot.financeJson ?: return
        val finance = gson.fromJson(raw, Finance::class.java)
            ?: throw JsonParseException("financeJson could not be decoded")
        if (finance.arenaSeatsLevel !in 1..MAX_FINANCE_UPGRADE_LEVEL ||
            finance.medicalStaffLevel !in 1..MAX_FINANCE_UPGRADE_LEVEL ||
            finance.scoutingLevel !in 1..MAX_FINANCE_UPGRADE_LEVEL
        ) {
            throw JsonParseException("Finance upgrade levels must be within the supported 1..5 range")
        }
        finance.sponsors.forEach { sponsor ->
            if (sponsor.yearsRemaining <= 0) {
                throw JsonParseException("Active finance sponsors must have remaining contract term")
            }
        }

        val managedTeam = snapshot.teamJson?.let { gson.fromJson(it, NbaTeam::class.java) }
            ?: season.teams.firstOrNull { it.name == season.userTeamName }
            ?: return
        val ticketOverride = snapshot.financeAdvancedJson
            ?.let { gson.fromJson(it, FinanceAdvanced::class.java) }
            ?.ticketPrice
            ?: 0
        val ticketPrice = maxOf(DEFAULT_MAX_TICKET_PRICE, ticketOverride)
        val expandedCapacity = managedTeam.arena.capacity.toLong() + (finance.arenaSeatsLevel - 1L) * ARENA_SEATS_PER_LEVEL
        if (expandedCapacity <= 0L || expandedCapacity > Int.MAX_VALUE.toLong()) {
            throw JsonParseException("Expanded arena capacity exceeds the supported Int range")
        }
        val gateRevenue = safeMultiply(expandedCapacity, ticketPrice.toLong(), "gate revenue")
        val annualSponsorRevenue = finance.sponsors.fold(0L) { acc, sponsor ->
            safeAdd(acc, sponsor.amountPerYear.toLong(), "sponsor revenue")
        }
        if (annualSponsorRevenue > Int.MAX_VALUE.toLong()) {
            throw JsonParseException("Imported sponsor revenue exceeds the supported aggregate range")
        }
        val sponsorPerGame = annualSponsorRevenue / MAX_REGULAR_SEASON_GAMES
        val maxNextCredit = safeAdd(gateRevenue, sponsorPerGame, "next-game credit")
        if (finance.budget.toLong() > Int.MAX_VALUE.toLong() - maxNextCredit) {
            throw JsonParseException("Imported budget leaves no headroom for normal game revenue")
        }

        val maxAnnualPayroll = snapshot.contractsJson?.let { contractsJson ->
            val type = object : TypeToken<List<com.example.models.PlayerContract>>() {}.type
            val contracts: List<com.example.models.PlayerContract> = gson.fromJson(contractsJson, type)
            contracts.fold(0L) { acc, contract -> safeAdd(acc, contract.salary, "contract payroll") }
        } ?: 0L
        val playerSalaryPerGame = (maxAnnualPayroll / MAX_REGULAR_SEASON_GAMES).coerceAtMost(Int.MAX_VALUE.toLong())
        val coachDebit = snapshot.coachJson?.let { coachJson ->
            gson.fromJson(coachJson, com.example.models.Coach::class.java)?.salary?.toLong() ?: 0L
        } ?: 0L
        val maxNextDebit = safeAdd(safeAdd(playerSalaryPerGame, OPERATIONS_DEBIT, "next-game debit"), coachDebit, "next-game debit")
        if (finance.budget.toLong() < Int.MIN_VALUE.toLong() + maxNextDebit) {
            throw JsonParseException("Imported budget leaves no headroom for normal game expenses")
        }
    }

    private fun validateAdvancedFinance(snapshot: GameStateRepository.GameStateSnapshot, gson: Gson) {
        val raw = snapshot.financeAdvancedJson ?: return
        val advanced = gson.fromJson(raw, FinanceAdvanced::class.java)
            ?: throw JsonParseException("financeAdvancedJson could not be decoded")
        val expenseParts = listOf(
            advanced.expenses.playerSalaries,
            advanced.expenses.staffSalaries,
            advanced.expenses.facilityMaintenance,
            advanced.expenses.travelLogistics,
            advanced.expenses.operationalExpenses,
            advanced.expenses.luxuryTaxPaid
        )
        val expenseTotal = expenseParts.fold(0L) { acc, value -> safeAdd(acc, value.toLong(), "advanced-finance expenses") }
        if (expenseTotal > Int.MAX_VALUE.toLong()) {
            throw JsonParseException("Imported advanced-finance expenses exceed the supported aggregate range")
        }
        val sponsorshipTotal = advanced.activeSponsorships.fold(0L) { acc, sponsorship ->
            if (sponsorship.yearsRemaining <= 0) {
                throw JsonParseException("Active sponsorships must have remaining contract term")
            }
            safeAdd(acc, sponsorship.annualAmount.toLong(), "active sponsorship revenue")
        }
        if (sponsorshipTotal > Int.MAX_VALUE.toLong()) {
            throw JsonParseException("Imported active sponsorship revenue exceeds the supported aggregate range")
        }
    }

    private fun validateNotifications(snapshot: GameStateRepository.GameStateSnapshot, gson: Gson) {
        val raw = snapshot.notificationsJson ?: return
        val type = object : TypeToken<List<AssistantCoachNotification>>() {}.type
        val notifications: List<AssistantCoachNotification> = gson.fromJson(raw, type)
        notifications.forEach { notification ->
            val bonusType = notification.recommendedBonusType
            val bonusLabel = notification.recommendedBonusLabel
            if ((bonusType == null) != (bonusLabel == null)) {
                throw JsonParseException("Notification recommendation type and label must be paired")
            }
            if (bonusType != null && bonusType !in SUPPORTED_BONUS_TYPES) {
                throw JsonParseException("Notification contains an unsupported recommendation type")
            }
            if (bonusLabel != null && bonusLabel.isBlank()) {
                throw JsonParseException("Notification recommendation label must not be blank")
            }
        }
    }

    private fun validateFacilities(snapshot: GameStateRepository.GameStateSnapshot, gson: Gson) {
        val raw = snapshot.facilitiesJson ?: return
        val facilities = gson.fromJson(raw, TeamFacilities::class.java)
            ?: throw JsonParseException("facilitiesJson could not be decoded")
        val expected = listOf(
            facilities.arena to FacilityType.ARENA,
            facilities.training to FacilityType.TRAINING_FACILITY,
            facilities.medical to FacilityType.MEDICAL_CENTER,
            facilities.scouting to FacilityType.SCOUTING_DEPT
        )
        expected.forEach { (facility, expectedType) ->
            if (facility.type != expectedType) {
                throw JsonParseException("Facility slot does not match its canonical type")
            }
        }
    }

    private fun validateBoxScore(snapshot: GameStateRepository.GameStateSnapshot, gson: Gson) {
        val raw = snapshot.latestBoxScoreJson ?: return
        val box = gson.fromJson(raw, MatchBoxScore::class.java)
            ?: throw JsonParseException("latestBoxScoreJson could not be decoded")
        val homeQuarterTotal = safeIntSum(box.homeQuarterScores, "home quarter scores")
        val awayQuarterTotal = safeIntSum(box.awayQuarterScores, "away quarter scores")
        val homePlayerTotal = safeIntSum(box.homePlayers.map { it.points }, "home player points")
        val awayPlayerTotal = safeIntSum(box.awayPlayers.map { it.points }, "away player points")
        if (homeQuarterTotal != box.homeScore || awayQuarterTotal != box.awayScore ||
            box.homeTeamTotals.points != box.homeScore || box.awayTeamTotals.points != box.awayScore ||
            homePlayerTotal != box.homeScore || awayPlayerTotal != box.awayScore
        ) {
            throw JsonParseException("Imported box-score totals do not reconcile with the recorded score")
        }
    }

    private fun validateStaffIdentityAndSalary(snapshot: GameStateRepository.GameStateSnapshot, gson: Gson) {
        val staff = snapshot.teamStaffJson?.let { gson.fromJson(it, TeamStaff::class.java) } ?: return
        val employed = buildList<StaffMember> {
            staff.headCoach?.let(::add)
            addAll(staff.assistants)
            staff.strengthCoach?.let(::add)
            staff.scout?.let(::add)
            staff.teamDoctor?.let(::add)
            addAll(staff.executives)
        }
        val employedIds = employed.map(StaffMember::id).toSet()
        val salaryTotal = employed.fold(0L) { acc, member -> safeAdd(acc, member.salary.toLong(), "team staff salaries") }
        if (salaryTotal > Int.MAX_VALUE.toLong()) {
            throw JsonParseException("Imported team staff salaries exceed the supported aggregate range")
        }
        snapshot.staffMarketJson?.let { raw ->
            val type = object : TypeToken<List<StaffMember>>() {}.type
            val market: List<StaffMember> = gson.fromJson(raw, type)
            if (market.any { it.id in employedIds }) {
                throw JsonParseException("Staff-market id collides with currently employed staff")
            }
        }
    }

    private fun validateStartingFive(snapshot: GameStateRepository.GameStateSnapshot, season: Season, gson: Gson) {
        val raw = snapshot.startingFiveJson ?: throw JsonParseException("startingFiveJson is required")
        val type = object : TypeToken<List<Player>>() {}.type
        val startingFive: List<Player> = gson.fromJson(raw, type)
        val managedTeam = snapshot.teamJson?.let { gson.fromJson(it, NbaTeam::class.java) }
            ?: season.teams.singleOrNull { it.name == season.userTeamName }
            ?: throw JsonParseException("Managed team could not be resolved")
        val canonical = managedTeam.players.associateBy(Player::id)
        startingFive.forEach { player ->
            val persisted = canonical[player.id]
                ?: throw JsonParseException("Starting-five player is outside the managed roster")
            if (persisted != player) {
                throw JsonParseException("Starting-five player conflicts with canonical managed-roster state")
            }
        }
    }

    private fun safeIntSum(values: List<Int>, label: String): Int {
        val total = values.fold(0L) { acc, value -> safeAdd(acc, value.toLong(), label) }
        if (total !in 0..Int.MAX_VALUE.toLong()) throw JsonParseException("$label exceed the supported Int range")
        return total.toInt()
    }

    private fun safeAdd(left: Long, right: Long, label: String): Long = try {
        Math.addExact(left, right)
    } catch (_: ArithmeticException) {
        throw JsonParseException("Imported $label overflow Long")
    }

    private fun safeMultiply(left: Long, right: Long, label: String): Long = try {
        Math.multiplyExact(left, right)
    } catch (_: ArithmeticException) {
        throw JsonParseException("Imported $label overflow Long")
    }

    private fun persistenceTeamId(team: NbaTeam): String =
        team.abbreviation.ifBlank { team.name.lowercase().replace("[^a-z0-9]".toRegex(), "_") }

    private companion object {
        const val SUPPORTED_SCHEMA_VERSION = 2
        const val MAX_REGULAR_SEASON_GAMES = 82
        const val MAX_FINANCE_UPGRADE_LEVEL = 5
        const val DEFAULT_MAX_TICKET_PRICE = 120
        const val ARENA_SEATS_PER_LEVEL = 2_000L
        const val OPERATIONS_DEBIT = 250_000L
        val SUPPORTED_POSITIONS = setOf("PG", "SG", "SF", "PF", "C")
        val SUPPORTED_BONUS_TYPES = setOf("ATTACK_BOOST", "DEFENSE_BOOST", "XP_BOOST", "MOTIVATION_BOOST")
        val PLAYER_REQUIRED_FIELDS = setOf(
            "id", "name", "position", "overall", "shooting", "defense", "rebound", "passing", "athleticism", "age"
        )
    }
}
