package com.example.domain.rules

import com.example.models.Player

object FreeAgencyRules {
    const val MAX_MARKET_SIZE = 180

    fun releaseCandidate(players: List<Player>): Player? =
        players.minWithOrNull(compareBy<Player> { it.overall }.thenByDescending { it.age })

    fun validateCandidate(player: Player): Boolean =
        player.age in 18..40 && player.overall in 40..99

    fun normalizeMarket(players: Collection<Player>): List<Player> =
        players
            .distinctBy { it.id }
            .filter(::validateCandidate)
            .sortedWith(
                compareByDescending<Player> { it.overall }
                    .thenBy { it.age }
                    .thenBy { it.id }
            )
            .take(MAX_MARKET_SIZE)
}
