package com.example.utils

import android.content.Context
import com.example.data.repository.GameStateRepository
import com.example.domain.rules.SavedGameStartupRules
import com.example.models.*
import com.example.simulator.RotationRules
import com.google.gson.Gson
import com.google.gson.JsonArray
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
            if (season.teams.count { persistenceTeamId(it) == managedTeamId } != 1) return@runBlocking false
            val seasonManagedTeamId = season.userTeamName?.let { userTeamName ->
                season.teams.singleOrNull { it.name == userTeamName }?.let(::persistenceTeamId)
            }
            if (season.userTeamName != null && seasonManagedTeamId != managedTeamId) return@runBlocking false
            val finance = snapshot.financeJson?.let { repository.fromJson(it, Finance::class.java) }

            repository.save(snapshot)
            SaveSlotManager.updateSlot(
                context = context,
                slotId = SaveSlotManager.getActiveSlot(context),
                team = team,
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
                item.name.length
                item.position.length
                true
            }.getOrDefault(false)
        }
        val validPlayerList: (Any) -> Boolean = { value ->
            value is List<*> && value.all { it is Player && validPlayer(it) }
        }
        val validTeam: (NbaTeam) -> Boolean = { team ->
            runCatching {
                team.name.length
                team.city.length
                team.abbreviation.length
                check(team.conference == "East" || team.conference == "West")
                team.arena.name.length
                team.arena.city.length
                check(team.arena.capacity > 0)
                team.arena.opened
                check(team.players.size >= RotationRules.MIN_PLAYERS_FOR_GAME)
                check(team.players.all(validPlayer))
                true
            }.getOrDefault(false)
        }
        val validCoach: (Any) -> Boolean = { value ->
            value is Coach && runCatching {
                value.name.length
                value.offensiveSkill in 0..100 &&
                    value.defensiveSkill in 0..100 &&
                    value.motivationalSkill in 0..100 &&
                    value.salary >= 0 &&
                    value.contractYears >= 0
            }.getOrDefault(false)
        }
        val validFinance: (Any) -> Boolean = { value ->
            value is Finance && runCatching {
                value.sponsors.forEach {
                    it.name.length
                    check(it.yearsRemaining >= 0)
                }
                value.expenses.forEach {
                    it.description.length
                    it.date.length
                }
                check(value.arenaSeatsLevel >= 1)
                check(value.medicalStaffLevel >= 1)
                check(value.scoutingLevel >= 1)
                true
            }.getOrDefault(false)
        }
        val validContracts: (Any) -> Boolean = { value ->
            value is List<*> && value.all {
                it is PlayerContract && it.salary >= 0 && it.yearsRemaining in 0..5
            }
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
                    it.wins; it.losses; it.gamesPlayed; it.totalPointsScored; it.totalPointsConceded
                }
                val teamsById = value.teams.associateBy(::persistenceTeamId)
                check(value.history.all { result ->
                    persistenceTeamId(result.homeTeam) in teamsById &&
                        persistenceTeamId(result.awayTeam) in teamsById &&
                        result.homeScore >= 0 && result.awayScore >= 0 && result.attendance >= 0 &&
                        runCatching { result.narration.length }.isSuccess
                })
                true
            }.getOrDefault(false)
        }
        val validHistory: (Any) -> Boolean = { value ->
            value is HistoryManager && runCatching {
                val seasonNumbers = value.seasons.map { it.seasonNumber }
                seasonNumbers.size == seasonNumbers.toSet().size && value.seasons.all { item ->
                    item.seasonNumber >= 1 &&
                        item.champion.isNotBlank() &&
                        item.finalScore.isNotBlank() &&
                        item.topScorer.isNotBlank() &&
                        item.topScorerPoints.isFinite() && item.topScorerPoints >= 0.0 &&
                        item.teamWins.entries.all { (name, wins) -> name.isNotBlank() && wins >= 0 } &&
                        item.playerStats.all(validPlayer)
                }
            }.getOrDefault(false)
        }
        val validTactics: (Any) -> Boolean = { value ->
            value is Tactics && runCatching { value.style.name; true }.getOrDefault(false)
        }
        val validAwards: (Any) -> Boolean = { value ->
            value is Awards && runCatching {
                check(validPlayer(value.mvp)); check(validPlayer(value.defensivePlayer)); check(validPlayer(value.sixthMan))
                check(validPlayer(value.rookieOfYear)); check(validPlayer(value.mostImproved))
                value.coachOfYearName.length; value.coachOfYearTeam.length
                true
            }.getOrDefault(false)
        }
        val validStaffMember: (StaffMember) -> Boolean = { item ->
            runCatching {
                item.name.length; item.specialty.length
                when (item) {
                    is HeadCoachStaff -> item.preferredStyle.name
                    is ExecutiveStaff -> item.roleTitle.length
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
                    it.type.name; it.name.length
                }
                true
            }.getOrDefault(false)
        }
        val validFinanceAdvanced: (Any) -> Boolean = { value ->
            value is FinanceAdvanced && runCatching {
                value.ownerObjective.length; value.revenues.totalRevenue(); value.expenses.totalExpenses()
                value.activeSponsorships.size
                value.activeSponsorships.forEach { it.brandName.length; it.type.length }
                true
            }.getOrDefault(false)
        }
        val validNotificationList: (Any) -> Boolean = { value ->
            value is List<*> && value.all { item ->
                item is AssistantCoachNotification && runCatching {
                    item.id.length; item.opponentName.length; item.coachName.length; item.coachRole.length; item.summary.length
                    item.keyStrengths.size; item.areasToImprove.size; item.playerHighlights.size; item.tacticalAdvice.length
                    item.keyStrengths.all { it.isNotBlank() } && item.areasToImprove.all { it.isNotBlank() } &&
                        item.playerHighlights.all { it.isNotBlank() }
                }.getOrDefault(false)
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
                value.matchId.length; value.dateString.length; value.homeTeamName.length; value.awayTeamName.length
                value.homeQuarterScores.size; value.awayQuarterScores.size
                value.homePlayers.forEach { it.playerName.length; it.position.length }
                value.awayPlayers.forEach { it.playerName.length; it.position.length }
                value.homeTeamTotals.teamName.length; value.awayTeamTotals.teamName.length
                true
            }.getOrDefault(false)
        }
        val validPlayoffResult: (Any) -> Boolean = { value ->
            value is Season.PlayoffResult && runCatching {
                value.eastChampion.name.length; value.westChampion.name.length; value.nbaChampion.name.length
                value.seriesResults.size
                value.seriesResults.forEach {
                    it.winner.name.length; it.games.size; it.roundName.length
                    it.team1?.name?.length; it.team2?.name?.length; it.mvp?.name?.length
                }
                value.mvp?.name?.length
                true
            }.getOrDefault(false)
        }

        val requiredPlayerFields = setOf("id", "name", "position", "overall", "shooting", "defense", "rebound", "passing", "athleticism", "age")
        val requiredCoachFields = setOf("id", "name", "offensiveSkill", "defensiveSkill", "motivationalSkill", "salary", "contractYears")
        val requiredFinanceFields = setOf("budget", "sponsors", "expenses", "coachSalaryPaid", "arenaSeatsLevel", "medicalStaffLevel", "scoutingLevel")
        val requiredContractFields = setOf("playerId", "salary", "yearsRemaining")
        val requiredArenaFields = setOf("name", "city", "capacity", "opened")
        val requiredSeasonFields = setOf("teams", "currentDay", "gamesPlayed", "seasonNumber", "currentMonth", "currentYear", "nextPlayerId", "standings", "history")
        val requiredGameFields = setOf("homeTeam", "awayTeam", "homeScore", "awayScore", "attendance", "homeStats", "awayStats", "injuries", "narration")
        val requiredHistoryFields = setOf("seasonNumber", "champion", "finalScore", "topScorer", "topScorerPoints", "teamWins", "playerStats")

        return validPayload(snapshot.teamJson, NbaTeam::class.java, JsonToken.BEGIN_OBJECT) { it is NbaTeam && validTeam(it) } &&
            hasRequiredTeamPlayerFields(snapshot.teamJson, requiredPlayerFields) &&
            hasRequiredTeamArenaFields(snapshot.teamJson, requiredArenaFields) &&
            snapshot.coachJson != null &&
            validPayload(snapshot.coachJson, Coach::class.java, JsonToken.BEGIN_OBJECT, validCoach) &&
            hasRequiredObjectFields(snapshot.coachJson, requiredCoachFields) &&
            snapshot.financeJson != null &&
            validPayload(snapshot.financeJson, Finance::class.java, JsonToken.BEGIN_OBJECT, validFinance) &&
            hasRequiredObjectFields(snapshot.financeJson, requiredFinanceFields) &&
            snapshot.tacticsJson != null &&
            validPayload(snapshot.tacticsJson, Tactics::class.java, JsonToken.BEGIN_OBJECT, validTactics) &&
            hasRequiredObjectFields(snapshot.tacticsJson, setOf("style", "pace", "defensivePressure", "offensiveRebound")) &&
            validPayload(snapshot.seasonJson, Season::class.java, JsonToken.BEGIN_OBJECT, validSeason) &&
            hasRequiredObjectFields(snapshot.seasonJson, requiredSeasonFields) &&
            hasRequiredSeasonPlayerFields(snapshot.seasonJson, requiredPlayerFields) &&
            hasRequiredSeasonTeamArenaFields(snapshot.seasonJson, requiredArenaFields) &&
            hasRequiredSeasonGameHistoryFields(snapshot.seasonJson, requiredGameFields) &&
            hasValidSeasonHistoryReferences(snapshot) &&
            snapshot.historyJson != null &&
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
            snapshot.contractsJson != null &&
            validPayload(snapshot.contractsJson, listContractType, JsonToken.BEGIN_ARRAY, validContracts) &&
            hasRequiredArrayObjectFields(snapshot.contractsJson, requiredContractFields) &&
            hasCompleteRosterContractCoverage(snapshot) &&
            validPayload(snapshot.staffMarketJson, listStaffType, JsonToken.BEGIN_ARRAY, validStaffList) &&
            validPayload(snapshot.notificationsJson, listNotificationType, JsonToken.BEGIN_ARRAY, validNotificationList) &&
            validPayload(snapshot.teamStaffJson, TeamStaff::class.java, JsonToken.BEGIN_OBJECT, validTeamStaff) &&
            validPayload(snapshot.facilitiesJson, TeamFacilities::class.java, JsonToken.BEGIN_OBJECT, validFacilities) &&
            validPayload(snapshot.financeAdvancedJson, FinanceAdvanced::class.java, JsonToken.BEGIN_OBJECT, validFinanceAdvanced) &&
            validPayload(snapshot.newsFeedJson, listNewsType, JsonToken.BEGIN_ARRAY, validNewsList) &&
            validPayload(snapshot.latestBoxScoreJson, MatchBoxScore::class.java, JsonToken.BEGIN_OBJECT, validMatchBoxScore) &&
            validPayload(snapshot.playoffResultJson, Season.PlayoffResult::class.java, JsonToken.BEGIN_OBJECT, validPlayoffResult)
    }

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
        val freeAgents = snapshot.freeAgentsJson?.let { gson.fromJson<List<Player>>(it, type) }.orEmpty()
        val draftRookies = snapshot.draftRookiesJson?.let { gson.fromJson<List<Player>>(it, type) }.orEmpty()
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
        val freeAgents = snapshot.freeAgentsJson?.let { gson.fromJson<List<Player>>(it, playerType) }.orEmpty()
        val draftRookies = snapshot.draftRookiesJson?.let { gson.fromJson<List<Player>>(it, playerType) }.orEmpty()
        val awards = snapshot.awardsJson?.let { gson.fromJson(it, Awards::class.java) }
        val persistedPlayerIds = buildSet {
            addAll(season.teams.flatMap { it.players }.map { it.id })
            addAll(freeAgents.map { it.id })
            addAll(draftRookies.map { it.id })
            awards?.let { addAll(listOf(it.mvp.id, it.defensivePlayer.id, it.sixthMan.id, it.rookieOfYear.id, it.mostImproved.id)) }
        }
        season.history.all { result ->
            result.homeStats.keys.all { it.id in persistedPlayerIds } &&
                result.awayStats.keys.all { it.id in persistedPlayerIds } &&
                result.injuries.all { it.player.id in persistedPlayerIds }
        }
    }.getOrDefault(false)

    private fun hasRequiredSeasonGameHistoryFields(payload: String?, requiredFields: Set<String>): Boolean {
        if (payload == null) return true
        return runCatching {
            val history = strictObject(payload).getAsJsonArray("history") ?: return@runCatching false
            hasRequiredFields(history, requiredFields)
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
