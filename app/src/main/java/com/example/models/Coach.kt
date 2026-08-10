package com.example.models

import java.io.Serializable

data class Coach(
    val id: Int,
    val name: String,
    var offensiveSkill: Int,
    var defensiveSkill: Int,
    var motivationalSkill: Int,
    var salary: Int,
    var contractYears: Int
) : Serializable {
    fun getOffensiveBonus(): Double = offensiveSkill / 100.0 * 0.2
    fun getDefensiveBonus(): Double = defensiveSkill / 100.0 * 0.2
    fun getMotivationalBonus(): Double = motivationalSkill / 100.0 * 0.1
}
