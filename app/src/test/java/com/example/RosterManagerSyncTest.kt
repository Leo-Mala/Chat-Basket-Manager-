package com.example

import com.example.models.Player
import com.example.models.NbaTeam
import com.example.models.Arena
import com.example.domain.roster.RosterManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RosterManagerSyncTest {
    private fun player(id: Int, overall: Int, available: Boolean = true) = Player(
        id = id,
        name = "P$id",
        position = listOf("PG", "SG", "SF", "PF", "C")[(id - 1) % 5],
        overall = overall,
        shooting = overall,
        defense = overall,
        rebound = overall,
        passing = overall,
        athleticism = overall
    ).apply {
        if (!available) {
            injured = true
            injuryDays = 3
        }
    }

    @Test
    fun syncRetainsHealthyStartersDropsInjuredAndFillsBestAvailable() {
        val players = listOf(
            player(1, 90), player(2, 88), player(3, 86, available = false),
            player(4, 84), player(5, 82), player(6, 80), player(7, 78)
        )
        val team = NbaTeam("Test", "TST", "City", "East", Arena("Arena", "City", 18000, 2020), players)
        val current = listOf(players[0], players[1], players[2], players[5], players[5])

        val synced = RosterManager().syncStartingFive(team, current)

        assertEquals(5, synced.size)
        assertEquals(5, synced.map { it.id }.toSet().size)
        assertFalse(synced.any { it.id == 3 })
        assertTrue(synced.any { it.id == 1 })
        assertTrue(synced.any { it.id == 2 })
        assertTrue(synced.any { it.id == 4 })
        assertTrue(synced.any { it.id == 5 })
        assertTrue(synced.any { it.id == 6 })
    }
}
