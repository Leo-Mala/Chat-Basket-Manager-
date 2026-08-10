package com.example.domain.rules

import com.example.models.NbaTeam
import com.example.models.Player

object TradeRules {
    fun isEligible(team: NbaTeam, player: Player): Boolean =
        team.players.any { it.id == player.id } && player.isAvailable()

    fun canTrade(from: NbaTeam, to: NbaTeam, outgoing: Player, incoming: Player): Boolean {
        if (from.name == to.name) return false
        if (!isEligible(from, outgoing)) return false
        if (to.players.none { it.id == incoming.id }) return false
        return kotlin.math.abs(outgoing.overall - incoming.overall) <= 12
    }
}
