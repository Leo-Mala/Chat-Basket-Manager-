package com.example.domain.contract

import com.example.domain.rules.ContractRules
import com.example.models.Player
import com.example.models.PlayerContract


data class ContractOffer(val salary: Long, val years: Int, val playerOption: Boolean = false, val noTrade: Boolean = false)
data class ContractEvaluation(val accepted: Boolean, val counterOffer: ContractOffer?, val reason: String)
data class ContractSeasonResult(val contracts: Map<Int, PlayerContract>, val expiredPlayerIds: Set<Int>)

/** Pure contract lifecycle rules. Persistence is handled by the repository. */
class ContractManager {
    fun recommendedOffer(player: Player): ContractOffer =
        ContractOffer(
            salary = ContractRules.annualSalary(player),
            years = when {
                player.age <= 24 -> 4
                player.age <= 29 -> 3
                else -> 2
            }
        )

    fun evaluate(player: Player, offer: ContractOffer): ContractEvaluation {
        val fair = ContractRules.annualSalary(player)
        val minimum = (fair * 0.82).toLong()
        if (offer.salary >= minimum && offer.years in 1..5) {
            val reason = if (offer.salary >= fair) {
                "Oferta em ou acima do valor de mercado"
            } else {
                "Oferta dentro da faixa aceitável"
            }
            return ContractEvaluation(true, null, reason)
        }
        val counter = ContractOffer(fair, offer.years.coerceIn(1, 5), offer.playerOption, offer.noTrade)
        return ContractEvaluation(false, counter, "Oferta abaixo do mínimo aproximado: $minimum")
    }

    fun create(player: Player, teamId: String, offer: ContractOffer): PlayerContract {
        require(offer.years in 1..5) { "Contrato deve ter entre 1 e 5 anos" }
        require(offer.salary >= 0) { "Salário inválido" }
        return PlayerContract(player.id, teamId, offer.salary, offer.years, offer.playerOption, offer.noTrade)
    }

    fun advanceSeason(current: Collection<PlayerContract>): ContractSeasonResult {
        val next = linkedMapOf<Int, PlayerContract>()
        val expired = linkedSetOf<Int>()
        current.forEach { contract ->
            if (contract.yearsRemaining <= 1) {
                expired += contract.playerId
            } else {
                next[contract.playerId] = contract.copy(yearsRemaining = contract.yearsRemaining - 1)
            }
        }
        return ContractSeasonResult(next, expired)
    }

    fun transfer(contract: PlayerContract?, newTeamId: String): PlayerContract? =
        contract?.copy(teamId = newTeamId)
}
