package com.example

import com.example.models.Season
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerIdAllocatorTest {
    @Test
    fun allocatorIsMonotonicAndNeverReusesIds() {
        val season = Season(emptyList(), nextPlayerId = 1000)
        val first = season.allocatePlayerIds(3).toList()
        val second = season.allocatePlayerIds(2).toList()

        assertEquals(listOf(1000, 1001, 1002), first)
        assertEquals(listOf(1003, 1004), second)
        assertEquals(1005, season.nextPlayerId)
        assertTrue((first + second).distinct().size == 5)
    }
}
