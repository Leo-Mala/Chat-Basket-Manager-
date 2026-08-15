package com.example.domain.roster

import com.example.domain.rules.FreeAgencyRules
import com.example.domain.season.FranchiseStrategyManager
import com.example.models.NbaTeam
import com.example.models.Player

/** Deterministic offseason roster management for CPU-controlled franchises. */
class AiRosterManager {
    data class Transaction(
        val teamName: String,
        val signedPlayerId: Int,
        val releasedPlayerId: Int? = null
    )

    data class Result(
        val teams: List<NbaTeam>,
        val freeAgents: List<Player>,
        val transactions: List<Transaction>
    )

    private val positions = listOf("PG", "SG", "SF", "PF", "C")

    fun rebalance(
        teams: List<NbaTeam>,
        freeAgents: Collection<Player>,
        userTeamName: String?,
        priorityTeamNames: List<String>,
        protectedPlayerIds: Set<Int> = emptySet(),
        maxUpgradesPerTeam: Int = 2,
        minimumUpgrade: Int = 4,
        minimumRosterSize: Int = 12,
        policiesByTeamName: Map<String, FranchiseStrategyManager.Policy> = emptyMap()
    ): Result {
        require(maxUpgradesPerTeam >= 0) { "maxUpgradesPerTeam must be non-negative" }
        require(minimumUpgrade >= 0) { "minimumUpgrade must be non-negative" }
        require(minimumRosterSize >= 0) { "minimumRosterSize must be non-negative" }

        val teamByName = teams.associateBy { it.name }.toMutableMap()
        val market = FreeAgencyRules.normalizeMarket(freeAgents).toMutableList()
        val signablePlayerIds = market.map { it.id }.toMutableSet()
        val transactions = mutableListOf<Transaction>()
        val orderedNames = (priorityTeamNames + teams.map { it.name }).distinct()

        orderedNames.forEach { teamName ->
            if (teamName == userTeamName) return@forEach
            var team = teamByName[teamName] ?: return@forEach
            val policy = policiesByTeamName[teamName]

            while (team.players.size < minimumRosterSize) {
                val eligible = eligibleMarket(market, signablePlayerIds, protectedPlayerIds)
                if (eligible.isEmpty()) break

                val neededPosition = weakestCoveredPosition(team)
                val samePosition = eligible.filter { it.position == neededPosition }
                val candidatePool = if (samePosition.isNotEmpty()) samePosition else eligible
                val candidate = bestCandidate(candidatePool, preferYouth = policy?.preferYouth == true) ?: break

                team = team.copy(players = team.players + candidate)
                teamByName[teamName] = team
                market.removeAll { it.id == candidate.id }
                signablePlayerIds.remove(candidate.id)
                transactions += Transaction(teamName = teamName, signedPlayerId = candidate.id)
            }

            val teamUpgradeLimit = policy?.freeAgencyUpgrades ?: maxUpgradesPerTeam
            val teamMinimumUpgrade = policy?.minimumFreeAgencyUpgrade ?: minimumUpgrade
            for (upgrade in 0 until teamUpgradeLimit) {
                val weakest = FreeAgencyRules.releaseCandidate(team.players) ?: break
                val eligible = eligibleMarket(market, signablePlayerIds, protectedPlayerIds)
                    .filter { it.overall >= weakest.overall + teamMinimumUpgrade }
                if (eligible.isEmpty()) break

                val samePosition = eligible.filter { it.position == weakest.position }
                val candidatePool = if (samePosition.isNotEmpty()) samePosition else eligible
                val candidate = bestCandidate(candidatePool, preferYouth = policy?.preferYouth == true) ?: break

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

    private fun eligibleMarket(
        market: List<Player>,
        signablePlayerIds: Set<Int>,
        protectedPlayerIds: Set<Int>
    ): List<Player> = market.filter {
        it.id in signablePlayerIds &&
            it.id !in protectedPlayerIds &&
            FreeAgencyRules.validateCandidate(it)
    }

    private fun bestCandidate(players: List<Player>, preferYouth: Boolean): Player? =
        if (preferYouth) {
            players.maxWithOrNull(
                compareBy<Player> { -it.overall }
                    .thenBy { it.age }
                    .thenBy { it.id }
            )
        } else {
            players.maxWithOrNull(
                compareBy<Player> { it.overall }
                    .thenBy { -it.age }
                    .thenBy { -it.id }
            )
        }

    private fun weakestCoveredPosition(team: NbaTeam): String =
        positions.minWithOrNull(
            compareBy<String> { position -> team.players.count { it.position == position } }
                .thenBy { position -> team.players.filter { it.position == position }.maxOfOrNull { it.overall } ?: 0 }
                .thenBy { positions.indexOf(it) }
        ) ?: positions.first()
}
