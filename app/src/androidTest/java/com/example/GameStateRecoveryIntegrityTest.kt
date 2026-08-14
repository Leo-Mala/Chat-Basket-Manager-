package com.example

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.NbaDataGenerator
import com.example.data.local.BasketDatabase
import com.example.data.local.SeasonEntity
import com.example.data.local.TeamEntity
import com.example.data.repository.GameStateRepository
import com.example.models.*
import com.example.simulator.GameSimulator
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameStateRecoveryIntegrityTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val db = Room.inMemoryDatabaseBuilder(context, BasketDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val gson = GsonBuilder().enableComplexMapKeySerialization().create()
    private val repository = GameStateRepository(context, db)

    @After
    fun close() = db.close()

    @Test
    fun savingShorterSnapshotReplacesCurrentSeasonRowsInsteadOfResurrectingOldGames() = runBlocking {
        val teams = NbaDataGenerator.getAllTeams()
        val managed = teams.first()
        val opponent = teams[1]
        val season = Season(teams, seasonNumber = 1, nextPlayerId = nextId(teams)).apply {
            userTeamName = managed.name
            addResult(game(managed, opponent, 110, 101))
            currentDay = 1
        }

        repository.save(snapshot(season))
        assertEquals(1, db.gameDao().all().count { it.seasonId == 1 })

        val rolledBack = Season(teams, currentDay = 0, gamesPlayed = 0, seasonNumber = 1, nextPlayerId = season.nextPlayerId).apply {
            userTeamName = managed.name
        }
        repository.save(snapshot(rolledBack))

        val loaded = repository.load()
        assertNotNull(loaded)
        val loadedSeason = gson.fromJson(loaded!!.seasonJson, Season::class.java)
        assertEquals(0, loadedSeason.currentDay)
        assertEquals(0, loadedSeason.gamesPlayed)
        assertTrue(loadedSeason.history.isEmpty())
        assertTrue(db.gameDao().all().none { it.seasonId == 1 })
        assertTrue(db.playerGameStatDao().all().isEmpty())
        assertTrue(db.gameInjuryDao().all().isEmpty())
        assertTrue(loadedSeason.standings.values.all { it.gamesPlayed == 0 && it.wins == 0 && it.losses == 0 })
    }

    @Test
    fun malformedReplacementSaveRollsBackAndKeepsLastGoodSnapshot() = runBlocking {
        val teams = NbaDataGenerator.getAllTeams()
        val managed = teams.first()
        val season = Season(teams, currentDay = 9, gamesPlayed = 0, seasonNumber = 1, nextPlayerId = nextId(teams)).apply {
            userTeamName = managed.name
        }
        val good = snapshot(season)
        repository.save(good)

        try {
            repository.save(good.copy(seasonJson = "{ definitely-not-valid-json"))
            fail("Malformed save must fail instead of replacing the last valid snapshot")
        } catch (_: Exception) {
            // Expected: parsing fails inside the Room transaction.
        }

        val loaded = repository.load()
        assertNotNull(loaded)
        val loadedSeason = gson.fromJson(loaded!!.seasonJson, Season::class.java)
        assertEquals(9, loadedSeason.currentDay)
        assertEquals(1, loadedSeason.seasonNumber)
        assertEquals(30, db.teamDao().all().size)
    }

    @Test
    fun clearIsAuthoritativeAndLeavesNoRecoverableCareerRows() = runBlocking {
        val teams = NbaDataGenerator.getAllTeams()
        val season = Season(teams, currentDay = 20, seasonNumber = 1, nextPlayerId = nextId(teams)).apply {
            userTeamName = teams.first().name
        }
        repository.save(snapshot(season))

        repository.clear()

        assertNull(repository.load())
        assertTrue(db.teamDao().all().isEmpty())
        assertTrue(db.playerDao().all().isEmpty())
        assertTrue(db.seasonDao().all().isEmpty())
        assertTrue(db.standingDao().all().isEmpty())
        assertTrue(db.gameDao().all().isEmpty())
        assertTrue(db.contractDao().all().isEmpty())
        assertTrue(db.seasonHistoryDao().all().isEmpty())
    }

    @Test
    fun partialNormalizedDatabaseIsRejectedInsteadOfLoadedAsAValidCareer() = runBlocking {
        db.teamDao().upsertAll(
            listOf(
                TeamEntity(
                    id = "TST",
                    name = "Test Team",
                    city = "Test City",
                    abbreviation = "TST",
                    conference = "East",
                    arenaName = "Test Arena",
                    arenaCity = "Test City",
                    arenaCapacity = 10000,
                    arenaOpened = 2020
                )
            )
        )
        db.seasonDao().upsert(
            SeasonEntity(
                id = 1,
                currentDay = 5,
                gamesPlayed = 5,
                seasonNumber = 1,
                currentMonth = 10,
                currentYear = 2025,
                userTeamId = "TST",
                nextPlayerId = 100
            )
        )

        try {
            repository.load()
            fail("Partial normalized data must be rejected")
        } catch (e: IllegalStateException) {
            assertTrue(e.message.orEmpty().contains("standings"))
        }
    }

    private fun snapshot(season: Season): GameStateRepository.GameStateSnapshot {
        val managed = season.teams.first { it.name == season.userTeamName }
        return GameStateRepository.GameStateSnapshot(
            teamJson = gson.toJson(managed),
            coachJson = gson.toJson(Coach(1, "Recovery Coach", 75, 76, 77, 300_000, 2)),
            financeJson = gson.toJson(Finance(90_000_000)),
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
            playoffResultJson = "recovery-phase-marker",
            difficulty = 2,
            injuriesEnabled = true,
            autoSubstitutionsEnabled = true
        )
    }

    private fun game(home: NbaTeam, away: NbaTeam, homeScore: Int, awayScore: Int) =
        GameSimulator.GameResult(
            homeTeam = home,
            awayTeam = away,
            homeScore = homeScore,
            awayScore = awayScore,
            attendance = 18000,
            homeStats = emptyMap(),
            awayStats = emptyMap(),
            injuries = emptyList(),
            narration = "Recovery test game"
        )

    private fun nextId(teams: List<NbaTeam>) = teams.flatMap { it.players }.maxOf { it.id } + 1
}
