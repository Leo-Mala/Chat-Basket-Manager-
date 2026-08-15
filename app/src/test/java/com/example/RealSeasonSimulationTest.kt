package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.NbaDataGenerator
import com.example.domain.season.SeasonManager
import com.example.models.Season
import com.example.simulator.GameSimulator
import com.example.simulator.SimulationConfig
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RealSeasonSimulationTest {
    @Test(timeout = 20_000)
    fun realSimulatorCompletesFullRegularSeasonWithoutUiEffects() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val teams = NbaDataGenerator.getAllTeams()
        val managed = teams.first()
        val season = Season(
            teams = teams,
            nextPlayerId = teams.asSequence().flatMap { it.players.asSequence() }.maxOf { it.id } + 1
        ).apply { userTeamName = managed.name }
        val seasonManager = SeasonManager()
        val simulator = GameSimulator(
            context,
            SimulationConfig(
                injuriesEnabled = false,
                managedTeam = managed,
                effectsEnabled = false
            )
        )

        try {
            repeat(82) { day ->
                val matchups = seasonManager.getMatchupsForDay(season, day)
                assertEquals(15, matchups.size)
                matchups.forEach { (home, away) ->
                    season.addResult(simulator.simulate(home, away))
                }
                season.advanceDay()
            }
        } finally {
            simulator.release()
        }

        assertEquals(82, season.currentDay)
        assertEquals(1_230, season.gamesPlayed)
        assertEquals(82, season.history.size)
        season.standings.values.forEach { record ->
            assertEquals(82, record.gamesPlayed)
            assertEquals(82, record.wins + record.losses)
        }
    }
}
