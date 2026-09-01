package com.example.utils

import android.content.Context
import com.example.data.repository.GameStateRepository
import com.example.domain.rules.SavedGameStartupRules
import com.example.models.*
import com.google.gson.Gson
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
            if (season.teams.none { it.name == team.name }) return@runBlocking false
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
        val nonNullList: (Any) -> Boolean = { value ->
            value is List<*> && value.all { it != null }
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

        return validPayload(snapshot.teamJson, NbaTeam::class.java, JsonToken.BEGIN_OBJECT) &&
            validPayload(snapshot.coachJson, Coach::class.java, JsonToken.BEGIN_OBJECT) &&
            validPayload(snapshot.financeJson, Finance::class.java, JsonToken.BEGIN_OBJECT) &&
            validPayload(snapshot.tacticsJson, Tactics::class.java, JsonToken.BEGIN_OBJECT) &&
            validPayload(snapshot.seasonJson, Season::class.java, JsonToken.BEGIN_OBJECT) &&
            validPayload(snapshot.historyJson, HistoryManager::class.java, JsonToken.BEGIN_OBJECT) &&
            validPayload(snapshot.awardsJson, Awards::class.java, JsonToken.BEGIN_OBJECT) &&
            validPayload(snapshot.startingFiveJson, listPlayerType, JsonToken.BEGIN_ARRAY, nonNullList) &&
            validPayload(snapshot.freeAgentsJson, listPlayerType, JsonToken.BEGIN_ARRAY, nonNullList) &&
            validPayload(snapshot.draftRookiesJson, listPlayerType, JsonToken.BEGIN_ARRAY, nonNullList) &&
            validPayload(snapshot.contractsJson, listContractType, JsonToken.BEGIN_ARRAY, nonNullList) &&
            validPayload(snapshot.staffMarketJson, listStaffType, JsonToken.BEGIN_ARRAY, nonNullList) &&
            validPayload(snapshot.notificationsJson, listNotificationType, JsonToken.BEGIN_ARRAY, nonNullList) &&
            validPayload(snapshot.teamStaffJson, TeamStaff::class.java, JsonToken.BEGIN_OBJECT) &&
            validPayload(snapshot.facilitiesJson, TeamFacilities::class.java, JsonToken.BEGIN_OBJECT) &&
            validPayload(snapshot.financeAdvancedJson, FinanceAdvanced::class.java, JsonToken.BEGIN_OBJECT) &&
            validPayload(snapshot.newsFeedJson, listNewsType, JsonToken.BEGIN_ARRAY, validNewsList) &&
            validPayload(snapshot.latestBoxScoreJson, MatchBoxScore::class.java, JsonToken.BEGIN_OBJECT) &&
            validPayload(snapshot.playoffResultJson, Season.PlayoffResult::class.java, JsonToken.BEGIN_OBJECT)
    }

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
