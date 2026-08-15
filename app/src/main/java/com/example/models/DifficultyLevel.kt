package com.example.models

/**
 * Single source of truth for career difficulty across setup, settings and simulation.
 * The persisted value remains an Int for backward-compatible saves.
 */
enum class DifficultyLevel(
    val value: Int,
    val label: String,
    val description: String,
    val userModifier: Double,
    val opponentModifier: Double
) {
    EASY(
        value = 0,
        label = "Fácil",
        description = "Maior vantagem em simulações e facilidade para evoluir a franquia.",
        userModifier = 1.06,
        opponentModifier = 0.94
    ),
    NORMAL(
        value = 1,
        label = "Normal / Médio",
        description = "Desafio equilibrado e realista para a temporada.",
        userModifier = 0.98,
        opponentModifier = 1.02
    ),
    HARD(
        value = 2,
        label = "Difícil",
        description = "Adversários mais competitivos e menor margem para erros.",
        userModifier = 0.94,
        opponentModifier = 1.06
    ),
    VERY_HARD(
        value = 3,
        label = "Muito Difícil",
        description = "Desafio extremo: sua equipe rende menos e os adversários recebem vantagem máxima nas simulações.",
        userModifier = 0.90,
        opponentModifier = 1.10
    );

    companion object {
        fun fromValue(value: Int): DifficultyLevel = entries.firstOrNull { it.value == value } ?: NORMAL
    }
}
