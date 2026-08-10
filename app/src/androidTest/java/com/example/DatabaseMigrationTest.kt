package com.example

import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.local.BasketDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BasketDatabase::class.java,
        emptyList()
    )

    @Test
    fun realMigrations2To5PreserveDataAndCreateRequiredSchema() {
        helper.createDatabase("migration-test", 2).apply {
            execSQL("INSERT INTO teams VALUES ('T1','Test','Test','T1','East','Arena','Test',10000,2020)")
            execSQL("INSERT INTO seasons VALUES (1,0,0,1,10,2025,'T1')")
            close()
        }

        val db = helper.runMigrationsAndValidate(
            "migration-test", 5, true,
            BasketDatabase.MIGRATION_2_3,
            BasketDatabase.MIGRATION_3_4,
            BasketDatabase.MIGRATION_4_5
        )

        db.query("SELECT name FROM teams WHERE id = 'T1'").use { cursor -> assertTrue(cursor.moveToFirst()) }
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
        db.close()
    }

    @Test
    fun realMigration3To4CreatesAllExpectedIndexes() {
        helper.createDatabase("migration-index-test", 3).apply {
            close()
        }

        val db = helper.runMigrationsAndValidate(
            "migration-index-test", 4, true,
            BasketDatabase.MIGRATION_3_4
        )

        val expected = listOf(
            "index_players_teamId", "index_players_active", "index_players_poolType",
            "index_games_seasonId", "index_games_homeTeamId", "index_games_awayTeamId",
            "index_player_game_stats_playerId", "index_player_game_stats_gameId",
            "index_game_injuries_playerId", "index_game_injuries_gameId",
            "index_contracts_teamId"
        )
        expected.forEach { name ->
            db.query("SELECT name FROM sqlite_master WHERE type = 'index' AND name = ?", arrayOf(name)).use { cursor ->
                assertTrue("Missing index $name", cursor.moveToFirst())
            }
        }
        db.close()
    }
    @Test
    fun realMigrations1To5CreateCurrentSchema() {
        helper.createDatabase("migration-1-5-test", 1).apply {
            close()
        }

        val db = helper.runMigrationsAndValidate(
            "migration-1-5-test", 5, true,
            BasketDatabase.MIGRATION_1_2,
            BasketDatabase.MIGRATION_2_3,
            BasketDatabase.MIGRATION_3_4,
            BasketDatabase.MIGRATION_4_5
        )

        db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'contracts'").use { cursor ->
            assertTrue(cursor.moveToFirst())
        }
        db.query("SELECT name FROM sqlite_master WHERE type = 'index' AND name = 'index_players_teamId'").use { cursor ->
            assertTrue(cursor.moveToFirst())
        }
        db.close()
    }

}
