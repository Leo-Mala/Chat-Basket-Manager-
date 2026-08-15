package com.example.domain.rules

import kotlin.math.abs

object LiveMatchRules {
    const val CLUTCH_MAX_MARGIN = 6

    fun shouldOfferClutch(
        isUserGame: Boolean,
        hasUsedLiveCoaching: Boolean,
        userScore: Int,
        opponentScore: Int
    ): Boolean = isUserGame &&
        !hasUsedLiveCoaching &&
        abs(userScore - opponentScore) <= CLUTCH_MAX_MARGIN

    /** The official score is always the sum of Q1..Q4 shown on screen. */
    fun scoreFromQuarters(quarterScores: List<Int>): Int = quarterScores.take(4).sum()
}
