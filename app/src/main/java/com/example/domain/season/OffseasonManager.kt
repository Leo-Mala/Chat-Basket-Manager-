package com.example.domain.season

import com.example.domain.contract.ContractManager
import com.example.domain.draft.AiDraftManager
import com.example.domain.draft.DraftManager
import com.example.domain.roster.AiRosterManager
import com.example.domain.rules.FreeAgencyRules
import com.example.domain.rules.SeasonRules
import com.example.domain.trade.AiTradeManager
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
    private val aiRosterManager: AiRosterManager = AiRosterManager(),
    private val draftManager: DraftManager = DraftManager(),
    private val aiDraftManager: AiDraftManager = AiDraftManager(draftManager),
    private val aiTradeManager: AiTradeManager = AiTradeManager()
) {
    /** Lightweight diagnostics describing the CPU market actions performed in one offseason. */
    data class Activity(
        val cpuTrades: Int = 0,
        val cpuFreeAgentSignings: Int = 0,
        val cpuDraftPicks: Int = 0
    )

    data class Result(
        val season: Season,
        val contracts: Map<Int, PlayerContract>,
        val freeAgents: List<Player>,
        val activity: Activity = Activity()
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

        // CPU-to-CPU trades happen before free agency and the draft. Keep this deliberately
        // conservative: only a small part of the league should reshuffle via trades each year.
        val aiTradeResult = aiTradeManager.rebalance(
            teams = advanced.teams,
            contracts = contractResult.contracts,
            userTeamName = advanced.userTeamName,
            priorityTeamNames = priorityTeamNames,
            maxTrades = 3,
            minimumBalanceGain = 2
        )
        advanced.teams = aiTradeResult.teams

        // Free agency supplements a CPU roster rather than rebuilding half of it every summer.
        // One clear upgrade per team, with a six-point OVR floor, keeps movement meaningful.
        val aiFreeAgencyResult = aiRosterManager.rebalance(
            teams = advanced.teams,
            freeAgents = agedExistingFreeAgents,
            userTeamName = advanced.userTeamName,
            priorityTeamNames = priorityTeamNames,
            maxUpgradesPerTeam = 1,
            minimumUpgrade = 6
        )
        advanced.teams = aiFreeAgencyResult.teams

        // Every CPU franchise receives one draft opportunity. The class is generated only
        // after the season transition, so these rookies begin their first season at age 19
        // without receiving an artificial year of progression. Worst records pick first.
        val cpuTeamCount = advanced.teams.count { it.name != advanced.userTeamName }
        val cpuDraftClass = draftManager.generateClass(
            season = advanced,
            freeAgents = aiFreeAgencyResult.freeAgents,
            scoutingLevel = 1,
            size = cpuTeamCount
        )
        val aiDraftResult = aiDraftManager.draftForCpu(
            teams = advanced.teams,
            rookies = cpuDraftClass,
            userTeamName = advanced.userTeamName,
            priorityTeamNames = priorityTeamNames
        )
        advanced.teams = aiDraftResult.teams

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
            freeAgents = FreeAgencyRules.normalizeMarket(
                aiFreeAgencyResult.freeAgents +
                    agedExpiredPlayers +
                    aiDraftResult.releasedPlayers +
                    aiDraftResult.undraftedRookies
            ),
            activity = Activity(
                cpuTrades = aiTradeResult.trades.size,
                cpuFreeAgentSignings = aiFreeAgencyResult.transactions.size,
                cpuDraftPicks = aiDraftResult.picks.size
            )
        )
    }
}
