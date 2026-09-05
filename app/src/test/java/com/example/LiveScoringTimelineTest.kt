package com.example

import com.example.domain.rules.LiveScoringSide
import com.example.domain.rules.LiveScoringTimeline
import com.example.models.Player
import com.example.simulator.LiveLineupRules
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveScoringTimelineTest {
    @Test
    fun timelinePreservesExactQuarterTotalsAcrossSeeds() {
        repeat(50) { seed ->
            val events = LiveScoringTimeline.build(37, 29, Random(seed))
            assertEquals(37, events.filter { it.side == LiveScoringSide.USER }.sumOf { it.points })
            assertEquals(29, events.filter { it.side == LiveScoringSide.OPPONENT }.sumOf { it.points })
            assertTrue(events.all { it.points in 1..3 })
            assertTrue(events.all { it.elapsedMillis in 1 until LiveScoringTimeline.QUARTER_REAL_DURATION_MS })
            assertTrue(events.zipWithNext().all { (a, b) -> a.elapsedMillis < b.elapsedMillis })
        }
    }

    @Test
    fun acceleratedClockMapsOneRealMinuteToTwelveGameMinutes() {
        assertEquals("12:00", LiveScoringTimeline.clockForElapsed(0))
        assertEquals("06:00", LiveScoringTimeline.clockForElapsed(30_000))
        assertEquals("00:00", LiveScoringTimeline.clockForElapsed(60_000))
        assertEquals("00:00", LiveScoringTimeline.clockForElapsed(90_000))
    }

    @Test
    fun manualSubstitutionKeepsExactlyFiveAndDoesNotMutatePreferredStartingFive() {
        val roster = (1..8).map { player(it, 90 - it) }
        val preferred = roster.take(5)
        val active = LiveLineupRules.initialLineup(roster, preferred)
        val result = LiveLineupRules.substitute(
            roster = roster,
            activeLineup = active,
            playerOutId = active.first().id,
            playerInId = roster[5].id
        )

        assertNotNull(result)
        result!!
        assertEquals(5, result.lineup.size)
        assertEquals(5, result.lineup.map { it.id }.distinct().size)
        assertTrue(result.lineup.any { it.id == roster[5].id })
        assertFalse(result.lineup.any { it.id == preferred.first().id })
        assertEquals(listOf(1, 2, 3, 4, 5), preferred.map { it.id })
    }

    @Test
    fun manualSubstitutionRejectsBenchPlayerWhoIsUnavailable() {
        val roster = (1..6).map { player(it, 90 - it) }.toMutableList()
        roster[5].injured = true
        roster[5].injuryDays = 10
        val active = LiveLineupRules.initialLineup(roster, roster.take(5))

        assertNull(
            LiveLineupRules.substitute(
                roster = roster,
                activeLineup = active,
                playerOutId = active.first().id,
                playerInId = roster[5].id
            )
        )
    }

    private fun player(id: Int, overall: Int) = Player(
        id = id,
        name = "Player $id",
        position = if (id % 5 == 0) "C" else "G",
        overall = overall,
        shooting = overall,
        defense = overall,
        rebound = overall,
        passing = overall,
        athleticism = overall
    )
}
