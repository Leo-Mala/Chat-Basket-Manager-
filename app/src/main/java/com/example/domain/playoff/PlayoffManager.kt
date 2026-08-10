package com.example.domain.playoff

import com.example.models.*

class PlayoffManager {
    fun isUserInPlayoffs(result: Season.PlayoffResult, teamName: String): Boolean =
        result.seriesResults.any { it.team1?.name == teamName || it.team2?.name == teamName }

    fun isUserInFinals(result: Season.PlayoffResult, teamName: String): Boolean =
        result.seriesResults.any { series ->
            (series.roundName.equals("Finais da NBA", true) || series.roundName.equals("Grande Final", true) || series.roundName.equals("FINALS", true)) &&
                (series.team1?.name == teamName || series.team2?.name == teamName)
        }

    fun userPrize(result: Season.PlayoffResult, teamName: String): Pair<Int, String> = when {
        result.nbaChampion.name == teamName -> 35_000_000 to "Prêmio Campeão da NBA 🏆"
        isUserInFinals(result, teamName) -> 18_000_000 to "Prêmio Vice-Campeão da NBA 🥈"
        isUserInPlayoffs(result, teamName) -> 8_000_000 to "Prêmio Participação nos Playoffs 🏀"
        else -> 0 to ""
    }

    fun playoffHomeGames(result: Season.PlayoffResult, teamName: String): Int = result.seriesResults
        .filter { it.team1?.name == teamName || it.team2?.name == teamName }
        .flatMap { it.games }
        .count { it.homeTeam.name == teamName }
}
