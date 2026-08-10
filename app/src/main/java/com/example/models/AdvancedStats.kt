package com.example.models

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import java.io.Serializable

@Immutable
@Stable
data class AdvancedPlayerStats(
    val playerId: Int,
    val playerName: String,
    val teamName: String,
    val gamesPlayed: Int,
    val per: Double, // Player Efficiency Rating
    val tsPercentage: Double, // True Shooting %
    val efgPercentage: Double, // Effective Field Goal %
    val assistTurnoverRatio: Double, // AST/TO
    val reboundPercentage: Double, // REB%
    val gameScore: Double // Single Game or Avg Performance Score
) : Serializable

object AdvancedStatsCalculator {

    fun calculateForPlayer(
        playerId: Int,
        playerName: String,
        teamName: String,
        gamesPlayed: Int,
        pts: Int,
        reb: Int,
        ast: Int,
        stl: Int,
        blk: Int,
        tov: Int,
        pf: Int,
        fgm: Int,
        fga: Int,
        tpm: Int,
        tpa: Int,
        ftm: Int,
        fta: Int,
        minutes: Int
    ): AdvancedPlayerStats {
        val safeGames = if (gamesPlayed < 1) 1 else gamesPlayed
        val safeMinutes = if (minutes < 1) 1 else minutes

        // PER calculation approximation
        val rawPer = ((pts + reb * 1.2 + ast * 1.5 + stl * 2.0 + blk * 2.0) - (tov * 1.5 + pf * 0.5 + (fga - fgm) * 0.8)) / (safeMinutes / 36.0)
        val per = (rawPer.coerceIn(5.0, 35.0) * 10).toInt() / 10.0

        // TS% = PTS / (2 * (FGA + 0.44 * FTA))
        val tsDenom = 2 * (fga + 0.44 * fta)
        val tsPct = if (tsDenom > 0) (pts / tsDenom) * 100 else 0.0

        // eFG% = (FGM + 0.5 * 3PM) / FGA
        val efgPct = if (fga > 0) ((fgm + 0.5 * tpm) / fga) * 100 else 0.0

        // AST/TO Ratio
        val astTo = if (tov > 0) ast.toDouble() / tov else ast.toDouble()

        // REB% estimate
        val rebPct = (reb.toDouble() / (safeMinutes * 0.5)).coerceIn(1.0, 30.0)

        // Game Score = PTS + 0.4 * FGM - 0.7 * FGA - 0.4*(FTA - FTM) + 0.7 * ORB + 0.3 * DRB + STL + 0.7 * AST + 0.7 * BLK - 0.4 * PF - TOV
        val gameScore = pts + 0.4 * fgm - 0.7 * fga - 0.4 * (fta - ftm) + 0.7 * ast + stl + 0.7 * blk - 0.4 * pf - tov

        return AdvancedPlayerStats(
            playerId = playerId,
            playerName = playerName,
            teamName = teamName,
            gamesPlayed = safeGames,
            per = per,
            tsPercentage = (tsPct * 10).toInt() / 10.0,
            efgPercentage = (efgPct * 10).toInt() / 10.0,
            assistTurnoverRatio = (astTo * 10).toInt() / 10.0,
            reboundPercentage = (rebPct * 10).toInt() / 10.0,
            gameScore = (gameScore * 10).toInt() / 10.0
        )
    }
}
