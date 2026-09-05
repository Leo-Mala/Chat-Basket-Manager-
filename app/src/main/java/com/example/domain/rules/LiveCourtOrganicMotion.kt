package com.example.domain.rules

import kotlin.math.sin

/** Presentation-only motion values used by the live-court animation. */
data class LiveCourtMotionSample(
    val xOffset: Float,
    val yOffset: Float,
    val reactionProgress: Float
)

data class LiveCourtFormationBias(
    val xOffset: Float,
    val yOffset: Float
)

/**
 * Produces deterministic, player-specific motion so the five markers do not move as one rigid
 * block. These values are visual only and never alter score, possession, tactics or saved state.
 */
object LiveCourtOrganicMotion {
    /**
     * Small deterministic spacing bias for each marker. It breaks overly geometric lines/arcs
     * without changing the intended play location or any basketball rule/state.
     */
    fun formationBias(
        playerIndex: Int,
        eventElapsedMillis: Long,
        formationSalt: Long = 0L
    ): LiveCourtFormationBias {
        require(playerIndex in 0..4) { "playerIndex must be between 0 and 4" }
        require(eventElapsedMillis >= 0L) { "eventElapsedMillis must be non-negative" }

        val seed = (eventElapsedMillis * 31L + playerIndex * 997L + formationSalt * 131L) and Long.MAX_VALUE
        val xBucket = (seed % 17L).toInt() - 8
        val yBucket = ((seed / 17L) % 21L).toInt() - 10

        return LiveCourtFormationBias(
            xOffset = xBucket * 0.00095f,
            yOffset = yBucket * 0.00145f
        )
    }

    fun playerSample(
        playerIndex: Int,
        elapsedMillis: Long,
        actionProgress: Float,
        eventElapsedMillis: Long
    ): LiveCourtMotionSample {
        require(playerIndex in 0..4) { "playerIndex must be between 0 and 4" }
        require(elapsedMillis >= 0L) { "elapsedMillis must be non-negative" }
        require(eventElapsedMillis >= 0L) { "eventElapsedMillis must be non-negative" }

        val clampedAction = actionProgress.coerceIn(0f, 1f)
        val seed = eventElapsedMillis + playerIndex * 997L
        val cadence = 1.55f + playerIndex * 0.19f + ((seed % 7L).toFloat() * 0.035f)
        val phase = playerIndex * 1.13f + ((seed % 19L).toFloat() * 0.17f)
        val seconds = elapsedMillis / 1_000f
        val activity = 0.45f + 0.55f * clampedAction
        val spacing = formationBias(
            playerIndex = playerIndex,
            eventElapsedMillis = eventElapsedMillis,
            formationSalt = 31L
        )

        val xOffset = spacing.xOffset +
            sin(seconds * cadence + phase) * (0.0028f + playerIndex * 0.00022f) * activity
        val yOffset = spacing.yOffset +
            sin(seconds * (cadence * 1.27f) + phase * 0.73f) *
            (0.0052f + (4 - playerIndex) * 0.00031f) * activity

        return LiveCourtMotionSample(
            xOffset = xOffset,
            yOffset = yOffset,
            reactionProgress = defenderReactionProgress(clampedAction, playerIndex, eventElapsedMillis)
        )
    }

    /** Defenders react with small, deterministic delays instead of mirroring offense exactly. */
    fun defenderReactionProgress(
        actionProgress: Float,
        defenderIndex: Int,
        eventElapsedMillis: Long
    ): Float {
        require(defenderIndex in 0..4) { "defenderIndex must be between 0 and 4" }
        require(eventElapsedMillis >= 0L) { "eventElapsedMillis must be non-negative" }

        val clamped = actionProgress.coerceIn(0f, 1f)
        val delay = 0.055f + defenderIndex * 0.018f + (eventElapsedMillis % 5L).toFloat() * 0.006f
        return ((clamped - delay) / (1f - delay)).coerceIn(0f, 1f)
    }

    /**
     * Curves a pass slightly away from a straight line. Positive/negative bend alternates by the
     * deterministic seed, while the curve always starts and ends at zero displacement.
     */
    fun passCurve(progress: Float, seed: Long): Float {
        val t = progress.coerceIn(0f, 1f)
        val direction = if ((seed and 1L) == 0L) 1f else -1f
        return sin(t * Math.PI).toFloat() * 0.055f * direction
    }

    /** Visible dribble rhythm with different cadence for each ball handler. */
    fun dribbleBounce(elapsedMillis: Long, handlerIndex: Int): Float {
        require(handlerIndex in 0..4) { "handlerIndex must be between 0 and 4" }
        require(elapsedMillis >= 0L) { "elapsedMillis must be non-negative" }
        val cadence = 8.4f + handlerIndex * 0.47f
        return sin((elapsedMillis / 1_000f) * cadence + handlerIndex * 0.61f) * 0.44f
    }
}
