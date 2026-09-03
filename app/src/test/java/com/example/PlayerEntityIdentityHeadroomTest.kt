package com.example

import com.example.data.local.PlayerEntity
import com.example.domain.rules.PlayerGenerationRules
import com.example.models.Season
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PlayerEntityIdentityHeadroomTest {
    private val message = "player id must preserve ${PlayerGenerationRules.FREE_AGENT_BATCH_SIZE} allocator slots below Int.MAX_VALUE"

    @Test
    fun maxIntPlayerIdIsRejectedBeforePersistenceCanOverflowAllocator() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            playerEntity(Int.MAX_VALUE)
        }
        assertEquals(message, error.message)
    }

    @Test
    fun idThatWouldLeaveTooLittleFreeAgentBatchHeadroomIsRejected() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            playerEntity(Int.MAX_VALUE - 2)
        }
        assertEquals(message, error.message)
    }

    @Test
    fun highestIdThatStillPreservesFullFreeAgentBatchIsRepresentable() {
        val highestSafeId = Int.MAX_VALUE - PlayerGenerationRules.FREE_AGENT_BATCH_SIZE - 1
        assertEquals(highestSafeId, playerEntity(highestSafeId).id)
    }

    @Test
    fun allocatorRejectsBatchBeforeMutatingWhenOnlyReservedHeadroomRemains() {
        val start = Int.MAX_VALUE - PlayerGenerationRules.FREE_AGENT_BATCH_SIZE
        val season = Season(teams = emptyList(), nextPlayerId = start)

        assertThrows(ArithmeticException::class.java) {
            season.allocatePlayerIds(PlayerGenerationRules.FREE_AGENT_BATCH_SIZE)
        }
        assertEquals(start, season.nextPlayerId)
    }

    @Test
    fun allocatorCanPersistLastSupportedBatchAndStillKeepsReservedWindow() {
        val batch = PlayerGenerationRules.FREE_AGENT_BATCH_SIZE
        val start = Int.MAX_VALUE - (batch * 2)
        val season = Season(teams = emptyList(), nextPlayerId = start)

        val ids = season.allocatePlayerIds(batch)

        assertEquals(start, ids.first)
        assertEquals(Int.MAX_VALUE - batch - 1, ids.last)
        assertEquals(Int.MAX_VALUE - batch, season.nextPlayerId)
        assertEquals(ids.last, playerEntity(ids.last).id)
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
