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
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DataExporterImportIntegrityTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val gson = AutoSaveManager.gson
    private val exportGson = Gson()
    private val testSlot = 3
    private lateinit var repository: GameStateRepository
    private var originalSlot: Int = 1

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
    fun validImportUpdatesActiveSlotMetadata() = runBlocking {
        val snapshot = snapshot(
            seasonNumber = 4,
            currentDay = 23,
            budget = 145_000_000,
            difficulty = 3,
            latestBoxScoreJson = "imported-box-score"
        )
        val file = writeSnapshot("valid-import.json", snapshot)

        assertTrue(DataExporter.importGame(context, file.absolutePath))

        val slot = SaveSlotManager.getSlots(context).single { it.slotId == testSlot }
        assertTrue(slot.occupied)
        assertEquals(NbaDataGenerator.getAllTeams().first().name, slot.teamName)
        assertEquals(4, slot.seasonNumber)
        assertEquals(23, slot.currentDay)
        assertEquals(145_000_000, slot.budget)
        assertEquals(3, slot.difficulty)

        val loaded = repository.load()
        assertNotNull(loaded)
        assertEquals("imported-box-score", loaded!!.latestBoxScoreJson)
    }

    @Test
    fun incompleteImportIsRejectedBeforeMutatingExistingCareer() = runBlocking {
        val existing = snapshot(
            seasonNumber = 2,
            currentDay = 11,
            budget = 120_000_000,
            difficulty = 2,
            latestBoxScoreJson = "existing-box-score"
        )
        repository.save(existing)
        val existingTeam = gson.fromJson(existing.teamJson, NbaTeam::class.java)
        val existingSeason = gson.fromJson(existing.seasonJson, Season::class.java)
        val existingFinance = gson.fromJson(existing.financeJson, Finance::class.java)
        SaveSlotManager.updateSlot(context, testSlot, existingTeam, existingSeason, existingFinance, existing.difficulty)

        val incomplete = existing.copy(
            teamJson = null,
            seasonJson = null,
            latestBoxScoreJson = "invalid-replacement"
        )
        val file = writeSnapshot("incomplete-import.json", incomplete)

        assertFalse(DataExporter.importGame(context, file.absolutePath))

        val loaded = repository.load()
        assertNotNull(loaded)
        val loadedSeason = gson.fromJson(loaded!!.seasonJson, Season::class.java)
        assertEquals(2, loadedSeason.seasonNumber)
        assertEquals(11, loadedSeason.currentDay)
        assertEquals("existing-box-score", loaded.latestBoxScoreJson)

        val slot = SaveSlotManager.getSlots(context).single { it.slotId == testSlot }
        assertTrue(slot.occupied)
        assertEquals(2, slot.seasonNumber)
        assertEquals(11, slot.currentDay)
        assertEquals(120_000_000, slot.budget)
    }

    @Test
    fun detachedManagedTeamImportIsRejectedBeforeMutatingExistingCareer() = runBlocking {
        val existing = snapshot(
            seasonNumber = 2,
            currentDay = 11,
            budget = 120_000_000,
            difficulty = 2,
            latestBoxScoreJson = "existing-box-score"
        )
        repository.save(existing)
        val existingTeam = gson.fromJson(existing.teamJson, NbaTeam::class.java)
        val existingSeason = gson.fromJson(existing.seasonJson, Season::class.java)
        val existingFinance = gson.fromJson(existing.financeJson, Finance::class.java)
        SaveSlotManager.updateSlot(context, testSlot, existingTeam, existingSeason, existingFinance, existing.difficulty)

        val importedTeam = NbaDataGenerator.getAllTeams()[1]
        val inconsistent = existing.copy(
            teamJson = gson.toJson(importedTeam),
            seasonJson = gson.toJson(existingSeason.apply {
                teams = teams.filterNot { it.name == importedTeam.name }
                userTeamName = importedTeam.name
            }),
            latestBoxScoreJson = "invalid-detached-team"
        )
        val file = writeSnapshot("detached-team-import.json", inconsistent)

        assertFalse(DataExporter.importGame(context, file.absolutePath))

        val loaded = repository.load()
        assertNotNull(loaded)
        assertEquals(existing.teamJson, loaded!!.teamJson)
        assertEquals("existing-box-score", loaded.latestBoxScoreJson)

        val slot = SaveSlotManager.getSlots(context).single { it.slotId == testSlot }
        assertTrue(slot.occupied)
        assertEquals(existingTeam.name, slot.teamName)
        assertEquals(2, slot.seasonNumber)
        assertEquals(11, slot.currentDay)
        assertEquals(120_000_000, slot.budget)
    }

    private fun snapshot(
        seasonNumber: Int,
        currentDay: Int,
        budget: Int,
        difficulty: Int,
        latestBoxScoreJson: String?
    ): GameStateRepository.GameStateSnapshot {
        val teams = NbaDataGenerator.getAllTeams()
        val managed = teams.first()
        val season = Season(
            teams = teams,
            currentDay = currentDay,
            gamesPlayed = currentDay,
            seasonNumber = seasonNumber
        ).apply {
            userTeamName = managed.name
        }
        return GameStateRepository.GameStateSnapshot(
            teamJson = gson.toJson(managed),
            coachJson = gson.toJson(Coach(1, "Import Coach", 80, 80, 80, 350_000, 3)),
            financeJson = gson.toJson(Finance(budget)),
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
            latestBoxScoreJson = latestBoxScoreJson,
            playoffResultJson = null,
            difficulty = difficulty,
            injuriesEnabled = true,
            autoSubstitutionsEnabled = true
        )
    }

    private fun writeSnapshot(name: String, snapshot: GameStateRepository.GameStateSnapshot): File =
        File(context.cacheDir, name).apply { writeText(exportGson.toJson(snapshot)) }
}
