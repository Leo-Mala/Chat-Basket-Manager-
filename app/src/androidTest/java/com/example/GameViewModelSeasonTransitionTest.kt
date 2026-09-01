package com.example

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.NbaDataGenerator
import com.example.domain.rules.SavedGameLoadState
import com.example.models.MatchBoxScore
import com.example.models.Season
import com.example.models.TeamBoxScore
import com.example.utils.AutoSaveManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameViewModelSeasonTransitionTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var viewModel: GameViewModel

    @Before
    fun setUp() = runBlocking {
        AutoSaveManager.init(context)
        AutoSaveManager.clearGameState()
        viewModel = GameViewModel(context.applicationContext as Application)
        withTimeout(5_000) {
            while (viewModel.savedGameLoadState == SavedGameLoadState.LOADING) delay(20)
        }
    }

    @After
    fun tearDown() = runBlocking {
        viewModel.saveGame()?.join()
        AutoSaveManager.clearGameState()
    }

    @Test
    fun advancingSeasonClearsLatestBoxScoreFromMemoryAndPersistence() = runBlocking {
        val teams = NbaDataGenerator.getAllTeams()
        val managed = teams.first()
        viewModel.managedTeam = managed
        viewModel.season = Season(
            teams = teams,
            currentDay = 82,
            gamesPlayed = 82,
            seasonNumber = 1
        ).apply {
            userTeamName = managed.name
        }
        viewModel.startingFive = managed.players.take(5)
        viewModel.latestBoxScore = sampleBoxScore(managed.name, teams[1].name)

        viewModel.advanceToNextSeason()

        assertEquals(2, viewModel.season?.seasonNumber)
        assertNull("A previous-season box score must be cleared in memory", viewModel.latestBoxScore)

        viewModel.saveGame()?.join()
        val persisted = AutoSaveManager.loadGameState()
        assertNull("A previous-season box score must not be persisted into the new season", persisted?.latestBoxScoreJson)
    }

    private fun sampleBoxScore(home: String, away: String): MatchBoxScore {
        val homeTotals = TeamBoxScore(home, 101, 45, 25, 7, 4, 11, 18, 38, 80, 11, 32, 14, 18)
        val awayTotals = TeamBoxScore(away, 99, 43, 24, 8, 5, 12, 19, 37, 82, 10, 31, 15, 20)
        return MatchBoxScore(
            matchId = "season-one-last-game",
            dateString = "Temporada 1",
            homeTeamName = home,
            awayTeamName = away,
            homeScore = 101,
            awayScore = 99,
            homeQuarterScores = listOf(24, 27, 23, 27),
            awayQuarterScores = listOf(25, 22, 26, 26),
            homePlayers = emptyList(),
            awayPlayers = emptyList(),
            homeTeamTotals = homeTotals,
            awayTeamTotals = awayTotals,
            mvpPlayerName = null
        )
    }
}
