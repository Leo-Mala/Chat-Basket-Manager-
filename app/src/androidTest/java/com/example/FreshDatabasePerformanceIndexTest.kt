package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.local.BasketDatabase
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FreshDatabasePerformanceIndexTest {
    @Test
    fun freshDatabaseHasSupplementalStatAndInjuryIndexes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbName = "fresh-index-parity-${System.nanoTime()}.db"
        context.deleteDatabase(dbName)

        val database = BasketDatabase.createDatabase(context, dbName)
        try {
            val sqlite = database.openHelper.writableDatabase
            val expected = listOf(
                "index_player_game_stats_playerId",
                "index_player_game_stats_gameId",
                "index_game_injuries_playerId",
                "index_game_injuries_gameId"
            )

            expected.forEach { name ->
                sqlite.query(
                    "SELECT name FROM sqlite_master WHERE type = 'index' AND name = ?",
                    arrayOf(name)
                ).use { cursor ->
                    assertTrue("Fresh database is missing supplemental index $name", cursor.moveToFirst())
                }
            }
        } finally {
            database.close()
            context.deleteDatabase(dbName)
        }
    }
}
