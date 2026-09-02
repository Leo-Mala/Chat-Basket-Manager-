package com.example.utils

import com.example.data.repository.GameStateRepository
import com.example.models.Finance
import com.example.models.HistoryManager
import com.example.models.MatchBoxScore
import com.example.models.NbaTeam
import com.example.models.Player
import com.example.models.Season
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.math.BigDecimal

/** Final cross-payload checks found during exact-head review of native save import. */
class ImportSnapshotFinalValidationFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (type.rawType != GameStateRepository.GameStateSnapshot::class.java) return null
        val delegate = gson.getDelegateAdapter(this, type)
        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T) = delegate.write(out, value)

            override fun read(input: JsonReader): T {
                val tree = JsonParser.parseReader(input)
                if (!tree.isJsonObject) throw JsonParseException("Save snapshot must be a JSON object")
                validateRawSnapshot(tree.asJsonObject)
                val value = delegate.fromJsonTree(tree)
                @Suppress("UNCHECKED_CAST")
                validateDecoded(value as GameStateRepository.GameStateSnapshot, gson)
                return value
            }
        }.nullSafe()
    }

    private fun validateRawSnapshot(root: JsonObject) {
        val difficulty = root.get("difficulty")
            ?: throw JsonParseException("difficulty is required")
        if (!difficulty.isJsonPrimitive || !difficulty.asJsonPrimitive.isNumber) {
            throw JsonParseException("difficulty must be an integer")
        }
        val decimal = try {
            BigDecimal(difficulty.asString)
        } catch (_: NumberFormatException) {
            throw JsonParseException("difficulty must be an integer")
        }
        try {
            decimal.intValueExact()
        } catch (_: ArithmeticException) {
            throw JsonParseException("difficulty must be an exact integer")
        }
    }

    private fun validateDecoded(snapshot: GameStateRepository.GameStateSnapshot, gson: Gson) {
        val season = snapshot.seasonJson?.let { gson.fromJson(it, Season::class.java) }
            ?: throw JsonParseException("seasonJson is required")
        validateLeagueAggregates(season)
        validateCalendarHeadroom(season)
        validateManagedHistory(season)
        validateBudgetLifecycleHeadroom(snapshot, season, gson)
        validateLatestBoxScore(snapshot, season, gson)
    }

    private fun validateLeagueAggregates(season: Season) {
        val totalWins = season.standings.values.sumOf { it.wins.toLong() }
        val totalLosses = season.standings.values.sumOf { it.losses.toLong() }
        if (totalWins != totalLosses) {
            throw JsonParseException("League-wide wins and losses do not reconcile")
        }
        val pointsScored = season.standings.values.sumOf { it.totalPointsScored.toLong() }
        val pointsConceded = season.standings.values.sumOf { it.totalPointsConceded.toLong() }
        if (pointsScored != pointsConceded) {
            throw JsonParseException("League-wide points scored and conceded do not reconcile")
        }
        val expectedGamesPlayed = season.currentDay.toLong() * (season.teams.size / 2L)
        if (season.gamesPlayed.toLong() != expectedGamesPlayed) {
            throw JsonParseException("season.gamesPlayed does not match completed schedule days")
        }
    }

    private fun validateCalendarHeadroom(season: Season) {
        if (season.currentYear >= Int.MAX_VALUE) {
            throw JsonParseException("currentYear leaves no headroom for calendar progression")
        }
    }

    private fun validateManagedHistory(season: Season) {
        val managedName = season.userTeamName
            ?: throw JsonParseException("Managed team is required")
        val managedResults = season.history.count { result ->
            result.homeTeam.name == managedName || result.awayTeam.name == managedName
        }
        val required = season.currentDay
        if (season.currentDay < MAX_REGULAR_SEASON_GAMES) {
            if (managedResults != required) {
                throw JsonParseException("Current-season history must contain one managed-team result per completed day")
            }
        } else if (managedResults < required) {
            throw JsonParseException("Current-season history is missing completed regular-season games")
        }
    }

    private fun validateBudgetLifecycleHeadroom(
        snapshot: GameStateRepository.GameStateSnapshot,
        season: Season,
        gson: Gson
    ) {
        val finance = snapshot.financeJson?.let { gson.fromJson(it, Finance::class.java) }
            ?: return
        val history = snapshot.historyJson?.let { gson.fromJson(it, HistoryManager::class.java) }
        val currentSeasonCompleted = history?.seasons?.any { it.seasonNumber == season.seasonNumber } == true
        if (currentSeasonCompleted && finance.budget.toLong() > Int.MAX_VALUE.toLong() - OFFSEASON_CREDIT) {
            throw JsonParseException("Imported budget leaves no headroom for offseason credit")
        }
    }

    private fun validateLatestBoxScore(
        snapshot: GameStateRepository.GameStateSnapshot,
        season: Season,
        gson: Gson
    ) {
        val raw = snapshot.latestBoxScoreJson ?: return
        val box = gson.fromJson(raw, MatchBoxScore::class.java)
            ?: throw JsonParseException("latestBoxScoreJson could not be decoded")
        val managedName = season.userTeamName
            ?: throw JsonParseException("Managed team is required")
        val latestManagedGame = season.history.lastOrNull { result ->
            result.homeTeam.name == managedName || result.awayTeam.name == managedName
        } ?: throw JsonParseException("Latest box score has no matching managed-team game")

        if (box.homeTeamName != latestManagedGame.homeTeam.name ||
            box.awayTeamName != latestManagedGame.awayTeam.name ||
            box.homeScore != latestManagedGame.homeScore ||
            box.awayScore != latestManagedGame.awayScore
        ) {
            throw JsonParseException("Latest box score does not match the latest managed-team game")
        }

        val homeHistoryIds = latestManagedGame.homeStats.keys.map(Player::id).toSet()
        val awayHistoryIds = latestManagedGame.awayStats.keys.map(Player::id).toSet()
        if (box.homePlayers.any { it.playerId !in homeHistoryIds } ||
            box.awayPlayers.any { it.playerId !in awayHistoryIds }
        ) {
            throw JsonParseException("Latest box-score players do not match the recorded game")
        }

        val canonicalTeamNames = season.teams.map(NbaTeam::name).toSet()
        if (box.homeTeamName !in canonicalTeamNames || box.awayTeamName !in canonicalTeamNames) {
            throw JsonParseException("Latest box score references a non-canonical team")
        }
    }

    private companion object {
        const val MAX_REGULAR_SEASON_GAMES = 82
        const val OFFSEASON_CREDIT = 85_000_000L
    }
}
