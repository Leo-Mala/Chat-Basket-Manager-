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

data class LiveCourtPlayPlan(
    val style: LiveCourtPlayStyle,
    val passCount: Int
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

    private fun indexFor(seed: Long, size: Int): Int {
        require(size > 0)
        return ((seed and Long.MAX_VALUE) % size).toInt()
    }
}
