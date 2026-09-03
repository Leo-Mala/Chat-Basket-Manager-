package com.example.utils

import com.example.data.repository.GameStateRepository
import com.example.models.Player
import com.example.models.Season
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/**
 * Extends the snapshot allocator/bounds gate to players that exist only in the current season's
 * game history. Those players became persistence inputs when detached game participants started
 * being stored as inactive HISTORICAL rows, so they must reserve the same allocator headroom as
 * every other persisted player source before an import is allowed to mutate Room.
 */
class ImportCurrentSeasonHistoryValidationFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (type.rawType != GameStateRepository.GameStateSnapshot::class.java) return null
        val delegate = gson.getDelegateAdapter(this, type)
        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T) = delegate.write(out, value)

            override fun read(input: JsonReader): T {
                val value = delegate.read(input)
                @Suppress("UNCHECKED_CAST")
                validateDecodedSnapshot(value as GameStateRepository.GameStateSnapshot, gson)
                return value
            }
        }.nullSafe()
    }

    private fun validateDecodedSnapshot(snapshot: GameStateRepository.GameStateSnapshot, gson: Gson) {
        val season = snapshot.seasonJson?.let { gson.fromJson(it, Season::class.java) }
            ?: throw JsonParseException("seasonJson is required")

        var maxHistoryPlayerId = 0
        fun recordHistoryPlayer(player: Player) {
            validatePlayerBounds(player)
            maxHistoryPlayerId = maxOf(maxHistoryPlayerId, player.id)
        }

        season.history.forEach { result ->
            result.homeStats.keys.forEach(::recordHistoryPlayer)
            result.awayStats.keys.forEach(::recordHistoryPlayer)
            result.injuries.forEach { injury -> recordHistoryPlayer(injury.player) }
        }

        if (maxHistoryPlayerId == 0) return
        val requiredHeadroom = season.teams.size.coerceAtLeast(1).toLong() * 12L + 12L
        if (maxHistoryPlayerId.toLong() > Int.MAX_VALUE.toLong() - requiredHeadroom) {
            throw JsonParseException("Current-season history player ids leave no safe allocator headroom")
        }
        if (season.nextPlayerId <= maxHistoryPlayerId) {
            throw JsonParseException("nextPlayerId must exceed every current-season history player id")
        }
    }

    private fun validatePlayerBounds(player: Player) {
        val ratings = listOf(
            player.overall,
            player.shooting,
            player.defense,
            player.rebound,
            player.passing,
            player.athleticism
        )
        if (ratings.any { it !in 0..99 }) {
            throw JsonParseException("Player ${player.id} contains a rating outside 0..99")
        }
        if (player.age <= 0) {
            throw JsonParseException("Player ${player.id} age must be positive")
        }
        if (player.id <= 0) {
            throw JsonParseException("Player id must be positive")
        }
        if (player.injured != (player.injuryDays > 0)) {
            throw JsonParseException("Player ${player.id} has inconsistent injury state")
        }
    }
}
