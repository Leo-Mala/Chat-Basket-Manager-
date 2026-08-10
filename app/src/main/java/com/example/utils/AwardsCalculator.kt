package com.example.utils

import com.example.models.Awards
import com.example.models.NbaTeam
import com.example.models.Player
import com.example.models.Season.SeasonRecord

object AwardsCalculator {

    fun calculateAwards(teams: List<NbaTeam>, standings: Map<String, SeasonRecord>? = null, userCoachName: String = "Você", userTeamName: String? = null): Awards {
        val allPlayers = teams.flatMap { it.players }

        val mvp = allPlayers.maxByOrNull { it.seasonPoints } ?: allPlayers.random()
        val defPlayer = allPlayers.maxByOrNull { it.seasonSteals + it.seasonBlocks } ?: allPlayers.random()
        val sixthMan = allPlayers.filter { it.seasonGames < 50 }.maxByOrNull { it.seasonPoints } ?: allPlayers.random()
        val rookies = allPlayers.filter { it.age <= 21 }
        val rookieOfYear = rookies.maxByOrNull { it.seasonPoints } ?: allPlayers.random()
        val mostImproved = allPlayers.maxByOrNull { it.trainings } ?: allPlayers.random()

        // Calculate Coach of the Year
        var bestTeamName = teams.maxByOrNull { it.getAverageOverall() }?.name ?: "Boston Celtics"
        
        fun isUser(teamName: String): Boolean {
            if (userTeamName == null) return false
            val t1 = teamName.trim().lowercase()
            val t2 = userTeamName.trim().lowercase()
            return t1 == t2 || t1.contains(t2) || t2.contains(t1)
        }

        var coachName = if (isUser(bestTeamName)) {
            userCoachName
        } else {
            "Técnico do " + bestTeamName.replace("Los Angeles ", "").replace("Golden State ", "").replace("New York ", "").replace("Boston ", "").replace("Chicago ", "").replace("Miami ", "")
        }
        
        if (standings != null && standings.isNotEmpty()) {
            val bestRecord = standings.maxByOrNull { it.value.wins }
            bestTeamName = bestRecord?.key ?: bestTeamName
            coachName = if (isUser(bestTeamName)) {
                userCoachName
            } else {
                "Técnico " + bestTeamName.split(" ").last()
            }
        }

        return Awards(
            mvp = mvp,
            defensivePlayer = defPlayer,
            sixthMan = sixthMan,
            rookieOfYear = rookieOfYear,
            mostImproved = mostImproved,
            coachOfYearName = coachName,
            coachOfYearTeam = bestTeamName
        )
    }
}
