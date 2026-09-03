package com.example

import com.example.data.local.PlayerEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PlayerEntityIdentityHeadroomTest {
    @Test
    fun maxIntPlayerIdIsRejectedBeforePersistenceCanOverflowAllocator() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            playerEntity(Int.MAX_VALUE)
        }

        assertEquals(
            "player id Int.MAX_VALUE would exhaust the persisted monotonic allocator",
            error.message
        )
    }

    @Test
    fun maxIntMinusOneRemainsRepresentable() {
        assertEquals(Int.MAX_VALUE - 1, playerEntity(Int.MAX_VALUE - 1).id)
    }

    private fun playerEntity(id: Int) = PlayerEntity(
        id = id,
        teamId = "T1",
        poolType = "ROSTER",
        active = true,
        startingFive = false,
        name = "Boundary Player",
        position = "PG",
        overall = 70,
        shooting = 70,
        defense = 70,
        rebound = 70,
        passing = 70,
        athleticism = 70,
        age = 22,
        xp = 0,
        trainings = 0,
        injured = false,
        injuryDays = 0,
        careerPoints = 0,
        careerRebounds = 0,
        careerAssists = 0,
        careerSteals = 0,
        careerBlocks = 0,
        careerGames = 0,
        championships = 0,
        mvps = 0,
        seasonPoints = 0,
        seasonRebounds = 0,
        seasonAssists = 0,
        seasonSteals = 0,
        seasonBlocks = 0,
        seasonGames = 0
    )
}
