package com.example.domain.rules

enum class LiveCourtPlayStyle {
    FREE_THROW,
    DRIVE,
    CUT,
    PICK_AND_ROLL,
    POST_UP,
    MID_RANGE,
    CORNER_THREE,
    WING_THREE,
    TOP_THREE
}

enum class LiveCourtPossessionPhase {
    TRANSITION,
    SETUP,
    ACTION,
    FINISH
}

data class LiveCourtPlayPlan(
    val style: LiveCourtPlayStyle,
    val passCount: Int
)

data class LiveCourtPossessionFlow(
    val phase: LiveCourtPossessionPhase,
    val transitionProgress: Float,
    val setupProgress: Float,
    val actionProgress: Float,
    val finishProgress: Float,
    val sequenceProgress: Float,
    val effectivePassCount: Int
)

/**
 * Deterministic presentation-only play selection for the live court.
 *
 * The match engine already decides the side, timing, and points of each scoring event. This
 * planner only chooses how that already-decided event is visually staged on the mini-court.
 * It never changes the score, player statistics, possession data, tactics, or saved game state.
 */
object LiveCourtPlayPlanner {
    private val twoPointStyles = listOf(
        LiveCourtPlayStyle.DRIVE,
        LiveCourtPlayStyle.CUT,
        LiveCourtPlayStyle.PICK_AND_ROLL,
        LiveCourtPlayStyle.POST_UP,
        LiveCourtPlayStyle.DRIVE,
        LiveCourtPlayStyle.CUT,
        LiveCourtPlayStyle.MID_RANGE
    )

    private val threePointStyles = listOf(
        LiveCourtPlayStyle.CORNER_THREE,
        LiveCourtPlayStyle.WING_THREE,
        LiveCourtPlayStyle.TOP_THREE
    )

    fun plan(
        points: Int,
        side: LiveScoringSide,
        quarter: Int,
        eventElapsedMillis: Long
    ): LiveCourtPlayPlan {
        require(points in 1..3) { "points must be between 1 and 3" }
        require(quarter in 1..4) { "quarter must be between 1 and 4" }
        require(eventElapsedMillis >= 0L) { "eventElapsedMillis must be non-negative" }

        val seed = eventElapsedMillis * 31L +
            quarter * 7_919L +
            points * 1_301L +
            if (side == LiveScoringSide.USER) 97L else 193L

        val style = when (points) {
            1 -> LiveCourtPlayStyle.FREE_THROW
            2 -> twoPointStyles[indexFor(seed, twoPointStyles.size)]
            else -> threePointStyles[indexFor(seed, threePointStyles.size)]
        }

        val passCount = when (style) {
            LiveCourtPlayStyle.FREE_THROW -> 0
            LiveCourtPlayStyle.DRIVE -> indexFor(seed / 3L + 11L, 2)
            LiveCourtPlayStyle.CUT -> 1 + indexFor(seed / 5L + 17L, 2)
            LiveCourtPlayStyle.PICK_AND_ROLL -> 1 + indexFor(seed / 7L + 23L, 2)
            LiveCourtPlayStyle.POST_UP -> 1
            LiveCourtPlayStyle.MID_RANGE -> 1 + indexFor(seed / 11L + 29L, 2)
            LiveCourtPlayStyle.CORNER_THREE,
            LiveCourtPlayStyle.WING_THREE,
            LiveCourtPlayStyle.TOP_THREE -> 2 + indexFor(seed / 13L + 31L, 2)
        }

        return LiveCourtPlayPlan(style = style, passCount = passCount)
    }

    /**
     * Splits one already-scheduled scoring possession into presentation phases. Short possessions
     * deliberately use fewer visible passes instead of cramming several movements into a few
     * frames. This affects animation only; the event time and score remain untouched.
     */
    fun flow(
        progress: Float,
        possessionDurationMillis: Long,
        plannedPassCount: Int
    ): LiveCourtPossessionFlow {
        require(possessionDurationMillis > 0L) { "possessionDurationMillis must be positive" }
        require(plannedPassCount >= 0) { "plannedPassCount must be non-negative" }

        val clamped = progress.coerceIn(0f, 1f)
        val transitionEnd = when {
            possessionDurationMillis < 1_200L -> 0.16f
            possessionDurationMillis < 1_800L -> 0.20f
            else -> 0.23f
        }
        val setupEnd = when {
            possessionDurationMillis < 1_200L -> 0.34f
            possessionDurationMillis < 1_800L -> 0.40f
            else -> 0.45f
        }
        val actionEnd = when {
            possessionDurationMillis < 1_200L -> 0.76f
            possessionDurationMillis < 1_800L -> 0.79f
            else -> 0.82f
        }

        fun segment(value: Float, start: Float, end: Float): Float {
            if (end <= start) return if (value >= end) 1f else 0f
            return ((value - start) / (end - start)).coerceIn(0f, 1f)
        }

        val effectivePassCount = minOf(
            plannedPassCount,
            when {
                possessionDurationMillis < 1_050L -> 0
                possessionDurationMillis < 1_500L -> 1
                possessionDurationMillis < 2_200L -> 2
                else -> 3
            }
        )

        val phase = when {
            clamped < transitionEnd -> LiveCourtPossessionPhase.TRANSITION
            clamped < setupEnd -> LiveCourtPossessionPhase.SETUP
            clamped < actionEnd -> LiveCourtPossessionPhase.ACTION
            else -> LiveCourtPossessionPhase.FINISH
        }

        return LiveCourtPossessionFlow(
            phase = phase,
            transitionProgress = segment(clamped, 0f, transitionEnd),
            setupProgress = segment(clamped, transitionEnd, setupEnd),
            actionProgress = segment(clamped, setupEnd, actionEnd),
            finishProgress = segment(clamped, actionEnd, 1f),
            sequenceProgress = segment(clamped, transitionEnd, 1f),
            effectivePassCount = effectivePassCount
        )
    }

    private fun indexFor(seed: Long, size: Int): Int {
        require(size > 0)
        return ((seed and Long.MAX_VALUE) % size).toInt()
    }
}
