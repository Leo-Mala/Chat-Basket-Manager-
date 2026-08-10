package com.example.models

import java.io.Serializable

/** Durable player contract owned by the player's current team. */
data class PlayerContract(
    val playerId: Int,
    val teamId: String?,
    val salary: Long,
    val yearsRemaining: Int,
    val playerOption: Boolean = false,
    val noTrade: Boolean = false
) : Serializable {
    init {
        require(salary >= 0) { "salary must be non-negative" }
        require(yearsRemaining in 0..5) { "yearsRemaining must be between 0 and 5" }
    }
}
