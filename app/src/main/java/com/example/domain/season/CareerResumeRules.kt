package com.example.domain.season

import com.example.models.GameState

/** Pure reconstruction of the high-level lifecycle after a persisted career is loaded. */
object CareerResumeRules {
    /** Starting a new career must always enter setup and must not reuse the loaded career state. */
    fun newCareerState(): GameState = GameState.SETUP

    fun resolve(currentDay: Int, hasPlayoffResult: Boolean, hasDraftClass: Boolean): GameState = when {
        hasDraftClass -> GameState.DRAFT
        currentDay < 82 -> GameState.ACTIVE
        hasPlayoffResult -> GameState.CHAMPIONSHIP_CELEBRATION
        else -> GameState.PLAYOFFS
    }
}
