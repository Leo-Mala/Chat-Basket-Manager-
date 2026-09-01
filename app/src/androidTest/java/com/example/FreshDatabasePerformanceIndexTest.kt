package com.example

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.local.BasketDatabase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FreshDatabasePerformanceIndexTest {
    private val expectedIndexes = listOf(
        "index_player_game_stats_playerId",
        "index_player_game_stats_gameId",
        "index_game_injuries_playerId",
        "index_game_injuries_gameId"
    )

    private fun hasIndex(sqlite: SupportSQLiteDatabase, name: String): Boolean =
        sqlite.query(
            "SELECT name FROM sqlite_master WHERE type = 'index' AND name = ?",
            arrayOf(name)
        ).use { cursor -> cursor.moveToFirst() }

    private fun assertSupplementalIndexes(sqlite: SupportSQLiteDatabase, messagePrefix: String) {
        expectedIndexes.forEach { name ->
            assertTrue("$messagePrefix $name", hasIndex(sqlite, name))
        }
    }

    @Test
    fun freshDatabaseHasSupplementalStatAndInjuryIndexes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbName = "fresh-index-parity-${System.nanoTime()}.db"
        context.deleteDatabase(dbName)

        val database = BasketDatabase.createDatabase(context, dbName)
        try {
            assertSupplementalIndexes(
                database.openHelper.writableDatabase,
                "Fresh database is missing supplemental index"
            )
        } finally {
            database.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun existingVersion7DatabaseRepairsMissingSupplementalIndexesOnOpen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbName = "existing-index-repair-${System.nanoTime()}.db"
        context.deleteDatabase(dbName)

        val original = BasketDatabase.createDatabase(context, dbName)
        try {
            val sqlite = original.openHelper.writableDatabase
            expectedIndexes.forEach { name -> sqlite.execSQL("DROP INDEX IF EXISTS $name") }
            expectedIndexes.forEach { name ->
                assertFalse("Test setup failed to remove supplemental index $name", hasIndex(sqlite, name))
            }
        } finally {
            original.close()
        }

        val reopened = BasketDatabase.createDatabase(context, dbName)
        try {
            assertSupplementalIndexes(
                reopened.openHelper.writableDatabase,
                "Existing database was not repaired with supplemental index"
            )
        } finally {
            reopened.close()
            context.deleteDatabase(dbName)
        }
    }
}
