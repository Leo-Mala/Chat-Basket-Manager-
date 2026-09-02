package com.example

import com.example.data.NbaDataGenerator
import com.example.data.repository.GameStateRepository
import com.example.models.HistoryManager
import com.example.models.Season
import com.example.models.SeasonHistory
import com.example.utils.ImportSnapshotValidationFactory
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import org.junit.Assert.assertThrows
import org.junit.Test

class ImportSnapshotLifecycleValidationTest {
    private val plain = GsonBuilder().enableComplexMapKeySerialization().create()
    private val guarded = GsonBuilder()
        .enableComplexMapKeySerialization()
        .registerTypeAdapterFactory(ImportSnapshotValidationFactory())
        .create()

    @Test
    fun rejectsLeagueGameCounterAheadOfCurrentScheduleProgress() {
        val season = baseSeason().apply {
            currentDay = 1
            gamesPlayed = Int.MAX_VALUE
        }

        assertRejected(baseSnapshot(season))
    }

    @Test
    fun rejectsAllocatorWithoutFullLifecycleHeadroom() {
        val season = baseSeason().apply {
            nextPlayerId = Int.MAX_VALUE - teams.size * 12
        }

        assertRejected(baseSnapshot(season))
    }

    @Test
    fun rejectsCompletedSeasonWhenPlayoffChampionDisagreesWithHistory() {
        val season = baseSeason()
        val eastChampion = season.teams.first()
        val westChampion = season.teams.first { it.conference == "West" }
        val playoff = Season.PlayoffResult(
            eastChampion = eastChampion,
            westChampion = westChampion,
            nbaChampion = eastChampion,
            mvp = null,
            seriesResults = listOf(
                Season.SeriesResult(
                    winner = eastChampion,
                    games = emptyList(),
                    roundName = "Finais da NBA",
                    team1 = eastChampion,
                    team2 = westChampion,
                    team1Wins = 4,
                    team2Wins = 2
                )
            )
        )
        val history = HistoryManager().apply {
            addSeason(
                SeasonHistory(
                    seasonNumber = season.seasonNumber,
                    champion = westChampion.name,
                    mvp = null,
                    finalScore = "Campeão NBA",
                    topScorer = "N/A",
                    topScorerPoints = 0.0
                )
            )
        }

        assertRejected(
            baseSnapshot(season).copy(
                historyJson = plain.toJson(history),
                playoffResultJson = plain.toJson(playoff)
            )
        )
    }

    private fun assertRejected(snapshot: GameStateRepository.GameStateSnapshot) {
        assertThrows(JsonParseException::class.java) {
            guarded.fromJson(plain.toJson(snapshot), GameStateRepository.GameStateSnapshot::class.java)
        }
    }

    private fun baseSeason(): Season {
        val teams = NbaDataGenerator.getAllTeams()
        val nextPlayerId = teams.flatMap { it.players }.maxOf { it.id } + 1
        return Season(
            teams = teams,
            currentDay = 0,
            gamesPlayed = 0,
            seasonNumber = 1,
            nextPlayerId = nextPlayerId
        ).apply {
            userTeamName = teams.first().name
        }
    }

    private fun baseSnapshot(season: Season): GameStateRepository.GameStateSnapshot {
        val managed = season.teams.first()
        return GameStateRepository.GameStateSnapshot(
            teamJson = plain.toJson(managed),
            coachJson = null,
            financeJson = null,
            tacticsJson = null,
            seasonJson = plain.toJson(season),
            historyJson = plain.toJson(HistoryManager()),
            awardsJson = null,
            startingFiveJson = plain.toJson(managed.players.take(5)),
            freeAgentsJson = plain.toJson(emptyList<Any>()),
            draftRookiesJson = plain.toJson(emptyList<Any>()),
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
}
