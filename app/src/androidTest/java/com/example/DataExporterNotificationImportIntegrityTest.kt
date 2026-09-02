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
class DataExporterNotificationImportIntegrityTest {
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
    fun incompleteNotificationIsRejectedBeforeMutatingExistingCareer() = runBlocking {
        val existing = snapshot()
        repository.save(existing)
        val existingTeam = gson.fromJson(existing.teamJson, NbaTeam::class.java)
        val existingSeason = gson.fromJson(existing.seasonJson, Season::class.java)
        val existingFinance = gson.fromJson(existing.financeJson, Finance::class.java)
        SaveSlotManager.updateSlot(context, testSlot, existingTeam, existingSeason, existingFinance, existing.difficulty)

        val invalid = existing.copy(notificationsJson = "[{}]")
        val file = File(context.cacheDir, "incomplete-notification-import.json").apply {
            writeText(exportGson.toJson(invalid))
        }

        assertFalse(DataExporter.importGame(context, file.absolutePath))

        val loaded = repository.load()
        assertNotNull(loaded)
        assertEquals(existing.teamJson, loaded!!.teamJson)
        assertEquals(existing.notificationsJson, loaded.notificationsJson)

        val slot = SaveSlotManager.getSlots(context).single { it.slotId == testSlot }
        assertTrue(slot.occupied)
        assertEquals(existingTeam.name, slot.teamName)
        assertEquals(existingSeason.seasonNumber, slot.seasonNumber)
        assertEquals(existingSeason.currentDay, slot.currentDay)
        assertEquals(existingFinance.budget, slot.budget)
    }

    private fun snapshot(): GameStateRepository.GameStateSnapshot {
        val teams = NbaDataGenerator.getAllTeams()
        val managed = teams.first()
        val season = Season(
            teams = teams,
            currentDay = 11,
            gamesPlayed = 11,
            seasonNumber = 2
        ).apply {
            userTeamName = managed.name
        }
        return GameStateRepository.GameStateSnapshot(
            teamJson = gson.toJson(managed),
            coachJson = gson.toJson(Coach(1, "Import Coach", 80, 80, 80, 350_000, 3)),
            financeJson = gson.toJson(Finance(120_000_000)),
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
    }
}
