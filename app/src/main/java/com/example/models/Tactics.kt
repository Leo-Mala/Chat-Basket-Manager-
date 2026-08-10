package com.example.models

import java.io.Serializable

enum class PlayStyle {
    FAST_BREAK,
    HALF_COURT,
    DEFENSIVE,
    BALANCED
}

data class Tactics(
    var style: PlayStyle = PlayStyle.BALANCED,
    var pace: Int = 50,
    var defensivePressure: Int = 50,
    var offensiveRebound: Int = 50
) : Serializable {
    fun getPaceFactor(): Double {
        return 1.0 + ((pace - 50) * 0.004)
    }

    fun getOffensiveModifier(): Double {
        val baseStyleMod = when (style) {
            PlayStyle.FAST_BREAK -> 1.10
            PlayStyle.HALF_COURT -> 0.95
            PlayStyle.DEFENSIVE -> 0.85
            PlayStyle.BALANCED -> 1.00
        }
        val offRebMod = 1.0 + ((offensiveRebound - 50) * 0.003)
        return baseStyleMod * offRebMod * getPaceFactor()
    }

    fun getDefensiveModifier(): Double {
        val baseStyleMod = when (style) {
            PlayStyle.FAST_BREAK -> 0.85
            PlayStyle.HALF_COURT -> 1.05
            PlayStyle.DEFENSIVE -> 1.20
            PlayStyle.BALANCED -> 1.00
        }
        val defPressMod = 1.0 + ((defensivePressure - 50) * 0.004)
        return baseStyleMod * defPressMod
    }
}
