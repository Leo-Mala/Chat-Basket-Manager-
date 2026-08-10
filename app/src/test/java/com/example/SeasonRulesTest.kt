package com.example

import com.example.domain.rules.SeasonRules
import org.junit.Assert.*
import org.junit.Test

class SeasonRulesTest {
    @Test fun `regular season completes at 82 games`() {
        assertFalse(SeasonRules.isRegularSeasonComplete(81))
        assertTrue(SeasonRules.isRegularSeasonComplete(82))
        assertTrue(SeasonRules.isRegularSeasonComplete(90))
    }

    @Test fun `nba home court pattern is 1 2 5 7`() {
        assertTrue(SeasonRules.homeTeamIsHigherSeedGame(1))
        assertTrue(SeasonRules.homeTeamIsHigherSeedGame(2))
        assertFalse(SeasonRules.homeTeamIsHigherSeedGame(3))
        assertFalse(SeasonRules.homeTeamIsHigherSeedGame(4))
        assertTrue(SeasonRules.homeTeamIsHigherSeedGame(5))
        assertFalse(SeasonRules.homeTeamIsHigherSeedGame(6))
        assertTrue(SeasonRules.homeTeamIsHigherSeedGame(7))
    }
}
