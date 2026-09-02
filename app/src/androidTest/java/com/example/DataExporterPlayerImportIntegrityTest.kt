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
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataExporterPlayerImportIntegrityTest {
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
    fun incompleteFreeAgentAndDraftPlayerImportsAreRejectedAtomically() = runBlocking {
        val existing = snapshot(seasonNumber = 2, currentDay = 11, budget = 120_000_000, difficulty = 2)
        repository.save(existing)
        val existingTeam = gson.fromJson(existing.teamJson, NbaTeam::class.java)
        val existingSeason = gson.fromJson(existing.seasonJson, Season::class.java)
        val existingFinance = gson.fromJson(existing.financeJson, Finance::class.java)
        SaveSlotManager.updateSlot(context, testSlot, existingTeam, existingSeason, existingFinance, existing.difficulty)

        val incompletePlayer = "[{\"id\":999,\"name\":\"X\",\"position\":\"PG\"}]"
        val invalidSnapshots = listOf(
            "incomplete-free-agent.json" to existing.copy(freeAgentsJson = incompletePlayer),
            "incomplete-draft-rookie.json" to existing.copy(draftRookiesJson = incompletePlayer)
        )

        invalidSnapshots.forEach { (name, invalid) ->
            val file = File(context.cacheDir, name).apply { writeText(exportGson.toJson(invalid)) }
            assertFalse(DataExporter.importGame(context, file.absolutePath))

            val loaded = repository.load()
            assertNotNull(loaded)
            assertEquals(existing.teamJson, loaded!!.teamJson)
            assertEquals(existing.freeAgentsJson, loaded.freeAgentsJson)
            assertEquals(existing.draftRookiesJson, loaded.draftRookiesJson)

            val slot = SaveSlotManager.getSlots(context).single { it.slotId == testSlot }
            assertTrue(slot.occupied)
            assertEquals(existingTeam.name, slot.teamName)
            assertEquals(2, slot.seasonNumber)
            assertEquals(11, slot.currentDay)
            assertEquals(120_000_000, slot.budget)
        }
    }

    private fun snapshot(
        seasonNumber: Int,
        currentDay: Int,
        budget: Int,
        difficulty: Int
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
            latestBoxScoreJson = null,
            playoffResultJson = null,
            difficulty = difficulty,
            injuriesEnabled = true,
            autoSubstitutionsEnabled = true
        )
    }
}
