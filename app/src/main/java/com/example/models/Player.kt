package com.example.models

import java.io.Serializable

data class Player(
    val id: Int,
    val name: String,
    val position: String,
    var overall: Int,
    var shooting: Int,
    var defense: Int,
    var rebound: Int,
    var passing: Int,
    var athleticism: Int,
    var age: Int = 22,
    var xp: Int = 0,
    var trainings: Int = 0,
    var injured: Boolean = false,
    var injuryDays: Int = 0,
    var careerPoints: Int = 0,
    var careerRebounds: Int = 0,
    var careerAssists: Int = 0,
    var careerSteals: Int = 0,
    var careerBlocks: Int = 0,
    var careerGames: Int = 0,
    var championships: Int = 0,
    var mvps: Int = 0,
    var seasonPoints: Int = 0,
    var seasonRebounds: Int = 0,
    var seasonAssists: Int = 0,
    var seasonSteals: Int = 0,
    var seasonBlocks: Int = 0,
    var seasonGames: Int = 0
) : Serializable {

    fun isAvailable(): Boolean = !injured || injuryDays <= 0

    fun train(attribute: String, cost: Int): Boolean {
        if (xp < cost) return false
        xp -= cost
        boostAttribute(attribute, 1)
        trainings++
        return true
    }

    fun calculateOverall(): Int {
        val raw = when (position) {
            "PG" -> (shooting * 0.32) + (passing * 0.40) + (athleticism * 0.15) + (defense * 0.08) + (rebound * 0.05)
            "SG" -> (shooting * 0.42) + (passing * 0.18) + (athleticism * 0.20) + (defense * 0.15) + (rebound * 0.05)
            "SF" -> (shooting * 0.30) + (passing * 0.18) + (athleticism * 0.22) + (defense * 0.20) + (rebound * 0.10)
            "PF" -> (shooting * 0.20) + (passing * 0.12) + (athleticism * 0.20) + (defense * 0.25) + (rebound * 0.23)
            "C"  -> (shooting * 0.12) + (passing * 0.08) + (athleticism * 0.15) + (defense * 0.32) + (rebound * 0.33)
            else -> (shooting + defense + rebound + passing + athleticism) / 5.0
        }
        return raw.toInt().coerceIn(50, 99)
    }

    fun boostAttribute(attr: String, amount: Int) {
        when (attr) {
            "shooting" -> shooting = (shooting + amount).coerceAtMost(99)
            "defense" -> defense = (defense + amount).coerceAtMost(99)
            "rebound" -> rebound = (rebound + amount).coerceAtMost(99)
            "passing" -> passing = (passing + amount).coerceAtMost(99)
            "athleticism" -> athleticism = (athleticism + amount).coerceAtMost(99)
        }
        overall = calculateOverall()
    }

    /**
     * Hidden deterministic career profile derived only from the durable player id.
     *
     * The profile is intentionally not persisted: the same player id always resolves to the same
     * growth tier and peak age after save/load, while careers can still include busts, ordinary
     * development, high-upside prospects and rare exceptional trajectories.
     */
    private fun developmentProfile(): Int {
        val roll = Math.floorMod(id.toLong() * 73L + 19L, 100L).toInt()
        return when {
            roll < 18 -> 0 // low ceiling / likely stagnation
            roll < 70 -> 1 // normal development
            roll < 92 -> 2 // high upside
            else -> 3      // exceptional upside
        }
    }

    private fun developmentGrowthPercent(): Int = when (developmentProfile()) {
        0 -> 18
        1 -> 48
        2 -> 75
        else -> 100
    }

    /** Individual prime window. High-upside players are more likely to bloom later. */
    private fun developmentPeakAge(): Int {
        val variation = Math.floorMod(id.toLong() * 37L + 11L, 100L).toInt()
        return when (developmentProfile()) {
            0 -> 26 + (variation % 4) // 26..29
            1 -> 28 + (variation % 5) // 28..32
            2 -> 30 + (variation % 5) // 30..34
            else -> 32 + (variation % 4) // 32..35
        }
    }

    private fun growthChance(baseChance: Int): Int =
        (baseChance * developmentGrowthPercent() / 100).coerceIn(0, 95)

    fun evolveInSeason(ptsInGame: Int = 0) {
        val earnedXp = (8 + ptsInGame / 4).coerceIn(8, 25)
        xp += earnedXp

        // Ten-game checkpoints keep development gradual. Chances are scaled by a stable hidden
        // career profile so not every 19-year-old follows essentially the same +10 OVR curve.
        if (seasonGames > 0 && seasonGames % 10 == 0) {
            val random = kotlin.random.Random(id * 31 + age * 997 + seasonGames * 7919 + seasonPoints)
            val all = listOf("shooting", "defense", "rebound", "passing", "athleticism")
            val peakAge = developmentPeakAge()
            when {
                age <= 22 -> {
                    val preferred = when (position) {
                        "PG" -> listOf("passing", "shooting", "athleticism")
                        "SG" -> listOf("shooting", "athleticism", "passing")
                        "SF" -> listOf("shooting", "defense", "athleticism")
                        "PF" -> listOf("rebound", "defense", "shooting")
                        "C"  -> listOf("defense", "rebound", "athleticism")
                        else -> all
                    }
                    if (random.nextInt(100) < growthChance(80)) {
                        boostAttribute(preferred[random.nextInt(preferred.size)], 1)
                    }
                }
                age in 23..25 -> {
                    if (random.nextInt(100) < growthChance(45)) {
                        boostAttribute(all[random.nextInt(all.size)], 1)
                    }
                }
                age <= peakAge -> {
                    val technical = listOf("shooting", "passing", "defense")
                    if (random.nextInt(100) < growthChance(28)) {
                        boostAttribute(technical[random.nextInt(technical.size)], 1)
                    }
                }
                else -> {
                    val yearsPastPeak = (age - peakAge).coerceAtLeast(1)
                    val physicalDeclineChance = (10 + yearsPastPeak * 6).coerceAtMost(50)
                    if (seasonGames > 40 && random.nextInt(100) < physicalDeclineChance && athleticism > 45) {
                        athleticism--
                        overall = calculateOverall()
                    }
                }
            }
        }
    }

    fun calculateSalary(): Int {
        return when (overall) {
            in 95..99 -> 12000000 + (overall - 95) * 2000000
            in 90..94 -> 8000000 + (overall - 90) * 800000
            in 80..89 -> 3500000 + (overall - 80) * 350000
            in 70..79 -> 1200000 + (overall - 70) * 120000
            else -> 500000 + overall * 5000
        }
    }

    fun advanceSeason() {
        age++
        val random = kotlin.random.Random(id * 1009 + age * 9176)
        val all = listOf("shooting", "defense", "rebound", "passing", "athleticism")
        val peakAge = developmentPeakAge()

        when {
            age <= 22 -> {
                if (random.nextInt(100) < growthChance(75)) {
                    boostAttribute(all[random.nextInt(all.size)], 1)
                }
                if (random.nextInt(100) < growthChance(20)) {
                    boostAttribute(all[random.nextInt(all.size)], 1)
                }
            }
            age <= 25 -> {
                if (random.nextInt(100) < growthChance(55)) {
                    boostAttribute(all[random.nextInt(all.size)], 1)
                }
            }
            age <= peakAge -> {
                val technical = listOf("shooting", "passing", "defense")
                if (random.nextInt(100) < growthChance(30)) {
                    boostAttribute(technical[random.nextInt(technical.size)], 1)
                }
            }
            else -> {
                val yearsPastPeak = (age - peakAge).coerceAtLeast(1)
                val athleticDeclineChance = (38 + yearsPastPeak * 10).coerceAtMost(95)
                if (athleticism > 40 && random.nextInt(100) < athleticDeclineChance) {
                    athleticism--
                    if (yearsPastPeak >= 3 && random.nextInt(100) < 35 && athleticism > 40) {
                        athleticism--
                    }
                }

                val technicalDeclineChance = (12 + yearsPastPeak * 7).coerceAtMost(65)
                if (random.nextInt(100) < technicalDeclineChance) {
                    when (all[random.nextInt(4)]) {
                        "shooting" -> shooting = (shooting - 1).coerceAtLeast(40)
                        "defense" -> defense = (defense - 1).coerceAtLeast(40)
                        "rebound" -> rebound = (rebound - 1).coerceAtLeast(40)
                        "passing" -> passing = (passing - 1).coerceAtLeast(40)
                    }
                }
            }
        }
        overall = calculateOverall()
    }

    fun advanceDay() {
        if (injured && injuryDays > 0) {
            injuryDays--
            if (injuryDays == 0) injured = false
        }
    }

    fun resetSeasonStats() {
        seasonPoints = 0
        seasonRebounds = 0
        seasonAssists = 0
        seasonSteals = 0
        seasonBlocks = 0
        seasonGames = 0
    }
}
