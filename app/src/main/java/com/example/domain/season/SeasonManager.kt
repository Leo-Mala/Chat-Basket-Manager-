package com.example.domain.season

import com.example.models.*

class SeasonManager {
    fun getMatchupsForDay(season: Season, day: Int): List<Pair<NbaTeam, NbaTeam>> {
        val n = season.teams.size
        if (n < 2 || n % 2 != 0 || day < 0) return emptyList()
        val rounds = n - 1
        val round = day % rounds
        val cycle = day / rounds
        val list = season.teams
        val active = mutableListOf<NbaTeam>().apply {
            add(list[0])
            for (i in 1 until n) add(list[(i + round - 1) % (n - 1) + 1])
        }
        val basePairs = (0 until n / 2).map { active[it] to active[n - 1 - it] }
        // Cycle 0/1 form a true home-and-away round robin (58 games for 30 teams).
        // The remaining 24 NBA regular-season dates alternate the whole round's home side,
        // yielding exactly 12 home and 12 away games per team in that third partial cycle.
        val swapHomeAway = when {
            cycle % 2 == 1 -> true
            cycle >= 2 -> round % 2 == 1
            else -> false
        }
        return if (swapHomeAway) basePairs.map { (home, away) -> away to home } else basePairs
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
