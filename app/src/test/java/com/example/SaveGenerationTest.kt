package com.example

import com.example.utils.SaveRequestCoordinator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression coverage for asynchronous save ordering and career reset invalidation. */
class SaveGenerationTest {
    @Test
    fun newerSaveInvalidatesOlderQueuedSnapshot() {
        val coordinator = SaveRequestCoordinator()
        val older = coordinator.nextSave()!!
        val newer = coordinator.nextSave()!!

        assertFalse(coordinator.isCurrent(older))
        assertTrue(coordinator.isCurrent(newer))
    }

    @Test
    fun resetBlocksSavesAndInvalidatesOldCareerTickets() {
        val coordinator = SaveRequestCoordinator()
        val oldCareerSave = coordinator.nextSave()!!
        val reset = coordinator.beginReset()

        assertFalse(coordinator.isCurrent(oldCareerSave))
        assertNull(coordinator.nextSave())
        assertTrue(coordinator.isCurrentReset(reset))
        assertTrue(coordinator.finishReset(reset))

        val newCareerSave = coordinator.nextSave()
        assertNotNull(newCareerSave)
        assertFalse(coordinator.isCurrent(oldCareerSave))
        assertTrue(coordinator.isCurrent(newCareerSave!!))
    }

    @Test
    fun newerResetSupersedesOlderResetCompletion() {
        val coordinator = SaveRequestCoordinator()
        val firstReset = coordinator.beginReset()
        val secondReset = coordinator.beginReset()

        assertFalse(coordinator.isCurrentReset(firstReset))
        assertFalse(coordinator.finishReset(firstReset))
        assertTrue(coordinator.isCurrentReset(secondReset))
        assertTrue(coordinator.finishReset(secondReset))
    }
}
