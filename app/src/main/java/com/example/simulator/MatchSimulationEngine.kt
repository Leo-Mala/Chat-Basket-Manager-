package com.example.simulator

import com.example.models.Player
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Pure basketball-statistics engine. It does not access Android, SharedPreferences,
 * notifications or audio. The caller is responsible for applying game-wide side effects.
 */
class MatchSimulationEngine(
    private val random: Random = Random.Default
) {
    data class PlayerLine(
        val player: Player,
        val minutes: Int,
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
        val ftAttempted: Int,
        val plusMinus: Int
    )

    data class TeamLines(
        val lines: List<PlayerLine>,
        val points: Int
    )

    fun generateTeamLines(
        players: List<Player>,
        teamPoints: Int,
        opponentPoints: Int,
        isHome: Boolean,
        offense: Double,
        defense: Double
    ): TeamLines {
        if (players.isEmpty()) return TeamLines(emptyList(), teamPoints.coerceAtLeast(0))

        val rotation = chooseRotation(players)
        val minutes = allocateTotal(
            total = 240,
            weights = rotation.map { playingTimeWeight(it) },
            minValue = 0,
            maxValue = 48
        )

        val pointWeights = rotation.map {
            val usage = offensiveUsage(it)
            usage * (0.82 + random.nextDouble() * 0.36)
        }
        val points = allocateTotal(
            total = teamPoints.coerceAtLeast(0),
            weights = pointWeights,
            minValue = 0,
            maxValue = teamPoints.coerceAtLeast(0)
        )

        val reboundTotal = (40 + random.nextInt(-5, 8) + ((defense - 75.0) / 8.0).roundToInt())
            .coerceIn(30, 60)
        val assistTotal = (22 + random.nextInt(-4, 5) + ((offense - 75.0) / 12.0).roundToInt())
            .coerceIn(14, 34)
        val stealTotal = (7 + random.nextInt(-2, 3) + ((defense - 75.0) / 20.0).roundToInt())
            .coerceIn(3, 12)
        val blockTotal = (5 + random.nextInt(-2, 3) + ((defense - 75.0) / 18.0).roundToInt())
            .coerceIn(2, 10)
        val turnoverTotal = (13 + random.nextInt(-3, 4) - ((offense - 75.0) / 20.0).roundToInt())
            .coerceIn(7, 20)
        val foulTotal = (19 + random.nextInt(-4, 5)).coerceIn(12, 28)

        // Use mapIndexed instead of rotation.indexOf(it) to avoid repeated O(n) lookups.
        val reboundWeights = rotation.mapIndexed { index, player -> player.rebound * (minutes[index].coerceAtLeast(1) + 4.0) }
        val assistWeights = rotation.mapIndexed { index, player -> player.passing * (minutes[index].coerceAtLeast(1) + 2.0) }
        val stealWeights = rotation.mapIndexed { index, player -> player.defense * (minutes[index].coerceAtLeast(1) + 2.0) }
        val blockWeights = rotation.mapIndexed { index, player ->
            val positionBonus = if (player.position == "C") 1.45 else if (player.position == "PF") 1.2 else 0.9
            player.defense * positionBonus * (minutes[index].coerceAtLeast(1) + 2.0)
        }
        val turnoverWeights = rotation.mapIndexed { index, player -> (player.shooting + player.passing + 30.0) * (minutes[index].coerceAtLeast(1) + 3.0) }
        val foulWeights = rotation.mapIndexed { index, player -> (100.0 - player.defense.coerceIn(0, 100) + 35.0) * (minutes[index].coerceAtLeast(1) + 3.0) }

        val rebounds = allocateTotal(reboundTotal, reboundWeights, 0, reboundTotal)
        val assists = allocateTotal(assistTotal, assistWeights, 0, assistTotal)
        val steals = allocateTotal(stealTotal, stealWeights, 0, stealTotal)
        val blocks = allocateTotal(blockTotal, blockWeights, 0, blockTotal)
        val turnovers = allocateTotal(turnoverTotal, turnoverWeights, 0, turnoverTotal)
        val fouls = allocateTotal(foulTotal, foulWeights, 0, 6)

        val plusMinus = allocateSigned(opponent = opponentPoints, own = teamPoints, minutes = minutes)

        val lines = rotation.mapIndexed { index, player ->
            val shot = buildShotLine(points[index], player, minutes[index])
            PlayerLine(
                player = player,
                minutes = minutes[index],
                points = points[index],
                rebounds = rebounds[index],
                assists = assists[index],
                steals = steals[index],
                blocks = blocks[index],
                turnovers = turnovers[index],
                fouls = fouls[index],
                fgMade = shot.fgMade,
                fgAttempted = shot.fgAttempted,
                threeMade = shot.threeMade,
                threeAttempted = shot.threeAttempted,
                ftMade = shot.ftMade,
                ftAttempted = shot.ftAttempted,
                plusMinus = plusMinus[index]
            )
        }

        return TeamLines(lines, lines.sumOf { it.points })
    }

    private data class ShotLine(
        val fgMade: Int,
        val fgAttempted: Int,
        val threeMade: Int,
        val threeAttempted: Int,
        val ftMade: Int,
        val ftAttempted: Int
    )

    private fun buildShotLine(points: Int, player: Player, minutes: Int): ShotLine {
        if (points <= 0 || minutes <= 0) return ShotLine(0, 0, 0, 0, 0, 0)

        val threePointShare = (0.22 + (player.shooting - 50).coerceIn(0, 49) / 100.0 * 0.28)
            .coerceIn(0.18, 0.48)
        var threeMade = ((points * threePointShare) / 3.0 + random.nextDouble(-0.6, 0.7)).roundToInt()
            .coerceIn(0, points / 3)

        var remaining = points - threeMade * 3
        val ftRate = (0.08 + player.athleticism / 100.0 * 0.10).coerceIn(0.08, 0.18)
        var ftMade = (points * ftRate + random.nextDouble(-0.7, 1.0)).roundToInt().coerceIn(0, remaining)
        remaining -= ftMade

        // Remaining points must be scored by 2-point field goals. If the remainder is odd,
        // convert one free throw/three-pointer choice so the identity always closes exactly.
        if (remaining % 2 != 0) {
            // Every odd remainder must contain at least one free throw.
            // This also fixes the 1-point player-line edge case.
            if (ftMade > 0) {
                ftMade--
                remaining++
            } else if (remaining > 0) {
                ftMade = 1
                remaining--
            } else if (threeMade > 0) {
                threeMade--
                remaining += 3
            }
        }
        val twoMade = remaining / 2
        val fgMade = twoMade + threeMade

        val twoAtt = if (twoMade == 0) 0 else {
            (twoMade / (0.43 + player.shooting / 500.0)).roundToInt().coerceAtLeast(twoMade)
        }
        val threeAtt = if (threeMade == 0) 0 else {
            (threeMade / (0.31 + player.shooting / 500.0)).roundToInt().coerceAtLeast(threeMade)
        }
        val ftAtt = if (ftMade == 0) 0 else {
            (ftMade / 0.78).roundToInt().coerceAtLeast(ftMade)
        }

        return ShotLine(
            fgMade = fgMade,
            fgAttempted = twoAtt + threeAtt,
            threeMade = threeMade,
            threeAttempted = threeAtt,
            ftMade = ftMade,
            ftAttempted = ftAtt
        )
    }

    private fun chooseRotation(players: List<Player>): List<Player> {
        // Use at most 10 players for a realistic rotation, while keeping at least one player
        // at each available position when possible.
        return players.sortedWith(
            compareByDescending<Player> { it.overall }
                .thenByDescending { it.athleticism }
        ).take(10.coerceAtMost(players.size))
    }

    private fun playingTimeWeight(player: Player): Double =
        (player.overall * 1.4 + player.athleticism * 0.35 + player.defense * 0.25).coerceAtLeast(1.0)

    private fun offensiveUsage(player: Player): Double =
        (player.shooting * 0.55 + player.passing * 0.22 + player.athleticism * 0.13 + player.overall * 0.10)
            .coerceAtLeast(1.0)

    private fun allocateTotal(total: Int, weights: List<Double>, minValue: Int, maxValue: Int): List<Int> {
        if (weights.isEmpty()) return emptyList()
        if (total <= 0) return List(weights.size) { minValue.coerceAtMost(maxValue) }

        val safeWeights = weights.map { it.coerceAtLeast(0.001) }
        val sum = safeWeights.sum()
        val raw = safeWeights.map { total * it / sum }
        val result = raw.map { it.toInt().coerceIn(minValue, maxValue) }.toMutableList()
        var remaining = total - result.sum()

        while (remaining > 0) {
            val candidates = result.indices.filter { result[it] < maxValue }
            if (candidates.isEmpty()) break
            val best = candidates.maxByOrNull { raw[it] - result[it] } ?: break
            result[best]++
            remaining--
        }
        while (remaining < 0) {
            val candidates = result.indices.filter { result[it] > minValue }
            if (candidates.isEmpty()) break
            val worst = candidates.minByOrNull { raw[it] - result[it] } ?: break
            result[worst]--
            remaining++
        }
        return result
    }

    private fun allocateSigned(opponent: Int, own: Int, minutes: List<Int>): List<Int> {
        val target = own - opponent
        if (minutes.isEmpty()) return emptyList()
        val totalMinutes = minutes.sum().coerceAtLeast(1)
        val values = minutes.map { (target * it.toDouble() / totalMinutes).roundToInt() }.toMutableList()
        var diff = target - values.sum()
        val order = minutes.indices.sortedByDescending { minutes[it] }
        var cursor = 0
        while (diff != 0 && order.isNotEmpty()) {
            val index = order[cursor % order.size]
            values[index] += if (diff > 0) 1 else -1
            diff += if (diff > 0) -1 else 1
            cursor++
        }
        return values
    }
}
