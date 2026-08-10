package com.example.models

import java.io.Serializable

data class Arena(
    val name: String,
    val city: String,
    val capacity: Int,
    val opened: Int
) : Serializable

data class NbaTeam(
    val name: String,
    val city: String,
    val abbreviation: String,
    val conference: String,
    val arena: Arena,
    val players: List<Player>
) : Serializable {
    fun getAverageOverall(): Double {
        if (players.isEmpty()) return 0.0
        return players.map { it.overall }.average()
    }

    val overall: Int
        get() = getAverageOverall().toInt()

    fun getBestPlayer(): Player? = players.maxByOrNull { it.overall }
}
