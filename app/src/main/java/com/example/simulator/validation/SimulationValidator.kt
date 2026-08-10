package com.example.simulator.validation

import com.example.simulator.MatchSimulationEngine

/**
 * Invariant checks for generated box scores. Kept pure so it can be used by
 * unit tests and optional debug assertions without depending on Android.
 */
object SimulationValidator {
    fun validateTeamLines(lines: MatchSimulationEngine.TeamLines): List<String> {
        val errors = mutableListOf<String>()
        val rows = lines.lines

        if (rows.sumOf { it.minutes } != 240) {
            errors += "Team minutes must equal 240; got ${rows.sumOf { it.minutes }}"
        }
        if (rows.sumOf { it.points } != lines.points) {
            errors += "Player points do not equal team points"
        }
        rows.forEach { line ->
            if (line.minutes !in 0..48) errors += "Invalid minutes for ${line.player.name}"
            if (line.points < 0 || line.rebounds < 0 || line.assists < 0 || line.steals < 0 || line.blocks < 0 || line.turnovers < 0 || line.fouls < 0) {
                errors += "Negative statistic for ${line.player.name}"
            }
            if (line.fgMade < line.threeMade) errors += "FGM < 3PM for ${line.player.name}"
            if (line.fgAttempted < line.fgMade) errors += "FGA < FGM for ${line.player.name}"
            if (line.threeAttempted < line.threeMade) errors += "3PA < 3PM for ${line.player.name}"
            if (line.ftAttempted < line.ftMade) errors += "FTA < FTM for ${line.player.name}"
            val calculated = 2 * (line.fgMade - line.threeMade) + 3 * line.threeMade + line.ftMade
            if (calculated != line.points) errors += "Scoring identity failed for ${line.player.name}"
        }
        return errors
    }

    fun isValidTeamLines(lines: MatchSimulationEngine.TeamLines): Boolean = validateTeamLines(lines).isEmpty()
}
