package com.example.domain.trade

import com.example.domain.rules.TradeRules
import com.example.domain.season.FranchiseStrategyManager
import com.example.models.NbaTeam
import com.example.models.Player
import com.example.models.PlayerContract
import kotlin.math.abs

/** Conservative deterministic CPU-to-CPU offseason trading. */
class AiTradeManager {
    data class Trade(
        val teamA: String,
        val teamB: String,
        val playerFromAId: Int,
        val playerFromBId: Int
    )

    data class Result(
        val teams: List<NbaTeam>,
        val trades: List<Trade>
    )

    private data class Candidate(
        val teamA: NbaTeam,
        val teamB: NbaTeam,
        val playerA: Player,
        val playerB: Player,
        val gain: Int,
        val strategicFit: Int
    )

    private val positions = listOf("PG", "SG", "SF", "PF", "C")

    fun rebalance(
        teams: List<NbaTeam>,
        contracts: Map<Int, PlayerContract>,
        userTeamName: String?,
        priorityTeamNames: List<String>,
        maxTrades: Int = 6,
        maxOverallDifference: Int = 3,
        minimumBalanceGain: Int = 1,
        policiesByTeamName: Map<String, FranchiseStrategyManager.Policy> = emptyMap()
    ): Result {
        require(maxTrades >= 0) { "maxTrades must be non-negative" }
        require(maxOverallDifference >= 0) { "maxOverallDifference must be non-negative" }
        require(minimumBalanceGain >= 0) { "minimumBalanceGain must be non-negative" }

        val teamByName = teams.associateBy { it.name }.toMutableMap()
        val orderedNames = (priorityTeamNames + teams.map { it.name }).distinct()
        val usedTeams = mutableSetOf<String>()
        val trades = mutableListOf<Trade>()

        for (teamName in orderedNames) {
            if (trades.size >= maxTrades) break
            if (teamName == userTeamName || teamName in usedTeams) continue
            val teamA = teamByName[teamName] ?: continue
            val policyA = policiesByTeamName[teamName]
            val needA = weakestPosition(teamA)
            val protectedA = protectedCore(teamA, policyA)

            val bestCandidate = orderedNames.asSequence()
                .filter { partnerName ->
                    partnerName != teamName &&
                        partnerName != userTeamName &&
                        partnerName !in usedTeams
                }
                .mapNotNull { partnerName ->
                    val teamB = teamByName[partnerName] ?: return@mapNotNull null
                    val policyB = policiesByTeamName[partnerName]
                    val needB = weakestPosition(teamB)
                    if (needA == needB) return@mapNotNull null
                    val protectedB = protectedCore(teamB, policyB)
                    bestMutualTrade(
                        teamA = teamA,
                        teamB = teamB,
                        needA = needA,
                        needB = needB,
                        contracts = contracts,
                        protectedA = protectedA,
                        protectedB = protectedB,
                        maxOverallDifference = maxOverallDifference,
                        minimumBalanceGain = minimumBalanceGain,
                        policyA = policyA,
                        policyB = policyB
                    )
                }
                .sortedWith(
                    compareByDescending<Candidate> { it.gain }
                        .thenByDescending { it.strategicFit }
                        .thenBy { abs(it.playerA.overall - it.playerB.overall) }
                        .thenBy { it.teamB.name }
                        .thenBy { it.playerA.id }
                        .thenBy { it.playerB.id }
                )
                .firstOrNull()
                ?: continue

            val updatedA = swapOut(bestCandidate.teamA, bestCandidate.playerA, bestCandidate.playerB)
            val updatedB = swapOut(bestCandidate.teamB, bestCandidate.playerB, bestCandidate.playerA)
            teamByName[updatedA.name] = updatedA
            teamByName[updatedB.name] = updatedB
            usedTeams += updatedA.name
            usedTeams += updatedB.name
            trades += Trade(
                teamA = updatedA.name,
                teamB = updatedB.name,
                playerFromAId = bestCandidate.playerA.id,
                playerFromBId = bestCandidate.playerB.id
            )
        }

        return Result(
            teams = teams.map { teamByName.getValue(it.name) },
            trades = trades
        )
    }

