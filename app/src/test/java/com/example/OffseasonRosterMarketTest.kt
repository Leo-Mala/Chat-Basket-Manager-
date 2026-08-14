package com.example

import com.example.data.NbaDataGenerator
import com.example.domain.roster.AiRosterManager
import com.example.domain.season.SeasonManager
import com.example.models.Player
import com.example.models.Season
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OffseasonRosterMarketTest {

    @Test
    fun cpuFreeAgencyFillsVacanciesWithoutReleasingAnotherPlayer() {
        val originalTeams = NbaDataGenerator.getAllTeams()
        val user = originalTeams.first()
        val cpuOriginal = originalTeams[1]
        val cpu = cpuOriginal.copy(players = cpuOriginal.players.take(10))
        val teams = originalTeams.map { if (it.name == cpu.name) cpu else it }
        val freeAgents = listOf(
            player(980_001, "PG", 80, 25),
            player(980_002, "C", 79, 24)
        )

        val result = AiRosterManager().rebalance(
            teams = teams,
            freeAgents = freeAgents,
            userTeamName = user.name,
            priorityTeamNames = listOf(cpu.name),
            maxUpgradesPerTeam = 0,
            minimumRosterSize = 12
        )

        val updatedCpu = result.teams.first { it.name == cpu.name }
        assertEquals(12, updatedCpu.players.size)
        assertEquals(2, result.transactions.size)
        assertTrue(result.transactions.all { it.teamName == cpu.name })
        result.transactions.forEach { assertNull(it.releasedPlayerId) }
        assertTrue(freeAgents.all { candidate -> updatedCpu.players.any { it.id == candidate.id } })
        assertTrue(freeAgents.none { candidate -> result.freeAgents.any { it.id == candidate.id } })
    }

    @Test
    fun coordinatedSeasonAdvanceCanSkipHiddenRosterReplenishment() {
        val base = NbaDataGenerator.getAllTeams()[1]
        val tenPlayers = List(10) { index -> player(981_000 + index, "PG", 76 + index % 3, 25) }
        val season = Season(
            teams = listOf(base.copy(players = tenPlayers)),
            seasonNumber = 7,
            nextPlayerId = 990_000
        )

        val advanced = SeasonManager().advanceSeason(season, replenishRosters = false)

        assertEquals(8, advanced.seasonNumber)
        assertEquals(10, advanced.teams.single().players.size)
        assertEquals(990_000, advanced.nextPlayerId)
        assertTrue(advanced.teams.single().players.all { it.age == 26 })
    }

    private fun player(id: Int, position: String, overall: Int, age: Int): Player =
        Player(id, "Market-$id", position, overall, overall, overall, overall, overall, overall, age)
}
