package com.example.utils

import com.example.data.repository.GameStateRepository
import com.example.models.Awards
import com.example.models.HistoryManager
import com.example.models.Player
import com.example.models.PlayerContract
import com.example.models.Season
import com.example.models.StaffMember
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
 * Additional snapshot-level import invariants that require either raw JSON type checks or
 * cross-payload identity reconciliation. The regular persistence Gson remains unchanged; this
 * factory is registered only by [AutoSaveManager.gson], which is also the import/export codec.
 */
class ImportSnapshotValidationFactory : TypeAdapterFactory {
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
        requireEmbeddedPayload(root, "startingFiveJson")
        validateRawPlayerBooleanFields(root)

        embeddedObject(root, "financeJson")?.let { finance ->
            val coachSalaryPaid = finance.get("coachSalaryPaid")
            requireJsonBoolean(coachSalaryPaid, "finance.coachSalaryPaid")

            val sponsors = finance.getAsJsonArray("sponsors")
                ?: throw JsonParseException("finance.sponsors must be an array")
            sponsors.forEachIndexed { index, element ->
                val sponsor = element.asObject("finance.sponsors[$index]")
                requireJsonString(sponsor.get("name"), "finance.sponsors[$index].name")
                requireJsonNumber(sponsor.get("amountPerYear"), "finance.sponsors[$index].amountPerYear")
                requireJsonNumber(sponsor.get("yearsRemaining"), "finance.sponsors[$index].yearsRemaining")
            }

            val expenses = finance.getAsJsonArray("expenses")
                ?: throw JsonParseException("finance.expenses must be an array")
            val expenseKeys = mutableSetOf<Pair<String, String>>()
            expenses.forEachIndexed { index, element ->
                val expense = element.asObject("finance.expenses[$index]")
                val description = requireJsonString(expense.get("description"), "finance.expenses[$index].description")
                requireJsonNumber(expense.get("amount"), "finance.expenses[$index].amount")
                val date = requireJsonString(expense.get("date"), "finance.expenses[$index].date")
                if (!expenseKeys.add(description to date)) {
                    throw JsonParseException("Duplicate finance expense identity: $description / $date")
                }
            }
        }

        embeddedArray(root, "staffMarketJson")?.let { market ->
            val ids = mutableSetOf<Int>()
            market.forEachIndexed { index, element ->
                val staff = element.asObject("staffMarket[$index]")
                val id = requireJsonNumber(staff.get("id"), "staffMarket[$index].id").asInt
                if (!ids.add(id)) throw JsonParseException("Duplicate staff-market id: $id")
            }
        }

        embeddedArray(root, "newsFeedJson")?.forEachIndexed { index, element ->
            val news = element.asObject("newsFeed[$index]")
            requireJsonBoolean(news.get("isRead"), "newsFeed[$index].isRead")
        }

