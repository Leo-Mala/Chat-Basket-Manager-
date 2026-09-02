package com.example.simulator

import com.example.models.Player

/**
 * Applies one game's persisted player counters without allowing non-negative career/season
 * totals to wrap through Int.MIN_VALUE on very long or boundary-imported careers.
 */
internal fun Player.applyGameStatsSafely(stats: GameSimulator.PlayerStats) {
    careerGames = saturatingAdd(careerGames, 1)
    careerPoints = saturatingAdd(careerPoints, stats.points)
    careerRebounds = saturatingAdd(careerRebounds, stats.rebounds)
    careerAssists = saturatingAdd(careerAssists, stats.assists)
    careerSteals = saturatingAdd(careerSteals, stats.steals)
    careerBlocks = saturatingAdd(careerBlocks, stats.blocks)

    seasonGames = saturatingAdd(seasonGames, 1)
    seasonPoints = saturatingAdd(seasonPoints, stats.points)
    seasonRebounds = saturatingAdd(seasonRebounds, stats.rebounds)
    seasonAssists = saturatingAdd(seasonAssists, stats.assists)
    seasonSteals = saturatingAdd(seasonSteals, stats.steals)
    seasonBlocks = saturatingAdd(seasonBlocks, stats.blocks)
}

private fun saturatingAdd(current: Int, increment: Int): Int =
    (current.toLong() + increment.toLong())
        .coerceAtMost(Int.MAX_VALUE.toLong())
        .toInt()
