package com.example

import com.example.data.NbaDataGenerator
import com.example.domain.draft.DraftManager
import com.example.models.Player
import com.example.models.Season
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow
import kotlin.math.sqrt

class DraftTalentDevelopmentAuditTest {

    private data class CareerResult(
        val entryOverall: Int,
        val peakOverall: Int,
        val peakAge: Int,
        val overallAt30: Int,
        val overallAt35: Int
    )

    @Test
    fun draftPipelineProducesDiverseSustainableCareers() {
        val teams = NbaDataGenerator.getAllTeams()
        val season = Season(
            teams = teams,
            nextPlayerId = teams.flatMap { it.players }.maxOf { it.id } + 1
        )
        val manager = DraftManager()
        val rookies = mutableListOf<Player>()

        repeat(100) {
            rookies += manager.generateClass(
                season = season,
                freeAgents = emptyList(),
                scoutingLevel = 3,
                size = 30
            )
        }

        val entryAverage = rookies.map { it.overall }.average()
        val entryStdDev = stdDev(rookies.map { it.overall.toDouble() })
        val entry90Share = rookies.count { it.overall >= 90 }.toDouble() / rookies.size
        val entry86Share = rookies.count { it.overall >= 86 }.toDouble() / rookies.size
        val entry82Share = rookies.count { it.overall >= 82 }.toDouble() / rookies.size
        val entryBelow76Share = rookies.count { it.overall < 76 }.toDouble() / rookies.size

        val careers = rookies.take(600).map { simulateCareer(it.copy()) }
        val peakGain = careers.map { it.peakOverall - it.entryOverall }
        val averagePeakGain = peakGain.average()
        val gainStdDev = stdDev(peakGain.map { it.toDouble() })
        val bustShare = peakGain.count { it <= 2 }.toDouble() / careers.size
        val peak90Share = careers.count { it.peakOverall >= 90 }.toDouble() / careers.size
        val peak95Share = careers.count { it.peakOverall >= 95 }.toDouble() / careers.size
        val averagePeakAge = careers.map { it.peakAge }.average()
        val veteransDecliningShare = careers.count { it.overallAt35 < it.peakOverall }.toDouble() / careers.size
        val averageDeclineFromPeakAt35 = careers.map { it.peakOverall - it.overallAt35 }.average()
        val averageAge30Vs35Drop = careers.map { it.overallAt30 - it.overallAt35 }.average()

        println(
            "DRAFT_DEVELOPMENT_AUDIT_SUMMARY" +
                "|classes=100" +
                "|prospects=${rookies.size}" +
                "|entryAvg=${"%.2f".format(entryAverage)}" +
                "|entryStdDev=${"%.2f".format(entryStdDev)}" +
                "|entry90Share=${"%.4f".format(entry90Share)}" +
                "|entry86Share=${"%.4f".format(entry86Share)}" +
                "|entry82Share=${"%.4f".format(entry82Share)}" +
                "|entryBelow76Share=${"%.4f".format(entryBelow76Share)}" +
                "|careerSample=${careers.size}" +
                "|avgPeakGain=${"%.2f".format(averagePeakGain)}" +
                "|gainStdDev=${"%.2f".format(gainStdDev)}" +
                "|bustShare=${"%.4f".format(bustShare)}" +
                "|peak90Share=${"%.4f".format(peak90Share)}" +
                "|peak95Share=${"%.4f".format(peak95Share)}" +
                "|avgPeakAge=${"%.2f".format(averagePeakAge)}" +
                "|veteransDecliningShare=${"%.4f".format(veteransDecliningShare)}" +
                "|avgDeclineAt35=${"%.2f".format(averageDeclineFromPeakAt35)}" +
                "|avgAge30To35Drop=${"%.2f".format(averageAge30Vs35Drop)}"
        )

        assertTrue("draft entry average escaped a sustainable band: $entryAverage", entryAverage in 72.0..82.0)
        assertTrue("draft classes lack rating diversity: $entryStdDev", entryStdDev >= 3.0)
        assertTrue("too many/few instant 90+ prospects: $entry90Share", entry90Share in 0.002..0.035)
        assertTrue("high-end prospect supply is implausible: $entry86Share", entry86Share in 0.025..0.13)
        assertTrue("strong-prospect supply is implausible: $entry82Share", entry82Share in 0.12..0.35)
        assertTrue("draft lacks lower-end prospects: $entryBelow76Share", entryBelow76Share >= 0.25)

        assertTrue("average development is too weak/strong: $averagePeakGain", averagePeakGain in 2.0..12.0)
        assertTrue("career outcomes are too homogeneous: $gainStdDev", gainStdDev >= 1.0)
        assertTrue("draft produces no meaningful bust population: $bustShare", bustShare >= 0.02)
        assertTrue("too many careers become 90+ stars: $peak90Share", peak90Share <= 0.25)
        assertTrue("90+ career pipeline disappeared: $peak90Share", peak90Share >= 0.02)
        assertTrue("too many careers become 95+ superstars: $peak95Share", peak95Share <= 0.10)
        assertTrue("career peaks occur at implausible ages: $averagePeakAge", averagePeakAge in 23.0..31.5)
        assertTrue("veterans fail to decline after their peak: $veteransDecliningShare", veteransDecliningShare >= 0.70)
        assertTrue("age-35 decline is too weak: $averageDeclineFromPeakAt35", averageDeclineFromPeakAt35 >= 1.0)
        assertTrue("players collectively improve from age 30 to 35: $averageAge30Vs35Drop", averageAge30Vs35Drop >= 0.0)
        assertTrue("overall exceeded model ceiling", careers.all { it.peakOverall <= 99 })
    }

    private fun simulateCareer(player: Player): CareerResult {
        val entry = player.overall
        var peak = player.overall
        var peakAge = player.age
        var at30 = player.overall
        var at35 = player.overall

        while (player.age < 35) {
            repeat(82) {
                val points = (12 + (player.overall - 70) / 2).coerceIn(8, 28)
                player.seasonGames++
                player.seasonPoints += points
                player.evolveInSeason(points)
                if (player.overall > peak) {
                    peak = player.overall
                    peakAge = player.age
                }
            }
            player.advanceSeason()
            if (player.overall > peak) {
                peak = player.overall
                peakAge = player.age
            }
            if (player.age == 30) at30 = player.overall
            if (player.age == 35) at35 = player.overall
            player.resetSeasonStats()
        }

        return CareerResult(entry, peak, peakAge, at30, at35)
    }

    private fun stdDev(values: List<Double>): Double {
        val mean = values.average()
        return sqrt(values.sumOf { (it - mean).pow(2) } / values.size)
    }
}
