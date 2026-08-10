package com.example.data.repository

import com.example.data.local.GameDao
import com.example.data.local.GameEntity
import com.example.data.local.GameInjuryDao
import com.example.data.local.GameInjuryEntity
import com.example.data.local.PlayerGameStatDao
import com.example.data.local.PlayerGameStatEntity

class GameRepository(
    private val gameDao: GameDao,
    private val statDao: PlayerGameStatDao,
    private val injuryDao: GameInjuryDao
) {
    suspend fun getGames(): List<GameEntity> = gameDao.all()
    suspend fun saveGames(games: List<GameEntity>) = gameDao.upsertAll(games)
    suspend fun saveStats(stats: List<PlayerGameStatEntity>) = statDao.upsertAll(stats)
    suspend fun saveInjuries(injuries: List<GameInjuryEntity>) = injuryDao.upsertAll(injuries)
}
