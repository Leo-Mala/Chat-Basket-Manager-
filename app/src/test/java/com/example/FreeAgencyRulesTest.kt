package com.example

import com.example.domain.rules.FreeAgencyRules
import com.example.models.Player
import org.junit.Assert.*
import org.junit.Test

class FreeAgencyRulesTest {
    @Test fun `release candidate prefers lowest overall`() {
        val players = listOf(
            Player(1, "A", "PG", 82, 82, 82, 82, 82, 82, 28),
            Player(2, "B", "SG", 74, 74, 74, 74, 74, 74, 24),
            Player(3, "C", "SF", 78, 78, 78, 78, 78, 78, 30)
        )
        assertEquals(2, FreeAgencyRules.releaseCandidate(players)?.id)
    }

    @Test fun `candidate validation rejects invalid age and rating`() {
        assertTrue(FreeAgencyRules.validateCandidate(Player(1, "A", "PG", 70, 70, 70, 70, 70, 70, 25)))
        assertFalse(FreeAgencyRules.validateCandidate(Player(2, "B", "PG", 39, 39, 39, 39, 39, 39, 25)))
        assertFalse(FreeAgencyRules.validateCandidate(Player(3, "C", "PG", 70, 70, 70, 70, 70, 70, 41)))
    }
}
