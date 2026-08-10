package com.example.models

/** High-level career lifecycle controlled by the session state machine. */
enum class GameState {
    SETUP,
    ACTIVE,
    PLAYOFFS,
    CHAMPIONSHIP_CELEBRATION,
    DRAFT
}
