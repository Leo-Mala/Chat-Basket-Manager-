package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.NbaDataGenerator
import com.example.domain.season.SeasonManager
import com.example.models.Finance
import com.example.models.HistoryManager
import com.example.models.Season
import com.example.models.Tactics
import com.example.simulator.GameSimulator
import com.example.simulator.SimulationConfig
import com.example.utils.AutoSaveManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FastForwardCheckpointPersistenceTest {
    // Regression for real GameSimulator work interleaved with Room/Gson checkpoints.
    @Test
    fun realSimulationCanPersistCheckpointsBetweenBatches() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        AutoSaveManager.init(context)
        AutoSaveManager.clearGameState()

        val teams = NbaDataGenerator.getAllTeams()
        val managed = teams.first()
        val season = Season(
            teams = teams,
            nextPlayerId = teams.asSequence().flatMap { it.players.asSequence() }.maxOf { it.id } + 1
        ).apply { userTeamName = managed.name }
        val seasonManager = SeasonManager()
        val simulator = GameSimulator(
            context,
            SimulationConfig(injuriesEnabled = false, managedTeam = managed, effectsEnabled = false)
        )

        try {
            repeat(20) { day ->
                val matchups = seasonManager.getMatchupsForDay(season, day)
                assertEquals(15, matchups.size)
                matchups.forEach { (home, away) -> season.addResult(simulator.simulate(home, away)) }
                season.advanceDay()

                if (season.currentDay % 10 == 0) {
                    AutoSaveManager.saveGameState(
                        team = managed,
                        season = season,
                        finance = Finance(),
                        tactics = Tactics(),
                        coach = null,
                        history = HistoryManager()
                    )
                    val snapshot = AutoSaveManager.loadGameState()
                    assertNotNull(snapshot)
                    val restored = AutoSaveManager.gson.fromJson(snapshot!!.seasonJson, Season::class.java)
                    assertEquals(season.currentDay, restored.currentDay)
                    assertEquals(season.gamesPlayed, restored.gamesPlayed)
                }
            }
        } finally {
            simulator.release()
            AutoSaveManager.clearGameState()
        }

        assertEquals(20, season.currentDay)
        assertEquals(300, season.gamesPlayed)
    }
}
