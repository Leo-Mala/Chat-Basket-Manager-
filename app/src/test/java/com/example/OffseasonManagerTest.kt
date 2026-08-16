package com.example

import com.example.data.NbaDataGenerator
import com.example.domain.contract.ContractManager
import com.example.domain.contract.ContractOffer
import com.example.domain.season.OffseasonManager
import com.example.models.Season
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OffseasonManagerTest {
    @Test
    fun userExpiredContractIsRenewedAndRosterPlayersReceiveContracts() {
        val teams = NbaDataGenerator.getAllTeams()
        val userTeam = teams.first()
        val season = Season(
            teams,
            nextPlayerId = teams.flatMap { it.players }.maxOf { it.id } + 1
        ).apply {
            userTeamName = userTeam.name
        }
        val player = userTeam.players.first()
        val contractManager = ContractManager()
        val contract = contractManager.create(player, userTeam.abbreviation, ContractOffer(1_000_000, 1))

        val result = OffseasonManager(contractManager).advance(season, mapOf(player.id to contract), emptyList())
        val managedAfter = result.season.teams.first { it.name == userTeam.name }
        val renewed = result.contracts[player.id]

        assertEquals(2, result.season.seasonNumber)
        assertTrue(managedAfter.players.any { it.id == player.id })
        assertFalse(result.freeAgents.any { it.id == player.id })
        assertNotNull(renewed)
        assertEquals(userTeam.abbreviation, renewed!!.teamId)
        assertTrue(renewed.yearsRemaining > 0)
        assertTrue(result.season.teams.flatMap { it.players }.all { result.contracts.containsKey(it.id) })
    }
}