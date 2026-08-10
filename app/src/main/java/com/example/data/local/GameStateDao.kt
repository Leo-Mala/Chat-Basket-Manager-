package com.example.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface GameStateDao {
    @Query("SELECT * FROM game_state WHERE id = 1 LIMIT 1")
    suspend fun get(): GameStateEntity?

    @Upsert
    suspend fun upsert(state: GameStateEntity)

    @Query("DELETE FROM game_state WHERE id = 1")
    suspend fun clear()
}
