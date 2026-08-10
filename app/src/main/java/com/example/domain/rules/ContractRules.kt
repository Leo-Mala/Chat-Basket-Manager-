package com.example.domain.rules

import com.example.models.Player
import kotlin.math.roundToLong

/** Centralized roster/contract rules used by free agency and trades. */
object ContractRules {
    const val MIN_ROSTER = 10
    const val STANDARD_ROSTER = 12
    const val MAX_ROSTER = 15

    fun marketValue(player: Player): Long {
        val performance = (player.overall * 1_000_000L)
        val ageFactor = when {
            player.age <= 23 -> 1.20
            player.age <= 28 -> 1.10
            player.age <= 32 -> 0.95
            else -> 0.75
        }
        return (performance * ageFactor).roundToLong().coerceAtLeast(250_000L)
    }

    fun annualSalary(player: Player): Long = (marketValue(player) * 0.085).roundToLong().coerceAtLeast(1_000_000L)

    fun canSign(rosterSize: Int): Boolean = rosterSize < MAX_ROSTER
    fun mustReleaseForStandardRoster(rosterSize: Int): Boolean = rosterSize >= STANDARD_ROSTER
}
