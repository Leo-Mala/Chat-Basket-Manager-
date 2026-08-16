package com.example

import com.example.domain.contract.ContractManager
import com.example.domain.season.UserRosterRecovery
import com.example.models.Arena
import com.example.models.HistoryManager
import com.example.models.NbaTeam
import com.example.models.Player
import com.example.models.SeasonHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserRosterRecoveryTest {
    private val contractManager = ContractManager()

    @Test
    fun fourDraftedPlayersRecoverPriorVeteransStillInFreeAgency() {
        val veterans = (1..8).map { player(it, "Veteran $it", age = 28, overall = 82 + (it % 4)) }
        val rookies = (101..104).map { player(it, "Rookie $it", age = 20, overall = 78 + (it % 5)) }
        val team = team(rookies)
        val history = HistoryManager().apply {
            addSeason(
                SeasonHistory(
                    seasonNumber = 4,
                    champion = team.name,
                    mvp = veterans.first().name,
                    finalScore = "4-2",
                    topScorer = veterans.first().name,
                    topScorerPoints = 25.0,
                    playerStats = veterans + rookies
                )
            )
        }
        val rookieContracts = rookies.associate { rookie ->
            rookie.id to contractManager.create(
                rookie,
                team.abbreviation,
                contractManager.recommendedOffer(rookie)
            )
        }

        val result = UserRosterRecovery(contractManager).recover(
            currentSeasonNumber = 5,
            currentDay = 0,
            team = team,
            history = history,
            freeAgents = veterans,
            contracts = rookieContracts
        )

        assertEquals(12, result.team.players.size)
        assertEquals(veterans.map { it.id }.toSet(), result.recoveredPlayerIds)
        assertTrue(veterans.all { veteran -> result.team.players.any { it.id == veteran.id } })
        assertTrue(veterans.all { it.id in result.contracts })
        assertFalse(result.freeAgents.any { it.id in result.recoveredPlayerIds })
    }

    @Test
    fun recoveryNeverStealsFormerPlayerWhoIsNoLongerAFreeAgent() {
        val veteran = player(1, "Signed Elsewhere", 29, 88)
        val rookies = (101..104).map { player(it, "Rookie $it", 20, 80) }
        val team = team(rookies)
        val history = HistoryManager().apply {
            addSeason(
                SeasonHistory(
                    seasonNumber = 4,
                    champion = "Other",
                    mvp = null,
                    finalScore = "4-3",
                    topScorer = veteran.name,
                    topScorerPoints = 20.0,
                    playerStats = listOf(veteran) + rookies
                )
            )
        }

        val result = UserRosterRecovery(contractManager).recover(
            currentSeasonNumber = 5,
            currentDay = 0,
            team = team,
            history = history,
            freeAgents = emptyList(),
            contracts = emptyMap()
        )

        assertTrue(result.recoveredPlayerIds.isEmpty())
        assertFalse(result.team.players.any { it.id == veteran.id })
    }

    private fun team(players: List<Player>) = NbaTeam(
        name = "Golden State Warriors",
        city = "San Francisco",
        abbreviation = "GSW",
        conference = "West",
        arena = Arena("Arena", "San Francisco", 18_000, 2019),
        players = players
    )

    private fun player(id: Int, name: String, age: Int, overall: Int) = Player(
        id = id,
        name = name,
        position = "SF",
        overall = overall,
        shooting = overall,
        defense = overall,
        rebound = overall,
        passing = overall,
        athleticism = overall,
        age = age
    )
}
