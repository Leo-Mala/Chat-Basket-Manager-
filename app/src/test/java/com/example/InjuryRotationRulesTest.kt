package com.example

import com.example.models.Player
import com.example.simulator.InjuryRules
import com.example.simulator.MatchSimulationEngine
import com.example.simulator.RotationRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class InjuryRotationRulesTest {

    private fun roster(): List<Player> = (1..12).map { index ->
        val position = listOf("PG", "SG", "SF", "PF", "C")[(index - 1) % 5]
        Player(
            id = index,
            name = "Player $index",
            position = position,
            overall = 82 - index / 3,
            shooting = 80,
            defense = 80,
            rebound = 80,
            passing = 80,
            athleticism = 80
        )
    }

    @Test
    fun injuryRateIsCalibratedAndMedicalStaffHelps() {
        assertEquals(8, InjuryRules.probabilityPerThousand(false, 1))
        assertEquals(8, InjuryRules.probabilityPerThousand(true, 1))
        assertEquals(4, InjuryRules.probabilityPerThousand(true, 5))

        val random = Random(42)
        val injuries = (1..100_000).count {
            InjuryRules.shouldInjure(random, InjuryRules.CPU_INJURY_PER_THOUSAND)
        }
        assertTrue("0.8% injury target escaped a broad deterministic band: $injuries", injuries in 650..950)
    }

    @Test
    fun injuryDurationRemainsBoundedAndMedicalStaffReducesRecoveryTime() {
        val cpuRandom = Random(7)
        val userRandom = Random(7)
        val cpuDurations = (1..1_000).map { InjuryRules.daysOut(cpuRandom, false, 1) }
        val eliteMedicalDurations = (1..1_000).map { InjuryRules.daysOut(userRandom, true, 5) }

        assertTrue(cpuDurations.all { it in 2..8 })
        assertTrue(eliteMedicalDurations.all { it in 1..4 })
        assertTrue(eliteMedicalDurations.average() < cpuDurations.average())
    }

    @Test
    fun healthyRotationNeverUsesInjuredPlayersWhenFiveAreAvailable() {
        val roster = roster()
        roster.takeLast(4).forEach {
            it.injured = true
            it.injuryDays = 5
        }

        val eligible = RotationRules.eligibleForGame(roster)
        assertEquals(8, eligible.size)
        assertTrue(eligible.none { it.injured })
    }

    @Test
    fun emergencyRotationRestoresFivePlayersAndStillProduces240Minutes() {
        val roster = roster()
        roster.drop(3).forEachIndexed { index, player ->
            player.injured = true
            player.injuryDays = index + 1
        }

        val eligible = RotationRules.eligibleForGame(roster)
        assertEquals(5, eligible.size)
        assertEquals(3, eligible.count { !it.injured })
        assertEquals(listOf(1, 2), eligible.filter { it.injured }.map { it.injuryDays }.sorted())

        val lines = MatchSimulationEngine(Random(99)).generateTeamLines(
            players = eligible,
            teamPoints = 110,
            opponentPoints = 105,
            isHome = true,
            offense = 80.0,
            defense = 80.0
        )
        assertEquals(240, lines.lines.sumOf { it.minutes })
        assertEquals(110, lines.lines.sumOf { it.points })
    }
}
