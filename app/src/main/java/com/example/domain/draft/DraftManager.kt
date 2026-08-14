package com.example.domain.draft

import com.example.domain.rules.FreeAgencyRules
import com.example.domain.rules.PlayerGenerationRules
import com.example.models.*

class DraftManager {
    data class DraftResult(val team: NbaTeam, val releasedPlayer: Player?)

    private val firstNames = listOf("Cooper", "Ace", "Dylan", "VJ", "Egor", "Karter", "Hugo", "Nolan", "Tre", "Drake", "Koa")
    private val lastNames = listOf("Flagg", "Bailey", "Harper", "Edgecombe", "Demin", "Knox", "Gonzalez", "Traore", "Johnson", "Powell")
    private val positions = listOf("PG", "SG", "SF", "PF", "C")

    fun generateClass(season: Season?, freeAgents: List<Player>, scoutingLevel: Int, size: Int = 6): List<Player> {
        require(scoutingLevel in 1..5) { "Nível de scouting deve estar entre 1 e 5" }
        require(size >= 0) { "Tamanho da classe de Draft inválido" }
        val ids = requireNotNull(season) { "Draft class requires an active season for globally unique player IDs" }.allocatePlayerIds(size)
        return List(size) { index ->
            val id = ids.first + index
            // Scouting affects information quality elsewhere; it must not manufacture stronger athletes.
            val random = kotlin.random.Random(id)
            PlayerGenerationRules.createBalancedPlayer(
                id = id,
                name = "${firstNames[random.nextInt(firstNames.size)]} ${lastNames[random.nextInt(lastNames.size)]}",
                position = positions[random.nextInt(positions.size)],
                targetOverall = random.nextInt(70, 85),
                age = 19,
                random = random
            )
        }
    }

    fun draft(team: NbaTeam, rookie: Player, maxRosterSize: Int = 12): DraftResult {
        require(rookie.age in 18..22) { "Rookie fora da faixa etária do Draft" }
        val players = team.players.toMutableList()
        if (players.any { it.id == rookie.id }) return DraftResult(team, null)
        val released = if (players.size >= maxRosterSize) FreeAgencyRules.releaseCandidate(players) else null
        released?.let { players.remove(it) }
        players += rookie
        return DraftResult(team.copy(players = players), released)
    }
}
