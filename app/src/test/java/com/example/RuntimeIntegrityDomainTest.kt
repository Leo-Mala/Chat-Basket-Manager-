package com.example

import com.example.data.NbaDataGenerator
import com.example.domain.draft.DraftManager
import com.example.domain.roster.RosterManager
import com.example.domain.season.SeasonManager
import com.example.models.Finance
import com.example.models.Season
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeIntegrityDomainTest {
    @Test
    fun releasedPlayersRemainIdentifiableForFreeAgency() {
        val teams = NbaDataGenerator.getAllTeams()
        val team = teams.first()
        val season = Season(teams, nextPlayerId = 10_000)
        val rookie = DraftManager().generateClass(season, emptyList(), 1, size = 1).first()
        val draftResult = DraftManager().draft(team, rookie, maxRosterSize = team.players.size)
        assertNotNull(draftResult.releasedPlayer)
        assertTrue(draftResult.team.players.none { it.id == draftResult.releasedPlayer!!.id })

        // Free agency only requires a release at the standard 12-player roster threshold.
        // Build that state explicitly instead of assuming the seed roster size.
        val fullRoster = teams.flatMap { it.players }.distinctBy { it.id }.take(12)
        val fullTeam = team.copy(players = fullRoster)
        val freeAgent = RosterManager().generateFreeAgents(season, emptyList()).players.first()
        val signing = RosterManager().signFreeAgent(fullTeam, Finance(Int.MAX_VALUE), freeAgent, 1)
        assertNotNull(signing)
        assertNotNull(signing!!.releasedPlayer)
        assertTrue(signing.team.players.none { it.id == signing.releasedPlayer!!.id })
        assertEquals(12, signing.team.players.size)
    }

    @Test
    fun eightyTwoGameScheduleBalancesHomeAndAway() {
        val season = Season(NbaDataGenerator.getAllTeams())
        val manager = SeasonManager()
        val homeCounts = mutableMapOf<String, Int>()
        val awayCounts = mutableMapOf<String, Int>()

        repeat(82) { day ->
            val matchups = manager.getMatchupsForDay(season, day)
            assertEquals(15, matchups.size)
            matchups.forEach { (home, away) ->
                homeCounts[home.abbreviation] = homeCounts.getOrDefault(home.abbreviation, 0) + 1
                awayCounts[away.abbreviation] = awayCounts.getOrDefault(away.abbreviation, 0) + 1
            }
        }

        season.teams.forEach { team ->
            assertEquals("home games for ${team.abbreviation}", 41, homeCounts[team.abbreviation])
            assertEquals("away games for ${team.abbreviation}", 41, awayCounts[team.abbreviation])
        }
    }
}
