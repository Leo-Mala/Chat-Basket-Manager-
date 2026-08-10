package com.example.domain.rules

object SeasonRules {
    const val REGULAR_SEASON_GAMES = 82
    const val PLAYOFF_SERIES_WINS = 4
    const val MAX_PLAYER_AGE = 38
    const val MIN_PLAYOFF_TEAMS_PER_CONFERENCE = 8

    fun isRegularSeasonComplete(gamesPlayed: Int): Boolean = gamesPlayed >= REGULAR_SEASON_GAMES

    fun homeTeamIsHigherSeedGame(gameNumber: Int): Boolean = gameNumber in setOf(1, 2, 5, 7)
}
