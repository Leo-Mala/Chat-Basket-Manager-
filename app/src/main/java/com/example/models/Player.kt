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

    fun evolveInSeason(ptsInGame: Int = 0) {
        // Gain XP from playing
        val earnedXp = (8 + ptsInGame / 4).coerceIn(8, 25)
        xp += earnedXp

        // Every 5 games played in season, trigger in-season progression/regression
        if (seasonGames > 0 && seasonGames % 5 == 0) {
            when {
                // Young prospects (age <= 22): rapid evolution
                age <= 22 -> {
                    val attr = when (position) {
                        "PG" -> listOf("passing", "shooting", "athleticism").random()
                        "SG" -> listOf("shooting", "athleticism", "passing").random()
                        "SF" -> listOf("shooting", "defense", "athleticism").random()
                        "PF" -> listOf("rebound", "defense", "shooting").random()
                        "C"  -> listOf("defense", "rebound", "athleticism").random()
                        else -> listOf("shooting", "defense", "rebound", "passing", "athleticism").random()
                    }
                    boostAttribute(attr, 1)
                }
                // Young players (23..25): steady progress
                age in 23..25 -> {
                    if ((1..100).random() <= 60) {
                        val attr = listOf("shooting", "defense", "rebound", "passing", "athleticism").random()
                        boostAttribute(attr, 1)
                    }
                }
                // Prime players (26..30): peak stability & slight technical gains
                age in 26..30 -> {
                    if ((1..100).random() <= 20) {
                        val attr = listOf("shooting", "passing", "defense").random()
                        boostAttribute(attr, 1)
                    }
                }
                // Veterans (> 30): slight physical wear from heavy season schedule
                else -> {
                    if (seasonGames > 35 && (1..100).random() <= 20) {
                        if (athleticism > 60) {
                            athleticism = (athleticism - 1).coerceAtLeast(50)
                            overall = calculateOverall()
                        }
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
        // Jovens (<= 25) evoluem mais
        if (age <= 25) {
            val improvement = (1..3).random()
            shooting = minOf(99, shooting + improvement)
            defense = minOf(99, defense + improvement)
            rebound = minOf(99, rebound + improvement)
            passing = minOf(99, passing + improvement)
            athleticism = minOf(99, athleticism + improvement)
        } else if (age in 26..32) {
            // Meia-idade: mantém ou perde um pouco
            if (kotlin.random.Random.nextBoolean()) {
                val change = (0..2).random()
                shooting = (shooting - change).coerceAtLeast(0)
            }
        } else {
            // Veteranos (> 32): declínio
            val decline = (1..3).random()
            athleticism = (athleticism - decline).coerceAtLeast(0)
            if (kotlin.random.Random.nextBoolean()) {
                shooting = (shooting - 1).coerceAtLeast(0)
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
