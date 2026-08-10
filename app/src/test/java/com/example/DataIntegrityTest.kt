package com.example

import com.example.data.NbaDataGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DataIntegrityTest {
    @Test
    fun newCareerGetsIndependentPlayerInstancesAndCleanStats() {
        val first = NbaDataGenerator.getAllTeams()
        val player = first.first().players.first()
        player.careerGames = 99
        player.seasonPoints = 1234
        player.injured = true

        val second = NbaDataGenerator.getAllTeams()
        val fresh = second.first().players.first()

        assertNotSame(player, fresh)
        assertEquals(0, fresh.careerGames)
        assertEquals(0, fresh.seasonPoints)
        assertTrue(!fresh.injured)
        assertEquals(player.id, fresh.id)
    }

    @Test
    fun seedPlayerIdsAreUnique() {
        val ids = NbaDataGenerator.getAllTeams().flatMap { it.players }.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
}
