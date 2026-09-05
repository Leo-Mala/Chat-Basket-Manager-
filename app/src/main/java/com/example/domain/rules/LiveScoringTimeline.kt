package com.example.domain.rules

import kotlin.random.Random

enum class LiveScoringSide {
    USER,
    OPPONENT
}

data class LiveScoreEvent(
    val elapsedMillis: Long,
    val side: LiveScoringSide,
    val points: Int
)

/**
 * Builds the presentation timeline for one accelerated NBA quarter.
 *
 * The match logic first decides the quarter totals. This timeline only decomposes those totals
 * into legal 1/2/3-point scoring events and spreads them across exactly one minute of active
 * real time. Pausing the UI stops active time; resuming continues from the same elapsed instant.
 */
object LiveScoringTimeline {
    const val QUARTER_REAL_DURATION_MS = 60_000L
    const val UI_TICK_MS = 100L
    private const val GAME_SECONDS_PER_QUARTER = 12 * 60

    fun build(
        userPoints: Int,
        opponentPoints: Int,
        random: Random = Random.Default
    ): List<LiveScoreEvent> {
        require(userPoints >= 0) { "userPoints must be non-negative" }
        require(opponentPoints >= 0) { "opponentPoints must be non-negative" }

        val plays = buildList {
            scoringPlays(userPoints, random).forEach { add(LiveScoringSide.USER to it) }
            scoringPlays(opponentPoints, random).forEach { add(LiveScoringSide.OPPONENT to it) }
        }.shuffled(random)

        if (plays.isEmpty()) return emptyList()

        val intervalWeights = List(plays.size + 1) { random.nextInt(65, 136) }
        val totalWeight = intervalWeights.sum().toLong()
        var cumulativeWeight = 0L

        return plays.mapIndexed { index, (side, points) ->
            cumulativeWeight += intervalWeights[index]
            val elapsed = (QUARTER_REAL_DURATION_MS * cumulativeWeight / totalWeight)
                .coerceIn(1L, QUARTER_REAL_DURATION_MS - 1L)
            LiveScoreEvent(elapsedMillis = elapsed, side = side, points = points)
        }
    }

    fun clockForElapsed(elapsedMillis: Long): String {
        val clamped = elapsedMillis.coerceIn(0L, QUARTER_REAL_DURATION_MS)
        val elapsedGameSeconds =
            (clamped * GAME_SECONDS_PER_QUARTER / QUARTER_REAL_DURATION_MS).toInt()
        val remaining = (GAME_SECONDS_PER_QUARTER - elapsedGameSeconds).coerceAtLeast(0)
        return "%02d:%02d".format(remaining / 60, remaining % 60)
    }

    private fun scoringPlays(total: Int, random: Random): List<Int> {
        var remaining = total
        val plays = mutableListOf<Int>()
        while (remaining > 0) {
            val points = when (remaining) {
                1 -> 1
                2 -> if (random.nextInt(100) < 85) 2 else 1
                else -> when (random.nextInt(100)) {
                    in 0..9 -> 1
                    in 10..44 -> 3
                    else -> 2
                }
            }.coerceAtMost(remaining)
            plays += points
            remaining -= points
        }
        return plays
    }
}
