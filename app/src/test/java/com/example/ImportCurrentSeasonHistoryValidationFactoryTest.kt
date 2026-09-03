package com.example

import com.example.data.NbaDataGenerator
import com.example.data.repository.GameStateRepository
import com.example.models.Player
import com.example.models.Season
import com.example.simulator.GameSimulator
import com.example.utils.ImportCurrentSeasonHistoryValidationFactory
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ImportCurrentSeasonHistoryValidationFactoryTest {
    private val plain = GsonBuilder()
        .enableComplexMapKeySerialization()
        .create()
    private val guarded = GsonBuilder()
        .enableComplexMapKeySerialization()
        .registerTypeAdapterFactory(ImportCurrentSeasonHistoryValidationFactory())
        .create()

    @Test
    fun rejectsHistoryOnlyPlayerThatExhaustsAllocatorHeadroom() {
        val teams = NbaDataGenerator.getAllTeams()
        val maxRosterId = teams.flatMap { it.players }.maxOf { it.id }
        val detached = teams.first().players.first().copy(
            id = Int.MAX_VALUE - 10,
            name = "History Allocator Overflow"
        )
        val season = seasonWithHistoryPlayer(teams, detached, maxRosterId + 1)

        assertThrows(JsonParseException::class.java) {
            decode(snapshot(season))
        }
    }

    @Test
    fun rejectsHistoryOnlyPlayerOutsidePersistedPlayerBounds() {
        val teams = NbaDataGenerator.getAllTeams()
        val maxRosterId = teams.flatMap { it.players }.maxOf { it.id }
        val detached = teams.first().players.first().copy(
            id = maxRosterId + 10,
            name = "History Invalid Rating",
            overall = 100
        )
        val season = seasonWithHistoryPlayer(teams, detached, detached.id + 1)

        assertThrows(JsonParseException::class.java) {
            decode(snapshot(season))
        }
    }

    @Test
    fun acceptsBoundedHistoryOnlyPlayerWithAllocatorHeadroom() {
        val teams = NbaDataGenerator.getAllTeams()
        val maxRosterId = teams.flatMap { it.players }.maxOf { it.id }
        val detached = teams.first().players.first().copy(
            id = maxRosterId + 10,
            name = "History Safe Allocator"
        )
        val season = seasonWithHistoryPlayer(teams, detached, detached.id + 1)

        assertNotNull(decode(snapshot(season)))
    }

    private fun seasonWithHistoryPlayer(
        teams: List<com.example.models.NbaTeam>,
        detached: Player,
        nextPlayerId: Int
    ): Season {
        val home = teams.first()
        val away = teams[1]
        return Season(
            teams = teams,
            currentDay = 1,
            gamesPlayed = teams.size / 2,
            seasonNumber = 1,
            nextPlayerId = nextPlayerId
        ).apply {
            userTeamName = home.name
            history += GameSimulator.GameResult(
                homeTeam = home,
                awayTeam = away,
                homeScore = 101,
                awayScore = 99,
                attendance = 18_000,
                homeStats = mapOf(detached to GameSimulator.PlayerStats(12, 3, 4, 1, 0, 2, 5)),
                awayStats = mapOf(away.players.first() to GameSimulator.PlayerStats(9, 2, 1, 0, 0, 1, -5)),
                injuries = emptyList(),
                narration = "Current-season history allocator fixture"
            )
        }
    }

    private fun snapshot(season: Season) = GameStateRepository.GameStateSnapshot(
        teamJson = plain.toJson(season.teams.first()),
        coachJson = null,
        financeJson = null,
        tacticsJson = null,
        seasonJson = plain.toJson(season),
        historyJson = null,
        awardsJson = null,
        startingFiveJson = plain.toJson(season.teams.first().players.take(5)),
        freeAgentsJson = plain.toJson(emptyList<Player>()),
        draftRookiesJson = plain.toJson(emptyList<Player>()),
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

    private fun decode(snapshot: GameStateRepository.GameStateSnapshot): GameStateRepository.GameStateSnapshot =
        guarded.fromJson(plain.toJson(snapshot), GameStateRepository.GameStateSnapshot::class.java)
}
