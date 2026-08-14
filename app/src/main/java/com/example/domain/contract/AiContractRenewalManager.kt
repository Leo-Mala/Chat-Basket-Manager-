package com.example.domain.contract

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
        maximumRenewalAge: Int = 33
    ): Result {
        require(maxRenewalsPerTeam >= 0) { "maxRenewalsPerTeam must be non-negative" }
        require(maximumRenewalAge >= 18) { "maximumRenewalAge must be at least 18" }

        val nextContracts = continuingContracts.toMutableMap()
        val renewed = linkedSetOf<Int>()

        teams.forEach { team ->
            if (team.name == userTeamName) return@forEach

            val retentionFloor = maxOf(74, team.getAverageOverall().toInt() - 3)
            team.players
                .asSequence()
                .filter { it.id in expiredPlayerIds }
                .filter { it.age <= maximumRenewalAge }
                .filter { it.overall >= retentionFloor }
                .sortedWith(
                    compareByDescending<com.example.models.Player> { it.overall }
                        .thenBy { it.age }
                        .thenBy { it.id }
                )
                .take(maxRenewalsPerTeam)
                .forEach { player ->
                    nextContracts[player.id] = contractManager.create(
                        player,
                        team.abbreviation,
                        contractManager.recommendedOffer(player)
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
