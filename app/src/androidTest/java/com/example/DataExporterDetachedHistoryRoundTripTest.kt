package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.NbaDataGenerator
import com.example.data.local.BasketDatabase
import com.example.data.repository.GameStateRepository
import com.example.models.AssistantCoachNotification
import com.example.models.Coach
import com.example.models.Finance
import com.example.models.FinanceAdvanced
import com.example.models.HistoryManager
import com.example.models.News
import com.example.models.NbaTeam
import com.example.models.Player
import com.example.models.PlayerContract
import com.example.models.Season
import com.example.models.StaffMember
import com.example.models.Tactics
import com.example.models.TeamFacilities
import com.example.models.TeamStaff
import com.example.simulator.GameSimulator
import com.example.utils.AutoSaveManager
import com.example.utils.DataExporter
import com.example.utils.SaveSlotManager
import com.google.gson.reflect.TypeToken
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataExporterDetachedHistoryRoundTripTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val gson = AutoSaveManager.gson
    private val testSlot = 3
    private lateinit var repository: GameStateRepository
    private var originalSlot: Int = 1
    private var exportedFile: File? = null

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
        exportedFile?.delete()
    }

    @Test
    fun nativeExportWithDetachedHistoryPlayerCanBeImportedLosslessly() = runBlocking {
        val fixture = validSnapshotWithDetachedHistoryPlayer()
        repository.save(fixture.snapshot)

        val exportPath = DataExporter.exportCurrentGame(context)
        assertNotNull(exportPath)
        exportedFile = File(exportPath!!)

        repository.clear()
        assertTrue(DataExporter.importGame(context, exportPath))

        val loaded = repository.load()
        assertNotNull(loaded)
        val loadedSeason = gson.fromJson(loaded!!.seasonJson, Season::class.java)
        val result = loadedSeason.history.single()
        assertTrue(result.homeStats.keys.any { it.id == fixture.detached.id && it == fixture.detached })
        assertTrue(result.injuries.any { it.player.id == fixture.detached.id && it.player == fixture.detached })

        assertFalse(loadedSeason.teams.flatMap { it.players }.any { it.id == fixture.detached.id })
        val playerListType = object : TypeToken<List<Player>>() {}.type
        val freeAgents: List<Player> = gson.fromJson(loaded.freeAgentsJson, playerListType)
        val draftRookies: List<Player> = gson.fromJson(loaded.draftRookiesJson, playerListType)
        assertFalse(freeAgents.any { it.id == fixture.detached.id })
        assertFalse(draftRookies.any { it.id == fixture.detached.id })
    }

    private fun validSnapshotWithDetachedHistoryPlayer(): Fixture {
        val teams = NbaDataGenerator.getAllTeams()
        val managed = teams.first()
        val opponent = teams[1]
        val maxRosterId = teams.flatMap { it.players }.maxOf { it.id }
        val detached = managed.players.first().copy(
            id = maxRosterId + 100,
            name = "Detached Native Export Player"
        )
        val opponentPlayer = opponent.players.first()
        val season = Season(
            teams = teams,
            currentDay = 1,
            gamesPlayed = teams.size / 2,
            seasonNumber = 1,
            nextPlayerId = detached.id + 10
        ).apply {
            userTeamName = managed.name
            standings.values.forEachIndexed { index, record ->
                record.gamesPlayed = 1
                if (index % 2 == 0) record.wins = 1 else record.losses = 1
                record.totalPointsScored = 95
                record.totalPointsConceded = 95
            }
            history += GameSimulator.GameResult(
                homeTeam = managed,
                awayTeam = opponent,
                homeScore = 101,
                awayScore = 99,
                attendance = 18_000,
                homeStats = mapOf(detached to GameSimulator.PlayerStats(12, 3, 4, 1, 0, 2, 5)),
                awayStats = mapOf(opponentPlayer to GameSimulator.PlayerStats(9, 2, 1, 0, 0, 1, -5)),
                injuries = listOf(GameSimulator.Injury(detached, 4)),
                narration = "Detached native export fixture"
            )
        }
        val contracts = teams.flatMap { team ->
            team.players.map { player ->
                PlayerContract(
                    playerId = player.id,
                    teamId = team.abbreviation,
                    salary = 1_000_000L,
                    yearsRemaining = 2
                )
            }
        }
        val snapshot = GameStateRepository.GameStateSnapshot(
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
            contractsJson = gson.toJson(contracts),
            staffMarketJson = gson.toJson(emptyList<StaffMember>()),
            notificationsJson = gson.toJson(emptyList<AssistantCoachNotification>()),
            teamStaffJson = gson.toJson(TeamStaff()),
            facilitiesJson = gson.toJson(TeamFacilities()),
            financeAdvancedJson = gson.toJson(FinanceAdvanced()),
            newsFeedJson = gson.toJson(emptyList<News>()),
            latestBoxScoreJson = null,
            playoffResultJson = null,
            difficulty = 1,
            injuriesEnabled = true,
            autoSubstitutionsEnabled = true
        )
        return Fixture(snapshot, detached)
    }

    private data class Fixture(
        val snapshot: GameStateRepository.GameStateSnapshot,
        val detached: Player
    )
}
