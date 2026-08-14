package com.example

import com.example.data.NbaDataGenerator
import com.example.domain.draft.DraftManager
import com.example.models.Player
import com.example.models.Season
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class DraftDevelopmentAuditTest {

    data class CareerOutcome(
        val initialOverall: Int,
        val peakOverall: Int,
        val peakAge: Int,
        val finalOverall: Int
    ) {
        val gain: Int get() = peakOverall - initialOverall
        val decline: Int get() = peakOverall - finalOverall
    }

    @Test
    fun hundredDraftClassesAndCareerArcsRemainDiverseAndSustainable() {
        val teams = NbaDataGenerator.getAllTeams()
        val season = Season(teams, nextPlayerId = teams.flatMap { it.players }.maxOf { it.id } + 1)
        val draftManager = DraftManager()
        val prospects = mutableListOf<Player>()

        repeat(100) {
            prospects += draftManager.generateClass(
                season = season,
                freeAgents = emptyList(),
                scoutingLevel = 3,
                size = 30
            )
        }

        val total = prospects.size.toDouble()
        val longShotShare = prospects.count { it.overall < 75 } / total
        val rotationShare = prospects.count { it.overall in 75..81 } / total
        val strongShare = prospects.count { it.overall in 82..85 } / total
        val eliteShare = prospects.count { it.overall in 86..90 } / total
        val generationalShare = prospects.count { it.overall >= 91 } / total
        val initialAverage = prospects.map { it.overall }.average()

        println(
            "DRAFT_AUDIT_SUMMARY|classes=100|prospects=${prospects.size}" +
                "|initialAverage=${fmt(initialAverage)}|longShotShare=${fmt(longShotShare)}" +
                "|rotationShare=${fmt(rotationShare)}|strongShare=${fmt(strongShare)}" +
                "|eliteShare=${fmt(eliteShare)}|generationalShare=${fmt(generationalShare)}"
        )

        assertTrue("Draft average too weak/strong: $initialAverage", initialAverage in 74.0..81.5)
        assertTrue("Too few developmental prospects: $longShotShare", longShotShare >= 0.15)
        assertTrue("Rotation tier collapsed: $rotationShare", rotationShare >= 0.25)
        assertTrue("Strong prospect tier implausible: $strongShare", strongShare in 0.07..0.28)
        assertTrue("Elite prospect tier implausible: $eliteShare", eliteShare in 0.025..0.14)
        assertTrue("Generational prospects should be rare but possible: $generationalShare", generationalShare in 0.002..0.035)

        // Track 600 careers spread uniformly across all 100 generated classes.
        val careerSample = prospects.filterIndexed { index, _ -> index % 5 == 0 }.take(600)
        val outcomes = careerSample.map(::simulateCareer)

        val stagnantShare = outcomes.count { it.gain <= 3 }.toDouble() / outcomes.size
        val moderateShare = outcomes.count { it.gain in 4..9 }.toDouble() / outcomes.size
        val breakoutShare = outcomes.count { it.gain >= 10 }.toDouble() / outcomes.size
        val reachedNinetyShare = outcomes.count { it.peakOverall >= 90 }.toDouble() / outcomes.size
        val earlyPeakShare = outcomes.count { it.peakAge <= 28 }.toDouble() / outcomes.size
        val primePeakShare = outcomes.count { it.peakAge in 29..33 }.toDouble() / outcomes.size
        val latePeakShare = outcomes.count { it.peakAge >= 34 }.toDouble() / outcomes.size
        val meaningfulDeclineShare = outcomes.count { it.decline >= 2 }.toDouble() / outcomes.size
        val averageGain = outcomes.map { it.gain }.average()
        val averagePeakAge = outcomes.map { it.peakAge }.average()
        val averageFinal = outcomes.map { it.finalOverall }.average()

        println(
            "DEVELOPMENT_AUDIT_SUMMARY|careers=${outcomes.size}|avgGain=${fmt(averageGain)}" +
                "|avgPeakAge=${fmt(averagePeakAge)}|avgFinal=${fmt(averageFinal)}" +
                "|stagnantShare=${fmt(stagnantShare)}|moderateShare=${fmt(moderateShare)}" +
                "|breakoutShare=${fmt(breakoutShare)}|reached90Share=${fmt(reachedNinetyShare)}" +
                "|earlyPeakShare=${fmt(earlyPeakShare)}|primePeakShare=${fmt(primePeakShare)}" +
                "|latePeakShare=${fmt(latePeakShare)}|declineShare=${fmt(meaningfulDeclineShare)}"
        )

        // Career outcomes must not be a single near-identical development curve.
        assertTrue("Almost nobody stagnates/busts: $stagnantShare", stagnantShare >= 0.08)
        assertTrue("Moderate development tier missing: $moderateShare", moderateShare >= 0.20)
        assertTrue("Breakout careers missing: $breakoutShare", breakoutShare >= 0.08)
        assertTrue("Too many careers reach 90+: $reachedNinetyShare", reachedNinetyShare <= 0.25)

        // Peak ages need genuine career-arc variation instead of every player peaking at the same age.
        assertTrue("No early-peak careers: $earlyPeakShare", earlyPeakShare >= 0.03)
        assertTrue("Prime-age peak population missing: $primePeakShare", primePeakShare >= 0.20)
        assertTrue("No late-peak careers: $latePeakShare", latePeakShare >= 0.08)
        assertTrue("Average peak age unrealistic: $averagePeakAge", averagePeakAge in 28.0..34.5)

        // Older players should normally give back some of their peak by retirement age.
        assertTrue("Veteran decline is too rare: $meaningfulDeclineShare", meaningfulDeclineShare >= 0.35)
        assertTrue("Average development is excessive: $averageGain", averageGain <= 13.0)
    }

    private fun simulateCareer(player: Player): CareerOutcome {
        val initial = player.overall
        var peak = player.overall
        var peakAge = player.age

        while (player.age < 38) {
            val representativePoints = (((player.overall - 55) * 0.65).roundToInt()).coerceIn(8, 28)
            repeat(82) {
                player.seasonGames++
                player.seasonPoints += representativePoints
                player.evolveInSeason(representativePoints)
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
            player.resetSeasonStats()
        }

        return CareerOutcome(initial, peak, peakAge, player.overall)
    }

    private fun fmt(value: Double): String = "%.4f".format(java.util.Locale.US, value)
}
