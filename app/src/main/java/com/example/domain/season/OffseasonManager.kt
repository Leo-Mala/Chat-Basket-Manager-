package com.example.domain.season

import com.example.domain.contract.ContractManager
import com.example.domain.roster.AiRosterManager
import com.example.domain.rules.FreeAgencyRules
import com.example.domain.rules.SeasonRules
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
    private val seasonManager: SeasonManager = SeasonManager(),
    private val aiRosterManager: AiRosterManager = AiRosterManager()
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
        val priorityTeamNames = currentSeason.standings.entries
            .sortedWith(
                compareBy<Map.Entry<String, Season.SeasonRecord>> { it.value.wins }
                    .thenBy { it.value.pointDifference }
                    .thenBy { it.key }
            )
            .map { it.key }

        val contractResult = contractManager.advanceSeason(currentContracts.values)
        val expiredIds = contractResult.expiredPlayerIds
        val expiredPlayers = currentSeason.teams
            .flatMap(NbaTeam::players)
            .filter { it.id in expiredIds }

        currentSeason.teams = currentSeason.teams.map { team ->
            team.copy(players = team.players.filterNot { it.id in expiredIds })
        }

        fun ageMarket(players: Collection<Player>): List<Player> = players
            .distinctBy { it.id }
            .onEach { player ->
                player.advanceSeason()
                player.injured = false
                player.injuryDays = 0
                player.resetSeasonStats()
            }
            .filter { it.age <= SeasonRules.MAX_PLAYER_AGE }

        // CPU teams act only on the market that already existed before this offseason.
        // Fresh contract expirations enter free agency after the CPU phase. This preserves
        // the game's existing contract-expiration phase and avoids instant CPU poaching.
        val agedExistingFreeAgents = ageMarket(currentFreeAgents)
        val agedExpiredPlayers = ageMarket(expiredPlayers)

        val advanced = seasonManager.advanceSeason(currentSeason)
        val aiResult = aiRosterManager.rebalance(
            teams = advanced.teams,
            freeAgents = agedExistingFreeAgents,
            userTeamName = advanced.userTeamName,
            priorityTeamNames = priorityTeamNames
        )
        advanced.teams = aiResult.teams

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
            freeAgents = FreeAgencyRules.normalizeMarket(aiResult.freeAgents + agedExpiredPlayers)
        )
    }
}
