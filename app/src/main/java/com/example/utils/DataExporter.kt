package com.example.utils

import android.content.Context
import com.example.data.repository.GameStateRepository
import com.example.domain.rules.SavedGameStartupRules
import com.example.models.*
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
    // Keep import validation aligned with the runtime save/load codec. In particular,
    // StaffMember is abstract and requires StaffMemberJsonAdapter.
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
            val snapshot = gson.fromJson(file.readText(), GameStateRepository.GameStateSnapshot::class.java)
                ?: return@runBlocking false
            if (!SavedGameStartupRules.hasRequiredCore(snapshot)) return@runBlocking false
            if (!hasValidJsonPayloads(snapshot)) return@runBlocking false

            // Validate and reconstruct the metadata before mutating the destination save.
            // This prevents an incomplete or internally inconsistent import from replacing
            // compatibility state while still reporting success.
            val repository = GameStateRepository(context)
            val team = repository.fromJson(snapshot.teamJson, NbaTeam::class.java)
                ?: return@runBlocking false
            val season = repository.fromJson(snapshot.seasonJson, Season::class.java)
                ?: return@runBlocking false
            val managedTeamId = persistenceTeamId(team)
            if (season.teams.count { persistenceTeamId(it) == managedTeamId } != 1) return@runBlocking false
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

    private fun hasValidJsonPayloads(snapshot: GameStateRepository.GameStateSnapshot): Boolean {
        val listPlayerType = object : TypeToken<List<Player>>() {}.type
        val listContractType = object : TypeToken<List<PlayerContract>>() {}.type
        val listStaffType = object : TypeToken<List<StaffMember>>() {}.type
        val listNotificationType = object : TypeToken<List<AssistantCoachNotification>>() {}.type
        val listNewsType = object : TypeToken<List<News>>() {}.type
        val validPlayer: (Player) -> Boolean = { item ->
            runCatching {
                // Gson may instantiate Kotlin objects with omitted required properties as
                // zero/null values. Explicit field presence is checked separately below, while
                // this verifies the runtime references that callers dereference directly.
                item.name.length
                item.position.length
                true
            }.getOrDefault(false)
        }
        val validPlayerList: (Any) -> Boolean = { value ->
            value is List<*> && value.all { item ->
                item is Player && validPlayer(item)
            }
        }
        val validTeam: (NbaTeam) -> Boolean = { team ->
            runCatching {
                team.name.length
                team.city.length
                team.abbreviation.length
                team.conference.length
                team.arena.name.length
                team.arena.city.length
                check(team.arena.capacity > 0)
                team.arena.opened
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
        val validContracts: (Any) -> Boolean = { value ->
            value is List<*> && value.all { item ->
                item is PlayerContract &&
                    item.salary >= 0 &&
                    item.yearsRemaining in 0..5
            }
        }
        val validSeason: (Any) -> Boolean = { value ->
            value is Season && runCatching {
                check(value.teams.all(validTeam))
                val teamNames = value.teams.map { it.name }
                check(teamNames.size == teamNames.toSet().size)
                val teamIds = value.teams.map(::persistenceTeamId)
                check(teamIds.size == teamIds.toSet().size)
                val playerIds = value.teams.flatMap { it.players }.map { it.id }
                check(playerIds.size == playerIds.toSet().size)
                // GameStateRepository.load() requires normalized standings to cover every team.
                // Reject an import before persistence when that invariant is already broken.
                check(value.standings.keys == teamNames.toSet())
                value.standings.values.forEach { record ->
                    record.wins
                    record.losses
                    record.gamesPlayed
                    record.totalPointsScored
                    record.totalPointsConceded
                }
                val teamIdSet = teamIds.toSet()
                check(value.history.all { result ->
                    persistenceTeamId(result.homeTeam) in teamIdSet &&
                        persistenceTeamId(result.awayTeam) in teamIdSet
                })
                true
            }.getOrDefault(false)
        }
        val validTactics: (Any) -> Boolean = { value ->
            value is Tactics && runCatching {
                value.style.name
                true
            }.getOrDefault(false)
        }
        val validAwards: (Any) -> Boolean = { value ->
            value is Awards && runCatching {
                check(validPlayer(value.mvp))
                check(validPlayer(value.defensivePlayer))
                check(validPlayer(value.sixthMan))
                check(validPlayer(value.rookieOfYear))
                check(validPlayer(value.mostImproved))
                value.coachOfYearName.length
                value.coachOfYearTeam.length
                true
            }.getOrDefault(false)
        }
        val validStaffMember: (StaffMember) -> Boolean = { item ->
            runCatching {
                // StaffMemberJsonAdapter can still construct concrete instances whose Kotlin
                // non-null references are null when required JSON fields are omitted.
                item.name.length
                item.specialty.length
                when (item) {
                    is HeadCoachStaff -> item.preferredStyle.name
                    is ExecutiveStaff -> item.roleTitle.length
                    else -> Unit
                }
                true
            }.getOrDefault(false)
        }
        val validStaffList: (Any) -> Boolean = { value ->
            value is List<*> && value.all { item ->
                item is StaffMember && validStaffMember(item)
            }
        }
        val validTeamStaff: (Any) -> Boolean = { value ->
            value is TeamStaff && runCatching {
                // TeamStaff methods and UI assume both collections are non-null. Optional single
                // staff positions may be empty, but every present member must be runtime-safe.
                value.assistants.size
                value.executives.size
                value.headCoach?.let { check(validStaffMember(it)) }
                value.strengthCoach?.let { check(validStaffMember(it)) }
                value.scout?.let { check(validStaffMember(it)) }
                value.teamDoctor?.let { check(validStaffMember(it)) }
                check(value.assistants.all(validStaffMember))
                check(value.executives.all(validStaffMember))
                true
            }.getOrDefault(false)
        }
        val validFacilities: (Any) -> Boolean = { value ->
            value is TeamFacilities && runCatching {
                listOf(value.arena, value.training, value.medical, value.scouting).forEach { facility ->
                    facility.type.name
                    facility.name.length
                }
                true
            }.getOrDefault(false)
        }
        val validFinanceAdvanced: (Any) -> Boolean = { value ->
            value is FinanceAdvanced && runCatching {
                // FinanceAdvancedScreen and calculations dereference these nested objects/lists.
                value.ownerObjective.length
                value.revenues.totalRevenue()
                value.expenses.totalExpenses()
                value.activeSponsorships.size
                value.activeSponsorships.forEach { deal ->
                    deal.brandName.length
                    deal.type.length
                }
                true
            }.getOrDefault(false)
        }
        val validNotificationList: (Any) -> Boolean = { value ->
            value is List<*> && value.all { item ->
                item is AssistantCoachNotification && runCatching {
                    // Gson can create Kotlin objects with null values for non-null properties.
                    // Touch every reference consumed by NotificationsTab before accepting import.
                    item.id.length
                    item.opponentName.length
                    item.coachName.length
                    item.coachRole.length
                    item.summary.length
                    item.keyStrengths.size
                    item.areasToImprove.size
                    item.playerHighlights.size
                    item.tacticalAdvice.length
                    item.keyStrengths.all { it.isNotBlank() } &&
                        item.areasToImprove.all { it.isNotBlank() } &&
                        item.playerHighlights.all { it.isNotBlank() }
                }.getOrDefault(false)
            }
        }
        val validNewsList: (Any) -> Boolean = { value ->
            value is List<*> && value.all { item ->
                item is News && runCatching {
                    item.title.isNotBlank() &&
                        item.content.isNotBlank() &&
                        item.dateString.isNotBlank() &&
                        item.type in NewsType.entries
                }.getOrDefault(false)
            }
        }
        val validMatchBoxScore: (Any) -> Boolean = { value ->
            value is MatchBoxScore && runCatching {
                // Gson can instantiate Kotlin data classes with missing non-null fields as null.
                // Touch every reference that BoxScoreScreen relies on so an incomplete object is
                // rejected here, before it can replace the destination career.
                value.matchId.length
                value.dateString.length
                value.homeTeamName.length
                value.awayTeamName.length
                value.homeQuarterScores.size
                value.awayQuarterScores.size
                value.homePlayers.forEach { player ->
                    player.playerName.length
                    player.position.length
                }
                value.awayPlayers.forEach { player ->
                    player.playerName.length
                    player.position.length
                }
                value.homeTeamTotals.teamName.length
                value.awayTeamTotals.teamName.length
                true
            }.getOrDefault(false)
        }
        val validPlayoffResult: (Any) -> Boolean = { value ->
            value is Season.PlayoffResult && runCatching {
                // Career resume and celebration paths immediately dereference champions and series.
                value.eastChampion.name.length
                value.westChampion.name.length
                value.nbaChampion.name.length
                value.seriesResults.size
                value.seriesResults.forEach { series ->
                    series.winner.name.length
                    series.games.size
                    series.roundName.length
                    series.team1?.name?.length
                    series.team2?.name?.length
                    series.mvp?.name?.length
                }
                value.mvp?.name?.length
                true
            }.getOrDefault(false)
        }
        val requiredPlayerFields = setOf(
            "id",
            "name",
            "position",
            "overall",
            "shooting",
            "defense",
            "rebound",
            "passing",
            "athleticism",
            "age"
        )
        val requiredCoachFields = setOf(
            "id",
            "name",
            "offensiveSkill",
            "defensiveSkill",
            "motivationalSkill",
            "salary",
            "contractYears"
        )
        val requiredContractFields = setOf("playerId", "salary", "yearsRemaining")
        val requiredArenaFields = setOf("name", "city", "capacity", "opened")

        return validPayload(snapshot.teamJson, NbaTeam::class.java, JsonToken.BEGIN_OBJECT) { value ->
                value is NbaTeam && validTeam(value)
            } &&
            hasRequiredTeamPlayerFields(snapshot.teamJson, requiredPlayerFields) &&
            hasRequiredTeamArenaFields(snapshot.teamJson, requiredArenaFields) &&
            validPayload(snapshot.coachJson, Coach::class.java, JsonToken.BEGIN_OBJECT, validCoach) &&
            hasRequiredObjectFields(snapshot.coachJson, requiredCoachFields) &&
            validPayload(snapshot.financeJson, Finance::class.java, JsonToken.BEGIN_OBJECT) &&
            validPayload(snapshot.tacticsJson, Tactics::class.java, JsonToken.BEGIN_OBJECT, validTactics) &&
            hasRequiredObjectFields(
                snapshot.tacticsJson,
                setOf("style", "pace", "defensivePressure", "offensiveRebound")
            ) &&
            validPayload(snapshot.seasonJson, Season::class.java, JsonToken.BEGIN_OBJECT, validSeason) &&
            hasRequiredSeasonPlayerFields(snapshot.seasonJson, requiredPlayerFields) &&
            hasRequiredSeasonTeamArenaFields(snapshot.seasonJson, requiredArenaFields) &&
            validPayload(snapshot.historyJson, HistoryManager::class.java, JsonToken.BEGIN_OBJECT) &&
            validPayload(snapshot.awardsJson, Awards::class.java, JsonToken.BEGIN_OBJECT, validAwards) &&
            hasRequiredAwardsPlayerFields(snapshot.awardsJson, requiredPlayerFields) &&
            validPayload(snapshot.startingFiveJson, listPlayerType, JsonToken.BEGIN_ARRAY, validPlayerList) &&
            hasRequiredArrayObjectFields(snapshot.startingFiveJson, requiredPlayerFields) &&
            validPayload(snapshot.freeAgentsJson, listPlayerType, JsonToken.BEGIN_ARRAY, validPlayerList) &&
            hasRequiredArrayObjectFields(snapshot.freeAgentsJson, requiredPlayerFields) &&
            validPayload(snapshot.draftRookiesJson, listPlayerType, JsonToken.BEGIN_ARRAY, validPlayerList) &&
            hasRequiredArrayObjectFields(snapshot.draftRookiesJson, requiredPlayerFields) &&
            validPayload(snapshot.contractsJson, listContractType, JsonToken.BEGIN_ARRAY, validContracts) &&
            hasRequiredArrayObjectFields(snapshot.contractsJson, requiredContractFields) &&
            validPayload(snapshot.staffMarketJson, listStaffType, JsonToken.BEGIN_ARRAY, validStaffList) &&
            validPayload(
                snapshot.notificationsJson,
                listNotificationType,
                JsonToken.BEGIN_ARRAY,
                validNotificationList
            ) &&
            validPayload(snapshot.teamStaffJson, TeamStaff::class.java, JsonToken.BEGIN_OBJECT, validTeamStaff) &&
            validPayload(snapshot.facilitiesJson, TeamFacilities::class.java, JsonToken.BEGIN_OBJECT, validFacilities) &&
            validPayload(
                snapshot.financeAdvancedJson,
                FinanceAdvanced::class.java,
                JsonToken.BEGIN_OBJECT,
                validFinanceAdvanced
            ) &&
            validPayload(snapshot.newsFeedJson, listNewsType, JsonToken.BEGIN_ARRAY, validNewsList) &&
            validPayload(
                snapshot.latestBoxScoreJson,
                MatchBoxScore::class.java,
                JsonToken.BEGIN_OBJECT,
                validMatchBoxScore
            ) &&
            validPayload(
                snapshot.playoffResultJson,
                Season.PlayoffResult::class.java,
                JsonToken.BEGIN_OBJECT,
                validPlayoffResult
            )
    }

    private fun hasRequiredObjectFields(payload: String?, requiredFields: Set<String>): Boolean {
        if (payload == null) return true
        return runCatching {
            val objectPayload = gson.fromJson(payload, JsonObject::class.java)
                ?: error("JSON payload did not decode to an object")
            hasRequiredFields(objectPayload, requiredFields)
        }.getOrDefault(false)
    }

    private fun hasRequiredArrayObjectFields(payload: String?, requiredFields: Set<String>): Boolean {
        if (payload == null) return true
        return runCatching {
            val arrayPayload = gson.fromJson(payload, JsonArray::class.java)
                ?: error("JSON payload did not decode to an array")
            arrayPayload.all { element ->
                element.isJsonObject && hasRequiredFields(element.asJsonObject, requiredFields)
            }
        }.getOrDefault(false)
    }

    private fun hasRequiredTeamPlayerFields(payload: String?, requiredFields: Set<String>): Boolean {
        if (payload == null) return true
        return runCatching {
            val team = gson.fromJson(payload, JsonObject::class.java)
                ?: error("Team payload did not decode to an object")
            val players = team.getAsJsonArray("players")
                ?: error("Team payload did not contain players")
            hasRequiredFields(players, requiredFields)
        }.getOrDefault(false)
    }

    private fun hasRequiredTeamArenaFields(payload: String?, requiredFields: Set<String>): Boolean {
        if (payload == null) return true
        return runCatching {
            val team = gson.fromJson(payload, JsonObject::class.java)
                ?: error("Team payload did not decode to an object")
            val arena = team.getAsJsonObject("arena")
                ?: error("Team payload did not contain arena")
            hasRequiredFields(arena, requiredFields)
        }.getOrDefault(false)
    }

    private fun hasRequiredSeasonPlayerFields(payload: String?, requiredFields: Set<String>): Boolean {
        if (payload == null) return true
        return runCatching {
            val season = gson.fromJson(payload, JsonObject::class.java)
                ?: error("Season payload did not decode to an object")
            val teams = season.getAsJsonArray("teams")
                ?: error("Season payload did not contain teams")
            teams.all { teamElement ->
                if (!teamElement.isJsonObject) return@all false
                val players = teamElement.asJsonObject.getAsJsonArray("players") ?: return@all false
                hasRequiredFields(players, requiredFields)
            }
        }.getOrDefault(false)
    }

    private fun hasRequiredSeasonTeamArenaFields(payload: String?, requiredFields: Set<String>): Boolean {
        if (payload == null) return true
        return runCatching {
            val season = gson.fromJson(payload, JsonObject::class.java)
                ?: error("Season payload did not decode to an object")
            val teams = season.getAsJsonArray("teams")
                ?: error("Season payload did not contain teams")
            teams.all { teamElement ->
                if (!teamElement.isJsonObject) return@all false
                val arena = teamElement.asJsonObject.getAsJsonObject("arena") ?: return@all false
                hasRequiredFields(arena, requiredFields)
            }
        }.getOrDefault(false)
    }

    private fun hasRequiredAwardsPlayerFields(payload: String?, requiredFields: Set<String>): Boolean {
        if (payload == null) return true
        return runCatching {
            val awards = gson.fromJson(payload, JsonObject::class.java)
                ?: error("Awards payload did not decode to an object")
            listOf("mvp", "defensivePlayer", "sixthMan", "rookieOfYear", "mostImproved").all { field ->
                val player = awards.getAsJsonObject(field) ?: return@all false
                hasRequiredFields(player, requiredFields)
            }
        }.getOrDefault(false)
    }

    private fun hasRequiredFields(
        arrayPayload: JsonArray,
        requiredFields: Set<String>
    ): Boolean = arrayPayload.all { element ->
        element.isJsonObject && hasRequiredFields(element.asJsonObject, requiredFields)
    }

    private fun hasRequiredFields(
        objectPayload: JsonObject,
        requiredFields: Set<String>
    ): Boolean = requiredFields.all { field ->
        objectPayload.has(field) && !objectPayload.get(field).isJsonNull
    }

    private fun persistenceTeamId(team: NbaTeam): String =
        team.abbreviation.ifBlank { team.name.lowercase().replace("[^a-z0-9]".toRegex(), "_") }

    private fun validPayload(
        payload: String?,
        type: Type,
        expectedRoot: JsonToken,
        validateDecoded: (Any) -> Boolean = { true }
    ): Boolean {
        if (payload == null) return true
        return runCatching {
            JsonReader(StringReader(payload)).use { reader ->
                reader.isLenient = false
                check(reader.peek() == expectedRoot) { "Unexpected JSON root" }
                reader.skipValue()
                check(reader.peek() == JsonToken.END_DOCUMENT) { "Trailing JSON content" }
            }
            val decoded = gson.fromJson<Any>(payload, type)
                ?: error("JSON payload did not match expected type")
            check(validateDecoded(decoded)) { "JSON payload contains invalid elements" }
        }.isSuccess
    }

    @Deprecated("Use exportCurrentGame/importGame. Generic preference import was removed with the Room migration.")
    fun importData(context: Context, filePath: String): Boolean = importGame(context, filePath)
}
