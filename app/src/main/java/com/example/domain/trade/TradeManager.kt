package com.example.domain.trade

import com.example.models.*
import com.example.domain.rules.TradeRules
import kotlin.random.Random

data class TradeProposal(val offeredPlayer: Player, val teamName: String)
data class TradeResult(val userTeam: NbaTeam, val updatedLeague: List<NbaTeam>)

class TradeManager {
    fun propose(season: Season, managedTeam: NbaTeam, myPlayer: Player): TradeProposal? {
        val candidates = season.teams
            .filter { it.name != managedTeam.name }
            .flatMap { team -> team.players.map { it to team.name } }
            .filter { (player, _) -> player.overall in (myPlayer.overall - 5)..(myPlayer.overall + 5) }
        val selected = candidates.randomOrNull() ?: return null
        return TradeProposal(selected.first, selected.second)
    }

    fun execute(season: Season, managedTeam: NbaTeam, myPlayer: Player, offeredPlayer: Player, outgoingContract: PlayerContract? = null): TradeResult? {
        val opponent = season.teams.firstOrNull { it.players.any { p -> p.id == offeredPlayer.id } } ?: return null
        if (outgoingContract?.noTrade == true) return null
        if (!TradeRules.canTrade(managedTeam, opponent, myPlayer, offeredPlayer)) return null
        val userPlayers = managedTeam.players.map { if (it.id == myPlayer.id) offeredPlayer else it }
        val updatedUser = managedTeam.copy(players = userPlayers)
        val league = season.teams.map { team ->
            when {
                team.name == managedTeam.name -> updatedUser
                team.name == opponent.name -> team.copy(players = team.players.map { if (it.id == offeredPlayer.id) myPlayer else it })
                else -> team
            }
        }
        return TradeResult(updatedUser, league)
    }
}
