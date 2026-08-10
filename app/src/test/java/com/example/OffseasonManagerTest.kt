package com.example

import com.example.data.NbaDataGenerator
import com.example.domain.contract.ContractManager
import com.example.domain.contract.ContractOffer
import com.example.domain.season.OffseasonManager
import com.example.models.Season
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OffseasonManagerTest {
    @Test
    fun expiredContractIsReleasedAndNewRosterPlayersReceiveContracts() {
        val teams = NbaDataGenerator.getAllTeams()
        val season = Season(teams, nextPlayerId = teams.flatMap { it.players }.maxOf { it.id } + 1)
        val player = teams.first().players.first()
        val contract = ContractManager().create(player, teams.first().abbreviation, ContractOffer(1_000_000, 1))

        val result = OffseasonManager().advance(season, mapOf(player.id to contract), emptyList())
        assertEquals(2, result.season.seasonNumber)
        assertTrue(result.freeAgents.any { it.id == player.id })
        assertTrue(result.contracts.values.none { it.playerId == player.id })
        assertTrue(result.season.teams.flatMap { it.players }.all { result.contracts.containsKey(it.id) })
    }
}
