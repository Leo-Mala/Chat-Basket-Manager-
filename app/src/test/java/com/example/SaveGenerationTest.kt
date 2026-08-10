package com.example

import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicLong

/** Documents the stale-save invalidation contract used by GameViewModel. */
class SaveGenerationTest {
    @Test
    fun newerGenerationInvalidatesOlderSnapshot() {
        val generation = AtomicLong(0)
        val old = generation.incrementAndGet()
        val newer = generation.incrementAndGet()
        assertTrue(old != generation.get())
        assertTrue(newer == generation.get())
    }
}
