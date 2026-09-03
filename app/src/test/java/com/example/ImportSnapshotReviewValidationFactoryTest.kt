package com.example

import com.example.data.NbaDataGenerator
import com.example.data.repository.GameStateRepository
import com.example.models.AssistantCoachNotification
import com.example.models.Facility
import com.example.models.FacilityType
import com.example.models.Finance
import com.example.models.FinanceAdvanced
import com.example.models.HistoryManager
import com.example.models.Player
import com.example.models.Season
import com.example.models.TeamFacilities
import com.example.simulator.GameSimulator
import com.example.utils.ImportSnapshotReviewValidationFactory
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ImportSnapshotReviewValidationFactoryTest {
    private val plain = Gson()
    // Season history uses Player keys in stat maps; encode fixtures exactly like AutoSaveManager.
    private val payload = GsonBuilder()
        .enableComplexMapKeySerialization()
        .create()
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

    @Test
    fun acceptsSelfConsistentHistoryOnlyPlayer() {
        val season = baseSeason()
        val home = season.teams[0]
        val away = season.teams[1]
        val maxRosterId = season.teams.flatMap { it.players }.maxOf { it.id }
        val detached = home.players.first().copy(
            id = maxRosterId + 100,
            name = "Detached History Player"
        )
        season.history += gameResult(home, away, detached, detached)

        val decoded = guarded.fromJson(
            plain.toJson(baseSnapshot(season = season)),
            GameStateRepository.GameStateSnapshot::class.java
        )

        assertNotNull(decoded)
    }

    @Test
    fun rejectsConflictingHistoryOnlyIdentityReuse() {
        val season = baseSeason()
        val home = season.teams[0]
        val away = season.teams[1]
        val maxRosterId = season.teams.flatMap { it.players }.maxOf { it.id }
        val detached = home.players.first().copy(
            id = maxRosterId + 100,
            name = "Detached History Player"
        )
        val conflicting = detached.copy(name = "Conflicting Detached Identity")
        season.history += gameResult(home, away, detached, conflicting)

        assertRejected(baseSnapshot(season = season))
    }

    private fun gameResult(
        home: com.example.models.NbaTeam,
        away: com.example.models.NbaTeam,
        statPlayer: Player,
        injuryPlayer: Player
    ) = GameSimulator.GameResult(
        homeTeam = home,
        awayTeam = away,
        homeScore = 101,
        awayScore = 99,
        attendance = 18_000,
        homeStats = mapOf(statPlayer to GameSimulator.PlayerStats(12, 3, 4, 1, 0, 2, 5)),
        awayStats = mapOf(away.players.first() to GameSimulator.PlayerStats(9, 2, 1, 0, 0, 1, -5)),
        injuries = listOf(GameSimulator.Injury(injuryPlayer, 4)),
        narration = "History-only player import fixture"
    )

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
            teamJson = payload.toJson(managed),
            coachJson = null,
            financeJson = finance?.let(payload::toJson),
            tacticsJson = null,
            seasonJson = payload.toJson(season),
            historyJson = payload.toJson(HistoryManager()),
            awardsJson = null,
            startingFiveJson = payload.toJson(managed.players.take(5)),
            freeAgentsJson = payload.toJson(emptyList<Player>()),
            draftRookiesJson = payload.toJson(emptyList<Player>()),
            contractsJson = null,
            staffMarketJson = null,
            notificationsJson = notificationsJson,
            teamStaffJson = null,
            facilitiesJson = facilities?.let(payload::toJson),
            financeAdvancedJson = financeAdvanced?.let(payload::toJson),
            newsFeedJson = null,
            latestBoxScoreJson = null,
            playoffResultJson = null,
            difficulty = 1,
            injuriesEnabled = true,
            autoSubstitutionsEnabled = true
        )
    }
}
