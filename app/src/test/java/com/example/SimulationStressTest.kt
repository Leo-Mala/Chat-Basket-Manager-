package com.example

import com.example.models.Player
import com.example.simulator.MatchSimulationEngine
import com.example.simulator.validation.SimulationValidator
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Regression/stress smoke test: catches accidental O(n) state leaks or broken invariants. */
class SimulationStressTest {
    @Test fun `engine survives 1000 generated team boxes`() {
        val roster = List(15) { i ->
            val o = 70 + (i % 20)
            Player(2000 + i, "Stress $i", listOf("PG", "SG", "SF", "PF", "C")[i % 5], o, o, o, o, o, o, 24)
        }
        val engine = MatchSimulationEngine(Random(1234))
        repeat(1000) { i ->
            val home = engine.generateTeamLines(roster, 90 + i % 40, 90 + (i * 3) % 40, true, 80.0, 80.0)
            assertTrue(SimulationValidator.isValidTeamLines(home))
        }
    }
}
