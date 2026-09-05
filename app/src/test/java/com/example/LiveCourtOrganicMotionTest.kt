package com.example

import com.example.domain.rules.LiveCourtOrganicMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveCourtOrganicMotionTest {
    @Test
    fun `same inputs produce deterministic motion`() {
        val first = LiveCourtOrganicMotion.playerSample(2, 1_250L, 0.63f, 8_400L)
        val second = LiveCourtOrganicMotion.playerSample(2, 1_250L, 0.63f, 8_400L)
        assertEquals(first, second)
    }

    @Test
    fun `players do not share one rigid movement profile`() {
        val samples = (0..4).map { index ->
            LiveCourtOrganicMotion.playerSample(index, 1_800L, 0.72f, 9_200L)
        }
        assertTrue(samples.map { it.xOffset }.distinct().size >= 4)
        assertTrue(samples.map { it.yOffset }.distinct().size >= 4)
    }

    @Test
    fun `defenders react after offense instead of mirroring instantly`() {
        val actionProgress = 0.50f
        val reactions = (0..4).map { index ->
            LiveCourtOrganicMotion.defenderReactionProgress(actionProgress, index, 7_100L)
        }
        assertTrue(reactions.all { it in 0f..actionProgress })
        assertTrue(reactions.distinct().size >= 3)
    }

    @Test
    fun `pass curve starts and ends on target line`() {
        assertEquals(0f, LiveCourtOrganicMotion.passCurve(0f, 12L), 0.00001f)
        assertEquals(0f, LiveCourtOrganicMotion.passCurve(1f, 12L), 0.00001f)
        assertNotEquals(0f, LiveCourtOrganicMotion.passCurve(0.5f, 12L))
    }

    @Test
    fun `pass curve alternates direction deterministically`() {
        val even = LiveCourtOrganicMotion.passCurve(0.5f, 10L)
        val odd = LiveCourtOrganicMotion.passCurve(0.5f, 11L)
        assertTrue(even > 0f)
        assertTrue(odd < 0f)
    }

    @Test
    fun `dribble cadence varies by handler`() {
        val first = LiveCourtOrganicMotion.dribbleBounce(1_400L, 0)
        val second = LiveCourtOrganicMotion.dribbleBounce(1_400L, 3)
        assertNotEquals(first, second)
    }
}