        embeddedArray(root, "contractsJson")?.forEachIndexed { index, element ->
            val contract = element.asObject("contracts[$index]")
            requireJsonString(contract.get("teamId"), "contracts[$index].teamId")
        }
    }

    private fun validateDecodedSnapshot(snapshot: GameStateRepository.GameStateSnapshot, gson: Gson) {
        val season = snapshot.seasonJson?.let { gson.fromJson(it, Season::class.java) }
            ?: throw JsonParseException("seasonJson is required")
        validateSeasonAllocator(season)
        validateSeasonGameCounter(season)

        val playerType = object : TypeToken<List<Player>>() {}.type
        val freeAgents: List<Player> = snapshot.freeAgentsJson?.let { gson.fromJson(it, playerType) }
            ?: throw JsonParseException("freeAgentsJson is required")
        val draftRookies: List<Player> = snapshot.draftRookiesJson?.let { gson.fromJson(it, playerType) }
            ?: throw JsonParseException("draftRookiesJson is required")

        val canonicalPlayers = LinkedHashMap<Int, Player>()
        val rosterOwnerByPlayerId = mutableMapOf<Int, String>()
        var maxPersistedPlayerId = 0
        fun recordPlayer(player: Player) {
            validatePlayerBounds(player)
            maxPersistedPlayerId = maxOf(maxPersistedPlayerId, player.id)
        }

        season.teams.forEach { team ->
            val teamId = persistenceTeamId(team)
            team.players.forEach { player ->
                recordPlayer(player)
                canonicalPlayers[player.id] = player
                rosterOwnerByPlayerId[player.id] = teamId
            }
        }
        freeAgents.forEach { player ->
            recordPlayer(player)
            canonicalPlayers[player.id] = player
        }
        draftRookies.forEach { player ->
            recordPlayer(player)
            canonicalPlayers[player.id] = player
        }

        snapshot.awardsJson?.let { rawAwards ->
            val awards = gson.fromJson(rawAwards, Awards::class.java)
                ?: throw JsonParseException("awardsJson could not be decoded")
            listOf(
                awards.mvp,
                awards.defensivePlayer,
                awards.sixthMan,
                awards.rookieOfYear,
                awards.mostImproved
            ).forEach { player ->
                recordPlayer(player)
                val canonical = canonicalPlayers[player.id]
                if (canonical != null && canonical != player) {
                    throw JsonParseException("Award player id ${player.id} conflicts with persisted player state")
                }
                canonicalPlayers.putIfAbsent(player.id, player)
            }
        }

        snapshot.historyJson?.let { historyJson ->
            val historyRoot = JsonParser.parseString(historyJson).asObject("historyJson")
            historyRoot.getAsJsonArray("seasons")?.forEach { seasonElement ->
                seasonElement.asObject("history.season").getAsJsonArray("playerStats")?.forEach { playerElement ->
                    val player = gson.fromJson(playerElement, Player::class.java)
                    recordPlayer(player)
                }
            }
        }

        val requiredAllocatorHeadroom = requiredPlayerIdHeadroom(season)
        if (maxPersistedPlayerId.toLong() > Int.MAX_VALUE.toLong() - requiredAllocatorHeadroom) {
            throw JsonParseException("Persisted player ids leave no safe allocator headroom")
        }
        if (season.nextPlayerId <= maxPersistedPlayerId) {
            throw JsonParseException("nextPlayerId must exceed every persisted player id")
        }

        snapshot.contractsJson?.let { rawContracts ->
            val contractType = object : TypeToken<List<PlayerContract>>() {}.type
            val contracts: List<PlayerContract> = gson.fromJson(rawContracts, contractType)
            var payroll = 0L
            contracts.forEach { contract ->
                try {
                    payroll = Math.addExact(payroll, contract.salary)
                } catch (_: ArithmeticException) {
                    throw JsonParseException("Imported contract payroll overflows Long")
                }
                val expectedTeamId = rosterOwnerByPlayerId[contract.playerId]
                    ?: throw JsonParseException("Contract ${contract.playerId} does not belong to a current roster player")
                if (contract.teamId != expectedTeamId) {
                    throw JsonParseException("Contract ${contract.playerId} has a non-canonical teamId")
                }
            }
        }

        validateCompletedPlayoffState(snapshot, season, gson)

        snapshot.staffMarketJson?.let { raw ->
            val staffType = object : TypeToken<List<StaffMember>>() {}.type
            val staff: List<StaffMember> = gson.fromJson(raw, staffType)
            if (staff.map { it.id }.distinct().size != staff.size) {
                throw JsonParseException("Duplicate staff-market ids")
            }
        }
    }

    private fun validatePlayerBounds(player: Player) {
        val ratings = listOf(
            player.overall,
            player.shooting,
            player.defense,
            player.rebound,
            player.passing,
            player.athleticism
        )
        if (ratings.any { it !in 0..99 }) {
            throw JsonParseException("Player ${player.id} contains a rating outside 0..99")
        }
        // MAX_PLAYER_AGE is an offseason retirement threshold, not a persistence-format ceiling.
        // Existing seed careers may legitimately contain older players until the next transition.
        if (player.age <= 0) {
            throw JsonParseException("Player ${player.id} age must be positive")
        }
        if (player.id <= 0) {
            throw JsonParseException("Player id must be positive")
        }
        if (player.injured != (player.injuryDays > 0)) {
            throw JsonParseException("Player ${player.id} has inconsistent injury state")
        }
    }

    private fun validateSeasonAllocator(season: Season) {
        // The legacy replenishment path can allocate 12 players per team. A season transition may
        // also be preceded by the six-player user draft class and followed by the six-player free-
        // agent batch. Reserve the entire existing chain so an accepted import cannot overflow on
        // its next supported lifecycle operation.
        val requiredHeadroom = requiredPlayerIdHeadroom(season)
        if (season.nextPlayerId <= 0 || season.nextPlayerId.toLong() > Int.MAX_VALUE.toLong() - requiredHeadroom) {
            throw JsonParseException("nextPlayerId cannot accommodate the next player-generation batch")
        }
    }

    private fun requiredPlayerIdHeadroom(season: Season): Long =
        season.teams.size.coerceAtLeast(1).toLong() * 12L + 12L

    private fun validateSeasonGameCounter(season: Season) {
        if (season.gamesPlayed < 0) {
            throw JsonParseException("season.gamesPlayed must be non-negative")
        }
        val gamesPerDay = season.teams.size / 2L
        val maximumPlayedThroughCurrentDay = season.currentDay.toLong() * gamesPerDay
        if (season.gamesPlayed.toLong() > maximumPlayedThroughCurrentDay) {
            throw JsonParseException("season.gamesPlayed exceeds the schedule progress for currentDay")
        }
    }

    private fun validateCompletedPlayoffState(
        snapshot: GameStateRepository.GameStateSnapshot,
        season: Season,
        gson: Gson
    ) {
        val history = snapshot.historyJson?.let { gson.fromJson(it, HistoryManager::class.java) }
            ?: return
        val currentHistory = history.seasons.singleOrNull { it.seasonNumber == season.seasonNumber }
        if (currentHistory != null && snapshot.playoffResultJson == null) {
            throw JsonParseException("Completed current season requires playoffResultJson")
        }
        val playoff = snapshot.playoffResultJson?.let { gson.fromJson(it, Season.PlayoffResult::class.java) }
            ?: return
        if (currentHistory == null) return

        val canonicalTeams = season.teams.associateBy(::persistenceTeamId)
        listOf(playoff.eastChampion, playoff.westChampion, playoff.nbaChampion).forEach { champion ->
            if (canonicalTeams[persistenceTeamId(champion)] == null) {
                throw JsonParseException("Playoff champion is outside the canonical league")
            }
        }
        if (currentHistory.champion != playoff.nbaChampion.name) {
            throw JsonParseException("Playoff champion does not match completed season history")
        }
        val finals = playoff.seriesResults.singleOrNull { series ->
            series.roundName.equals("Finais da NBA", true) ||
                series.roundName.equals("Grande Final", true) ||
                series.roundName.equals("FINALS", true)
        } ?: throw JsonParseException("Completed playoff result must contain exactly one finals series")
        val finalists = listOfNotNull(finals.team1?.name, finals.team2?.name).toSet()
        val conferenceChampions = setOf(playoff.eastChampion.name, playoff.westChampion.name)
        if (finalists != conferenceChampions || finals.winner.name != playoff.nbaChampion.name) {
            throw JsonParseException("Playoff finals do not reconcile with conference and NBA champions")
        }
    }

    private fun validateRawPlayerBooleanFields(root: JsonObject) {
        val embeddedFields = listOf(
            "teamJson",
            "seasonJson",
            "historyJson",
            "awardsJson",
            "startingFiveJson",
            "freeAgentsJson",
            "draftRookiesJson",
            "latestBoxScoreJson",
            "playoffResultJson"
        )
        embeddedFields.forEach { field ->
            val raw = root.get(field) ?: return@forEach
            if (raw.isJsonNull) return@forEach
            if (!raw.isJsonPrimitive || !raw.asJsonPrimitive.isString) return@forEach
            val parsed = JsonParser.parseString(raw.asString)
            validatePlayerBooleanFieldsRecursively(parsed, field)
        }
    }

    private fun validatePlayerBooleanFieldsRecursively(element: JsonElement, path: String) {
        when {
            element.isJsonArray -> element.asJsonArray.forEachIndexed { index, child ->
                validatePlayerBooleanFieldsRecursively(child, "$path[$index]")
            }
            element.isJsonObject -> {
                val obj = element.asJsonObject
                val looksLikePlayer = listOf(
                    "id", "name", "position", "overall", "shooting", "defense",
                    "rebound", "passing", "athleticism", "age"
                ).all(obj::has)
                if (looksLikePlayer) {
                    requireJsonBoolean(obj.get("injured"), "$path.injured")
                }
                obj.entrySet().forEach { (name, child) ->
                    validatePlayerBooleanFieldsRecursively(child, "$path.$name")
                }
            }
        }
    }

    private fun requireEmbeddedPayload(root: JsonObject, field: String) {
        val raw = root.get(field)
        if (raw == null || raw.isJsonNull || !raw.isJsonPrimitive || !raw.asJsonPrimitive.isString) {
            throw JsonParseException("$field is required and must be a JSON string payload")
        }
    }

    private fun embeddedObject(root: JsonObject, field: String): JsonObject? {
        val raw = root.get(field) ?: return null
        if (raw.isJsonNull) return null
        if (!raw.isJsonPrimitive || !raw.asJsonPrimitive.isString) {
            throw JsonParseException("$field must be a JSON string payload")
        }
        return JsonParser.parseString(raw.asString).asObject(field)
    }

    private fun embeddedArray(root: JsonObject, field: String): Iterable<JsonElement>? {
        val raw = root.get(field) ?: return null
        if (raw.isJsonNull) return null
        if (!raw.isJsonPrimitive || !raw.asJsonPrimitive.isString) {
            throw JsonParseException("$field must be a JSON string payload")
        }
        val parsed = JsonParser.parseString(raw.asString)
        if (!parsed.isJsonArray) throw JsonParseException("$field must contain a JSON array")
        return parsed.asJsonArray
    }

    private fun persistenceTeamId(team: com.example.models.NbaTeam): String =
        team.abbreviation.ifBlank { team.name.lowercase().replace("[^a-z0-9]".toRegex(), "_") }

    private fun JsonElement.asObject(label: String): JsonObject {
        if (!isJsonObject) throw JsonParseException("$label must be an object")
        return asJsonObject
    }

    private fun requireJsonBoolean(value: JsonElement?, label: String): Boolean {
        if (value == null || !value.isJsonPrimitive || !value.asJsonPrimitive.isBoolean) {
            throw JsonParseException("$label must be a boolean")
        }
        return value.asBoolean
    }

    private fun requireJsonNumber(value: JsonElement?, label: String): JsonElement {
        if (value == null || !value.isJsonPrimitive || !value.asJsonPrimitive.isNumber) {
            throw JsonParseException("$label must be numeric")
        }
        return value
    }

    private fun requireJsonString(value: JsonElement?, label: String): String {
        if (value == null || !value.isJsonPrimitive || !value.asJsonPrimitive.isString) {
            throw JsonParseException("$label must be a string")
        }
        return value.asString
    }
}
