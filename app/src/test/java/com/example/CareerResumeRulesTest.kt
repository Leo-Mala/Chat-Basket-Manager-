package com.example

import com.example.domain.season.CareerResumeRules
import com.example.models.GameState
import org.junit.Assert.assertEquals
import org.junit.Test

class CareerResumeRulesTest {
    @Test
    fun regularSeasonResumesActive() {
        assertEquals(GameState.ACTIVE, CareerResumeRules.resolve(41, hasPlayoffResult = false, hasDraftClass = false))
    }

    @Test
    fun completedRegularSeasonWithoutResultResumesPlayoffs() {
        assertEquals(GameState.PLAYOFFS, CareerResumeRules.resolve(82, hasPlayoffResult = false, hasDraftClass = false))
    }

    @Test
    fun completedPlayoffsResumeCelebration() {
        assertEquals(GameState.CHAMPIONSHIP_CELEBRATION, CareerResumeRules.resolve(82, hasPlayoffResult = true, hasDraftClass = false))
    }

    @Test
    fun persistedDraftClassHasPriorityOverOldPlayoffMarker() {
        assertEquals(GameState.DRAFT, CareerResumeRules.resolve(82, hasPlayoffResult = true, hasDraftClass = true))
    }
}
