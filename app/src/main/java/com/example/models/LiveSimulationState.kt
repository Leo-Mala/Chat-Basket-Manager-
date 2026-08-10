package com.example.models

import com.example.simulator.GameSimulator

/** Presentation-neutral state emitted while a match is being simulated. */
data class LiveSimulationState(
    val homeScore: Int,
    val awayScore: Int,
    val quarter: Int,
    val timeLeft: String,
    val narration: String,
    val isFinished: Boolean,
    val result: GameSimulator.GameResult? = null
)
