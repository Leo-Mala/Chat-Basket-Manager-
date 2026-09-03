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

    /** Credits non-negative XP without allowing a valid persisted value to wrap negative. */
    fun addXpSafely(amount: Int) {
        require(amount >= 0) { "XP credit must be non-negative" }
        xp = (xp.toLong() + amount.toLong())
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

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

    /** Individual prime window. Higher-upside careers retain a credible late-blooming tail. */
    private fun developmentPeakAge(): Int {
        val variation = Math.floorMod(id.toLong() * 37L + 11L, 100L).toInt()
        return when (developmentProfile()) {
            0 -> 26 + (variation % 4) // 26..29
            1 -> 28 + (variation % 5) // 28..32
            2 -> 31 + (variation % 5) // 31..35
            else -> 33 + (variation % 4) // 33..36
        }
    }

    private fun growthChance(baseChance: Int): Int =
        (baseChance * developmentGrowthPercent() / 100).coerceIn(0, 95)

    /**
     * High-upside players develop more slowly in the early prime and concentrate more of their
     * remaining growth in the final three seasons before their personal peak. This shifts timing
     * without simply adding extra career growth or inflating the league-wide 90+ population.
     */
    private fun primeGrowthChance(baseChance: Int, currentAge: Int, peakAge: Int): Int {
        val scaled = growthChance(baseChance)
        if (developmentProfile() < 2) return scaled
        return if (currentAge < peakAge - 2) {
            (scaled * 35 / 100).coerceAtLeast(1)
        } else {
            (scaled * 150 / 100).coerceAtMost(95)
        }
    }

    /**
     * Prime development should polish skills that actually matter for the player's role.
     * This prevents late-career growth events from being wasted on very low-weight attributes,
     * especially for centers and power forwards, while still keeping the OVR gain gradual.
     */
    private fun primeAttributes(): List<String> = when (position) {
        "PG" -> listOf("passing", "shooting")
        "SG" -> listOf("shooting", "passing", "defense")
        "SF" -> listOf("shooting", "defense", "passing")
        "PF" -> listOf("defense", "rebound", "shooting")
        "C" -> listOf("defense", "rebound")
        else -> listOf("shooting", "defense", "rebound", "passing")
    }

    /**
     * A small deterministic late-maturation pulse gives the late-blooming tail an observable OVR
     * peak at 34+ without increasing development for the majority of players. It only applies to
     * high/exceptional profiles whose personal peak is already in the late-prime window.
     */
    private fun applyLateMaturationPulse(peakAge: Int) {
        val profile = developmentProfile()
        if (profile < 2 || peakAge < 34 || age != peakAge) return
        val prime = primeAttributes()
        val repetitions = if (profile == 3) 8 else 6
        repeat(repetitions) { index ->
            boostAttribute(prime[index % prime.size], 1)
        }
    }

    fun evolveInSeason(ptsInGame: Int = 0) {
        val earnedXp = (8 + ptsInGame / 4).coerceIn(8, 25)
        addXpSafely(earnedXp)

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
                    val prime = primeAttributes()
                    if (random.nextInt(100) < primeGrowthChance(36, age, peakAge)) {
                        boostAttribute(prime[random.nextInt(prime.size)], 1)
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
                val prime = primeAttributes()
                if (random.nextInt(100) < primeGrowthChance(40, age, peakAge)) {
                    boostAttribute(prime[random.nextInt(prime.size)], 1)
                }
                applyLateMaturationPulse(peakAge)
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
