package com.example

import com.example.models.Player
import com.example.simulator.MatchSimulationEngine
import com.example.simulator.validation.SimulationValidator
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class SimulationValidatorTest {
    private fun roster() = List(10) { i ->
        val o = 72 + i
        Player(i + 100, "P$i", listOf("PG", "SG", "SF", "PF", "C")[i % 5], o, o, o, o, o, o, 25)
    }

    @Test fun `validator accepts generated team lines`() {
        val lines = MatchSimulationEngine(Random(77)).generateTeamLines(roster(), 118, 111, true, 84.0, 82.0)
        assertTrue(SimulationValidator.isValidTeamLines(lines))
        assertTrue(SimulationValidator.validateTeamLines(lines).isEmpty())
    }
}
