package com.example

import com.example.models.Player
import com.example.simulator.MatchSimulationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchSimulationEngineTest {

    private fun roster(): List<Player> = listOf(
        Player(1, "PG", "PG", 88, 90, 78, 65, 94, 88),
        Player(2, "SG", "SG", 86, 92, 75, 62, 76, 87),
        Player(3, "SF", "SF", 84, 86, 82, 76, 72, 84),
        Player(4, "PF", "PF", 83, 78, 88, 91, 70, 76),
        Player(5, "C", "C", 85, 75, 91, 95, 68, 72),
        Player(6, "Sixth", "SG", 78, 82, 74, 58, 70, 79),
        Player(7, "Seventh", "SF", 75, 78, 77, 65, 68, 75),
        Player(8, "Eighth", "PF", 73, 72, 80, 82, 60, 70),
        Player(9, "Ninth", "PG", 72, 76, 70, 55, 78, 73),
        Player(10, "Tenth", "C", 70, 68, 82, 86, 55, 66)
    )

    @Test
    fun `team lines always close statistically`() {
        val engine = MatchSimulationEngine(kotlin.random.Random(42))
        repeat(100) {
            val home = engine.generateTeamLines(roster(), 112, 104, true, 82.0, 80.0)
            val away = engine.generateTeamLines(roster(), 104, 112, false, 79.0, 83.0)

            assertEquals(112, home.lines.sumOf { it.points })
            assertEquals(104, away.lines.sumOf { it.points })
            assertEquals(240, home.lines.sumOf { it.minutes })
            assertEquals(240, away.lines.sumOf { it.minutes })
            assertEquals(8, home.lines.sumOf { it.plusMinus })
            assertEquals(-8, away.lines.sumOf { it.plusMinus })

            (home.lines + away.lines).forEach { line ->
                assertTrue(line.fgMade >= line.threeMade)
                assertTrue(line.fgAttempted >= line.fgMade)
                assertTrue(line.threeAttempted >= line.threeMade)
                assertTrue(line.ftAttempted >= line.ftMade)
                assertEquals(
                    line.points,
                    2 * (line.fgMade - line.threeMade) + 3 * line.threeMade + line.ftMade
                )
            }
        }
    }
}
