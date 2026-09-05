package com.example

import com.example.domain.rules.LiveCourtPlayPlanner
import com.example.domain.rules.LiveCourtPlayStyle
import com.example.domain.rules.LiveScoringSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveCourtPlayPlannerTest {
    @Test
    fun `free throw events always use free throw presentation`() {
        repeat(20) { index ->
            val plan = LiveCourtPlayPlanner.plan(
                points = 1,
                side = if (index % 2 == 0) LiveScoringSide.USER else LiveScoringSide.OPPONENT,
                quarter = index % 4 + 1,
                eventElapsedMillis = index * 713L
            )
            assertEquals(LiveCourtPlayStyle.FREE_THROW, plan.style)
            assertEquals(0, plan.passCount)
        }
    }

    @Test
    fun `two point events stay in two point visual families and vary`() {
        val allowed = setOf(
            LiveCourtPlayStyle.DRIVE,
            LiveCourtPlayStyle.CUT,
            LiveCourtPlayStyle.PICK_AND_ROLL,
            LiveCourtPlayStyle.POST_UP,
            LiveCourtPlayStyle.MID_RANGE
        )
        val observed = (1..40).map { index ->
            LiveCourtPlayPlanner.plan(
                points = 2,
                side = if (index % 2 == 0) LiveScoringSide.USER else LiveScoringSide.OPPONENT,
                quarter = index % 4 + 1,
                eventElapsedMillis = index * 587L
            ).style
        }.toSet()

        assertTrue(observed.all { it in allowed })
        assertTrue("Expected varied two-point presentations, got $observed", observed.size >= 4)
    }

    @Test
    fun `three point events stay on perimeter and vary`() {
        val allowed = setOf(
            LiveCourtPlayStyle.CORNER_THREE,
            LiveCourtPlayStyle.WING_THREE,
            LiveCourtPlayStyle.TOP_THREE
        )
        val observed = (1..30).map { index ->
            LiveCourtPlayPlanner.plan(
                points = 3,
                side = LiveScoringSide.USER,
                quarter = index % 4 + 1,
                eventElapsedMillis = index * 431L
            ).style
        }.toSet()

        assertTrue(observed.all { it in allowed })
        assertTrue("Expected varied three-point presentations, got $observed", observed.size >= 2)
    }

    @Test
    fun `same event always produces same visual plan`() {
        val first = LiveCourtPlayPlanner.plan(
            points = 2,
            side = LiveScoringSide.OPPONENT,
            quarter = 3,
            eventElapsedMillis = 12_345L
        )
        val second = LiveCourtPlayPlanner.plan(
            points = 2,
            side = LiveScoringSide.OPPONENT,
            quarter = 3,
            eventElapsedMillis = 12_345L
        )

        assertEquals(first, second)
    }
}
