package com.example.domain.season

import com.example.models.*

class SeasonManager {
    fun getMatchupsForDay(season: Season, day: Int): List<Pair<NbaTeam, NbaTeam>> {
        val n = season.teams.size
        if (n < 2 || n % 2 != 0) return emptyList()
        val rounds = n - 1
        val r = day % rounds
        val list = season.teams
        val active = mutableListOf<NbaTeam>().apply {
            add(list[0])
            for (i in 1 until n) add(list[(i + r - 1) % (n - 1) + 1])
        }
        return (0 until n / 2).map { active[it] to active[n - 1 - it] }
    }

    fun nextOpponent(season: Season, managedTeam: NbaTeam): Pair<NbaTeam, Boolean>? {
        val matchup = getMatchupsForDay(season, season.currentDay)
            .firstOrNull { it.first.name == managedTeam.name || it.second.name == managedTeam.name }
            ?: return null
        val home = matchup.first.name == managedTeam.name
        return (if (home) matchup.second else matchup.first) to home
    }

    fun advanceSeason(season: Season): Season = season.advanceSeason()
}
