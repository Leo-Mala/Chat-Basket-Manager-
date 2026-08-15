package com.example

import com.example.data.NbaDataGenerator
import com.example.domain.contract.AiContractRenewalManager
import com.example.domain.season.FranchiseStrategyManager
import com.example.models.Season
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FranchiseStrategyManagerTest {
    private val manager = FranchiseStrategyManager()

    @Test
    fun franchiseContextProducesDistinctStrategies() {
        val base = NbaDataGenerator.getAllTeams().first()
        val contender = base.copy(players = base.players.map { it.copy(age = 28, overall = maxOf(84, it.overall)) })
        val rebuild = base.copy(players = base.players.map { it.copy(age = 23, overall = 70) })
        val young = base.copy(players = base.players.map { it.copy(age = 23, overall = maxOf(76, it.overall)) })
        val aging = base.copy(players = base.players.map { it.copy(age = 31, overall = maxOf(77, it.overall)) })

        assertEquals(FranchiseStrategyManager.Strategy.CONTENDER, manager.classify(contender, Season.SeasonRecord(wins = 58, losses = 24, gamesPlayed = 82)))
        assertEquals(FranchiseStrategyManager.Strategy.REBUILD, manager.classify(rebuild, Season.SeasonRecord(wins = 22, losses = 60, gamesPlayed = 82)))
        assertEquals(FranchiseStrategyManager.Strategy.YOUNG_CORE, manager.classify(young, Season.SeasonRecord(wins = 40, losses = 42, gamesPlayed = 82)))
        assertEquals(FranchiseStrategyManager.Strategy.AGING_CORE, manager.classify(aging, Season.SeasonRecord(wins = 40, losses = 42, gamesPlayed = 82)))
    }

    @Test
    fun contenderRetainsMoreThanRebuildWhileUserRemainsManual() {
        val teams = NbaDataGenerator.getAllTeams().take(3)
        val contender = teams[0]
        val rebuild = teams[1]
        val user = teams[2]
        val expiredIds = teams.flatMap { it.players }.map { it.id }.toSet()
        val policies = mapOf(
            contender.name to manager.policy(FranchiseStrategyManager.Strategy.CONTENDER),
            rebuild.name to manager.policy(FranchiseStrategyManager.Strategy.REBUILD),
            user.name to manager.policy(FranchiseStrategyManager.Strategy.CONTENDER)
        )

        val result = AiContractRenewalManager().renewExpiring(
            teams = teams,
            continuingContracts = emptyMap(),
            expiredPlayerIds = expiredIds,
            userTeamName = user.name,
            policiesByTeamName = policies
        )

        val contenderRenewals = contender.players.count { it.id in result.renewedPlayerIds }
        val rebuildRenewals = rebuild.players.count { it.id in result.renewedPlayerIds }
        val userRenewals = user.players.count { it.id in result.renewedPlayerIds }

        assertTrue("Contender should retain multiple core players", contenderRenewals >= 2)
        assertTrue("Rebuild should retain at most one expiring player", rebuildRenewals <= 1)
        assertEquals("User renewals must remain manual", 0, userRenewals)
    }
}
