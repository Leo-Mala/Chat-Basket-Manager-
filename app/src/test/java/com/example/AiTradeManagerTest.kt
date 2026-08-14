package com.example

import com.example.domain.season.OffseasonManager
import com.example.domain.trade.AiTradeManager
import com.example.models.Arena
import com.example.models.NbaTeam
import com.example.models.Player
import com.example.models.PlayerContract
import com.example.models.Season
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiTradeManagerTest {

    @Test
    fun cpuTeamsTradeForMutualPositionNeedWithoutTouchingUserTeam() {
        val user = balancedTeam("User", "USR", 1)
        val teamA = pgWeakTeam("A", "AAA", 100)
        val teamB = centerWeakTeam("B", "BBB", 200)
        val userIds = user.players.map { it.id }
        val contracts = contractsFor(listOf(user, teamA, teamB))
        val aCenter = teamA.players.single { it.id == 109 }
        val bPointGuard = teamB.players.single { it.id == 203 }

        val result = AiTradeManager().rebalance(
            teams = listOf(user, teamA, teamB),
            contracts = contracts,
            userTeamName = user.name,
            priorityTeamNames = listOf(teamA.name, teamB.name),
            maxTrades = 1
        )

        assertEquals(1, result.trades.size)
        val trade = result.trades.single()
        assertEquals(teamA.name, trade.teamA)
        assertEquals(teamB.name, trade.teamB)
        assertEquals(aCenter.id, trade.playerFromAId)
        assertEquals(bPointGuard.id, trade.playerFromBId)
        assertEquals(userIds, result.teams.first { it.name == user.name }.players.map { it.id })
        assertTrue(result.teams.first { it.name == teamA.name }.players.any { it.id == bPointGuard.id })
        assertTrue(result.teams.first { it.name == teamB.name }.players.any { it.id == aCenter.id })
        assertEquals(36, result.teams.flatMap { it.players }.map { it.id }.distinct().size)
    }

    @Test
    fun noTradeClauseBlocksCpuDeal() {
        val teamA = pgWeakTeam("A", "AAA", 100)
        val teamB = centerWeakTeam("B", "BBB", 200)
        val contracts = contractsFor(listOf(teamA, teamB)).toMutableMap()
        teamB.players.filter { it.position == "PG" }.forEach { player ->
            contracts[player.id] = contracts.getValue(player.id).copy(noTrade = true)
        }

        val result = AiTradeManager().rebalance(
            teams = listOf(teamA, teamB),
            contracts = contracts,
            userTeamName = null,
            priorityTeamNames = listOf(teamA.name, teamB.name),
            maxTrades = 1
        )

        assertTrue(result.trades.isEmpty())
    }

    @Test
    fun cpuRejectsTradeWhenOverallGapExceedsConservativeLimit() {
        val teamA = pgWeakTeam("A", "AAA", 100).let { team ->
            team.copy(players = team.players.map { player ->
                if (player.id == 109) boosted(player, 86) else player
            })
        }
        val teamB = centerWeakTeam("B", "BBB", 200)
        val contracts = contractsFor(listOf(teamA, teamB))

        val result = AiTradeManager().rebalance(
            teams = listOf(teamA, teamB),
            contracts = contracts,
            userTeamName = null,
            priorityTeamNames = listOf(teamA.name, teamB.name),
            maxTrades = 1,
            maxOverallDifference = 3
        )

        assertTrue(result.trades.isEmpty())
    }

    @Test
    fun offseasonTradeMovesContractsToNewCpuTeams() {
        val user = balancedTeam("User", "USR", 1)
        val teamA = pgWeakTeam("A", "AAA", 100)
        val teamB = centerWeakTeam("B", "BBB", 200)
        val aCenterId = 109
        val bPointGuardId = 203
        val contracts = contractsFor(listOf(user, teamA, teamB))
        val season = Season(
            teams = listOf(user, teamA, teamB),
            nextPlayerId = 1_000
        ).apply {
            userTeamName = user.name
            standings.getValue(user.name).wins = 40
            standings.getValue(teamA.name).wins = 10
            standings.getValue(teamB.name).wins = 20
        }
        val userIds = user.players.map { it.id }.toSet()

        val result = OffseasonManager().advance(
            currentSeason = season,
            currentContracts = contracts,
            currentFreeAgents = emptyList()
        )

        val nextUser = result.season.teams.first { it.name == user.name }
        val nextA = result.season.teams.first { it.name == teamA.name }
        val nextB = result.season.teams.first { it.name == teamB.name }

        assertTrue(nextUser.players.map { it.id }.containsAll(userIds))
        assertTrue(nextA.players.any { it.id == bPointGuardId })
        assertTrue(nextB.players.any { it.id == aCenterId })
        assertEquals(teamA.abbreviation, result.contracts.getValue(bPointGuardId).teamId)
        assertEquals(teamB.abbreviation, result.contracts.getValue(aCenterId).teamId)
        assertFalse(result.contracts.getValue(bPointGuardId).noTrade)
        assertFalse(result.contracts.getValue(aCenterId).noTrade)
    }

    private fun contractsFor(teams: List<NbaTeam>): Map<Int, PlayerContract> =
        teams.flatMap { team ->
            team.players.map { player ->
                player.id to PlayerContract(
                    playerId = player.id,
                    teamId = team.abbreviation,
                    salary = 2_000_000,
                    yearsRemaining = 2,
                    noTrade = false
                )
            }
        }.toMap()

    private fun pgWeakTeam(name: String, abbreviation: String, startId: Int): NbaTeam = team(
        name,
        abbreviation,
        listOf(
            player(startId + 0, "PG", 60),
            player(startId + 1, "PG", 61),
            player(startId + 2, "SG", 94),
            player(startId + 3, "SG", 82),
            player(startId + 4, "SF", 90),
            player(startId + 5, "SF", 83),
            player(startId + 6, "PF", 88),
            player(startId + 7, "PF", 84),
            player(startId + 8, "C", 95),
            player(startId + 9, "C", 78),
            player(startId + 10, "SG", 80),
            player(startId + 11, "SF", 81)
        )
    )

    private fun centerWeakTeam(name: String, abbreviation: String, startId: Int): NbaTeam = team(
        name,
        abbreviation,
        listOf(
            player(startId + 0, "C", 60),
            player(startId + 1, "C", 61),
            player(startId + 2, "PG", 95),
            player(startId + 3, "PG", 78),
            player(startId + 4, "SG", 90),
            player(startId + 5, "SG", 82),
            player(startId + 6, "SF", 94),
            player(startId + 7, "SF", 83),
            player(startId + 8, "PF", 88),
            player(startId + 9, "PF", 84),
            player(startId + 10, "SG", 80),
            player(startId + 11, "SF", 81)
        )
    )

    private fun balancedTeam(name: String, abbreviation: String, startId: Int): NbaTeam = team(
        name,
        abbreviation,
        List(12) { index ->
            player(
                startId + index,
                listOf("PG", "SG", "SF", "PF", "C")[index % 5],
                80 + (index % 4)
            )
        }
    )

    private fun team(name: String, abbreviation: String, players: List<Player>): NbaTeam =
        NbaTeam(
            name = name,
            city = name,
            abbreviation = abbreviation,
            conference = "East",
            arena = Arena("$name Arena", name, 20_000, 2000),
            players = players
        )

    private fun player(id: Int, position: String, overall: Int): Player =
        Player(
            id = id,
            name = "P-$id",
            position = position,
            overall = overall,
            shooting = overall,
            defense = overall,
            rebound = overall,
            passing = overall,
            athleticism = overall,
            age = 25
        )

    private fun boosted(player: Player, overall: Int): Player =
        player.copy(
            overall = overall,
            shooting = overall,
            defense = overall,
            rebound = overall,
            passing = overall,
            athleticism = overall
        )
}
