package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.NbaDataGenerator
import com.example.data.local.BasketDatabase
import com.example.data.repository.GameStateRepository
import com.example.models.*
import com.example.utils.AutoSaveManager
import com.example.utils.DataExporter
import com.example.utils.SaveSlotManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataExporterSnapshotBoundaryIntegrityTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val gson = AutoSaveManager.gson
    private val exportGson = Gson()
    private val testSlot = 3
    private lateinit var repository: GameStateRepository
    private var originalSlot = 1

    @Before
    fun setUp() = runBlocking {
        originalSlot = SaveSlotManager.getActiveSlot(context)
        SaveSlotManager.setActiveSlot(context, testSlot)
        SaveSlotManager.clearPendingNewSlot(context)
        repository = GameStateRepository(context, BasketDatabase.getInstance(context, testSlot))
        repository.clear()
        SaveSlotManager.clearSlotMetadata(context, testSlot)
    }

    @After
    fun tearDown() = runBlocking {
        repository.clear()
        SaveSlotManager.clearSlotMetadata(context, testSlot)
        SaveSlotManager.clearPendingNewSlot(context)
        SaveSlotManager.setActiveSlot(context, originalSlot)
    }

    @Test
    fun snapshotBoundaryCorruptionIsRejectedAtomically() = runBlocking {
        val teams = NbaDataGenerator.getAllTeams()
        val managed = teams.first()
        val season = Season(teams, currentDay = 31, gamesPlayed = 31, seasonNumber = 5).apply {
            userTeamName = managed.name
        }
        val seed = GameStateRepository.GameStateSnapshot(
            teamJson = gson.toJson(managed),
            coachJson = gson.toJson(Coach(1, "Integrity Coach", 80, 80, 80, 300_000, 3)),
            financeJson = gson.toJson(Finance(180_000_000)),
            tacticsJson = gson.toJson(Tactics()),
            seasonJson = gson.toJson(season),
            historyJson = gson.toJson(HistoryManager()),
            awardsJson = null,
            startingFiveJson = gson.toJson(managed.players.take(5)),
            freeAgentsJson = gson.toJson(emptyList<Player>()),
            draftRookiesJson = gson.toJson(emptyList<Player>()),
            contractsJson = gson.toJson(emptyList<PlayerContract>()),
            staffMarketJson = gson.toJson(emptyList<StaffMember>()),
            notificationsJson = gson.toJson(emptyList<AssistantCoachNotification>()),
            teamStaffJson = gson.toJson(TeamStaff()),
            facilitiesJson = gson.toJson(TeamFacilities()),
            financeAdvancedJson = gson.toJson(FinanceAdvanced()),
            newsFeedJson = gson.toJson(emptyList<News>()),
            latestBoxScoreJson = null,
            playoffResultJson = null,
            difficulty = 2,
            injuriesEnabled = true,
            autoSubstitutionsEnabled = true
        )
        repository.save(seed)
        val existing = requireNotNull(repository.load())
        val existingTeam = gson.fromJson(existing.teamJson, NbaTeam::class.java)
        val existingSeason = gson.fromJson(existing.seasonJson, Season::class.java)
        val existingFinance = gson.fromJson(existing.financeJson, Finance::class.java)
        SaveSlotManager.updateSlot(context, testSlot, existingTeam, existingSeason, existingFinance, existing.difficulty)

        val seasonMissingProgress = gson.fromJson(existing.seasonJson, JsonObject::class.java).apply {
            remove("currentDay")
        }
        val invalidConference = gson.fromJson(existing.seasonJson, JsonObject::class.java).apply {
            getAsJsonArray("teams")[0].asJsonObject.addProperty("conference", "North")
        }
        val history = HistoryManager().apply {
            addSeason(SeasonHistory(4, "Champion", null, "4-2", "Top Scorer", 29.5))
        }
        val historyMissingSeasonNumber = gson.toJsonTree(history).asJsonObject.apply {
            getAsJsonArray("seasons")[0].asJsonObject.remove("seasonNumber")
        }
        val reusedRosterId = existingSeason.teams.first().players.first()

        val nestedCases = listOf(
            "missing-season-progress.json" to existing.copy(seasonJson = gson.toJson(seasonMissingProgress)),
            "unsupported-conference.json" to existing.copy(seasonJson = gson.toJson(invalidConference)),
            "reused-pool-player-id.json" to existing.copy(freeAgentsJson = gson.toJson(listOf(reusedRosterId))),
            "missing-history-season-number.json" to existing.copy(historyJson = gson.toJson(historyMissingSeasonNumber))
        )
        nestedCases.forEach { (name, invalid) ->
            val file = File(context.cacheDir, name).apply { writeText(exportGson.toJson(invalid)) }
            assertRejectedAndUnchanged(file, existing, existingTeam)
        }

        listOf("difficulty", "injuriesEnabled", "autoSubstitutionsEnabled").forEach { field ->
            val outer = exportGson.toJsonTree(existing).asJsonObject.apply { remove(field) }
            val file = File(context.cacheDir, "missing-$field.json").apply { writeText(exportGson.toJson(outer)) }
            assertRejectedAndUnchanged(file, existing, existingTeam)
        }
    }

    private suspend fun assertRejectedAndUnchanged(
        file: File,
        existing: GameStateRepository.GameStateSnapshot,
        existingTeam: NbaTeam
    ) {
        assertFalse("Expected ${file.name} to be rejected", DataExporter.importGame(context, file.absolutePath))
        val loaded = requireNotNull(repository.load())
        assertEquals(existing.teamJson, loaded.teamJson)
        assertEquals(existing.seasonJson, loaded.seasonJson)
        assertEquals(existing.freeAgentsJson, loaded.freeAgentsJson)
        assertEquals(existing.historyJson, loaded.historyJson)
        assertEquals(existing.difficulty, loaded.difficulty)
        assertEquals(existing.injuriesEnabled, loaded.injuriesEnabled)
        assertEquals(existing.autoSubstitutionsEnabled, loaded.autoSubstitutionsEnabled)
        val slot = SaveSlotManager.getSlots(context).single { it.slotId == testSlot }
        assertTrue(slot.occupied)
        assertEquals(existingTeam.name, slot.teamName)
        assertEquals(5, slot.seasonNumber)
        assertEquals(31, slot.currentDay)
        assertEquals(180_000_000, slot.budget)
    }
}
