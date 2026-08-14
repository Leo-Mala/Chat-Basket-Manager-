package com.example

import com.example.data.NbaDataGenerator
import com.example.domain.contract.ContractManager
import com.example.domain.season.OffseasonManager
import com.example.domain.season.SeasonManager
import com.example.models.PlayerContract
import com.example.models.Season
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Endurance test for schedule, identity, contracts, rosters and offseason progression. */
class LongRunningCareerTest {
    @Test
    fun careerRemainsStructurallyValidForTwentyFiveSeasons() {
        val seedTeams = NbaDataGenerator.getAllTeams()
        var season = Season(
            teams = seedTeams,
            seasonNumber = 1,
            nextPlayerId = seedTeams.asSequence().flatMap { it.players.asSequence() }.maxOf { it.id } + 1
        )
        season.userTeamName = seedTeams.first().name

        val contractManager = ContractManager()
        val offseasonManager = OffseasonManager(contractManager)
        val seasonManager = SeasonManager()
        var contracts: Map<Int, PlayerContract> = seedTeams.flatMap { team ->
            team.players.map { player ->
                player.id to contractManager.create(player, team.abbreviation, contractManager.recommendedOffer(player))
            }
        }.toMap()
        var freeAgents = emptyList<com.example.models.Player>()

        repeat(25) {
            val allPlayers = season.teams.flatMap { it.players }
            assertEquals("duplicate active player IDs", allPlayers.size, allPlayers.map { it.id }.distinct().size)
            assertTrue("nextPlayerId must stay ahead of active IDs", season.nextPlayerId > (allPlayers.maxOfOrNull { it.id } ?: 0))
            assertTrue("each team must have a valid roster", season.teams.all { it.players.size in 0..18 })
            assertTrue("every roster player must have a contract", allPlayers.all { contracts.containsKey(it.id) })

            val appearances = mutableMapOf<String, Int>()
            val homes = mutableMapOf<String, Int>()
            repeat(82) { day ->
                val matchups = seasonManager.getMatchupsForDay(season, day)
                assertEquals(15, matchups.size)
                matchups.forEach { (home, away) ->
                    appearances[home.abbreviation] = appearances.getOrDefault(home.abbreviation, 0) + 1
                    appearances[away.abbreviation] = appearances.getOrDefault(away.abbreviation, 0) + 1
                    homes[home.abbreviation] = homes.getOrDefault(home.abbreviation, 0) + 1
                }
            }
            season.teams.forEach { team ->
                assertEquals("82 scheduled games for ${team.abbreviation}", 82, appearances[team.abbreviation])
                assertEquals("41 home games for ${team.abbreviation}", 41, homes[team.abbreviation])
            }

            val transition = offseasonManager.advance(season, contracts, freeAgents)
            season = transition.season
            contracts = transition.contracts
            freeAgents = transition.freeAgents
        }

        assertEquals(26, season.seasonNumber)
        val finalPlayers = season.teams.flatMap { it.players }
        assertEquals(finalPlayers.size, finalPlayers.map { it.id }.distinct().size)
        assertTrue(finalPlayers.all { contracts.containsKey(it.id) })
    }
}
