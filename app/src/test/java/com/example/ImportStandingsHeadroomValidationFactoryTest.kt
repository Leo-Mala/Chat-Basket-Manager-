package com.example

import com.example.data.repository.GameStateRepository
import com.example.models.Season
import com.example.utils.ImportStandingsHeadroomValidationFactory
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ImportStandingsHeadroomValidationFactoryTest {
    private val plainGson = Gson()
    private val validatingGson = GsonBuilder()
        .registerTypeAdapterFactory(ImportStandingsHeadroomValidationFactory())
        .create()

    @Test
    fun exactRemainingScheduleHeadroomIsAccepted() {
        val season = Season(emptyList(), currentDay = 80)
        val maximumSafeTotal = Int.MAX_VALUE - (2 * 152)
        season.standings["Test Team"] = Season.SeasonRecord(
            wins = 40,
            losses = 40,
            gamesPlayed = 80,
            totalPointsScored = maximumSafeTotal,
            totalPointsConceded = maximumSafeTotal
        )

        val decoded = validatingGson.fromJson(
            plainGson.toJson(snapshot(season)),
            GameStateRepository.GameStateSnapshot::class.java
        )

        assertEquals(plainGson.toJson(season), decoded.seasonJson)
    }

    @Test
    fun onePointBeyondFinalScoreHeadroomIsRejected() {
        val season = Season(emptyList(), currentDay = 80)
        val unsafeTotal = Int.MAX_VALUE - (2 * 152) + 1
        season.standings["Test Team"] = Season.SeasonRecord(
            wins = 40,
            losses = 40,
            gamesPlayed = 80,
            totalPointsScored = unsafeTotal,
            totalPointsConceded = unsafeTotal
        )

        assertThrows(Exception::class.java) {
            validatingGson.fromJson(
                plainGson.toJson(snapshot(season)),
                GameStateRepository.GameStateSnapshot::class.java
            )
        }
    }

    @Test
    fun preTieBaseScoreHeadroomIsRejected() {
        val season = Season(emptyList(), currentDay = 80)
        val oldBaseScoreThreshold = Int.MAX_VALUE - (2 * 145)
        season.standings["Test Team"] = Season.SeasonRecord(
            wins = 40,
            losses = 40,
            gamesPlayed = 80,
            totalPointsScored = oldBaseScoreThreshold,
            totalPointsConceded = oldBaseScoreThreshold
        )

        assertThrows(Exception::class.java) {
            validatingGson.fromJson(
                plainGson.toJson(snapshot(season)),
                GameStateRepository.GameStateSnapshot::class.java
            )
        }
    }

    private fun snapshot(season: Season) = GameStateRepository.GameStateSnapshot(
        teamJson = null,
        coachJson = null,
        financeJson = null,
        tacticsJson = null,
        seasonJson = plainGson.toJson(season),
        historyJson = null,
        awardsJson = null,
        startingFiveJson = null,
        freeAgentsJson = null,
        draftRookiesJson = null,
        contractsJson = null,
        staffMarketJson = null,
        notificationsJson = null,
        teamStaffJson = null,
        facilitiesJson = null,
        financeAdvancedJson = null,
        newsFeedJson = null,
        latestBoxScoreJson = null,
        playoffResultJson = null,
        difficulty = 1,
        injuriesEnabled = true,
        autoSubstitutionsEnabled = true
    )
}
