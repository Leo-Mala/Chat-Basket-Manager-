package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.NbaDataGenerator
import com.example.data.local.BasketDatabase
import com.example.data.repository.GameStateRepository
import com.example.models.Player
import com.example.models.Season
import com.example.simulator.GameSimulator
import com.example.utils.AutoSaveManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetachedGamePlayerPersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val gson = AutoSaveManager.gson
    private lateinit var database: BasketDatabase
    private lateinit var repository: GameStateRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, BasketDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = GameStateRepository(context, database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun detachedCurrentSeasonPlayerSurvivesCleanRoomRoundTrip() = runBlocking {
        val teams = NbaDataGenerator.getAllTeams()
        val managed = teams.first()
        val opponent = teams[1]
        val maxRosterId = teams.flatMap { it.players }.maxOf { it.id }
        val detached = managed.players.first().copy(
            id = maxRosterId + 100,
            name = "Departed Box Score Player"
        )
        val opponentPlayer = opponent.players.first()
        val season = Season(
            teams = teams,
            currentDay = 1,
            gamesPlayed = 1,
            seasonNumber = 1,
            nextPlayerId = detached.id + 1
        ).apply {
            userTeamName = managed.name
            history += GameSimulator.GameResult(
                homeTeam = managed,
                awayTeam = opponent,
                homeScore = 101,
                awayScore = 99,
                attendance = 18_000,
                homeStats = mapOf(detached to GameSimulator.PlayerStats(12, 3, 4, 1, 0, 2, 5)),
                awayStats = mapOf(opponentPlayer to GameSimulator.PlayerStats(9, 2, 1, 0, 0, 1, -5)),
                injuries = listOf(GameSimulator.Injury(detached, 4)),
                narration = "Detached player persistence fixture"
            )
        }
        val snapshot = GameStateRepository.GameStateSnapshot(
            teamJson = gson.toJson(managed),
            coachJson = null,
            financeJson = null,
            tacticsJson = null,
            seasonJson = gson.toJson(season),
            historyJson = null,
            awardsJson = null,
            startingFiveJson = gson.toJson(managed.players.take(5)),
            freeAgentsJson = gson.toJson(emptyList<Player>()),
            draftRookiesJson = gson.toJson(emptyList<Player>()),
            contractsJson = null,
            staffMarketJson = null,
            notificationsJson = null,
            teamStaffJson = null,
            facilitiesJson = null,
            financeAdvancedJson = null,
            newsFeedJson = null,
            latestBoxScoreJson = null,
            playoffResultJson = null,
            difficulty = 1,
            injuriesEnabled = true,
            autoSubstitutionsEnabled = true
        )

        repository.save(snapshot)

        val loaded = repository.load()
        assertNotNull(loaded)
        val loadedSeason = gson.fromJson(loaded!!.seasonJson, Season::class.java)
        val result = loadedSeason.history.single()
        assertTrue(result.homeStats.keys.any { it.id == detached.id && it.name == detached.name })
        assertEquals(12, result.homeStats.entries.single { it.key.id == detached.id }.value.points)
        assertTrue(result.injuries.any { it.player.id == detached.id && it.daysOut == 4 })
    }
}
