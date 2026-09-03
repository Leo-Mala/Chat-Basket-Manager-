package com.example

import com.example.models.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerAgeOverflowTest {
    private fun extremePlayer(age: Int, seasonGames: Int = 0): Player = Player(
        id = 77,
        name = "Extreme Veteran",
        position = "PG",
        overall = 80,
        shooting = 80,
        defense = 80,
        rebound = 80,
        passing = 80,
        athleticism = 80,
        age = age,
        seasonGames = seasonGames
    )

    @Test
    fun advanceSeasonDoesNotWrapMaximumAgeNegative() {
        val player = extremePlayer(Int.MAX_VALUE)

        player.advanceSeason()

        assertEquals(Int.MAX_VALUE, player.age)
        assertTrue(player.age > 0)
        assertTrue(player.overall in 50..99)
        assertTrue(player.athleticism in 40..99)
    }

    @Test
    fun advanceSeasonCanReachMaximumAgeWithoutWrapping() {
        val player = extremePlayer(Int.MAX_VALUE - 1)

        player.advanceSeason()

        assertEquals(Int.MAX_VALUE, player.age)
        assertTrue(player.overall in 50..99)
    }

    @Test
    fun inSeasonEvolutionAtMaximumAgeKeepsValidState() {
        val player = extremePlayer(Int.MAX_VALUE, seasonGames = 50)

        player.evolveInSeason()

        assertEquals(Int.MAX_VALUE, player.age)
        assertTrue(player.xp >= 0)
        assertTrue(player.overall in 50..99)
        assertTrue(player.athleticism in 40..99)
    }
}
