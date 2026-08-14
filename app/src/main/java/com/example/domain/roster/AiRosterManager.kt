package com.example.domain.roster

import com.example.domain.rules.FreeAgencyRules
import com.example.models.NbaTeam
import com.example.models.Player

/** Deterministic offseason roster improvements for CPU-controlled franchises. */
class AiRosterManager {
    data class Transaction(
        val teamName: String,
        val signedPlayerId: Int,
        val releasedPlayerId: Int
    )

    data class Result(
        val teams: List<NbaTeam>,
        val freeAgents: List<Player>,
        val transactions: List<Transaction>
    )

    fun rebalance(
        teams: List<NbaTeam>,
        freeAgents: Collection<Player>,
        userTeamName: String?,
        priorityTeamNames: List<String>,
        protectedPlayerIds: Set<Int> = emptySet(),
        maxUpgradesPerTeam: Int = 2,
        minimumUpgrade: Int = 4
    ): Result {
        require(maxUpgradesPerTeam >= 0) { "maxUpgradesPerTeam must be non-negative" }
        require(minimumUpgrade >= 0) { "minimumUpgrade must be non-negative" }

        val teamByName = teams.associateBy { it.name }.toMutableMap()
        val market = FreeAgencyRules.normalizeMarket(freeAgents).toMutableList()
        // Only players who entered this CPU free-agency phase are eligible to be signed.
        // Players released by a CPU team during this phase return to the market, but cannot
        // trigger a same-window chain of CPU sign/release transactions.
        val signablePlayerIds = market.map { it.id }.toMutableSet()
        val transactions = mutableListOf<Transaction>()
        val orderedNames = (priorityTeamNames + teams.map { it.name }).distinct()

        orderedNames.forEach { teamName ->
            if (teamName == userTeamName) return@forEach
            var team = teamByName[teamName] ?: return@forEach

            for (upgrade in 0 until maxUpgradesPerTeam) {
                val weakest = team.players.minWithOrNull(
                    compareBy<Player> { it.overall }.thenByDescending { it.age }.thenBy { it.id }
                ) ?: break

                val eligible = market.filter {
                    it.id in signablePlayerIds &&
                        it.id !in protectedPlayerIds &&
                        FreeAgencyRules.validateCandidate(it) &&
                        it.overall >= weakest.overall + minimumUpgrade
                }
                if (eligible.isEmpty()) break

                val samePosition = eligible.filter { it.position == weakest.position }
                val candidatePool = if (samePosition.isNotEmpty()) samePosition else eligible
                val candidate = candidatePool.maxWithOrNull(
                    compareBy<Player> { it.overall }
                        .thenBy { -it.age }
                        .thenBy { -it.id }
                ) ?: break

                val updatedPlayers = team.players.toMutableList()
                val weakestIndex = updatedPlayers.indexOfFirst { it.id == weakest.id }
                if (weakestIndex < 0) break
                updatedPlayers[weakestIndex] = candidate
                team = team.copy(players = updatedPlayers)
                teamByName[teamName] = team

                market.removeAll { it.id == candidate.id }
                signablePlayerIds.remove(candidate.id)
                market += weakest
                transactions += Transaction(teamName, candidate.id, weakest.id)
            }
        }

        return Result(
            teams = teams.map { teamByName.getValue(it.name) },
            freeAgents = FreeAgencyRules.normalizeMarket(market),
            transactions = transactions
        )
    }
}
