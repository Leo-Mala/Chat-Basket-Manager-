package com.example.simulator

import com.example.models.Coach
import com.example.models.DifficultyLevel
import com.example.models.NbaTeam
import com.example.models.Tactics
import kotlin.math.roundToInt
import kotlin.random.Random

data class TeamSimulationProfile(
    val offense: Double,
    val defense: Double,
    val pace: Double = 1.0,
    val homeAdvantage: Double = 0.0
)

/** Centralized balance model for score generation. */
object SimulationRules {
    private const val LEAGUE_AVERAGE_SCORE = 113.0
    // A 20-point team-quality gap must create a strong favorite, not a guaranteed winner.
    private const val MATCHUP_SCALE = 0.34
    private const val SCORE_VARIANCE = 14.0

    fun difficultyUserModifier(difficulty: Int): Double = DifficultyLevel.fromValue(difficulty).userModifier

    fun difficultyOpponentModifier(difficulty: Int): Double = DifficultyLevel.fromValue(difficulty).opponentModifier

    fun teamOffense(team: NbaTeam): Double {
        val players = team.players.filter { it.isAvailable() }
        if (players.isEmpty()) return 72.0
        val sorted = players.sortedByDescending { it.overall }
        val starters = sorted.take(5)
        val bench = sorted.drop(5)
        val starter = starters.map { it.shooting * .55 + it.passing * .30 + it.athleticism * .15 }.average()
        val reserve = bench.take(5).let { list -> if (list.isEmpty()) starter else list.map { it.shooting * .55 + it.passing * .30 + it.athleticism * .15 }.average() }
        return starter * .78 + reserve * .22
    }

    fun teamDefense(team: NbaTeam): Double {
        val players = team.players.filter { it.isAvailable() }
        if (players.isEmpty()) return 72.0
        val sorted = players.sortedByDescending { it.overall }
        val starters = sorted.take(5)
        val bench = sorted.drop(5)
        val starter = starters.map { it.defense * .65 + it.rebound * .35 }.average()
        val reserve = bench.take(5).let { list -> if (list.isEmpty()) starter else list.map { it.defense * .65 + it.rebound * .35 }.average() }
        return starter * .78 + reserve * .22
    }

    fun profile(team: NbaTeam, tactics: Tactics, coach: Coach?, home: Boolean): TeamSimulationProfile {
        val off = teamOffense(team) * tactics.getOffensiveModifier() * (1.0 + (coach?.getOffensiveBonus() ?: 0.0))
        val def = teamDefense(team) * tactics.getDefensiveModifier() * (1.0 + (coach?.getDefensiveBonus() ?: 0.0))
        return TeamSimulationProfile(
            offense = off,
            defense = def,
            pace = tactics.getPaceFactor(),
            // A modest two-point home edge produces a meaningful but not dominant advantage.
            homeAdvantage = if (home) 2.0 else 0.0
        )
    }

    fun expectedScore(offense: TeamSimulationProfile, opponent: TeamSimulationProfile, rng: Random): Int {
        val paceFactor = ((offense.pace + opponent.pace) / 2.0).coerceIn(.82, 1.20)
        val matchup = (offense.offense - opponent.defense) * MATCHUP_SCALE
        val raw = LEAGUE_AVERAGE_SCORE + matchup + offense.homeAdvantage
        // Basketball outcomes need enough game-to-game noise for real upsets to remain possible.
        val variance = rng.nextDouble(-SCORE_VARIANCE, SCORE_VARIANCE)
        return (raw * paceFactor + variance).roundToInt().coerceIn(78, 145)
    }
}
