package com.example

import com.example.data.local.SeasonEntity
import com.example.domain.rules.PlayerGenerationRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SeasonEntityAllocatorHeadroomTest {
    @Test
    fun persistedAllocatorAcceptsExactReservedHeadroom() {
        val maxPersistedNextId = Int.MAX_VALUE - PlayerGenerationRules.FREE_AGENT_BATCH_SIZE

        val entity = seasonEntity(nextPlayerId = maxPersistedNextId)

        assertEquals(maxPersistedNextId, entity.nextPlayerId)
    }

    @Test
    fun persistedAllocatorRejectsLessThanReservedHeadroom() {
        val exhaustedNextId = Int.MAX_VALUE - PlayerGenerationRules.FREE_AGENT_BATCH_SIZE + 1

        assertThrows(IllegalArgumentException::class.java) {
            seasonEntity(nextPlayerId = exhaustedNextId)
        }
    }

    @Test
    fun persistedAllocatorRejectsNonPositiveIdentity() {
        assertThrows(IllegalArgumentException::class.java) {
            seasonEntity(nextPlayerId = 0)
        }
    }

    private fun seasonEntity(nextPlayerId: Int) = SeasonEntity(
        id = 1,
        currentDay = 0,
        gamesPlayed = 0,
        seasonNumber = 1,
        currentMonth = 10,
        currentYear = 2025,
        userTeamId = "BOS",
        nextPlayerId = nextPlayerId
    )
}
