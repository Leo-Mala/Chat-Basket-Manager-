package com.example

import com.example.data.NbaDataGenerator
import com.example.domain.contract.ContractManager
import com.example.domain.roster.AiRosterManager
import com.example.domain.season.OffseasonManager
import com.example.models.Player
import com.example.models.Season
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiRosterManagerTest {

    @Test
    fun cpuTeamSignsClearUpgradeAndUserRosterIsUntouched() {
        val teams = NbaDataGenerator.getAllTeams()
        val user = teams[0]
        val cpu = teams[1]
        val userIds = user.players.map { it.id }
        val weakest = cpu.players.minByOrNull { it.overall }!!
        val elite = player(900_001, weakest.position, 95, 25)

        val result = AiRosterManager().rebalance(
            teams = teams,
            freeAgents = listOf(elite),
            userTeamName = user.name,
            priorityTeamNames = listOf(cpu.name),
            maxUpgradesPerTeam = 1
        )

        assertEquals(userIds, result.teams.first { it.name == user.name }.players.map { it.id })
        val updatedCpu = result.teams.first { it.name == cpu.name }
        assertTrue(updatedCpu.players.any { it.id == elite.id })
        assertTrue(result.freeAgents.any { it.id == weakest.id })
        assertEquals(1, result.transactions.size)
        assertEquals(cpu.name, result.transactions.single().teamName)
    }

    @Test
    fun protectedFreeAgentCannotBeSignedByCpu() {
        val teams = NbaDataGenerator.getAllTeams()
        val cpu = teams[1]
        val weakest = cpu.players.minByOrNull { it.overall }!!
        val protectedStar = player(900_002, weakest.position, 96, 26)

        val result = AiRosterManager().rebalance(
            teams = teams,
            freeAgents = listOf(protectedStar),
            userTeamName = teams[0].name,
            priorityTeamNames = listOf(cpu.name),
            protectedPlayerIds = setOf(protectedStar.id)
        )

        assertTrue(result.transactions.isEmpty())
        assertTrue(result.freeAgents.any { it.id == protectedStar.id })
        assertFalse(result.teams.flatMap { it.players }.any { it.id == protectedStar.id })
    }

    @Test
    fun cpuDoesNotMakeMarginalMoveBelowMinimumUpgrade() {
        val teams = NbaDataGenerator.getAllTeams()
        val cpu = teams[1]
        val weakest = cpu.players.minByOrNull { it.overall }!!
        val marginal = player(900_003, weakest.position, weakest.overall + 3, 24)

        val result = AiRosterManager().rebalance(
            teams = teams,
            freeAgents = listOf(marginal),
            userTeamName = teams[0].name,
            priorityTeamNames = listOf(cpu.name),
            maxUpgradesPerTeam = 1,
            minimumUpgrade = 4
        )

        assertTrue(result.transactions.isEmpty())
        assertTrue(result.freeAgents.any { it.id == marginal.id })
    }

    @Test
    fun offseasonCreatesContractForCpuFreeAgentSigning() {
        val teams = NbaDataGenerator.getAllTeams()
        val season = Season(teams, nextPlayerId = nextId(teams)).apply {
            userTeamName = teams[0].name
        }
        val eliteId = season.allocatePlayerIds(1).first
        val elite = player(eliteId, "PG", 95, 25)

        val result = OffseasonManager().advance(season, emptyMap(), listOf(elite))
        val signedTeam = result.season.teams.firstOrNull { team -> team.players.any { it.id == eliteId } }

        assertNotNull(signedTeam)
        val contract = result.contracts[eliteId]
        assertNotNull(contract)
        assertEquals(signedTeam!!.abbreviation, contract!!.teamId)
        assertFalse(result.freeAgents.any { it.id == eliteId })
    }

    @Test
    fun userExpiredStarRemainsAvailableInsteadOfBeingAutoSignedByCpu() {
        val teams = NbaDataGenerator.getAllTeams()
        val user = teams[0]
        val star = user.players.maxByOrNull { it.overall }!!
        star.overall = 94
        star.shooting = 94
        star.defense = 94
        star.rebound = 94
        star.passing = 94
        star.athleticism = 94
        star.age = 25
        val season = Season(teams, nextPlayerId = nextId(teams)).apply {
            userTeamName = user.name
        }
        val contractManager = ContractManager()
        val contracts = mapOf(
            star.id to contractManager.create(
                star,
                user.abbreviation,
                contractManager.recommendedOffer(star).copy(years = 1)
            )
        )

        val result = OffseasonManager(contractManager).advance(season, contracts, emptyList())

        assertTrue(result.freeAgents.any { it.id == star.id })
        assertFalse(result.season.teams.flatMap { it.players }.any { it.id == star.id })
        assertFalse(result.contracts.containsKey(star.id))
    }

    private fun player(id: Int, position: String, overall: Int, age: Int): Player =
        Player(id, "AI-$id", position, overall, overall, overall, overall, overall, overall, age)

    private fun nextId(teams: List<com.example.models.NbaTeam>): Int =
        teams.flatMap { it.players }.maxOf { it.id } + 1
}
