package com.example.utils

/**
 * Coordinates asynchronous snapshots for one GameViewModel.
 *
 * A save ticket is valid only while it is the newest request in the current career.
 * Career resets temporarily block new saves and permanently invalidate every older ticket.
 */
class SaveRequestCoordinator {
    data class SaveTicket internal constructor(
        val generation: Long,
        val request: Long
    )

    private var generation = 0L
    private var request = 0L
    private var resetInProgress = false

    @Synchronized
    fun nextSave(): SaveTicket? {
        if (resetInProgress) return null
        request += 1
        return SaveTicket(generation = generation, request = request)
    }

    @Synchronized
    fun isCurrent(ticket: SaveTicket): Boolean =
        !resetInProgress && ticket.generation == generation && ticket.request == request

    @Synchronized
    fun beginReset(): Long {
        generation += 1
        request += 1
        resetInProgress = true
        return generation
    }

    @Synchronized
    fun isCurrentReset(token: Long): Boolean =
        resetInProgress && token == generation

    @Synchronized
    fun finishReset(token: Long): Boolean {
        if (!isCurrentReset(token)) return false
        resetInProgress = false
        return true
    }
}
