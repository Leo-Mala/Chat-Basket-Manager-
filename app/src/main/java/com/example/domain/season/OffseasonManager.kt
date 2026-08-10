package com.example.domain.season

import com.example.domain.contract.ContractManager
import com.example.models.NbaTeam
import com.example.models.Player
import com.example.models.PlayerContract
import com.example.models.Season

/**
 * Coordinates the pure offseason transition. It owns roster/contract lifecycle rules;
 * UI, finance and persistence remain outside this class.
 */
class OffseasonManager(
    private val contractManager: ContractManager = ContractManager(),
    private val seasonManager: SeasonManager = SeasonManager()
) {
    data class Result(
        val season: Season,
        val contracts: Map<Int, PlayerContract>,
        val freeAgents: List<Player>
    )

    fun advance(
        currentSeason: Season,
        currentContracts: Map<Int, PlayerContract>,
        currentFreeAgents: List<Player>
    ): Result {
        val contractResult = contractManager.advanceSeason(currentContracts.values)
        val expiredIds = contractResult.expiredPlayerIds
        val expiredPlayers = currentSeason.teams
            .flatMap(NbaTeam::players)
            .filter { it.id in expiredIds }

        currentSeason.teams = currentSeason.teams.map { team ->
            team.copy(players = team.players.filterNot { it.id in expiredIds })
        }

        val advanced = seasonManager.advanceSeason(currentSeason)
        val nextContracts = contractResult.contracts.toMutableMap()

        advanced.teams.forEach { team ->
            team.players.forEach { player ->
                val existing = nextContracts[player.id]
                if (existing != null) {
                    nextContracts[player.id] = existing.copy(teamId = team.abbreviation)
                } else {
                    nextContracts[player.id] = contractManager.create(
                        player,
                        team.abbreviation,
                        contractManager.recommendedOffer(player)
                    )
                }
            }
        }

        val activeRosterIds = advanced.teams.flatMap { it.players }.map { it.id }.toSet()
        nextContracts.keys.removeAll { it !in activeRosterIds }

        return Result(
            season = advanced,
            contracts = nextContracts,
            freeAgents = (currentFreeAgents + expiredPlayers).distinctBy { it.id }
        )
    }
}
