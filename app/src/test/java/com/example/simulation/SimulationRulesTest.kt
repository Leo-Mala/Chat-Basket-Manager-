package com.example.simulation

import com.example.models.*
import com.example.simulator.MatchSimulationEngine
import com.example.simulator.SimulationRules
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SimulationRulesTest {
    private fun player(id: Int, ovr: Int) = Player(id, "P$id", "SG", ovr, ovr, ovr, ovr, ovr, ovr, 24)

    @Test fun expectedScoreStaysWithinBasketballRange() {
        val team = NbaTeam("A", "A", "A", "East", Arena("A", "A", 18000, 2000), List(12) { player(it, 80) })
        val p1 = SimulationRules.profile(team, Tactics(), null, true)
        val p2 = SimulationRules.profile(team, Tactics(), null, false)
        repeat(200) {
            val score = SimulationRules.expectedScore(p1, p2, Random(it))
            assertTrue(score in 78..145)
        }
    }

    @Test fun difficultyGetsStrictlyHarderAtEachLevel() {
        val userModifiers = (0..3).map(SimulationRules::difficultyUserModifier)
        val opponentModifiers = (0..3).map(SimulationRules::difficultyOpponentModifier)

        assertTrue(userModifiers.zipWithNext().all { (easier, harder) -> easier > harder })
        assertTrue(opponentModifiers.zipWithNext().all { (easier, harder) -> easier < harder })
        assertTrue(SimulationRules.difficultyUserModifier(3) == 0.90)
        assertTrue(SimulationRules.difficultyOpponentModifier(3) == 1.10)
        assertTrue(DifficultyLevel.entries.map { it.value } == listOf(0, 1, 2, 3))
        assertTrue(DifficultyLevel.fromValue(3).label == "Muito Difícil")
        assertTrue(DifficultyLevel.fromValue(999) == DifficultyLevel.NORMAL)
    }

    @Test fun engineProducesNonNegativeStats() {
        val players = List(12) { player(it, 75 + (it % 10)) }
        val result = MatchSimulationEngine(Random(42)).generateTeamLines(players, 112, 108, true, 82.0, 80.0)
        assertTrue(result.points == 112)
        assertTrue(result.lines.sumOf { it.minutes } == 240)
        assertTrue(result.lines.all { it.points >= 0 && it.rebounds >= 0 && it.assists >= 0 })
        assertTrue(result.lines.all { it.fgMade <= it.fgAttempted && it.threeMade <= it.threeAttempted && it.ftMade <= it.ftAttempted })
    }
}
