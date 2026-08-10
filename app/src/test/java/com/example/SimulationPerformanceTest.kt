package com.example

import com.example.models.Player
import com.example.simulator.MatchSimulationEngine
import com.example.simulator.validation.SimulationValidator
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Lightweight regression budget: simulation throughput must remain bounded on CI hardware. */
class SimulationPerformanceTest {
    @Test(timeout = 8000)
    fun engineCompletesTwoThousandTeamBoxesWithinBudget() {
        val roster = List(18) { i ->
            val overall = 72 + (i % 16)
            Player(5000 + i, "Perf $i", listOf("PG", "SG", "SF", "PF", "C")[i % 5], overall, overall, overall, overall, overall, overall, 24)
        }
        val engine = MatchSimulationEngine(Random(2026))
        repeat(2000) { index ->
            val result = engine.generateTeamLines(roster, 85 + index % 45, 80 + index % 50, index % 2 == 0, 78.0, 79.0)
            assertTrue(SimulationValidator.isValidTeamLines(result))
        }
    }
}
