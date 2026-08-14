package com.example.domain.draft

import com.example.models.*

class DraftManager {
    data class DraftResult(val team: NbaTeam, val releasedPlayer: Player?)

    private val firstNames = listOf("Cooper", "Ace", "Dylan", "VJ", "Egor", "Karter", "Hugo", "Nolan", "Tre", "Drake", "Koa")
    private val lastNames = listOf("Flagg", "Bailey", "Harper", "Edgecombe", "Demin", "Knox", "Gonzalez", "Traore", "Johnson", "Powell")
    private val positions = listOf("PG", "SG", "SF", "PF", "C")

    fun generateClass(season: Season?, freeAgents: List<Player>, scoutingLevel: Int, size: Int = 6): List<Player> {
        val minOvr = 69 + scoutingLevel
        val maxOvr = 81 + (scoutingLevel * 1.5).toInt()
        val ids = requireNotNull(season) { "Draft class requires an active season for globally unique player IDs" }.allocatePlayerIds(size)
        return List(size) { index ->
            val random = kotlin.random.Random(ids.first + index)
            val ovr = random.nextInt(minOvr, maxOvr + 1)
            Player(
                ids.elementAt(index),
                "${firstNames[random.nextInt(firstNames.size)]} ${lastNames[random.nextInt(lastNames.size)]}",
                positions[random.nextInt(positions.size)],
                ovr,
                ovr - random.nextInt(0, 9), ovr - random.nextInt(0, 9),
                ovr - random.nextInt(0, 9), ovr - random.nextInt(0, 9),
                ovr - random.nextInt(0, 9), 19
            )
        }
    }

    fun draft(team: NbaTeam, rookie: Player, maxRosterSize: Int = 12): DraftResult {
        require(rookie.age in 18..22) { "Rookie fora da faixa etária do Draft" }
        val players = team.players.toMutableList()
        if (players.any { it.id == rookie.id }) return DraftResult(team, null)
        val released = if (players.size >= maxRosterSize) players.minByOrNull { it.overall } else null
        released?.let { players.remove(it) }
        players += rookie
        return DraftResult(team.copy(players = players), released)
    }
}
