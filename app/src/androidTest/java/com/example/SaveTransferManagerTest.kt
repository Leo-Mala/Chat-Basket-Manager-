package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.NbaDataGenerator
import com.example.data.local.BasketDatabase
import com.example.data.repository.GameStateRepository
import com.example.models.*
import com.example.simulator.GameSimulator
import com.example.utils.AutoSaveManager
import com.example.utils.SaveSlotManager
import com.example.utils.SaveTransferManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class SaveTransferManagerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val gson = AutoSaveManager.gson
    private val sourceSlot = 2
    private val targetSlot = 3
    private lateinit var sourceRepository: GameStateRepository
    private lateinit var targetRepository: GameStateRepository
    private var originalSlot: Int = 1

    @Before
    fun setUp() = runBlocking {
        originalSlot = SaveSlotManager.getActiveSlot(context)
        SaveSlotManager.clearPendingNewSlot(context)
        sourceRepository = GameStateRepository(context, BasketDatabase.getInstance(context, sourceSlot))
        targetRepository = GameStateRepository(context, BasketDatabase.getInstance(context, targetSlot))
        sourceRepository.clear()
        targetRepository.clear()
        SaveSlotManager.clearSlotMetadata(context, sourceSlot)
        SaveSlotManager.clearSlotMetadata(context, targetSlot)
        SaveSlotManager.setActiveSlot(context, sourceSlot)
    }

    @After
    fun tearDown() = runBlocking {
        sourceRepository.clear()
        targetRepository.clear()
        SaveSlotManager.clearSlotMetadata(context, sourceSlot)
        SaveSlotManager.clearSlotMetadata(context, targetSlot)
        SaveSlotManager.clearPendingNewSlot(context)
        SaveSlotManager.setActiveSlot(context, originalSlot)
    }

    @Test
    fun exportedCareerCanBeImportedIntoAnotherPhysicalSlot() = runBlocking {
        val source = snapshot(teamIndex = 0, seasonNumber = 4, currentDay = 17, budget = 135_000_000)
        persist(sourceRepository, sourceSlot, source)

        val output = ByteArrayOutputStream()
        assertTrue(SaveTransferManager.exportActiveSlot(context, output))
        assertTrue(output.size() > 0)

        assertTrue(
            SaveTransferManager.importIntoSlot(
                context,
                ByteArrayInputStream(output.toByteArray()),
                targetSlot
            )
        )
        assertEquals(targetSlot, SaveSlotManager.getActiveSlot(context))

        val sourceLoaded = sourceRepository.load()
        val targetLoaded = targetRepository.load()
        assertNotNull(sourceLoaded)
        assertNotNull(targetLoaded)
        assertEquals(sourceLoaded!!.teamJson, targetLoaded!!.teamJson)
        assertSeasonContentEquals(sourceLoaded.seasonJson, targetLoaded.seasonJson)
        assertEquals(sourceLoaded.financeJson, targetLoaded.financeJson)

        val targetMetadata = SaveSlotManager.getSlots(context).single { it.slotId == targetSlot }
        assertTrue(targetMetadata.occupied)
        assertEquals(NbaDataGenerator.getAllTeams().first().name, targetMetadata.teamName)
        assertEquals(4, targetMetadata.seasonNumber)
        assertEquals(17, targetMetadata.currentDay)
    }

    @Test
    fun rejectedImportRestoresPreviousActiveSlotAndPreservesTargetCareer() = runBlocking {
        val target = snapshot(teamIndex = 1, seasonNumber = 3, currentDay = 9, budget = 111_000_000)
        persist(targetRepository, targetSlot, target)
        val canonicalTargetBeforeImport = targetRepository.load()
        assertNotNull(canonicalTargetBeforeImport)
        SaveSlotManager.setActiveSlot(context, sourceSlot)

        assertFalse(
            SaveTransferManager.importIntoSlot(
                context,
                ByteArrayInputStream("{not-valid-json".toByteArray()),
                targetSlot
            )
        )
        assertEquals(sourceSlot, SaveSlotManager.getActiveSlot(context))

        val loadedTarget = targetRepository.load()
        assertNotNull(loadedTarget)
        assertEquals(canonicalTargetBeforeImport!!.teamJson, loadedTarget!!.teamJson)
        assertEquals(canonicalTargetBeforeImport.seasonJson, loadedTarget.seasonJson)
        assertEquals(canonicalTargetBeforeImport.financeJson, loadedTarget.financeJson)

        val metadata = SaveSlotManager.getSlots(context).single { it.slotId == targetSlot }
        assertTrue(metadata.occupied)
        assertEquals(NbaDataGenerator.getAllTeams()[1].name, metadata.teamName)
        assertEquals(3, metadata.seasonNumber)
        assertEquals(9, metadata.currentDay)
    }

    private fun assertSeasonContentEquals(expectedJson: String?, actualJson: String?) {
        val expected = gson.fromJson(expectedJson, Season::class.java)
        val actual = gson.fromJson(actualJson, Season::class.java)

        assertEquals(expected.currentDay, actual.currentDay)
        assertEquals(expected.gamesPlayed, actual.gamesPlayed)
        assertEquals(expected.seasonNumber, actual.seasonNumber)
        assertEquals(expected.nextPlayerId, actual.nextPlayerId)
        assertEquals(expected.userTeamName, actual.userTeamName)
        assertEquals(
            expected.teams.map { team -> team.name to team.players.map { it.id } },
            actual.teams.map { team -> team.name to team.players.map { it.id } }
        )
        assertEquals(gson.toJson(expected.standings), gson.toJson(actual.standings))
        assertEquals(historySignature(expected), historySignature(actual))
    }

    private fun historySignature(season: Season): List<List<Any>> = season.history.map { game ->
        listOf(
            game.homeTeam.name,
            game.awayTeam.name,
            game.homeScore,
            game.awayScore,
            game.attendance,
            game.narration,
            game.homeStats.entries
                .sortedBy { it.key.id }
                .map { it.key.id to it.value },
            game.awayStats.entries
                .sortedBy { it.key.id }
                .map { it.key.id to it.value },
            game.injuries.map { it.player.id to it.daysOut }
        )
    }

    private suspend fun persist(
        repository: GameStateRepository,
        slotId: Int,
        snapshot: GameStateRepository.GameStateSnapshot
    ) {
        repository.save(snapshot)
        val team = gson.fromJson(snapshot.teamJson, NbaTeam::class.java)
        val season = gson.fromJson(snapshot.seasonJson, Season::class.java)
        val finance = gson.fromJson(snapshot.financeJson, Finance::class.java)
        SaveSlotManager.updateSlot(context, slotId, team, season, finance, snapshot.difficulty)
    }

    private fun snapshot(
        teamIndex: Int,
        seasonNumber: Int,
        currentDay: Int,
        budget: Int
    ): GameStateRepository.GameStateSnapshot {
        val teams = NbaDataGenerator.getAllTeams()
        val managed = teams[teamIndex]
        val nextPlayerId = teams.flatMap { it.players }.maxOf { it.id } + 1
        val season = Season(
            teams = teams,
            currentDay = currentDay,
            gamesPlayed = currentDay * (teams.size / 2),
            seasonNumber = seasonNumber,
            nextPlayerId = nextPlayerId
        ).apply {
            userTeamName = managed.name
            standings.values.forEachIndexed { index, record ->
                record.gamesPlayed = currentDay
                if (index % 2 == 0) record.wins = currentDay else record.losses = currentDay
                record.totalPointsScored = currentDay * 95
                record.totalPointsConceded = currentDay * 95
            }
            repeat(currentDay) { dayIndex ->
                val opponent = teams[(teamIndex + 1 + dayIndex) % teams.size].let {
                    if (it.name == managed.name) teams[(teamIndex + 2 + dayIndex) % teams.size] else it
                }
                val homePlayer = managed.players.first()
                val awayPlayer = opponent.players.first()
                history += GameSimulator.GameResult(
                    homeTeam = managed,
                    awayTeam = opponent,
                    homeScore = 100,
                    awayScore = 90,
                    attendance = 15_000,
                    homeStats = mapOf(homePlayer to GameSimulator.PlayerStats(100, 0, 0, 0, 0, 0, 10)),
                    awayStats = mapOf(awayPlayer to GameSimulator.PlayerStats(90, 0, 0, 0, 0, 0, -10)),
                    injuries = emptyList(),
                    narration = "Transfer fixture day ${dayIndex + 1}"
                )
            }
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

        return GameStateRepository.GameStateSnapshot(
            teamJson = gson.toJson(managed),
            coachJson = gson.toJson(Coach(1, "Transfer Coach", 80, 80, 80, 350_000, 3)),
            financeJson = gson.toJson(Finance(budget)),
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
            difficulty = 2,
            injuriesEnabled = true,
            autoSubstitutionsEnabled = true
        )
    }
}
