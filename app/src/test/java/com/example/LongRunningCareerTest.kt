package com.example

import com.example.data.NbaDataGenerator
import com.example.models.Season
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Endurance test for identity, roster size and season progression over a long career. */
class LongRunningCareerTest {
    @Test
    fun careerRemainsStructurallyValidForTwentyFiveSeasons() {
        var season = Season(
            teams = NbaDataGenerator.getAllTeams(),
            seasonNumber = 1,
            nextPlayerId = NbaDataGenerator.getAllTeams()
                .asSequence().flatMap { it.players.asSequence() }.maxOf { it.id } + 1
        )

        repeat(25) {
            val allPlayers = season.teams.flatMap { it.players }
            assertEquals("duplicate active player IDs", allPlayers.size, allPlayers.map { it.id }.distinct().size)
            assertTrue("nextPlayerId must stay ahead of active IDs", season.nextPlayerId > (allPlayers.maxOfOrNull { it.id } ?: 0))
            assertTrue("each team must have a valid roster", season.teams.all { it.players.size in 0..18 })
            season = season.advanceSeason()
        }

        assertEquals(26, season.seasonNumber)
    }
}
