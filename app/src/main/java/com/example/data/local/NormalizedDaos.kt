package com.example.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface TeamDao {
    @Query("SELECT * FROM teams ORDER BY name") suspend fun all(): List<TeamEntity>
    @Upsert suspend fun upsertAll(items: List<TeamEntity>)
    @Query("DELETE FROM teams") suspend fun clear()
}

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players ORDER BY id") suspend fun all(): List<PlayerEntity>
    @Upsert suspend fun upsertAll(items: List<PlayerEntity>)
    @Query("UPDATE players SET active = 0, teamId = NULL, startingFive = 0, poolType = 'HISTORICAL'") suspend fun archiveAll()
    @Query("DELETE FROM players") suspend fun clear()
}

@Dao
interface CoachDao {
    @Query("SELECT * FROM coaches LIMIT 1") suspend fun get(): CoachEntity?
    @Upsert suspend fun upsert(item: CoachEntity)
    @Query("DELETE FROM coaches") suspend fun clear()
}

@Dao
interface FinanceDao {
    @Query("SELECT * FROM finances WHERE id = 1") suspend fun get(): FinanceEntity?
    @Upsert suspend fun upsert(item: FinanceEntity)
    @Query("DELETE FROM finances") suspend fun clear()
}

@Dao
interface TacticsDao {
    @Query("SELECT * FROM tactics WHERE id = 1") suspend fun get(): TacticsEntity?
    @Upsert suspend fun upsert(item: TacticsEntity)
    @Query("DELETE FROM tactics") suspend fun clear()
}

@Dao
interface SeasonDao {
    @Query("SELECT * FROM seasons ORDER BY seasonNumber") suspend fun all(): List<SeasonEntity>
    @Upsert suspend fun upsert(item: SeasonEntity)
    @Query("DELETE FROM seasons") suspend fun clear()
}

@Dao
interface StandingDao {
    @Query("SELECT * FROM standings ORDER BY seasonId, teamId") suspend fun all(): List<StandingEntity>
    @Upsert suspend fun upsertAll(items: List<StandingEntity>)
    @Query("DELETE FROM standings WHERE seasonId = :seasonId") suspend fun deleteForSeason(seasonId: Int)
    @Query("DELETE FROM standings") suspend fun clear()
}

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY seasonId, id") suspend fun all(): List<GameEntity>
    @Upsert suspend fun upsertAll(items: List<GameEntity>)
    @Query("DELETE FROM games WHERE seasonId = :seasonId") suspend fun deleteForSeason(seasonId: Int)
    @Query("DELETE FROM games") suspend fun clear()
}

@Dao
interface PlayerGameStatDao {
    @Query("SELECT * FROM player_game_stats ORDER BY gameId, playerId") suspend fun all(): List<PlayerGameStatEntity>
    @Upsert suspend fun upsertAll(items: List<PlayerGameStatEntity>)
    @Query("DELETE FROM player_game_stats WHERE gameId IN (SELECT id FROM games WHERE seasonId = :seasonId)") suspend fun deleteForSeason(seasonId: Int)
    @Query("DELETE FROM player_game_stats") suspend fun clear()
}

@Dao
interface GameInjuryDao {
    @Query("SELECT * FROM game_injuries ORDER BY gameId, playerId") suspend fun all(): List<GameInjuryEntity>
    @Upsert suspend fun upsertAll(items: List<GameInjuryEntity>)
    @Query("DELETE FROM game_injuries WHERE gameId IN (SELECT id FROM games WHERE seasonId = :seasonId)") suspend fun deleteForSeason(seasonId: Int)
    @Query("DELETE FROM game_injuries") suspend fun clear()
}

@Dao
interface AwardDao {
    @Query("SELECT * FROM awards ORDER BY seasonId") suspend fun all(): List<AwardEntity>
    @Upsert suspend fun upsert(item: AwardEntity)
    @Query("DELETE FROM awards WHERE seasonId = :seasonId") suspend fun deleteForSeason(seasonId: Int)
    @Query("DELETE FROM awards") suspend fun clear()
}

@Dao
interface SeasonHistoryDao {
    @Query("SELECT * FROM season_history ORDER BY seasonNumber") suspend fun all(): List<SeasonHistoryEntity>
    @Upsert suspend fun upsertAll(items: List<SeasonHistoryEntity>)
    @Query("DELETE FROM season_history") suspend fun clear()
}

@Dao
interface SeasonHistoryTeamWinDao {
    @Query("SELECT * FROM season_history_team_wins ORDER BY seasonNumber, teamId") suspend fun all(): List<SeasonHistoryTeamWinEntity>
    @Upsert suspend fun upsertAll(items: List<SeasonHistoryTeamWinEntity>)
    @Query("DELETE FROM season_history_team_wins") suspend fun clear()
}

@Dao
interface SeasonHistoryPlayerDao {
    @Query("SELECT * FROM season_history_players ORDER BY seasonNumber, playerId") suspend fun all(): List<SeasonHistoryPlayerEntity>
    @Upsert suspend fun upsertAll(items: List<SeasonHistoryPlayerEntity>)
    @Query("DELETE FROM season_history_players") suspend fun clear()
}
