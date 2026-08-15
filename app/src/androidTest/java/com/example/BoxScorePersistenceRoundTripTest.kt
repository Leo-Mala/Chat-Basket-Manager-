package com.example

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.NbaDataGenerator
import com.example.data.local.BasketDatabase
import com.example.data.repository.GameStateRepository
import com.example.models.*
import com.example.simulator.GameSimulator
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BoxScorePersistenceRoundTripTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val db = Room.inMemoryDatabaseBuilder(context, BasketDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val gson = GsonBuilder().enableComplexMapKeySerialization().create()
    private val repository = GameStateRepository(context, db)

    @After
    fun close() = db.close()

    @Test
    fun homeAndAwayPlayerStatsKeepTheirSideAfterRoomRoundTrip() = runBlocking {
        val teams = NbaDataGenerator.getAllTeams()
        val home = teams[0]
        val away = teams[1]
        val homePlayer = home.players.first()
        val awayPlayer = away.players.first()
        val season = Season(
            teams = teams,
            currentDay = 1,
            gamesPlayed = 1,
            seasonNumber = 1,
            nextPlayerId = teams.flatMap { it.players }.maxOf { it.id } + 1
        ).apply {
            userTeamName = home.name
            history += GameSimulator.GameResult(
                homeTeam = home,
                awayTeam = away,
                homeScore = 112,
                awayScore = 99,
                attendance = 18_000,
                homeStats = mapOf(homePlayer to GameSimulator.PlayerStats(31, 7, 8, 2, 1, 3, 14)),
                awayStats = mapOf(awayPlayer to GameSimulator.PlayerStats(19, 5, 4, 1, 0, 2, -14)),
                injuries = emptyList(),
                narration = "box-score-round-trip"
            )
        }

        repository.save(snapshot(season, home))
        val loaded = requireNotNull(repository.load())
        val loadedSeason = gson.fromJson(loaded.seasonJson, Season::class.java)
        val game = loadedSeason.history.single()

        assertEquals(setOf(homePlayer.id), game.homeStats.keys.map { it.id }.toSet())
        assertEquals(setOf(awayPlayer.id), game.awayStats.keys.map { it.id }.toSet())
        assertFalse(game.homeStats.keys.any { it.id == awayPlayer.id })
        assertFalse(game.awayStats.keys.any { it.id == homePlayer.id })
        assertEquals(31, game.homeStats.entries.single().value.points)
        assertEquals(19, game.awayStats.entries.single().value.points)

        val persisted = db.playerGameStatDao().all()
        assertTrue(persisted.any { it.playerId == homePlayer.id && it.teamId == home.abbreviation })
        assertTrue(persisted.any { it.playerId == awayPlayer.id && it.teamId == away.abbreviation })
    }

    private fun snapshot(season: Season, managed: NbaTeam) = GameStateRepository.GameStateSnapshot(
        teamJson = gson.toJson(managed),
        coachJson = gson.toJson(Coach(1, "Box Coach", 80, 80, 80, 350_000, 3)),
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
