package com.example.models

/** High-level career lifecycle controlled by the session state machine. */
enum class GameState {
    SETUP,
    LOAD_ERROR,
    ACTIVE,
    PLAYOFFS,
    CHAMPIONSHIP_CELEBRATION,
    DRAFT
}