    private fun bestMutualTrade(
        teamA: NbaTeam,
        teamB: NbaTeam,
        needA: String,
        needB: String,
        contracts: Map<Int, PlayerContract>,
        protectedA: Set<Int>,
        protectedB: Set<Int>,
        maxOverallDifference: Int,
        minimumBalanceGain: Int,
        policyA: FranchiseStrategyManager.Policy?,
        policyB: FranchiseStrategyManager.Policy?
    ): Candidate? {
        val beforeA = balanceScore(teamA)
        val beforeB = balanceScore(teamB)
        val outgoingA = teamA.players.filter { player ->
            player.position == needB &&
                player.id !in protectedA &&
                teamA.players.count { it.position == player.position } >= 2 &&
                tradeEligible(player, contracts)
        }
        val outgoingB = teamB.players.filter { player ->
            player.position == needA &&
                player.id !in protectedB &&
                teamB.players.count { it.position == player.position } >= 2 &&
                tradeEligible(player, contracts)
        }

        return outgoingA.asSequence()
            .flatMap { playerA -> outgoingB.asSequence().map { playerB -> playerA to playerB } }
            .filter { (playerA, playerB) ->
                abs(playerA.overall - playerB.overall) <= maxOverallDifference &&
                    TradeRules.canTrade(teamA, teamB, playerA, playerB) &&
                    TradeRules.canTrade(teamB, teamA, playerB, playerA)
            }
            .mapNotNull { (playerA, playerB) ->
                val afterA = balanceScore(swapOut(teamA, playerA, playerB))
                val afterB = balanceScore(swapOut(teamB, playerB, playerA))
                val gainA = afterA - beforeA
                val gainB = afterB - beforeB
                if (gainA < minimumBalanceGain || gainB < minimumBalanceGain) return@mapNotNull null
                Candidate(
                    teamA = teamA,
                    teamB = teamB,
                    playerA = playerA,
                    playerB = playerB,
                    gain = gainA + gainB,
                    strategicFit = strategyFit(policyA, outgoing = playerA, incoming = playerB) +
                        strategyFit(policyB, outgoing = playerB, incoming = playerA)
                )
            }
            .sortedWith(
                compareByDescending<Candidate> { it.gain }
                    .thenByDescending { it.strategicFit }
                    .thenBy { abs(it.playerA.overall - it.playerB.overall) }
                    .thenBy { it.playerA.id }
                    .thenBy { it.playerB.id }
            )
            .firstOrNull()
    }

    private fun protectedCore(
        team: NbaTeam,
        policy: FranchiseStrategyManager.Policy?
    ): Set<Int> {
        val protectedCount = when (policy?.strategy) {
            FranchiseStrategyManager.Strategy.CONTENDER -> 3
            FranchiseStrategyManager.Strategy.REBUILD -> 1
            FranchiseStrategyManager.Strategy.YOUNG_CORE -> 2
            FranchiseStrategyManager.Strategy.AGING_CORE -> 1
            FranchiseStrategyManager.Strategy.BALANCED,
            null -> 2
        }
        return team.players
            .sortedWith(compareByDescending<Player> { it.overall }.thenBy { it.age }.thenBy { it.id })
            .take(protectedCount)
            .map { it.id }
            .toSet()
    }

    private fun strategyFit(
        policy: FranchiseStrategyManager.Policy?,
        outgoing: Player,
        incoming: Player
    ): Int = when (policy?.strategy) {
        FranchiseStrategyManager.Strategy.CONTENDER -> incoming.overall - outgoing.overall
        FranchiseStrategyManager.Strategy.REBUILD,
        FranchiseStrategyManager.Strategy.YOUNG_CORE,
        FranchiseStrategyManager.Strategy.AGING_CORE -> outgoing.age - incoming.age
        FranchiseStrategyManager.Strategy.BALANCED,
        null -> 0
    }

    private fun tradeEligible(player: Player, contracts: Map<Int, PlayerContract>): Boolean {
        val contract = contracts[player.id] ?: return false
        return contract.yearsRemaining > 0 && !contract.noTrade && player.isAvailable()
    }

    private fun weakestPosition(team: NbaTeam): String =
        positions.minWithOrNull(
            compareBy<String> { positionStrength(team, it) }
                .thenBy { positions.indexOf(it) }
        ) ?: positions.first()

    private fun positionStrength(team: NbaTeam, position: String): Int =
        team.players.filter { it.position == position }.maxOfOrNull { it.overall } ?: 0

    private fun balanceScore(team: NbaTeam): Int =
        positions.sumOf { positionStrength(team, it) }

    private fun swapOut(team: NbaTeam, outgoing: Player, incoming: Player): NbaTeam =
        team.copy(players = team.players.map { if (it.id == outgoing.id) incoming else it })
}
