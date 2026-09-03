package com.example.domain.roster

import com.example.models.*
import com.example.domain.rules.ContractRules
import com.example.domain.rules.FreeAgencyRules
import com.example.domain.rules.PlayerGenerationRules

data class FreeAgentResult(val players: List<Player>)
data class SigningResult(val team: NbaTeam, val finance: Finance, val releasedPlayer: Player?)

class RosterManager {
    fun bestLineup(team: NbaTeam): List<Player> = team.players.filter { it.isAvailable() }.sortedByDescending { it.overall }.take(5)

    fun syncStartingFive(team: NbaTeam, current: List<Player>): List<Player> {
        val available = team.players.filter { it.isAvailable() }
        val retained = current.mapNotNull { old -> available.firstOrNull { it.id == old.id } }.distinctBy { it.id }.take(5).toMutableList()
        available.sortedByDescending { it.overall }.forEach { p -> if (retained.size < 5 && retained.none { it.id == p.id }) retained += p }
        return retained
    }

    fun generateFreeAgents(season: Season?, draft: List<Player>): FreeAgentResult {
        val first = listOf("Kyrie", "Luka", "James", "Kevin", "Giannis", "Joel", "Nikola", "Stephen", "Kawhi", "Anthony", "Jayson", "Russell", "Chris", "Paul", "Trae")
        val last = listOf("Irving", "Doncic", "Harden", "Durant", "Antetokounmpo", "Embiid", "Jokic", "Curry", "Leonard", "Davis", "Tatum", "Westbrook", "George", "Paul", "Young")
        val positions = listOf("PG", "SG", "SF", "PF", "C")
        val batchSize = PlayerGenerationRules.FREE_AGENT_BATCH_SIZE
        val ids = requireNotNull(season) { "Free-agent generation requires an active season for globally unique player IDs" }.allocatePlayerIds(batchSize)
        return FreeAgentResult(List(batchSize) { index ->
            val id = ids.first + index
            val random = kotlin.random.Random(id)
            PlayerGenerationRules.createBalancedPlayer(
                id = id,
                name = "${first[random.nextInt(first.size)]} ${last[random.nextInt(last.size)]}",
                position = positions[random.nextInt(positions.size)],
                targetOverall = random.nextInt(72, 85),
                age = 20 + random.nextInt(0, 13),
                random = random
            )
        })
    }

    fun signFreeAgent(team: NbaTeam, finance: Finance, player: Player, day: Int): SigningResult? {
        if (!FreeAgencyRules.validateCandidate(player)) return null
        val signingBonus = ContractRules.signingBonus(player).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        if (finance.budget < signingBonus) return null
        val players = team.players.toMutableList()
        if (!ContractRules.canSign(players.size)) return null
        val released = if (ContractRules.mustReleaseForStandardRoster(players.size)) FreeAgencyRules.releaseCandidate(players) else null
        released?.let { players.remove(it) }
        players += player
        val updatedFinance = finance.copy(
            budget = finance.budget - signingBonus,
            expenses = (finance.expenses + Expense("Bônus de assinatura: ${player.name}", signingBonus, "Dia $day")).toMutableList()
        )
        return SigningResult(team.copy(players = players), updatedFinance, released)
    }
}
