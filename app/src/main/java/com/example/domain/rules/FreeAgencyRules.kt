package com.example.domain.rules

import com.example.models.Player

object FreeAgencyRules {
    fun releaseCandidate(players: List<Player>): Player? =
        players.minWithOrNull(compareBy<Player> { it.overall }.thenByDescending { it.age })

    fun validateCandidate(player: Player): Boolean =
        player.age in 18..40 && player.overall in 40..99
}
