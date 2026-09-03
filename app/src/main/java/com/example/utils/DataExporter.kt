package com.example.utils

import android.content.Context
import com.example.data.repository.GameStateRepository
import com.example.domain.rules.SavedGameStartupRules
import com.example.models.*
import com.example.simulator.GameSimulator
import com.example.simulator.RotationRules
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.File
import java.io.FileWriter
import java.io.StringReader
import java.lang.reflect.Type
import kotlinx.coroutines.runBlocking

/** Import/export uses the Room-backed save instead of SharedPreferences. */
object DataExporter {
    private val gson: Gson = AutoSaveManager.gson

    fun exportData(context: Context, data: Map<String, Any>): String {
        val json = gson.toJson(data)
        val file = File(context.filesDir, "basket_data.json")
        FileWriter(file).use { it.write(json) }
        return file.absolutePath
    }

    fun exportCurrentGame(context: Context): String? = runBlocking {
        val snapshot = GameStateRepository(context).load() ?: return@runBlocking null
        val file = File(context.filesDir, "basket_game_save.json")
        file.writeText(gson.toJson(snapshot))
        file.absolutePath
    }

    /** Imports the native Room snapshot format produced by exportCurrentGame. */
    fun importGame(context: Context, filePath: String): Boolean = runBlocking {
        try {
            val file = File(filePath)
            if (!file.exists()) return@runBlocking false
            val rawSnapshot = file.readText()
            if (!hasRequiredSnapshotSettings(rawSnapshot)) return@runBlocking false
            val snapshot = gson.fromJson(rawSnapshot, GameStateRepository.GameStateSnapshot::class.java)
                ?: return@runBlocking false
            if (!SavedGameStartupRules.hasRequiredCore(snapshot)) return@runBlocking false
            if (!hasValidJsonPayloads(snapshot)) return@runBlocking false

            val repository = GameStateRepository(context)
            val team = repository.fromJson(snapshot.teamJson, NbaTeam::class.java)
                ?: return@runBlocking false
            val season = repository.fromJson(snapshot.seasonJson, Season::class.java)
                ?: return@runBlocking false
            val managedTeamId = persistenceTeamId(team)
            val seasonManagedTeam = season.teams.singleOrNull { persistenceTeamId(it) == managedTeamId }
                ?: return@runBlocking false
            val userTeamName = season.userTeamName ?: return@runBlocking false
            val referencedManagedTeam = season.teams.singleOrNull { it.name == userTeamName }
                ?: return@runBlocking false
            if (persistenceTeamId(referencedManagedTeam) != managedTeamId) return@runBlocking false
            // The native exporter duplicates the managed team in teamJson and seasonJson.
            // They must describe the same state or slot metadata can diverge from Room.
            if (seasonManagedTeam != team) return@runBlocking false
            val finance = snapshot.financeJson?.let { repository.fromJson(it, Finance::class.java) }

            repository.save(snapshot)
            SaveSlotManager.updateSlot(
                context = context,
                slotId = SaveSlotManager.getActiveSlot(context),
                team = seasonManagedTeam,
                season = season,
                finance = finance,
                difficulty = snapshot.difficulty
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun hasRequiredSnapshotSettings(rawSnapshot: String): Boolean = runCatching {
        val root = strictObject(rawSnapshot)
        check(hasRequiredFields(root, setOf("difficulty", "injuriesEnabled", "autoSubstitutionsEnabled")))
        val difficulty = root.get("difficulty")
        val injuries = root.get("injuriesEnabled")
        val autoSubs = root.get("autoSubstitutionsEnabled")
        check(difficulty.isJsonPrimitive && difficulty.asJsonPrimitive.isNumber)
        check(DifficultyLevel.entries.any { it.value == difficulty.asInt })
        check(injuries.isJsonPrimitive && injuries.asJsonPrimitive.isBoolean)
        check(autoSubs.isJsonPrimitive && autoSubs.asJsonPrimitive.isBoolean)
        true
    }.getOrDefault(false)

    private fun hasValidJsonPayloads(snapshot: GameStateRepository.GameStateSnapshot): Boolean {
        val listPlayerType = object : TypeToken<List<Player>>() {}.type
        val listContractType = object : TypeToken<List<PlayerContract>>() {}.type
        val listStaffType = object : TypeToken<List<StaffMember>>() {}.type
        val listNotificationType = object : TypeToken<List<AssistantCoachNotification>>() {}.type
        val listNewsType = object : TypeToken<List<News>>() {}.type

        val validPlayer: (Player) -> Boolean = { item ->
            runCatching {
                check(item.id > 0)
                check(item.name.isNotBlank())
                check(item.position.isNotBlank())
                check(item.overall >= 0)
                check(item.shooting >= 0)
                check(item.defense >= 0)
                check(item.rebound >= 0)
                check(item.passing >= 0)
                check(item.athleticism >= 0)
                check(item.age > 0)
                check(item.xp >= 0)
                check(item.trainings >= 0)
                check(item.injuryDays >= 0)
                check(item.careerPoints >= 0)
                check(item.careerRebounds >= 0)
                check(item.careerAssists >= 0)
                check(item.careerSteals >= 0)
                check(item.careerBlocks >= 0)
                check(item.careerGames >= 0)
                check(item.championships >= 0)
                check(item.mvps >= 0)
                check(item.seasonPoints >= 0)
                check(item.seasonRebounds >= 0)
                check(item.seasonAssists >= 0)
                check(item.seasonSteals >= 0)
                check(item.seasonBlocks >= 0)
                check(item.seasonGames >= 0)
                true
            }.getOrDefault(false)
        }
        val validPlayerList: (Any) -> Boolean = { value ->
            value is List<*> && value.all { it is Player && validPlayer(it) }
        }
        val validTeam: (NbaTeam) -> Boolean = { team ->
            runCatching {
                check(team.name.isNotBlank())
                check(team.city.isNotBlank())
                check(team.abbreviation.isNotBlank())
                check(team.conference == "East" || team.conference == "West")
                check(team.arena.name.isNotBlank())
                check(team.arena.city.isNotBlank())
                check(team.arena.capacity > 0)
                team.arena.opened
                check(team.players.size >= RotationRules.MIN_PLAYERS_FOR_GAME)
                check(team.players.all(validPlayer))
                true
            }.getOrDefault(false)
        }
        val validCoach: (Any) -> Boolean = { value ->
            value is Coach && runCatching {
                check(value.name.isNotBlank())
                value.offensiveSkill in 0..100 &&
                    value.defensiveSkill in 0..100 &&
                    value.motivationalSkill in 0..100 &&
                    value.salary >= 0 &&
                    value.contractYears >= 0
            }.getOrDefault(false)
        }
        val validFinance: (Any) -> Boolean = { value ->
            value is Finance && runCatching {
                value.budget // Negative cash is a valid debt state produced by normal gameplay.
                value.coachSalaryPaid
                val sponsorNames = value.sponsors.map { it.name }
                check(sponsorNames.size == sponsorNames.toSet().size)
                value.sponsors.forEach {
                    check(it.name.isNotBlank())
                    check(it.amountPerYear >= 0)
                    check(it.yearsRemaining >= 0)
                }
                value.expenses.forEach {
                    check(it.description.isNotBlank())
                    check(it.date.isNotBlank())
                    check(it.amount >= 0)
                }
                check(value.arenaSeatsLevel >= 1)
                check(value.medicalStaffLevel >= 1)
                check(value.scoutingLevel >= 1)
                true
            }.getOrDefault(false)
        }
        val validContracts: (Any) -> Boolean = { value ->
            value is List<*> && value.all {
                it is PlayerContract && it.playerId > 0 && it.salary >= 0 && it.yearsRemaining in 0..5
            }
        }
        val validPlayerStats: (GameSimulator.PlayerStats) -> Boolean = { stats ->
            stats.points >= 0 && stats.rebounds >= 0 && stats.assists >= 0 &&
                stats.steals >= 0 && stats.blocks >= 0 && stats.turnovers >= 0
        }
        val validGameResult: (GameSimulator.GameResult) -> Boolean = { result ->
            runCatching {
                check(validTeam(result.homeTeam))
                check(validTeam(result.awayTeam))
                check(persistenceTeamId(result.homeTeam) != persistenceTeamId(result.awayTeam))
                check(result.homeScore >= 0)
                check(result.awayScore >= 0)
                check(result.attendance >= 0)
                check(result.narration.isNotBlank())
                check(result.homeStats.keys.all(validPlayer))
                check(result.awayStats.keys.all(validPlayer))
                check(result.homeStats.values.all(validPlayerStats))
                check(result.awayStats.values.all(validPlayerStats))
                val homeIds = result.homeStats.keys.map { it.id }
                val awayIds = result.awayStats.keys.map { it.id }
                check(homeIds.size == homeIds.toSet().size)
                check(awayIds.size == awayIds.toSet().size)
                check(homeIds.toSet().intersect(awayIds.toSet()).isEmpty())
                val injuryIds = result.injuries.map { it.player.id }
                check(injuryIds.size == injuryIds.toSet().size)
                result.injuries.forEach {
                    check(validPlayer(it.player))
                    check(it.daysOut >= 0)
                }
                true
            }.getOrDefault(false)
        }
        val validSeason: (Any) -> Boolean = { value ->
            value is Season && runCatching {
                check(value.currentDay >= 0)
                check(value.gamesPlayed >= 0)
                check(value.seasonNumber >= 1)
                check(value.currentMonth in 1..12)
                check(value.currentYear > 0)
                check(value.nextPlayerId > 0)
                check(value.teams.all(validTeam))
                check(value.teams.count { it.conference == "East" } >= 8)
                check(value.teams.count { it.conference == "West" } >= 8)
                val teamNames = value.teams.map { it.name }
                check(teamNames.size == teamNames.toSet().size)
                val teamIds = value.teams.map(::persistenceTeamId)
                check(teamIds.size == teamIds.toSet().size)
                val playerIds = value.teams.flatMap { it.players }.map { it.id }
                check(playerIds.size == playerIds.toSet().size)
                check(value.standings.keys == teamNames.toSet())
                value.standings.values.forEach {
                    check(it.wins >= 0)
                    check(it.losses >= 0)
                    check(it.gamesPlayed >= 0)
                    check(it.totalPointsScored >= 0)
                    check(it.totalPointsConceded >= 0)
                }
                val teamsById = value.teams.associateBy(::persistenceTeamId)
                check(value.history.all { result ->
                    persistenceTeamId(result.homeTeam) in teamsById &&
                        persistenceTeamId(result.awayTeam) in teamsById &&
                        validGameResult(result)
                })
                true
            }.getOrDefault(false)
        }
        val validHistory: (Any) -> Boolean = { value ->
            value is HistoryManager && runCatching {
                val seasonNumbers = value.seasons.map { it.seasonNumber }
                seasonNumbers.size == seasonNumbers.toSet().size && value.seasons.all { item ->
                    val playerIds = item.playerStats.map { it.id }
                    item.seasonNumber >= 1 &&
                        item.champion.isNotBlank() &&
                        item.finalScore.isNotBlank() &&
                        item.topScorer.isNotBlank() &&
                        item.topScorerPoints.isFinite() && item.topScorerPoints >= 0.0 &&
                        item.teamWins.entries.all { (name, wins) -> name.isNotBlank() && wins >= 0 } &&
                        item.playerStats.all(validPlayer) &&
                        playerIds.size == playerIds.toSet().size
                }
            }.getOrDefault(false)
        }
        val validTactics: (Any) -> Boolean = { value ->
            value is Tactics && runCatching {
                value.style.name
                value.pace in 0..100 && value.defensivePressure in 0..100 && value.offensiveRebound in 0..100
            }.getOrDefault(false)
        }
        val validAwards: (Any) -> Boolean = { value ->
            value is Awards && runCatching {
                check(validPlayer(value.mvp)); check(validPlayer(value.defensivePlayer)); check(validPlayer(value.sixthMan))
                check(validPlayer(value.rookieOfYear)); check(validPlayer(value.mostImproved))
                check(value.coachOfYearName.isNotBlank()); check(value.coachOfYearTeam.isNotBlank())
                true
            }.getOrDefault(false)
        }
        val validStaffMember: (StaffMember) -> Boolean = { item ->
            runCatching {
                check(item.id > 0)
                check(item.name.isNotBlank())
                check(item.level in 0..100)
                check(item.salary >= 0)
                check(item.contractYears >= 0)
                check(item.specialty.isNotBlank())
                when (item) {
                    is HeadCoachStaff -> {
                        check(item.offensiveSkill in 0..100)
                        check(item.defensiveSkill in 0..100)
                        check(item.motivationalSkill in 0..100)
                        check(item.experience >= 0)
                        check(item.reputation in 0..100)
                        item.preferredStyle.name
                        check(item.playerDevelopment in 0..100)
                        check(item.gameManagement in 0..100)
                    }
                    is ExecutiveStaff -> check(item.roleTitle.isNotBlank())
                    else -> Unit
                }
                true
            }.getOrDefault(false)
        }
        val validStaffList: (Any) -> Boolean = { value ->
            value is List<*> && value.all { it is StaffMember && validStaffMember(it) }
        }
        val validTeamStaff: (Any) -> Boolean = { value ->
            value is TeamStaff && runCatching {
                value.assistants.size; value.executives.size
                value.headCoach?.let { check(validStaffMember(it)) }
                value.strengthCoach?.let { check(validStaffMember(it)) }
                value.scout?.let { check(validStaffMember(it)) }
                value.teamDoctor?.let { check(validStaffMember(it)) }
                check(value.assistants.all(validStaffMember)); check(value.executives.all(validStaffMember))
                true
            }.getOrDefault(false)
        }
        val validFacilities: (Any) -> Boolean = { value ->
            value is TeamFacilities && runCatching {
                listOf(value.arena, value.training, value.medical, value.scouting).forEach {
                    it.type.name
                    check(it.name.isNotBlank())
                    check(it.level in 1..it.maxLevel)
                    check(it.maxLevel >= 1)
                    check(it.baseUpgradeCost > 0)
                }
                true
            }.getOrDefault(false)
        }
        val validFinanceAdvanced: (Any) -> Boolean = { value ->
            value is FinanceAdvanced && runCatching {
                check(value.salaryCap > 0)
                check(value.luxuryTaxThreshold > 0)
                check(value.ticketPrice >= 0)
                check(value.ticketPriceMultiplier.isFinite() && value.ticketPriceMultiplier >= 0.0)
                check(value.fanSatisfaction in 0..100)
                check(value.ownerSatisfaction in 0..100)
                check(value.ownerObjective.isNotBlank())
                check(value.revenues.ticketRevenue >= 0)
                check(value.revenues.sponsorshipRevenue >= 0)
                check(value.revenues.merchandiseRevenue >= 0)
                check(value.revenues.broadcastingRevenue >= 0)
                check(value.revenues.playoffRevenue >= 0)
                check(value.expenses.playerSalaries >= 0)
                check(value.expenses.staffSalaries >= 0)
                check(value.expenses.facilityMaintenance >= 0)
                check(value.expenses.travelLogistics >= 0)
                check(value.expenses.operationalExpenses >= 0)
                check(value.expenses.luxuryTaxPaid >= 0)
                value.activeSponsorships.forEach {
                    check(it.brandName.isNotBlank())
                    check(it.type.isNotBlank())
                    check(it.annualAmount >= 0)
                    check(it.yearsRemaining >= 0)
                    check(it.goalBonus >= 0)
                }
                true
            }.getOrDefault(false)
        }
        val validNotificationList: (Any) -> Boolean = { value ->
            value is List<*> && value.all { item ->
                item is AssistantCoachNotification && runCatching {
                    check(item.id.isNotBlank())
                    check(item.gameDay >= 0)
                    check(item.seasonNumber >= 1)
                    check(item.opponentName.isNotBlank())
                    check(item.userScore >= 0)
                    check(item.opponentScore >= 0)
                    check(item.timestamp > 0)
                    check(item.coachName.isNotBlank())
                    check(item.coachRole.isNotBlank())
                    check(item.summary.isNotBlank())
                    check(item.tacticalAdvice.isNotBlank())
                    item.isWin; item.isRead; item.isBonusApplied
                    item.keyStrengths.all { it.isNotBlank() } && item.areasToImprove.all { it.isNotBlank() } &&
                        item.playerHighlights.all { it.isNotBlank() }
                }.getOrDefault(false)
            } && value.filterIsInstance<AssistantCoachNotification>().let { notifications ->
                notifications.size == value.size && notifications.map { it.id }.let { ids -> ids.size == ids.toSet().size }
            }
        }
        val validNewsList: (Any) -> Boolean = { value ->
            value is List<*> && value.all { item ->
                item is News && runCatching {
                    item.title.isNotBlank() && item.content.isNotBlank() && item.dateString.isNotBlank() && item.type in NewsType.entries
                }.getOrDefault(false)
            }
        }
        val validMatchBoxScore: (Any) -> Boolean = { value ->
            value is MatchBoxScore && runCatching {
                check(value.matchId.isNotBlank())
                check(value.dateString.isNotBlank())
                check(value.homeTeamName.isNotBlank())
                check(value.awayTeamName.isNotBlank())
                check(value.homeScore >= 0 && value.awayScore >= 0)
                check(value.homeQuarterScores.all { it >= 0 })
                check(value.awayQuarterScores.all { it >= 0 })
                value.homePlayers.forEach { check(validPlayerBoxScore(it)) }
                value.awayPlayers.forEach { check(validPlayerBoxScore(it)) }
                check(validTeamBoxScore(value.homeTeamTotals))
                check(validTeamBoxScore(value.awayTeamTotals))
                true
            }.getOrDefault(false)
        }
        val validPlayoffResult: (Any) -> Boolean = { value ->
            value is Season.PlayoffResult && runCatching {
                check(validTeam(value.eastChampion))
                check(validTeam(value.westChampion))
                check(validTeam(value.nbaChampion))
                value.seriesResults.forEach {
                    check(validTeam(it.winner))
                    check(it.roundName.isNotBlank())
                    check(it.team1Wins >= 0 && it.team2Wins >= 0)
                    it.team1?.let { team -> check(validTeam(team)) }
                    it.team2?.let { team -> check(validTeam(team)) }
                    it.mvp?.let { player -> check(validPlayer(player)) }
                    check(it.games.all(validGameResult))
                }
                value.mvp?.let { check(validPlayer(it)) }
                true
            }.getOrDefault(false)
        }

        val requiredPlayerFields = setOf(
            "id", "name", "position", "overall", "shooting", "defense", "rebound", "passing", "athleticism", "age",
            "xp", "trainings", "injured", "injuryDays", "careerPoints", "careerRebounds", "careerAssists", "careerSteals",
            "careerBlocks", "careerGames", "championships", "mvps", "seasonPoints", "seasonRebounds", "seasonAssists",
            "seasonSteals", "seasonBlocks", "seasonGames"
        )
        val requiredCoachFields = setOf("id", "name", "offensiveSkill", "defensiveSkill", "motivationalSkill", "salary", "contractYears")
        val requiredFinanceFields = setOf("budget", "sponsors", "expenses", "coachSalaryPaid", "arenaSeatsLevel", "medicalStaffLevel", "scoutingLevel")
        val requiredContractFields = setOf("playerId", "salary", "yearsRemaining", "playerOption", "noTrade")
        val requiredArenaFields = setOf("name", "city", "capacity", "opened")
        val requiredSeasonFields = setOf("teams", "currentDay", "gamesPlayed", "seasonNumber", "currentMonth", "currentYear", "nextPlayerId", "standings", "history")
        val requiredGameFields = setOf("homeTeam", "awayTeam", "homeScore", "awayScore", "attendance", "homeStats", "awayStats", "injuries", "narration")
        val requiredPlayerStatFields = setOf("points", "rebounds", "assists", "steals", "blocks", "turnovers", "plusMinus")
        val requiredHistoryFields = setOf("seasonNumber", "champion", "finalScore", "topScorer", "topScorerPoints", "teamWins", "playerStats")

        return snapshot.coachJson != null && snapshot.financeJson != null && snapshot.tacticsJson != null &&
            snapshot.historyJson != null && snapshot.contractsJson != null &&
            snapshot.freeAgentsJson != null && snapshot.draftRookiesJson != null &&
            validPayload(snapshot.teamJson, NbaTeam::class.java, JsonToken.BEGIN_OBJECT) { it is NbaTeam && validTeam(it) } &&
            hasRequiredTeamPlayerFields(snapshot.teamJson, requiredPlayerFields) &&
            hasRequiredTeamArenaFields(snapshot.teamJson, requiredArenaFields) &&
            validPayload(snapshot.coachJson, Coach::class.java, JsonToken.BEGIN_OBJECT, validCoach) &&
            hasRequiredObjectFields(snapshot.coachJson, requiredCoachFields) &&
            validPayload(snapshot.financeJson, Finance::class.java, JsonToken.BEGIN_OBJECT, validFinance) &&
            hasRequiredObjectFields(snapshot.financeJson, requiredFinanceFields) &&
            validPayload(snapshot.tacticsJson, Tactics::class.java, JsonToken.BEGIN_OBJECT, validTactics) &&
            hasRequiredObjectFields(snapshot.tacticsJson, setOf("style", "pace", "defensivePressure", "offensiveRebound")) &&
            validPayload(snapshot.seasonJson, Season::class.java, JsonToken.BEGIN_OBJECT, validSeason) &&
            hasRequiredObjectFields(snapshot.seasonJson, requiredSeasonFields) &&
            hasRequiredSeasonPlayerFields(snapshot.seasonJson, requiredPlayerFields) &&
            hasRequiredSeasonTeamArenaFields(snapshot.seasonJson, requiredArenaFields) &&
            hasRequiredSeasonStandingsFields(snapshot.seasonJson) &&
            hasRequiredSeasonGameHistoryFields(snapshot.seasonJson, requiredGameFields, requiredPlayerStatFields) &&
            hasValidSeasonHistoryReferences(snapshot) &&
            validPayload(snapshot.historyJson, HistoryManager::class.java, JsonToken.BEGIN_OBJECT, validHistory) &&
            hasRequiredHistoryFields(snapshot.historyJson, requiredHistoryFields, requiredPlayerFields) &&
            validPayload(snapshot.awardsJson, Awards::class.java, JsonToken.BEGIN_OBJECT, validAwards) &&
            hasRequiredAwardsPlayerFields(snapshot.awardsJson, requiredPlayerFields) &&
            validPayload(snapshot.startingFiveJson, listPlayerType, JsonToken.BEGIN_ARRAY, validPlayerList) &&
            hasRequiredArrayObjectFields(snapshot.startingFiveJson, requiredPlayerFields) &&
            hasValidStartingFive(snapshot) &&
            validPayload(snapshot.freeAgentsJson, listPlayerType, JsonToken.BEGIN_ARRAY, validPlayerList) &&
            hasRequiredArrayObjectFields(snapshot.freeAgentsJson, requiredPlayerFields) &&
            validPayload(snapshot.draftRookiesJson, listPlayerType, JsonToken.BEGIN_ARRAY, validPlayerList) &&
            hasRequiredArrayObjectFields(snapshot.draftRookiesJson, requiredPlayerFields) &&
            hasDisjointPersistencePlayerIds(snapshot) &&
            validPayload(snapshot.contractsJson, listContractType, JsonToken.BEGIN_ARRAY, validContracts) &&
            hasRequiredArrayObjectFields(snapshot.contractsJson, requiredContractFields) &&
            hasValidContractFlags(snapshot.contractsJson) &&
            hasCompleteRosterContractCoverage(snapshot) &&
            validPayload(snapshot.staffMarketJson, listStaffType, JsonToken.BEGIN_ARRAY, validStaffList) &&
            hasRequiredStaffMarketFields(snapshot.staffMarketJson) &&
            validPayload(snapshot.notificationsJson, listNotificationType, JsonToken.BEGIN_ARRAY, validNotificationList) &&
            hasRequiredNotificationFields(snapshot.notificationsJson) &&
            validPayload(snapshot.teamStaffJson, TeamStaff::class.java, JsonToken.BEGIN_OBJECT, validTeamStaff) &&
            hasRequiredTeamStaffFields(snapshot.teamStaffJson) &&
            validPayload(snapshot.facilitiesJson, TeamFacilities::class.java, JsonToken.BEGIN_OBJECT, validFacilities) &&
            hasRequiredFacilityFields(snapshot.facilitiesJson) &&
            validPayload(snapshot.financeAdvancedJson, FinanceAdvanced::class.java, JsonToken.BEGIN_OBJECT, validFinanceAdvanced) &&
            hasRequiredFinanceAdvancedFields(snapshot.financeAdvancedJson) &&
            validPayload(snapshot.newsFeedJson, listNewsType, JsonToken.BEGIN_ARRAY, validNewsList) &&
            validPayload(snapshot.latestBoxScoreJson, MatchBoxScore::class.java, JsonToken.BEGIN_OBJECT, validMatchBoxScore) &&
            hasRequiredBoxScoreFields(snapshot.latestBoxScoreJson) &&
            validPayload(snapshot.playoffResultJson, Season.PlayoffResult::class.java, JsonToken.BEGIN_OBJECT, validPlayoffResult) &&
            hasRequiredPlayoffGameFields(snapshot.playoffResultJson, requiredGameFields, requiredPlayerStatFields)
    }

    private fun validPlayerBoxScore(value: PlayerBoxScore): Boolean = runCatching {
        value.playerId > 0 && value.playerName.isNotBlank() && value.position.isNotBlank() &&
            value.minutesPlayed >= 0 && value.points >= 0 && value.rebounds >= 0 &&
            value.offensiveRebounds >= 0 && value.defensiveRebounds >= 0 && value.assists >= 0 &&
            value.steals >= 0 && value.blocks >= 0 && value.turnovers >= 0 && value.fouls >= 0 &&
            value.fgMade >= 0 && value.fgAttempted >= value.fgMade &&
            value.threeMade >= 0 && value.threeAttempted >= value.threeMade &&
            value.ftMade >= 0 && value.ftAttempted >= value.ftMade
    }.getOrDefault(false)

    private fun validTeamBoxScore(value: TeamBoxScore): Boolean = runCatching {
        value.teamName.isNotBlank() && value.points >= 0 && value.rebounds >= 0 && value.assists >= 0 &&
            value.steals >= 0 && value.blocks >= 0 && value.turnovers >= 0 && value.fouls >= 0 &&
            value.fgMade >= 0 && value.fgAttempted >= value.fgMade &&
            value.threeMade >= 0 && value.threeAttempted >= value.threeMade &&
            value.ftMade >= 0 && value.ftAttempted >= value.ftMade
    }.getOrDefault(false)

    private fun hasValidStartingFive(snapshot: GameStateRepository.GameStateSnapshot): Boolean = runCatching {
        val payload = snapshot.startingFiveJson ?: return@runCatching true
        val type = object : TypeToken<List<Player>>() {}.type
        val startingFive = gson.fromJson<List<Player>>(payload, type) ?: return@runCatching false
        val team = gson.fromJson(snapshot.teamJson, NbaTeam::class.java) ?: return@runCatching false
        val season = gson.fromJson(snapshot.seasonJson, Season::class.java) ?: return@runCatching false
        val managedTeam = season.teams.singleOrNull { persistenceTeamId(it) == persistenceTeamId(team) }
            ?: return@runCatching false
        val ids = startingFive.map { it.id }
        ids.size == ids.toSet().size && ids.all { id -> managedTeam.players.any { it.id == id } }
    }.getOrDefault(false)

    private fun hasDisjointPersistencePlayerIds(snapshot: GameStateRepository.GameStateSnapshot): Boolean = runCatching {
        val season = gson.fromJson(snapshot.seasonJson, Season::class.java) ?: return@runCatching false
        val type = object : TypeToken<List<Player>>() {}.type
        val freeAgents = snapshot.freeAgentsJson?.let { gson.fromJson<List<Player>>(it, type) } ?: return@runCatching false
        val draftRookies = snapshot.draftRookiesJson?.let { gson.fromJson<List<Player>>(it, type) } ?: return@runCatching false
        val ids = season.teams.flatMap { it.players }.map { it.id } + freeAgents.map { it.id } + draftRookies.map { it.id }
        ids.size == ids.toSet().size
    }.getOrDefault(false)

    private fun hasCompleteRosterContractCoverage(snapshot: GameStateRepository.GameStateSnapshot): Boolean = runCatching {
        val season = gson.fromJson(snapshot.seasonJson, Season::class.java) ?: return@runCatching false
        val type = object : TypeToken<List<PlayerContract>>() {}.type
        val contracts = snapshot.contractsJson?.let { gson.fromJson<List<PlayerContract>>(it, type) }
            ?: return@runCatching false
        val rosterIds = season.teams.flatMap { it.players }.map { it.id }.toSet()
        val contractIds = contracts.map { it.playerId }
        contractIds.size == rosterIds.size && contractIds.size == contractIds.toSet().size && contractIds.toSet() == rosterIds
    }.getOrDefault(false)

    private fun hasValidSeasonHistoryReferences(snapshot: GameStateRepository.GameStateSnapshot): Boolean = runCatching {
        val season = gson.fromJson(snapshot.seasonJson, Season::class.java) ?: return@runCatching false
        val playerType = object : TypeToken<List<Player>>() {}.type
        val freeAgents = snapshot.freeAgentsJson?.let { gson.fromJson<List<Player>>(it, playerType) } ?: return@runCatching false
        val draftRookies = snapshot.draftRookiesJson?.let { gson.fromJson<List<Player>>(it, playerType) } ?: return@runCatching false
        val awards = snapshot.awardsJson?.let { gson.fromJson(it, Awards::class.java) }
        val canonicalPlayers = LinkedHashMap<Int, Player>()
        season.teams.flatMap { it.players }.forEach { canonicalPlayers[it.id] = it }
        freeAgents.forEach { canonicalPlayers[it.id] = it }
        draftRookies.forEach { canonicalPlayers[it.id] = it }
        awards?.let {
            listOf(it.mvp, it.defensivePlayer, it.sixthMan, it.rookieOfYear, it.mostImproved)
                .forEach { player -> canonicalPlayers.putIfAbsent(player.id, player) }
        }

        fun validateHistoricalPlayer(player: Player): Boolean {
            val canonical = canonicalPlayers[player.id]
            return if (canonical == null) {
                canonicalPlayers[player.id] = player
                true
            } else {
                player == canonical
            }
        }

        season.history.all { result ->
            val homeIds = result.homeStats.keys.map { it.id }
            val awayIds = result.awayStats.keys.map { it.id }
            homeIds.toSet().intersect(awayIds.toSet()).isEmpty() &&
                result.homeStats.keys.all(::validateHistoricalPlayer) &&
                result.awayStats.keys.all(::validateHistoricalPlayer) &&
                result.injuries.all { validateHistoricalPlayer(it.player) }
        }
    }.getOrDefault(false)

    private fun hasRequiredSeasonGameHistoryFields(
        payload: String?,
        requiredFields: Set<String>,
        requiredPlayerStatFields: Set<String>
    ): Boolean {
        if (payload == null) return true
        return runCatching {
            val history = strictObject(payload).getAsJsonArray("history") ?: return@runCatching false
            history.all { element ->
                element.isJsonObject && hasRequiredFields(element.asJsonObject, requiredFields) &&
                    hasRequiredGameStatFields(element.asJsonObject, requiredPlayerStatFields)
            }
        }.getOrDefault(false)
    }

    private fun hasRequiredGameStatFields(game: JsonObject, requiredPlayerStatFields: Set<String>): Boolean =
        listOf("homeStats", "awayStats").all { field ->
            hasRequiredComplexMapValueFields(game.get(field), requiredPlayerStatFields)
        }

    private fun hasRequiredComplexMapValueFields(element: JsonElement?, requiredFields: Set<String>): Boolean {
        if (element == null || element.isJsonNull) return false
        return when {
            element.isJsonArray -> element.asJsonArray.all { entry ->
                entry.isJsonArray && entry.asJsonArray.size() == 2 &&
                    entry.asJsonArray[1].isJsonObject && hasRequiredFields(entry.asJsonArray[1].asJsonObject, requiredFields)
            }
            element.isJsonObject -> element.asJsonObject.entrySet().all { (_, value) ->
                value.isJsonObject && hasRequiredFields(value.asJsonObject, requiredFields)
            }
            else -> false
        }
    }

    private fun hasRequiredSeasonStandingsFields(payload: String?): Boolean {
        if (payload == null) return true
        return runCatching {
            val standings = strictObject(payload).getAsJsonObject("standings") ?: return@runCatching false
            val fields = setOf("wins", "losses", "gamesPlayed", "totalPointsScored", "totalPointsConceded")
            standings.entrySet().all { (_, element) ->
                element.isJsonObject && hasRequiredFields(element.asJsonObject, fields) &&
                    fields.all { field ->
                        val raw = element.asJsonObject.get(field)
                        raw.isJsonPrimitive && raw.asJsonPrimitive.isNumber && raw.asInt >= 0
                    }
            }
        }.getOrDefault(false)
    }

    private fun hasRequiredHistoryFields(payload: String?, requiredFields: Set<String>, playerFields: Set<String>): Boolean {
        if (payload == null) return true
        return runCatching {
            val history = strictObject(payload)
            val seasons = history.getAsJsonArray("seasons") ?: return@runCatching false
            seasons.all { element ->
                if (!element.isJsonObject) return@all false
                val item = element.asJsonObject
                if (!hasRequiredFields(item, requiredFields)) return@all false
                val players = item.getAsJsonArray("playerStats") ?: return@all false
                hasRequiredFields(players, playerFields)
            }
        }.getOrDefault(false)
    }

    private fun hasRequiredStaffMarketFields(payload: String?): Boolean {
        if (payload == null) return true
        return runCatching {
            val array = gson.fromJson(payload, JsonArray::class.java) ?: return@runCatching false
            array.all { it.isJsonObject && hasRequiredStaffObjectFields(it.asJsonObject) }
        }.getOrDefault(false)
    }

    private fun hasRequiredTeamStaffFields(payload: String?): Boolean {
        if (payload == null) return true
        return runCatching {
            val root = strictObject(payload)
            check(hasRequiredFields(root, setOf("assistants", "executives")))
            val assistants = root.getAsJsonArray("assistants") ?: return@runCatching false
            val executives = root.getAsJsonArray("executives") ?: return@runCatching false
            check(assistants.all { it.isJsonObject && hasRequiredStaffObjectFields(it.asJsonObject) })
            check(executives.all { it.isJsonObject && hasRequiredStaffObjectFields(it.asJsonObject, executive = true))
            root.get("headCoach")?.takeUnless { it.isJsonNull }?.let {
                check(it.isJsonObject && hasRequiredStaffObjectFields(it.asJsonObject, headCoach = true))
            }
            listOf("strengthCoach", "scout", "teamDoctor").forEach { field ->
                root.get(field)?.takeUnless { it.isJsonNull }?.let {
                    check(it.isJsonObject && hasRequiredStaffObjectFields(it.asJsonObject))
                }
            }
            true
        }.getOrDefault(false)
    }

    private fun hasRequiredStaffObjectFields(obj: JsonObject, headCoach: Boolean = false, executive: Boolean = false): Boolean {
        val common = setOf("id", "name", "level", "salary", "contractYears", "specialty")
        if (!hasRequiredFields(obj, common)) return false
        val rawLevel = obj.get("level")
        val rawSalary = obj.get("salary")
        val rawYears = obj.get("contractYears")
        if (!rawLevel.isJsonPrimitive || !rawLevel.asJsonPrimitive.isNumber || rawLevel.asInt !in 0..100) return false
        if (!rawSalary.isJsonPrimitive || !rawSalary.asJsonPrimitive.isNumber || rawSalary.asInt < 0) return false
        if (!rawYears.isJsonPrimitive || !rawYears.asJsonPrimitive.isNumber || rawYears.asInt < 0) return false
        val isHead = headCoach || obj.has("offensiveSkill") || obj.has("defensiveSkill") || obj.has("gameManagement") || obj.get("_staffType")?.asString == "HEAD_COACH"
        val isExecutive = executive || obj.has("roleTitle") || obj.get("_staffType")?.asString == "EXECUTIVE"
        if (isHead && !hasRequiredFields(obj, setOf("offensiveSkill", "defensiveSkill", "motivationalSkill", "experience", "reputation", "preferredStyle", "playerDevelopment", "gameManagement"))) return false
        if (isExecutive && !hasRequiredFields(obj, setOf("roleTitle"))) return false
        return true
    }

    private fun hasRequiredNotificationFields(payload: String?): Boolean {
        if (payload == null) return true
        val required = setOf(
            "id", "gameDay", "seasonNumber", "opponentName", "isWin", "userScore", "opponentScore", "timestamp", "isRead",
            "coachName", "coachRole", "summary", "keyStrengths", "areasToImprove", "playerHighlights", "tacticalAdvice", "isBonusApplied"
        )
        return runCatching {
            val array = gson.fromJson(payload, JsonArray::class.java) ?: return@runCatching false
            val ids = mutableListOf<String>()
            check(array.all { element ->
                if (!element.isJsonObject) return@all false
                val obj = element.asJsonObject
                if (!hasRequiredFields(obj, required)) return@all false
                val id = obj.get("id")
                if (!id.isJsonPrimitive || !id.asJsonPrimitive.isString || id.asString.isBlank()) return@all false
                ids += id.asString
                obj.get("gameDay").asInt >= 0 && obj.get("seasonNumber").asInt >= 1 &&
                    obj.get("userScore").asInt >= 0 && obj.get("opponentScore").asInt >= 0 && obj.get("timestamp").asLong > 0 &&
                    obj.get("isWin").asJsonPrimitive.isBoolean && obj.get("isRead").asJsonPrimitive.isBoolean &&
                    obj.get("isBonusApplied").asJsonPrimitive.isBoolean
            })
            ids.size == ids.toSet().size
        }.getOrDefault(false)
    }

    private fun hasValidContractFlags(payload: String?): Boolean {
        if (payload == null) return true
        return runCatching {
            val array = gson.fromJson(payload, JsonArray::class.java) ?: return@runCatching false
            array.all { element ->
                if (!element.isJsonObject) return@all false
                val obj = element.asJsonObject
                listOf("playerOption", "noTrade").all { field ->
                    val raw = obj.get(field)
                    raw != null && raw.isJsonPrimitive && raw.asJsonPrimitive.isBoolean
                }
            }
        }.getOrDefault(false)
    }

    private fun hasRequiredFacilityFields(payload: String?): Boolean {
        if (payload == null) return true
        return runCatching {
            val root = strictObject(payload)
            listOf("arena", "training", "medical", "scouting").all { field ->
                val facility = root.getAsJsonObject(field) ?: return@all false
                if (!hasRequiredFields(facility, setOf("type", "name", "level", "maxLevel", "baseUpgradeCost"))) return@all false
                val level = facility.get("level").asInt
                val maxLevel = facility.get("maxLevel").asInt
                level >= 1 && maxLevel >= 1 && level <= maxLevel && facility.get("baseUpgradeCost").asInt > 0
            }
        }.getOrDefault(false)
    }

    private fun hasRequiredFinanceAdvancedFields(payload: String?): Boolean {
        if (payload == null) return true
        return runCatching {
            val root = strictObject(payload)
            check(hasRequiredFields(root, setOf("salaryCap", "luxuryTaxThreshold", "ticketPrice", "ticketPriceMultiplier", "fanSatisfaction", "ownerSatisfaction", "ownerObjective", "revenues", "expenses", "activeSponsorships")))
            check(root.get("salaryCap").asInt > 0)
            check(root.get("luxuryTaxThreshold").asInt > 0)
            check(root.get("ticketPrice").asInt >= 0)
            check(root.get("ticketPriceMultiplier").asDouble >= 0.0)
            check(root.get("fanSatisfaction").asInt in 0..100)
            check(root.get("ownerSatisfaction").asInt in 0..100)
            val revenues = root.getAsJsonObject("revenues") ?: return@runCatching false
            check(hasRequiredFields(revenues, setOf("ticketRevenue", "sponsorshipRevenue", "merchandiseRevenue", "broadcastingRevenue", "playoffRevenue")))
            val expenses = root.getAsJsonObject("expenses") ?: return@runCatching false
            check(hasRequiredFields(expenses, setOf("playerSalaries", "staffSalaries", "facilityMaintenance", "travelLogistics", "operationalExpenses", "luxuryTaxPaid")))
            val sponsorships = root.getAsJsonArray("activeSponsorships") ?: return@runCatching false
            check(sponsorships.all { element ->
                element.isJsonObject && hasRequiredFields(element.asJsonObject, setOf("brandName", "type", "annualAmount", "yearsRemaining", "goalBonus"))
            })
            true
        }.getOrDefault(false)
    }

    private fun hasRequiredBoxScoreFields(payload: String?): Boolean {
        if (payload == null) return true
        val matchFields = setOf("matchId", "dateString", "homeTeamName", "awayTeamName", "homeScore", "awayScore", "homeQuarterScores", "awayQuarterScores", "homePlayers", "awayPlayers", "homeTeamTotals", "awayTeamTotals")
        val playerFields = setOf("playerId", "playerName", "position", "minutesPlayed", "points", "rebounds", "offensiveRebounds", "defensiveRebounds", "assists", "steals", "blocks", "turnovers", "fouls", "fgMade", "fgAttempted", "threeMade", "threeAttempted", "ftMade", "ftAttempted", "plusMinus")
        val teamFields = setOf("teamName", "points", "rebounds", "assists", "steals", "blocks", "turnovers", "fouls", "fgMade", "fgAttempted", "threeMade", "threeAttempted", "ftMade", "ftAttempted")
        return runCatching {
            val root = strictObject(payload)
            check(hasRequiredFields(root, matchFields))
            check(root.getAsJsonArray("homePlayers")?.let { hasRequiredFields(it, playerFields) } == true)
            check(root.getAsJsonArray("awayPlayers")?.let { hasRequiredFields(it, playerFields) } == true)
            check(root.getAsJsonObject("homeTeamTotals")?.let { hasRequiredFields(it, teamFields) } == true)
            check(root.getAsJsonObject("awayTeamTotals")?.let { hasRequiredFields(it, teamFields) } == true)
            true
        }.getOrDefault(false)
    }

    private fun hasRequiredPlayoffGameFields(
        payload: String?,
        requiredGameFields: Set<String>,
        requiredPlayerStatFields: Set<String>
    ): Boolean {
        if (payload == null) return true
        return runCatching {
            val root = strictObject(payload)
            val series = root.getAsJsonArray("seriesResults") ?: return@runCatching false
            series.all { seriesElement ->
                if (!seriesElement.isJsonObject) return@all false
                val seriesObject = seriesElement.asJsonObject
                if (!hasRequiredFields(seriesObject, setOf("winner", "games", "roundName", "team1Wins", "team2Wins"))) return@all false
                val games = seriesObject.getAsJsonArray("games") ?: return@all false
                games.all { gameElement ->
                    gameElement.isJsonObject && hasRequiredFields(gameElement.asJsonObject, requiredGameFields) &&
                        hasRequiredGameStatFields(gameElement.asJsonObject, requiredPlayerStatFields)
                }
            }
        }.getOrDefault(false)
    }

    private fun hasRequiredObjectFields(payload: String?, requiredFields: Set<String>): Boolean {
        if (payload == null) return true
        return runCatching { hasRequiredFields(strictObject(payload), requiredFields) }.getOrDefault(false)
    }

    private fun hasRequiredArrayObjectFields(payload: String?, requiredFields: Set<String>): Boolean {
        if (payload == null) return true
        return runCatching {
            val array = gson.fromJson(payload, JsonArray::class.java) ?: return@runCatching false
            hasRequiredFields(array, requiredFields)
        }.getOrDefault(false)
    }

    private fun hasRequiredTeamPlayerFields(payload: String?, requiredFields: Set<String>): Boolean {
        if (payload == null) return true
        return runCatching {
            val team = strictObject(payload)
            val players = team.getAsJsonArray("players") ?: return@runCatching false
            hasRequiredFields(players, requiredFields)
        }.getOrDefault(false)
    }

    private fun hasRequiredTeamArenaFields(payload: String?, requiredFields: Set<String>): Boolean {
        if (payload == null) return true
        return runCatching {
            val arena = strictObject(payload).getAsJsonObject("arena") ?: return@runCatching false
            hasRequiredFields(arena, requiredFields)
        }.getOrDefault(false)
    }

    private fun hasRequiredSeasonPlayerFields(payload: String?, requiredFields: Set<String>): Boolean {
        if (payload == null) return true
        return runCatching {
            val teams = strictObject(payload).getAsJsonArray("teams") ?: return@runCatching false
            teams.all {
                it.isJsonObject && it.asJsonObject.getAsJsonArray("players")?.let { players -> hasRequiredFields(players, requiredFields) } == true
            }
        }.getOrDefault(false)
    }

    private fun hasRequiredSeasonTeamArenaFields(payload: String?, requiredFields: Set<String>): Boolean {
        if (payload == null) return true
        return runCatching {
            val teams = strictObject(payload).getAsJsonArray("teams") ?: return@runCatching false
            teams.all {
                it.isJsonObject && it.asJsonObject.getAsJsonObject("arena")?.let { arena -> hasRequiredFields(arena, requiredFields) } == true
            }
        }.getOrDefault(false)
    }

    private fun hasRequiredAwardsPlayerFields(payload: String?, requiredFields: Set<String>): Boolean {
        if (payload == null) return true
        return runCatching {
            val awards = strictObject(payload)
            listOf("mvp", "defensivePlayer", "sixthMan", "rookieOfYear", "mostImproved").all { field ->
                awards.getAsJsonObject(field)?.let { hasRequiredFields(it, requiredFields) } == true
            }
        }.getOrDefault(false)
    }

    private fun hasRequiredFields(arrayPayload: JsonArray, requiredFields: Set<String>): Boolean =
        arrayPayload.all { it.isJsonObject && hasRequiredFields(it.asJsonObject, requiredFields) }

    private fun hasRequiredFields(objectPayload: JsonObject, requiredFields: Set<String>): Boolean =
        requiredFields.all { objectPayload.has(it) && !objectPayload.get(it).isJsonNull }

    private fun strictObject(payload: String): JsonObject {
        JsonReader(StringReader(payload)).use { reader ->
            reader.isLenient = false
            check(reader.peek() == JsonToken.BEGIN_OBJECT)
            reader.skipValue()
            check(reader.peek() == JsonToken.END_DOCUMENT)
        }
        return gson.fromJson(payload, JsonObject::class.java) ?: error("JSON object expected")
    }

    private fun persistenceTeamId(team: NbaTeam): String =
        team.abbreviation.ifBlank { team.name.lowercase().replace("[^a-z0-9]".toRegex(), "_") }

    private fun validPayload(payload: String?, type: Type, expectedRoot: JsonToken, validateDecoded: (Any) -> Boolean = { true }): Boolean {
        if (payload == null) return true
        return runCatching {
            JsonReader(StringReader(payload)).use { reader ->
                reader.isLenient = false
                check(reader.peek() == expectedRoot)
                reader.skipValue()
                check(reader.peek() == JsonToken.END_DOCUMENT)
            }
            val decoded = gson.fromJson<Any>(payload, type) ?: error("JSON payload did not match expected type")
            check(validateDecoded(decoded))
        }.isSuccess
    }

    @Deprecated("Use exportCurrentGame/importGame. Generic preference import was removed with the Room migration.")
    fun importData(context: Context, filePath: String): Boolean = importGame(context, filePath)
}
