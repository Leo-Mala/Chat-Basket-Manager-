package com.example.models

import java.io.Serializable

data class SeasonHistory(
    val seasonNumber: Int,
    val champion: String,
    val mvp: String?,
    val finalScore: String,
    val topScorer: String,
    val topScorerPoints: Double,
    val teamWins: Map<String, Int> = emptyMap(),
    val playerStats: List<Player> = emptyList()
) : Serializable
