package com.example.models

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import java.io.Serializable

@Immutable
@Stable
data class PlayerBoxScore(
    val playerId: Int,
    val playerName: String,
    val position: String,
    val minutesPlayed: Int,
    val points: Int,
    val rebounds: Int,
    val offensiveRebounds: Int,
    val defensiveRebounds: Int,
    val assists: Int,
    val steals: Int,
    val blocks: Int,
    val turnovers: Int,
    val fouls: Int,
    val fgMade: Int,
    val fgAttempted: Int,
    val threeMade: Int,
    val threeAttempted: Int,
    val ftMade: Int,
    val ftAttempted: Int,
    val plusMinus: Int
) : Serializable

@Immutable
@Stable
data class TeamBoxScore(
    val teamName: String,
    val points: Int,
    val rebounds: Int,
    val assists: Int,
    val steals: Int,
    val blocks: Int,
    val turnovers: Int,
    val fouls: Int,
    val fgMade: Int,
    val fgAttempted: Int,
    val threeMade: Int,
    val threeAttempted: Int,
    val ftMade: Int,
    val ftAttempted: Int
) : Serializable

@Immutable
@Stable
data class MatchBoxScore(
    val matchId: String,
    val dateString: String,
    val homeTeamName: String,
    val awayTeamName: String,
    val homeScore: Int,
    val awayScore: Int,
    val homeQuarterScores: List<Int>,
    val awayQuarterScores: List<Int>,
    val homePlayers: List<PlayerBoxScore>,
    val awayPlayers: List<PlayerBoxScore>,
    val homeTeamTotals: TeamBoxScore,
    val awayTeamTotals: TeamBoxScore,
    val mvpPlayerName: String? = null
) : Serializable
