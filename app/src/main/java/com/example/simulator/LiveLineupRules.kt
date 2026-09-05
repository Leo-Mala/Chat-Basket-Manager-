package com.example.simulator

import com.example.models.Player

data class LiveLineupSubstitution(
    val lineup: List<Player>,
    val playerOut: Player,
    val playerIn: Player
)

/** Match-local lineup rules. They never mutate the persisted pre-game starting five. */
object LiveLineupRules {
    const val PLAYERS_ON_COURT = 5

    fun initialLineup(roster: List<Player>, preferred: List<Player>): List<Player> {
        val eligible = RotationRules.eligibleForGame(roster)
        val eligibleIds = eligible.mapTo(hashSetOf()) { it.id }
        val preferredEligible = preferred
            .asSequence()
            .filter { it.id in eligibleIds }
            .distinctBy { it.id }
            .toList()
        val fillers = eligible
            .asSequence()
            .filter { candidate -> preferredEligible.none { it.id == candidate.id } }
            .sortedWith(compareByDescending<Player> { it.overall }.thenByDescending { it.athleticism })
            .toList()
        return (preferredEligible + fillers).distinctBy { it.id }.take(PLAYERS_ON_COURT)
    }

    fun bench(roster: List<Player>, activeLineup: List<Player>): List<Player> {
        val activeIds = activeLineup.mapTo(hashSetOf()) { it.id }
        return RotationRules.eligibleForGame(roster)
            .filterNot { it.id in activeIds }
            .sortedWith(compareByDescending<Player> { it.overall }.thenByDescending { it.athleticism })
    }

    fun substitute(
        roster: List<Player>,
        activeLineup: List<Player>,
        playerOutId: Int,
        playerInId: Int
    ): LiveLineupSubstitution? {
        if (activeLineup.size != PLAYERS_ON_COURT) return null
        if (activeLineup.map { it.id }.distinct().size != PLAYERS_ON_COURT) return null

        val playerOut = activeLineup.firstOrNull { it.id == playerOutId } ?: return null
        val eligibleById = RotationRules.eligibleForGame(roster).associateBy { it.id }
        val playerIn = eligibleById[playerInId] ?: return null
        if (activeLineup.any { it.id == playerIn.id }) return null

        val updated = activeLineup.map { if (it.id == playerOut.id) playerIn else it }
        if (updated.size != PLAYERS_ON_COURT || updated.map { it.id }.distinct().size != PLAYERS_ON_COURT) {
            return null
        }
        return LiveLineupSubstitution(updated, playerOut, playerIn)
    }
}
