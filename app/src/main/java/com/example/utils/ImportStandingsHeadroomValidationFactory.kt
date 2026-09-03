package com.example.utils

import com.example.data.repository.GameStateRepository
import com.example.models.Season
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/**
 * Ensures an imported regular-season standings row can complete the remaining schedule
 * without overflowing its persisted Int point accumulators. SimulationRules caps the base
 * score at 145, and GameSimulator may add up to seven points when resolving a tie, so the
 * maximum final team score that can reach the standings accumulator is 152.
 */
class ImportStandingsHeadroomValidationFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (type.rawType != GameStateRepository.GameStateSnapshot::class.java) return null
        val delegate = gson.getDelegateAdapter(this, type)
        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T) = delegate.write(out, value)

            override fun read(input: JsonReader): T {
                val value = delegate.read(input)
                @Suppress("UNCHECKED_CAST")
                validate(value as GameStateRepository.GameStateSnapshot, gson)
                return value
            }
        }.nullSafe()
    }

    private fun validate(snapshot: GameStateRepository.GameStateSnapshot, gson: Gson) {
        val season = snapshot.seasonJson?.let { gson.fromJson(it, Season::class.java) }
            ?: throw JsonParseException("seasonJson is required")

        season.standings.forEach { (teamName, record) ->
            val gamesRemaining = (MAX_REGULAR_SEASON_GAMES - record.gamesPlayed).coerceAtLeast(0)
            val requiredHeadroom = gamesRemaining.toLong() * MAX_FINAL_TEAM_SCORE_PER_GAME
            val maximumSafeTotal = Int.MAX_VALUE.toLong() - requiredHeadroom
            if (record.totalPointsScored.toLong() > maximumSafeTotal ||
                record.totalPointsConceded.toLong() > maximumSafeTotal
            ) {
                throw JsonParseException(
                    "Standings point totals for $teamName leave insufficient accumulator headroom"
                )
            }
        }
    }

    private companion object {
        const val MAX_REGULAR_SEASON_GAMES = 82
        const val MAX_FINAL_TEAM_SCORE_PER_GAME = 152L
    }
}
