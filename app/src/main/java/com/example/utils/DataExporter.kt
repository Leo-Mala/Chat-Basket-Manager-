package com.example.utils

import android.content.Context
import com.example.data.repository.GameStateRepository
import com.example.domain.rules.SavedGameStartupRules
import com.example.models.Finance
import com.example.models.NbaTeam
import com.example.models.Season
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.io.File
import java.io.FileWriter
import kotlinx.coroutines.runBlocking

/** Import/export uses the Room-backed save instead of SharedPreferences. */
object DataExporter {
    private val gson = Gson()

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
        val payloads = listOfNotNull(
            snapshot.teamJson,
            snapshot.coachJson,
            snapshot.financeJson,
            snapshot.tacticsJson,
            snapshot.seasonJson,
            snapshot.historyJson,
            snapshot.awardsJson,
            snapshot.startingFiveJson,
            snapshot.freeAgentsJson,
            snapshot.draftRookiesJson,
            snapshot.contractsJson,
            snapshot.staffMarketJson,
            snapshot.notificationsJson,
            snapshot.teamStaffJson,
            snapshot.facilitiesJson,
            snapshot.financeAdvancedJson,
            snapshot.newsFeedJson,
            snapshot.latestBoxScoreJson,
            snapshot.playoffResultJson
        )
        return payloads.all { payload ->
            runCatching { JsonParser.parseString(payload) }.isSuccess
        }
    }

    @Deprecated("Use exportCurrentGame/importGame. Generic preference import was removed with the Room migration.")
    fun importData(context: Context, filePath: String): Boolean = importGame(context, filePath)
}
