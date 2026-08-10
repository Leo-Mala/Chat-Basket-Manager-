package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        GameStateEntity::class,
        TeamEntity::class,
        PlayerEntity::class,
        CoachEntity::class,
        FinanceEntity::class,
        TacticsEntity::class,
        SeasonEntity::class,
        StandingEntity::class,
        GameEntity::class,
        PlayerGameStatEntity::class,
        GameInjuryEntity::class,
        AwardEntity::class,
        SeasonHistoryEntity::class,
        SeasonHistoryTeamWinEntity::class,
        SeasonHistoryPlayerEntity::class,
        SponsorEntity::class,
        ExpenseEntity::class,
        ContractEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class BasketDatabase : RoomDatabase() {
    abstract fun gameStateDao(): GameStateDao
    abstract fun teamDao(): TeamDao
    abstract fun playerDao(): PlayerDao
    abstract fun coachDao(): CoachDao
    abstract fun financeDao(): FinanceDao
    abstract fun tacticsDao(): TacticsDao
    abstract fun seasonDao(): SeasonDao
    abstract fun standingDao(): StandingDao
    abstract fun gameDao(): GameDao
    abstract fun playerGameStatDao(): PlayerGameStatDao
    abstract fun gameInjuryDao(): GameInjuryDao
    abstract fun awardDao(): AwardDao
    abstract fun seasonHistoryDao(): SeasonHistoryDao
    abstract fun seasonHistoryTeamWinDao(): SeasonHistoryTeamWinDao
    abstract fun seasonHistoryPlayerDao(): SeasonHistoryPlayerDao
    abstract fun sponsorDao(): SponsorDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun contractDao(): ContractDao

    companion object {
        @Volatile private var INSTANCE: BasketDatabase? = null

        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS teams (
                    id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, city TEXT NOT NULL,
                    abbreviation TEXT NOT NULL, conference TEXT NOT NULL, arenaName TEXT NOT NULL,
                    arenaCity TEXT NOT NULL, arenaCapacity INTEGER NOT NULL, arenaOpened INTEGER NOT NULL)""")
                db.execSQL("""CREATE TABLE IF NOT EXISTS players (
                    id INTEGER NOT NULL PRIMARY KEY, teamId TEXT, poolType TEXT NOT NULL,
                    active INTEGER NOT NULL, startingFive INTEGER NOT NULL, name TEXT NOT NULL, position TEXT NOT NULL,
                    overall INTEGER NOT NULL, shooting INTEGER NOT NULL, defense INTEGER NOT NULL,
                    rebound INTEGER NOT NULL, passing INTEGER NOT NULL, athleticism INTEGER NOT NULL,
                    age INTEGER NOT NULL, xp INTEGER NOT NULL, trainings INTEGER NOT NULL,
                    injured INTEGER NOT NULL, injuryDays INTEGER NOT NULL, careerPoints INTEGER NOT NULL,
                    careerRebounds INTEGER NOT NULL, careerAssists INTEGER NOT NULL, careerSteals INTEGER NOT NULL,
                    careerBlocks INTEGER NOT NULL, careerGames INTEGER NOT NULL, championships INTEGER NOT NULL,
                    mvps INTEGER NOT NULL, seasonPoints INTEGER NOT NULL, seasonRebounds INTEGER NOT NULL,
                    seasonAssists INTEGER NOT NULL, seasonSteals INTEGER NOT NULL, seasonBlocks INTEGER NOT NULL,
                    seasonGames INTEGER NOT NULL)""")
                db.execSQL("CREATE TABLE IF NOT EXISTS coaches (id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL, offensiveSkill INTEGER NOT NULL, defensiveSkill INTEGER NOT NULL, motivationalSkill INTEGER NOT NULL, salary INTEGER NOT NULL, contractYears INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS finances (id INTEGER NOT NULL PRIMARY KEY, budget INTEGER NOT NULL, coachSalaryPaid INTEGER NOT NULL, arenaSeatsLevel INTEGER NOT NULL, medicalStaffLevel INTEGER NOT NULL, scoutingLevel INTEGER NOT NULL, sponsorsJson TEXT NOT NULL, expensesJson TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS tactics (id INTEGER NOT NULL PRIMARY KEY, style TEXT NOT NULL, pace INTEGER NOT NULL, defensivePressure INTEGER NOT NULL, offensiveRebound INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS seasons (id INTEGER NOT NULL PRIMARY KEY, currentDay INTEGER NOT NULL, gamesPlayed INTEGER NOT NULL, seasonNumber INTEGER NOT NULL, currentMonth INTEGER NOT NULL, currentYear INTEGER NOT NULL, userTeamId TEXT)")
                db.execSQL("CREATE TABLE IF NOT EXISTS standings (seasonId INTEGER NOT NULL, teamId TEXT NOT NULL, wins INTEGER NOT NULL, losses INTEGER NOT NULL, gamesPlayed INTEGER NOT NULL, totalPointsScored INTEGER NOT NULL, totalPointsConceded INTEGER NOT NULL, PRIMARY KEY(seasonId, teamId))")
                db.execSQL("CREATE TABLE IF NOT EXISTS games (id TEXT NOT NULL PRIMARY KEY, seasonId INTEGER NOT NULL, homeTeamId TEXT NOT NULL, awayTeamId TEXT NOT NULL, homeScore INTEGER NOT NULL, awayScore INTEGER NOT NULL, attendance INTEGER NOT NULL, narration TEXT NOT NULL, roundName TEXT)")
                db.execSQL("CREATE TABLE IF NOT EXISTS player_game_stats (gameId TEXT NOT NULL, playerId INTEGER NOT NULL, points INTEGER NOT NULL, rebounds INTEGER NOT NULL, assists INTEGER NOT NULL, steals INTEGER NOT NULL, blocks INTEGER NOT NULL, turnovers INTEGER NOT NULL, plusMinus INTEGER NOT NULL, PRIMARY KEY(gameId, playerId))")
                db.execSQL("CREATE TABLE IF NOT EXISTS game_injuries (gameId TEXT NOT NULL, playerId INTEGER NOT NULL, daysOut INTEGER NOT NULL, PRIMARY KEY(gameId, playerId))")
                db.execSQL("CREATE TABLE IF NOT EXISTS awards (seasonId INTEGER NOT NULL PRIMARY KEY, mvpPlayerId INTEGER NOT NULL, defensivePlayerId INTEGER NOT NULL, sixthManPlayerId INTEGER NOT NULL, rookieOfYearPlayerId INTEGER NOT NULL, mostImprovedPlayerId INTEGER NOT NULL, coachOfYearName TEXT NOT NULL, coachOfYearTeam TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS season_history (seasonNumber INTEGER NOT NULL PRIMARY KEY, champion TEXT NOT NULL, mvp TEXT, finalScore TEXT NOT NULL, topScorer TEXT NOT NULL, topScorerPoints REAL NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS season_history_team_wins (seasonNumber INTEGER NOT NULL, teamId TEXT NOT NULL, wins INTEGER NOT NULL, PRIMARY KEY(seasonNumber, teamId))")
                db.execSQL("CREATE TABLE IF NOT EXISTS sponsors (financeId INTEGER NOT NULL, name TEXT NOT NULL, amountPerYear INTEGER NOT NULL, yearsRemaining INTEGER NOT NULL, PRIMARY KEY(financeId, name))")
                db.execSQL("CREATE TABLE IF NOT EXISTS finance_expenses (financeId INTEGER NOT NULL, description TEXT NOT NULL, amount INTEGER NOT NULL, date TEXT NOT NULL, PRIMARY KEY(financeId, description, date))")
                db.execSQL("CREATE TABLE IF NOT EXISTS season_history_players (seasonNumber INTEGER NOT NULL, playerId INTEGER NOT NULL, name TEXT NOT NULL, position TEXT NOT NULL, overall INTEGER NOT NULL, shooting INTEGER NOT NULL, defense INTEGER NOT NULL, rebound INTEGER NOT NULL, passing INTEGER NOT NULL, athleticism INTEGER NOT NULL, age INTEGER NOT NULL, xp INTEGER NOT NULL, trainings INTEGER NOT NULL, careerPoints INTEGER NOT NULL, careerRebounds INTEGER NOT NULL, careerAssists INTEGER NOT NULL, careerSteals INTEGER NOT NULL, careerBlocks INTEGER NOT NULL, careerGames INTEGER NOT NULL, championships INTEGER NOT NULL, mvps INTEGER NOT NULL, seasonPoints INTEGER NOT NULL, seasonRebounds INTEGER NOT NULL, seasonAssists INTEGER NOT NULL, seasonSteals INTEGER NOT NULL, seasonBlocks INTEGER NOT NULL, seasonGames INTEGER NOT NULL, PRIMARY KEY(seasonNumber, playerId))")
            }
        }

        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS contracts (
                    playerId INTEGER NOT NULL PRIMARY KEY,
                    teamId TEXT,
                    salary INTEGER NOT NULL,
                    yearsRemaining INTEGER NOT NULL,
                    playerOption INTEGER NOT NULL,
                    noTrade INTEGER NOT NULL
                )""")
            }
        }

        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_players_teamId ON players(teamId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_players_active ON players(active)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_players_poolType ON players(poolType)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_games_seasonId ON games(seasonId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_games_homeTeamId ON games(homeTeamId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_games_awayTeamId ON games(awayTeamId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_player_game_stats_playerId ON player_game_stats(playerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_player_game_stats_gameId ON player_game_stats(gameId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_game_injuries_playerId ON game_injuries(playerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_game_injuries_gameId ON game_injuries(gameId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_contracts_teamId ON contracts(teamId)")
            }
        }

        internal val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE seasons ADD COLUMN nextPlayerId INTEGER NOT NULL DEFAULT 1")
                db.execSQL("UPDATE seasons SET nextPlayerId = COALESCE((SELECT MAX(id) + 1 FROM players), 1) WHERE nextPlayerId <= 1")
            }
        }

        fun getInstance(context: Context): BasketDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BasketDatabase::class.java,
                    "basket_manager.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build().also { INSTANCE = it }
            }
    }
}
