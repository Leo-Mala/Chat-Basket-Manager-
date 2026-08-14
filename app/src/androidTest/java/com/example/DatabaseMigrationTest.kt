package com.example

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.local.BasketDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    private fun withFreshDatabase(
        name: String,
        block: (SupportSQLiteDatabase) -> Unit
    ) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(name)

        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) = Unit
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        try {
            block(helper.writableDatabase)
        } finally {
            helper.close()
            context.deleteDatabase(name)
        }
    }

    private fun createVersion2Schema(db: SupportSQLiteDatabase) {
        // Version 1 predates the normalized tables. MIGRATION_1_2 is deliberately
        // CREATE TABLE IF NOT EXISTS based, so applying it to a fresh v1 database
        // reproduces the normalized version-2 schema without fabricated Room JSON.
        BasketDatabase.MIGRATION_1_2.migrate(db)
    }

    @Test
    fun realMigrations2To5PreserveDataAndCreateRequiredSchema() =
        withFreshDatabase("migration-test") { db ->
            createVersion2Schema(db)
            db.execSQL("INSERT INTO teams VALUES ('T1','Test','Test','T1','East','Arena','Test',10000,2020)")
            db.execSQL("INSERT INTO seasons VALUES (1,0,0,1,10,2025,'T1')")

            BasketDatabase.MIGRATION_2_3.migrate(db)
            BasketDatabase.MIGRATION_3_4.migrate(db)
            BasketDatabase.MIGRATION_4_5.migrate(db)

            db.query("SELECT name FROM teams WHERE id = 'T1'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Test", cursor.getString(0))
            }
            db.query("SELECT nextPlayerId FROM seasons WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
            db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'contracts'").use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
            db.query("SELECT name FROM sqlite_master WHERE type = 'index' AND name = 'index_contracts_teamId'").use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
        }

    @Test
    fun realMigration3To4CreatesAllExpectedIndexes() =
        withFreshDatabase("migration-index-test") { db ->
            createVersion2Schema(db)
            BasketDatabase.MIGRATION_2_3.migrate(db)
            BasketDatabase.MIGRATION_3_4.migrate(db)

            val expected = listOf(
                "index_players_teamId", "index_players_active", "index_players_poolType",
                "index_games_seasonId", "index_games_homeTeamId", "index_games_awayTeamId",
                "index_player_game_stats_playerId", "index_player_game_stats_gameId",
                "index_game_injuries_playerId", "index_game_injuries_gameId",
                "index_contracts_teamId"
            )
            expected.forEach { name ->
                db.query(
                    "SELECT name FROM sqlite_master WHERE type = 'index' AND name = ?",
                    arrayOf(name)
                ).use { cursor ->
                    assertTrue("Missing index $name", cursor.moveToFirst())
                }
            }
        }

    @Test
    fun realMigrations1To5CreateCurrentSchema() =
        withFreshDatabase("migration-1-5-test") { db ->
            BasketDatabase.MIGRATION_1_2.migrate(db)
            BasketDatabase.MIGRATION_2_3.migrate(db)
            BasketDatabase.MIGRATION_3_4.migrate(db)
            BasketDatabase.MIGRATION_4_5.migrate(db)

            db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'contracts'").use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
            db.query("SELECT name FROM sqlite_master WHERE type = 'index' AND name = 'index_players_teamId'").use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
            db.query("PRAGMA table_info(seasons)").use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                var foundNextPlayerId = false
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == "nextPlayerId") {
                        foundNextPlayerId = true
                        break
                    }
                }
                assertTrue("Missing seasons.nextPlayerId after migration 4->5", foundNextPlayerId)
            }
        }
}
