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

    // evolveInSeason() credits XP immediately after this function. Managed-team flows then add
    // their existing win/loss bonus (8 or 15 XP) after the simulator returns. Reserve enough
    // headroom for both sequential credits so neither can wrap a valid near-limit persisted XP
    // negative. Normal-range XP remains untouched.
    val earnedXp = (8 + stats.points / 4).coerceIn(8, 25)
    val requiredHeadroom = earnedXp + MAX_POST_GAME_XP_BONUS
    if (xp >= 0 && xp > Int.MAX_VALUE - requiredHeadroom) {
        xp = Int.MAX_VALUE - requiredHeadroom
    }
}

private fun saturatingAdd(current: Int, increment: Int): Int =
    (current.toLong() + increment.toLong())
        .coerceAtMost(Int.MAX_VALUE.toLong())
        .toInt()

private const val MAX_POST_GAME_XP_BONUS = 15
