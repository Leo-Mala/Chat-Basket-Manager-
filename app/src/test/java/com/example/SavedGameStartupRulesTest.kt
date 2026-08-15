package com.example

import com.example.data.repository.GameStateRepository
import com.example.domain.rules.SavedGameStartupRules
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedGameStartupRulesTest {
    @Test
    fun normalizedCareerDoesNotDisappearOnlyBecauseCoachPayloadIsMissing() {
        assertTrue(SavedGameStartupRules.hasRequiredCore(snapshot(team = "{}", season = "{}", coach = null)))
        assertFalse(SavedGameStartupRules.hasRequiredCore(snapshot(team = null, season = "{}", coach = "{}")))
        assertFalse(SavedGameStartupRules.hasRequiredCore(snapshot(team = "{}", season = null, coach = "{}")))
        assertFalse(SavedGameStartupRules.hasRequiredCore(null))
    }

    private fun snapshot(team: String?, season: String?, coach: String?) = GameStateRepository.GameStateSnapshot(
        teamJson = team,
        coachJson = coach,
        financeJson = null,
        tacticsJson = null,
        seasonJson = season,
        historyJson = null,
        awardsJson = null,
        startingFiveJson = null,
        freeAgentsJson = null,
        draftRookiesJson = null,
        contractsJson = null,
        staffMarketJson = null,
        notificationsJson = null,
        teamStaffJson = null,
        facilitiesJson = null,
        financeAdvancedJson = null,
        newsFeedJson = null,
        latestBoxScoreJson = null,
        playoffResultJson = null,
        difficulty = 1,
        injuriesEnabled = true,
        autoSubstitutionsEnabled = true
    )
}
