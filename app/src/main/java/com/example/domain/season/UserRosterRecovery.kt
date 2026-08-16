package com.example.domain.season

import com.example.domain.contract.ContractManager
import com.example.domain.rules.SeasonRules
import com.example.models.HistoryManager
import com.example.models.NbaTeam
import com.example.models.Player
import com.example.models.PlayerContract

/**
 * Repairs the specific legacy corruption where the managed roster was silently emptied
 * by expired contracts while those same players were moved to free agency.
 *
 * Recovery is intentionally conservative, safe and idempotent:
 * - only the first few days of a new season are eligible;
 * - the immediately preceding season history must exist;
 * - only players that were on that historical managed roster AND are still free agents
 *   can be restored;
 * - players already signed by another team are never taken back;
 * - retired/over-age players are never recreated.
 */
class UserRosterRecovery(
    private val contractManager: ContractManager = ContractManager()
) {
    data class Result(
        val team: NbaTeam,
        val freeAgents: List<Player>,
        val contracts: Map<Int, PlayerContract>,
        val recoveredPlayerIds: Set<Int> = emptySet()
    )

    fun recover(
        currentSeasonNumber: Int,
        currentDay: Int,
        team: NbaTeam,
        history: HistoryManager,
        freeAgents: List<Player>,
        contracts: Map<Int, PlayerContract>,
        maxRosterSize: Int = 12
    ): Result {
        val unchanged = Result(team, freeAgents, contracts)
        if (currentDay > 5 || team.players.size >= maxRosterSize) return unchanged

        val previousSeason = history.seasons.maxByOrNull { it.seasonNumber } ?: return unchanged
        if (previousSeason.seasonNumber != currentSeasonNumber - 1) return unchanged
        if (previousSeason.playerStats.isEmpty()) return unchanged

        val currentIds = team.players.map { it.id }.toSet()
        val freeAgentsById = freeAgents
            .asSequence()
            .filter { it.age <= SeasonRules.MAX_PLAYER_AGE }
            .associateBy { it.id }

        // Preserve historical roster order so the repair is deterministic. We use the
        // current free-agent Player object because it already contains the offseason age,
        // development and reset season stats; history is only proof of prior ownership.
        val targetSize = minOf(maxRosterSize, previousSeason.playerStats.size)
        val needed = (targetSize - team.players.size).coerceAtLeast(0)
        val recovered = previousSeason.playerStats
            .asSequence()
            .filter { it.id !in currentIds }
            .mapNotNull { freeAgentsById[it.id] }
            .distinctBy { it.id }
            .take(needed)
            .toList()

        if (recovered.isEmpty()) return unchanged

        val recoveredIds = recovered.map { it.id }.toSet()
        val repairedTeam = team.copy(players = (team.players + recovered).distinctBy { it.id })
        val repairedContracts = contracts.toMutableMap().apply {
            recovered.forEach { player ->
                put(
                    player.id,
                    contractManager.create(
                        player,
                        repairedTeam.abbreviation,
                        contractManager.recommendedOffer(player)
                    )
                )
            }
        }

        return Result(
            team = repairedTeam,
            freeAgents = freeAgents.filterNot { it.id in recoveredIds },
            contracts = repairedContracts,
            recoveredPlayerIds = recoveredIds
        )
    }
}