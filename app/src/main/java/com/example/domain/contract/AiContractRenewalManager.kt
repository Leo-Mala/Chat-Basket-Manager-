package com.example.domain.contract

import com.example.domain.season.FranchiseStrategyManager
import com.example.models.NbaTeam
import com.example.models.PlayerContract

/** Deterministic CPU contract retention before players enter free agency. */
class AiContractRenewalManager(
    private val contractManager: ContractManager = ContractManager()
) {
    data class Result(
        val contracts: Map<Int, PlayerContract>,
        val renewedPlayerIds: Set<Int>,
        val unrenewedExpiredPlayerIds: Set<Int>
    )

    fun renewExpiring(
        teams: List<NbaTeam>,
        continuingContracts: Map<Int, PlayerContract>,
        expiredPlayerIds: Set<Int>,
        userTeamName: String?,
        maxRenewalsPerTeam: Int = 3,
        maximumRenewalAge: Int = 33,
        policiesByTeamName: Map<String, FranchiseStrategyManager.Policy> = emptyMap()
    ): Result {
        require(maxRenewalsPerTeam >= 0) { "maxRenewalsPerTeam must be non-negative" }
        require(maximumRenewalAge >= 18) { "maximumRenewalAge must be at least 18" }

        val nextContracts = continuingContracts.toMutableMap()
        val renewed = linkedSetOf<Int>()

        teams.forEach { team ->
            if (team.name == userTeamName) return@forEach

            val policy = policiesByTeamName[team.name]
            val teamMaxRenewals = policy?.maxRenewals ?: maxRenewalsPerTeam
            val teamMaximumAge = policy?.maximumRenewalAge ?: maximumRenewalAge
            val retentionFloor = maxOf(74, team.getAverageOverall().toInt() - 3)
            team.players
                .asSequence()
                .filter { it.id in expiredPlayerIds }
                .filter { it.age <= teamMaximumAge }
                .filter { it.overall >= retentionFloor }
                .sortedWith(
                    if (policy?.preferYouth == true) {
                        compareBy<com.example.models.Player> { it.age }
                            .thenByDescending { it.overall }
                            .thenBy { it.id }
                    } else {
                        compareByDescending<com.example.models.Player> { it.overall }
                            .thenBy { it.age }
                            .thenBy { it.id }
                    }
                )
                .take(teamMaxRenewals)
                .forEach { player ->
                    val marketOffer = contractManager.recommendedOffer(player)
                    val renewalOffer = if (player.overall >= 90) {
                        // CPU teams pay a modest retention premium for proven stars. This keeps
                        // long-lived star contracts economically distinct from declining rotation
                        // contracts without changing the user's negotiation rules.
                        marketOffer.copy(salary = marketOffer.salary * 110L / 100L)
                    } else {
                        marketOffer
                    }
                    nextContracts[player.id] = contractManager.create(
                        player,
                        team.abbreviation,
                        renewalOffer
                    )
                    renewed += player.id
                }
        }

        return Result(
            contracts = nextContracts,
            renewedPlayerIds = renewed,
            unrenewedExpiredPlayerIds = expiredPlayerIds - renewed
        )
    }
}
