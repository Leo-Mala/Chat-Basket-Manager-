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
class BoxScoreStatSideMigrationTest {
    @Test
    fun migration6To7AddsAndBackfillsStatTeamIdentity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration-box-score-side-test"
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
            val db = helper.writableDatabase
            BasketDatabase.MIGRATION_1_2.migrate(db)
            db.execSQL("CREATE TABLE IF NOT EXISTS game_state (id INTEGER NOT NULL PRIMARY KEY)")
            BasketDatabase.MIGRATION_2_3.migrate(db)
            BasketDatabase.MIGRATION_3_4.migrate(db)
            BasketDatabase.MIGRATION_4_5.migrate(db)
            BasketDatabase.MIGRATION_5_6.migrate(db)

            db.execSQL("INSERT INTO teams VALUES ('HOM','Home','Home','HOM','East','Arena','Home',10000,2020)")
            db.execSQL("INSERT INTO teams VALUES ('AWY','Away','Away','AWY','West','Arena','Away',10000,2020)")
            db.execSQL(
                "INSERT INTO players (id,teamId,poolType,active,startingFive,name,position,overall,shooting,defense,rebound,passing,athleticism,age,xp,trainings,injured,injuryDays,careerPoints,careerRebounds,careerAssists,careerSteals,careerBlocks,careerGames,championships,mvps,seasonPoints,seasonRebounds,seasonAssists,seasonSteals,seasonBlocks,seasonGames) " +
                    "VALUES (1,'HOM','ROSTER',1,1,'Home Player','PG',80,80,80,80,80,80,25,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0)"
            )
            db.execSQL("INSERT INTO games VALUES ('g1',1,'HOM','AWY',110,100,18000,'migration',NULL)")
            db.execSQL("INSERT INTO player_game_stats VALUES ('g1',1,25,5,6,1,0,2,8)")

            BasketDatabase.MIGRATION_6_7.migrate(db)

            db.query("PRAGMA table_info(player_game_stats)").use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                var found = false
                while (cursor.moveToNext()) if (cursor.getString(nameIndex) == "teamId") found = true
                assertTrue("Missing player_game_stats.teamId after migration 6->7", found)
            }
            db.query("SELECT teamId FROM player_game_stats WHERE gameId = 'g1' AND playerId = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("HOM", cursor.getString(0))
            }
        } finally {
            helper.close()
            context.deleteDatabase(name)
        }
    }
}
