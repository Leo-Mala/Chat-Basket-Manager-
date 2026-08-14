package com.example

import com.example.models.Arena
import com.example.models.NbaTeam
import com.example.models.Player
import com.example.models.Tactics
import com.example.simulator.MatchSimulationEngine
import com.example.simulator.SimulationRules
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MatchEngineStatisticalAuditTest {

    @Test
    fun scoreModelAndBoxScoreStayWithinBasketballRanges() {
        val equalHome = team("Equal Home", 10_000, 80)
        val equalAway = team("Equal Away", 20_000, 80)
        val strong = team("Strong", 30_000, 90)
        val weak = team("Weak", 40_000, 70)
        val tactics = Tactics()

        var homeWins = 0
        var strongWins = 0
        var scoreTotal = 0L
        val games = 10_000

        repeat(games) { i ->
            val rng = Random(i * 7919 + 17)
            val hp = SimulationRules.profile(equalHome, tactics, null, home = true)
            val ap = SimulationRules.profile(equalAway, tactics, null, home = false)
            var hs = SimulationRules.expectedScore(hp, ap, rng)
            var ascore = SimulationRules.expectedScore(ap, hp, rng)
            if (hs == ascore) if (rng.nextBoolean()) hs++ else ascore++
            if (hs > ascore) homeWins++
            scoreTotal += hs + ascore

            val strongHome = i % 2 == 0
            val sp = SimulationRules.profile(strong, tactics, null, home = strongHome)
            val wp = SimulationRules.profile(weak, tactics, null, home = !strongHome)
            var ss = SimulationRules.expectedScore(sp, wp, rng)
            var ws = SimulationRules.expectedScore(wp, sp, rng)
            if (ss == ws) if (rng.nextBoolean()) ss++ else ws++
            if (ss > ws) strongWins++
        }

        val homeWinRate = homeWins.toDouble() / games
        val strongWinRate = strongWins.toDouble() / games
        val averageTeamScore = scoreTotal.toDouble() / (games * 2)

        val roster = listOf(
            player(1, "PG", 91, 92, 78, 65, 94, 90),
            player(2, "SG", 89, 94, 76, 62, 80, 90),
            player(3, "SF", 86, 88, 84, 78, 76, 86),
            player(4, "PF", 84, 78, 89, 92, 70, 80),
            player(5, "C", 85, 73, 92, 96, 66, 76),
            player(6, "SG", 80, 84, 76, 60, 74, 81),
            player(7, "SF", 78, 80, 79, 68, 72, 78),
            player(8, "PF", 76, 74, 82, 84, 64, 74),
            player(9, "PG", 74, 78, 72, 58, 82, 76),
            player(10, "C", 72, 68, 84, 88, 58, 70)
        )
        val engine = MatchSimulationEngine(Random(123456))
        var fgMade = 0L
        var fgAtt = 0L
        var threeMade = 0L
        var threeAtt = 0L
        var ftMade = 0L
        var ftAtt = 0L
        var rebounds = 0L
        var assists = 0L
        var topScorerPoints = 0L
        var topScorerMinutes = 0L
        var sampledTeamPoints = 0L
        val samples = 5_000

        repeat(samples) { i ->
            val pts = 104 + (i % 19)
            val lines = engine.generateTeamLines(roster, pts, 108, isHome = i % 2 == 0, offense = 82.0, defense = 81.0).lines
            fgMade += lines.sumOf { it.fgMade }
            fgAtt += lines.sumOf { it.fgAttempted }
            threeMade += lines.sumOf { it.threeMade }
            threeAtt += lines.sumOf { it.threeAttempted }
            ftMade += lines.sumOf { it.ftMade }
            ftAtt += lines.sumOf { it.ftAttempted }
            rebounds += lines.sumOf { it.rebounds }
            assists += lines.sumOf { it.assists }
            val top = lines.maxByOrNull { it.points }!!
            topScorerPoints += top.points
            topScorerMinutes += top.minutes
            sampledTeamPoints += pts
        }

        val fgPct = fgMade.toDouble() / fgAtt
        val threePct = threeMade.toDouble() / threeAtt
        val ftPct = ftMade.toDouble() / ftAtt
        val avgRebounds = rebounds.toDouble() / samples
        val avgAssists = assists.toDouble() / samples
        val topScorerShare = topScorerPoints.toDouble() / sampledTeamPoints
        val avgTopMinutes = topScorerMinutes.toDouble() / samples

        println(
            "MATCH_ENGINE_AUDIT_SUMMARY" +
                "|homeWinRate=${"%.4f".format(homeWinRate)}" +
                "|strongWinRate=${"%.4f".format(strongWinRate)}" +
                "|avgTeamScore=${"%.2f".format(averageTeamScore)}" +
                "|fgPct=${"%.4f".format(fgPct)}" +
                "|threePct=${"%.4f".format(threePct)}" +
                "|ftPct=${"%.4f".format(ftPct)}" +
                "|avgRebounds=${"%.2f".format(avgRebounds)}" +
                "|avgAssists=${"%.2f".format(avgAssists)}" +
                "|topScorerShare=${"%.4f".format(topScorerShare)}" +
                "|avgTopMinutes=${"%.2f".format(avgTopMinutes)}"
        )

        assertTrue("home advantage is too weak/strong: $homeWinRate", homeWinRate in 0.52..0.62)
        assertTrue("strong-vs-weak outcomes are too random/deterministic: $strongWinRate", strongWinRate in 0.72..0.95)
        assertTrue("average team score escaped target band: $averageTeamScore", averageTeamScore in 105.0..120.0)
        assertTrue("field-goal percentage is unrealistic: $fgPct", fgPct in 0.43..0.53)
        assertTrue("three-point percentage is unrealistic: $threePct", threePct in 0.30..0.42)
        assertTrue("free-throw percentage is unrealistic: $ftPct", ftPct in 0.70..0.88)
        assertTrue("rebound volume is unrealistic: $avgRebounds", avgRebounds in 35.0..52.0)
        assertTrue("assist volume is unrealistic: $avgAssists", avgAssists in 18.0..30.0)
        assertTrue("scoring distribution is too flat/concentrated: $topScorerShare", topScorerShare in 0.18..0.36)
        assertTrue("top scorer minutes are unrealistic: $avgTopMinutes", avgTopMinutes in 24.0..38.0)
    }

    private fun team(name: String, idBase: Int, overall: Int): NbaTeam = NbaTeam(
        name = name,
        city = name,
        abbreviation = name.take(3).uppercase(),
        conference = "East",
        arena = Arena("Arena", name, 18_000, 2000),
        players = List(12) { index ->
            Player(idBase + index, "$name-$index", listOf("PG", "SG", "SF", "PF", "C")[index % 5], overall, overall, overall, overall, overall, overall, 25)
        }
    )

    private fun player(id: Int, pos: String, ovr: Int, shot: Int, def: Int, reb: Int, pass: Int, ath: Int) =
        Player(id, "P$id", pos, ovr, shot, def, reb, pass, ath, 25)
}
