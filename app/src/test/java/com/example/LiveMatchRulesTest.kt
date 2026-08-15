package com.example

import com.example.domain.rules.LiveMatchRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveMatchRulesTest {
    @Test
    fun blowoutDoesNotOfferFifteenSecondClutchDecision() {
        assertFalse(LiveMatchRules.shouldOfferClutch(true, false, 138, 124))
    }

    @Test
    fun closeGameOffersClutchBeforeItHasBeenUsed() {
        assertTrue(LiveMatchRules.shouldOfferClutch(true, false, 101, 98))
        assertFalse(LiveMatchRules.shouldOfferClutch(true, true, 101, 98))
    }

    @Test
    fun finalScoreUsesExactlyFourDisplayedQuarters() {
        assertEquals(138, LiveMatchRules.scoreFromQuarters(listOf(33, 33, 34, 38)))
        assertEquals(140, LiveMatchRules.scoreFromQuarters(listOf(33, 33, 34, 40)))
        // A duplicated phantom Q4/Q5 can never inflate the official final score.
        assertEquals(140, LiveMatchRules.scoreFromQuarters(listOf(33, 33, 34, 40, 37)))
    }
}
