package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.NbaDataGenerator
import com.example.models.Season
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PostseasonIntegrityTest {
    @Test
    fun playoffSeriesDoesNotMutateRegularSeasonStandings() {
        val teams = NbaDataGenerator.getAllTeams()
        val season = Season(teams)
        season.userTeamName = teams.first().name
        season.standings[teams[0].name]?.apply { wins = 50; losses = 32; gamesPlayed = 82 }
        season.standings[teams[1].name]?.apply { wins = 48; losses = 34; gamesPlayed = 82 }
        val before0 = season.standings[teams[0].name]!!.copy()
        val before1 = season.standings[teams[1].name]!!.copy()
        val leagueGamesBefore = season.gamesPlayed
        val context = ApplicationProvider.getApplicationContext<Context>()

        season.simulateSeries(teams[0], teams[1], "Teste", context)

        assertEquals(before0, season.standings[teams[0].name])
        assertEquals(before1, season.standings[teams[1].name])
        assertEquals(leagueGamesBefore, season.gamesPlayed)
    }
}
