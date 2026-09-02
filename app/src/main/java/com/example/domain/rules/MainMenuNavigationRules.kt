package com.example.domain.rules

/**
 * Prevents leaving the active career while whole-season fast-forward is mutating the
 * current Season. Save-slot switching changes the physical Room database, so exposing
 * the main menu before fast-forward finishes would allow later checkpoints to target a
 * different slot.
 */
object MainMenuNavigationRules {
    fun canExitToMainMenu(seasonSimulationInProgress: Boolean): Boolean =
        !seasonSimulationInProgress
}
