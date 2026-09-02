package com.example

import com.example.models.Player
import com.example.simulator.GameSimulator
import com.example.simulator.applyGameStatsSafely
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerStatAccumulatorTest {
    @Test
    fun normalGameStatsAreAccumulatedExactly() {
        val player = player()
        val stats = GameSimulator.PlayerStats(
            points = 31,
            rebounds = 11,
            assists = 7,
            steals = 2,
            blocks = 1,
            turnovers = 3,
            plusMinus = 8
        )

        player.applyGameStatsSafely(stats)

        assertEquals(1, player.careerGames)
        assertEquals(31, player.careerPoints)
        assertEquals(11, player.careerRebounds)
        assertEquals(7, player.careerAssists)
        assertEquals(2, player.careerSteals)
        assertEquals(1, player.careerBlocks)
        assertEquals(1, player.seasonGames)
        assertEquals(31, player.seasonPoints)
        assertEquals(11, player.seasonRebounds)
        assertEquals(7, player.seasonAssists)
        assertEquals(2, player.seasonSteals)
        assertEquals(1, player.seasonBlocks)
    }

    @Test
    fun boundaryCountersSaturateInsteadOfWrappingNegative() {
        val player = player().copy(
            careerGames = Int.MAX_VALUE,
            careerPoints = Int.MAX_VALUE - 5,
            careerRebounds = Int.MAX_VALUE - 1,
            careerAssists = Int.MAX_VALUE,
            careerSteals = Int.MAX_VALUE - 1,
            careerBlocks = Int.MAX_VALUE,
            seasonGames = Int.MAX_VALUE,
            seasonPoints = Int.MAX_VALUE - 5,
            seasonRebounds = Int.MAX_VALUE - 1,
            seasonAssists = Int.MAX_VALUE,
            seasonSteals = Int.MAX_VALUE - 1,
            seasonBlocks = Int.MAX_VALUE
        )
        val stats = GameSimulator.PlayerStats(
            points = 10,
            rebounds = 4,
            assists = 3,
            steals = 2,
            blocks = 2,
            turnovers = 0,
            plusMinus = 0
        )

        player.applyGameStatsSafely(stats)

        assertEquals(Int.MAX_VALUE, player.careerGames)
        assertEquals(Int.MAX_VALUE, player.careerPoints)
        assertEquals(Int.MAX_VALUE, player.careerRebounds)
        assertEquals(Int.MAX_VALUE, player.careerAssists)
        assertEquals(Int.MAX_VALUE, player.careerSteals)
        assertEquals(Int.MAX_VALUE, player.careerBlocks)
        assertEquals(Int.MAX_VALUE, player.seasonGames)
        assertEquals(Int.MAX_VALUE, player.seasonPoints)
        assertEquals(Int.MAX_VALUE, player.seasonRebounds)
        assertEquals(Int.MAX_VALUE, player.seasonAssists)
        assertEquals(Int.MAX_VALUE, player.seasonSteals)
        assertEquals(Int.MAX_VALUE, player.seasonBlocks)
    }

    private fun player() = Player(
        id = 1,
        name = "Boundary Player",
        position = "PG",
        overall = 80,
        shooting = 80,
        defense = 80,
        rebound = 80,
        passing = 80,
        athleticism = 80
    )
}
