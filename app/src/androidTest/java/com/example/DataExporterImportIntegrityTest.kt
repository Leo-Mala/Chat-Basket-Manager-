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
        val importedBoxScore = validBoxScoreJson("imported-box-score")
        val snapshot = snapshot(4, 23, 145_000_000, 3, importedBoxScore)
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
        assertEquals(importedBoxScore, loaded!!.latestBoxScoreJson)
    }

    @Test
    fun incompleteImportIsRejectedBeforeMutatingExistingCareer() = runBlocking {
        val existingBoxScore = validBoxScoreJson("existing-box-score")
        val existing = snapshot(2, 11, 120_000_000, 2, existingBoxScore)
        persistExisting(existing)

        val incomplete = existing.copy(
            teamJson = null,
            seasonJson = null,
            latestBoxScoreJson = validBoxScoreJson("invalid-replacement")
        )
        assertFalse(DataExporter.importGame(context, writeSnapshot("incomplete-import.json", incomplete).absolutePath))
        assertExistingUnchanged(existing, existingBoxScore)
    }

    @Test
    fun detachedManagedTeamImportIsRejectedBeforeMutatingExistingCareer() = runBlocking {
        val existingBoxScore = validBoxScoreJson("existing-box-score")
        val existing = snapshot(2, 11, 120_000_000, 2, existingBoxScore)
        persistExisting(existing)
        val existingSeason = gson.fromJson(existing.seasonJson, Season::class.java)
        val importedTeam = NbaDataGenerator.getAllTeams()[1]
        val inconsistent = existing.copy(
            teamJson = gson.toJson(importedTeam),
            seasonJson = gson.toJson(existingSeason.apply {
                teams = teams.filterNot { it.name == importedTeam.name }
                userTeamName = importedTeam.name
            }),
            latestBoxScoreJson = validBoxScoreJson("invalid-detached-team")
        )

        assertFalse(DataExporter.importGame(context, writeSnapshot("detached-team-import.json", inconsistent).absolutePath))
        assertExistingUnchanged(existing, existingBoxScore)
    }

    @Test
    fun malformedSecondaryJsonIsRejectedBeforeMutatingExistingCareer() = runBlocking {
        val existingBoxScore = validBoxScoreJson("existing-box-score")
        val existing = snapshot(2, 11, 120_000_000, 2, existingBoxScore)
        persistExisting(existing)
        val malformed = existing.copy(
            newsFeedJson = "{",
            latestBoxScoreJson = validBoxScoreJson("invalid-replacement")
        )

        assertFalse(DataExporter.importGame(context, writeSnapshot("malformed-secondary-import.json", malformed).absolutePath))
        assertExistingUnchanged(existing, existingBoxScore)
    }

    @Test
    fun wrongShapedSecondaryJsonIsRejectedBeforeMutatingExistingCareer() = runBlocking {
        val existingBoxScore = validBoxScoreJson("existing-box-score")
        val existing = snapshot(2, 11, 120_000_000, 2, existingBoxScore)
        persistExisting(existing)
        val wrongShaped = existing.copy(
            newsFeedJson = "\"corrupt\"",
            latestBoxScoreJson = validBoxScoreJson("invalid-replacement")
        )

        assertFalse(DataExporter.importGame(context, writeSnapshot("wrong-shaped-secondary-import.json", wrongShaped).absolutePath))
        assertExistingUnchanged(existing, existingBoxScore)
    }

    @Test
    fun incompleteBoxScoreIsRejectedBeforeMutatingExistingCareer() = runBlocking {
        val existingBoxScore = validBoxScoreJson("existing-box-score")
        val existing = snapshot(2, 11, 120_000_000, 2, existingBoxScore)
        persistExisting(existing)
        val incompleteBoxScore = existing.copy(latestBoxScoreJson = "{}")

        assertFalse(DataExporter.importGame(context, writeSnapshot("incomplete-box-score-import.json", incompleteBoxScore).absolutePath))
        assertExistingUnchanged(existing, existingBoxScore)
    }

    private suspend fun persistExisting(snapshot: GameStateRepository.GameStateSnapshot) {
        repository.save(snapshot)
        val team = gson.fromJson(snapshot.teamJson, NbaTeam::class.java)
        val season = gson.fromJson(snapshot.seasonJson, Season::class.java)
        val finance = gson.fromJson(snapshot.financeJson, Finance::class.java)
        SaveSlotManager.updateSlot(context, testSlot, team, season, finance, snapshot.difficulty)
    }

    private suspend fun assertExistingUnchanged(
        existing: GameStateRepository.GameStateSnapshot,
        existingBoxScore: String
    ) {
        val existingTeam = gson.fromJson(existing.teamJson, NbaTeam::class.java)
        val loaded = repository.load()
        assertNotNull(loaded)
        assertEquals(existing.teamJson, loaded!!.teamJson)
        assertEquals(existingBoxScore, loaded.latestBoxScoreJson)

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
        val nextPlayerId = teams.flatMap { it.players }.maxOf { it.id } + 1
        val season = Season(
            teams = teams,
            currentDay = currentDay,
            gamesPlayed = currentDay,
            seasonNumber = seasonNumber,
            nextPlayerId = nextPlayerId
        ).apply {
            userTeamName = managed.name
            standings.values.forEachIndexed { index, record ->
                record.gamesPlayed = currentDay
                if (index % 2 == 0) record.wins = currentDay else record.losses = currentDay
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
            coachJson = gson.toJson(Coach(1, "Import Coach", 80, 80, 80, 350_000, 3)),
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
            latestBoxScoreJson = latestBoxScoreJson,
            playoffResultJson = null,
            difficulty = difficulty,
            injuriesEnabled = true,
            autoSubstitutionsEnabled = true
        )
    }

    private fun validBoxScoreJson(matchId: String): String = gson.toJson(
        MatchBoxScore(
            matchId = matchId,
            dateString = "2026-09-01",
            homeTeamName = "Home",
            awayTeamName = "Away",
            homeScore = 0,
            awayScore = 0,
            homeQuarterScores = listOf(0, 0, 0, 0),
            awayQuarterScores = listOf(0, 0, 0, 0),
            homePlayers = emptyList(),
            awayPlayers = emptyList(),
            homeTeamTotals = TeamBoxScore(
                teamName = "Home", points = 0, rebounds = 0, assists = 0, steals = 0, blocks = 0,
                turnovers = 0, fouls = 0, fgMade = 0, fgAttempted = 0, threeMade = 0,
                threeAttempted = 0, ftMade = 0, ftAttempted = 0
            ),
            awayTeamTotals = TeamBoxScore(
                teamName = "Away", points = 0, rebounds = 0, assists = 0, steals = 0, blocks = 0,
                turnovers = 0, fouls = 0, fgMade = 0, fgAttempted = 0, threeMade = 0,
                threeAttempted = 0, ftMade = 0, ftAttempted = 0
            )
        )
    )

    private fun writeSnapshot(name: String, snapshot: GameStateRepository.GameStateSnapshot): File =
        File(context.cacheDir, name).apply { writeText(exportGson.toJson(snapshot)) }
}
