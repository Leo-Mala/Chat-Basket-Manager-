package com.example

import com.example.domain.season.FranchiseStrategyManager
import com.example.domain.trade.AiTradeManager
import com.example.models.Arena
import com.example.models.NbaTeam
import com.example.models.Player
import com.example.models.PlayerContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FranchiseTradeStrategyTest {
    private val strategyManager = FranchiseStrategyManager()

    @Test
    fun contenderProtectsCoreWhileRebuildCanMoveVeteranForYoungerFit() {
        val teamA = teamNeedingPointGuard()
        val teamB = teamNeedingCenter()
        val teams = listOf(teamA, teamB)
        val contracts = contractsFor(teams)
        val priority = listOf(teamA.name, teamB.name)
        val tradeManager = AiTradeManager()

        val contenderPolicies = mapOf(
            teamA.name to strategyManager.policy(FranchiseStrategyManager.Strategy.CONTENDER),
            teamB.name to strategyManager.policy(FranchiseStrategyManager.Strategy.CONTENDER)
        )
        val contenderResult = tradeManager.rebalance(
            teams = teams,
            contracts = contracts,
            userTeamName = null,
            priorityTeamNames = priority,
            maxTrades = 1,
            policiesByTeamName = contenderPolicies
        )
        assertTrue("Contenders should protect their top-three core in this matchup", contenderResult.trades.isEmpty())

        val rebuildPolicies = mapOf(
            teamA.name to strategyManager.policy(FranchiseStrategyManager.Strategy.REBUILD),
            teamB.name to strategyManager.policy(FranchiseStrategyManager.Strategy.REBUILD)
        )
        val rebuildResult = tradeManager.rebalance(
            teams = teams,
            contracts = contracts,
            userTeamName = null,
            priorityTeamNames = priority,
            maxTrades = 1,
            policiesByTeamName = rebuildPolicies
        )

        assertEquals(1, rebuildResult.trades.size)
        val trade = rebuildResult.trades.single()
        assertEquals(109, trade.playerFromAId)
        assertEquals(203, trade.playerFromBId)
        assertTrue(rebuildResult.teams.first { it.name == teamA.name }.players.any { it.id == 203 })
        assertTrue(rebuildResult.teams.first { it.name == teamB.name }.players.any { it.id == 109 })
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

    private fun teamNeedingPointGuard(): NbaTeam = team(
        name = "Rebuild A",
        abbreviation = "RBA",
        players = listOf(
            player(100, "PG", 60, 24),
            player(101, "PG", 61, 23),
            player(102, "SG", 95, 25),
            player(103, "SG", 82, 25),
            player(104, "SF", 94, 26),
            player(105, "SF", 83, 24),
            player(106, "PF", 88, 24),
            player(107, "PF", 84, 24),
            player(108, "C", 70, 22),
            player(109, "C", 91, 33),
            player(110, "SG", 80, 23),
            player(111, "SF", 81, 23)
        )
    )

    private fun teamNeedingCenter(): NbaTeam = team(
        name = "Rebuild B",
        abbreviation = "RBB",
        players = listOf(
            player(200, "C", 60, 23),
            player(201, "C", 61, 22),
            player(202, "PG", 70, 24),
            player(203, "PG", 90, 22),
            player(204, "SG", 95, 26),
            player(205, "SG", 82, 24),
            player(206, "SF", 94, 25),
            player(207, "SF", 83, 24),
            player(208, "PF", 88, 24),
            player(209, "PF", 84, 24),
            player(210, "SG", 80, 23),
            player(211, "SF", 81, 23)
        )
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

    private fun player(id: Int, position: String, overall: Int, age: Int): Player =
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
            age = age
        )
}
