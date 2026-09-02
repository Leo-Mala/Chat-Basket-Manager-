package com.example

import com.example.domain.rules.MainMenuNavigationRules
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainMenuNavigationRulesTest {
    @Test
    fun seasonFastForwardBlocksExitToMainMenu() {
        assertFalse(MainMenuNavigationRules.canExitToMainMenu(seasonSimulationInProgress = true))
    }

    @Test
    fun idleCareerCanExitToMainMenu() {
        assertTrue(MainMenuNavigationRules.canExitToMainMenu(seasonSimulationInProgress = false))
    }
}
