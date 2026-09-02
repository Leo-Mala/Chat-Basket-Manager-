package com.example

import com.example.data.NbaDataGenerator
import com.example.data.repository.GameStateRepository
import com.example.models.AssistantCoachNotification
import com.example.models.Facility
import com.example.models.FacilityType
import com.example.models.Finance
import com.example.models.FinanceAdvanced
import com.example.models.TeamFacilities
import com.example.models.Season
import com.example.utils.ImportSnapshotReviewValidationFactory
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import org.junit.Assert.assertThrows
import org.junit.Test

class ImportSnapshotReviewValidationFactoryTest {
    private val plain = Gson()
    private val guarded = GsonBuilder()
        .enableComplexMapKeySerialization()
        .registerTypeAdapterFactory(ImportSnapshotReviewValidationFactory())
        .create()

    @Test
    fun rejectsUnsupportedSchema() {
        assertRejected(baseSnapshot().copy(schemaVersion = 3))
    }

    @Test
    fun rejectsStandingsThatDoNotMatchCurrentDay() {
        val season = baseSeason().apply { currentDay = 1 }
        assertRejected(baseSnapshot(season = season))
    }

    @Test
    fun rejectsFinanceUpgradeOutsideRuntimeRange() {
        assertRejected(baseSnapshot(finance = Finance(arenaSeatsLevel = 6)))
    }

    @Test
    fun rejectsAdvancedExpenseAggregateOverflow() {
        val advanced = FinanceAdvanced().apply {
            expenses.playerSalaries = Int.MAX_VALUE
            expenses.staffSalaries = 1
        }
        assertRejected(baseSnapshot(financeAdvanced = advanced))
    }

    @Test
    fun rejectsMismatchedFacilitySlotType() {
        val facilities = TeamFacilities(
            arena = Facility(FacilityType.TRAINING_FACILITY, "Wrong slot")
        )
        assertRejected(baseSnapshot(facilities = facilities))
    }

    @Test
    fun rejectsUnsupportedCoachRecommendationType() {
        val notification = AssistantCoachNotification(
            id = "review-test",
            gameDay = 0,
            seasonNumber = 1,
            opponentName = "Opponent",
            isWin = true,
            userScore = 100,
            opponentScore = 90,
            timestamp = 1L,
            coachName = "Coach",
            coachRole = "Tático & Ataque",
            summary = "Summary",
            keyStrengths = listOf("Strength"),
            areasToImprove = listOf("Area"),
            playerHighlights = listOf("Player"),
            tacticalAdvice = "Advice",
            recommendedBonusType = "UNSUPPORTED",
            recommendedBonusLabel = "Apply"
        )
        assertRejected(baseSnapshot(notificationsJson = plain.toJson(listOf(notification))))
    }

    @Test
    fun rejectsSeasonNumberWithoutOffseasonHeadroom() {
        val season = baseSeason().apply { seasonNumber = Int.MAX_VALUE }
        assertRejected(baseSnapshot(season = season))
    }

    @Test
    fun rejectsBudgetWithoutNormalRevenueHeadroom() {
        assertRejected(baseSnapshot(finance = Finance(budget = Int.MAX_VALUE)))
    }

    private fun assertRejected(snapshot: GameStateRepository.GameStateSnapshot) {
        assertThrows(JsonParseException::class.java) {
            guarded.fromJson(plain.toJson(snapshot), GameStateRepository.GameStateSnapshot::class.java)
        }
    }

    private fun baseSeason(): Season {
        val teams = NbaDataGenerator.getAllTeams()
        return Season(teams = teams, currentDay = 0, gamesPlayed = 0, seasonNumber = 1).apply {
            userTeamName = teams.first().name
        }
    }

    private fun baseSnapshot(
        season: Season = baseSeason(),
        finance: Finance? = null,
        financeAdvanced: FinanceAdvanced? = null,
        facilities: TeamFacilities? = null,
        notificationsJson: String? = null
    ): GameStateRepository.GameStateSnapshot {
        val managed = season.teams.first()
        return GameStateRepository.GameStateSnapshot(
            teamJson = plain.toJson(managed),
            coachJson = null,
            financeJson = finance?.let(plain::toJson),
            tacticsJson = null,
            seasonJson = plain.toJson(season),
            historyJson = null,
            awardsJson = null,
            startingFiveJson = null,
            freeAgentsJson = null,
            draftRookiesJson = null,
            contractsJson = null,
            staffMarketJson = null,
            notificationsJson = notificationsJson,
            teamStaffJson = null,
            facilitiesJson = facilities?.let(plain::toJson),
            financeAdvancedJson = financeAdvanced?.let(plain::toJson),
            newsFeedJson = null,
            latestBoxScoreJson = null,
            playoffResultJson = null,
            difficulty = 1,
            injuriesEnabled = true,
            autoSubstitutionsEnabled = true
        )
    }
}
