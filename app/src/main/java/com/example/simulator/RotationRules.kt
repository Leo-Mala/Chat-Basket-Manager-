package com.example.simulator

import com.example.models.Player

/** Builds a legal game-day rotation before box-score generation. */
object RotationRules {
    const val MIN_PLAYERS_FOR_GAME = 5

    fun eligibleForGame(players: List<Player>, minimumPlayers: Int = MIN_PLAYERS_FOR_GAME): List<Player> {
        require(minimumPlayers > 0)
        val unique = players.distinctBy { it.id }
        val available = unique.filter { it.isAvailable() }
        if (available.size >= minimumPlayers) return available

        // A career save should normally have a deep enough roster that this path is never needed.
        // If several injuries overlap, use the closest-to-returning injured players as emergency
        // participants rather than emitting an impossible box score with fewer than 240 minutes.
        val emergency = unique
            .filterNot { it.isAvailable() }
            .sortedWith(
                compareBy<Player> { it.injuryDays }
                    .thenByDescending { it.overall }
                    .thenBy { it.id }
            )
            .take((minimumPlayers - available.size).coerceAtLeast(0))

        return available + emergency
    }
}
