package com.example.data.repository

import com.example.data.local.PlayerDao
import com.example.data.local.PlayerEntity

class PlayerRepository(private val dao: PlayerDao) {
    suspend fun getAll(): List<PlayerEntity> = dao.all()
    suspend fun upsertAll(players: List<PlayerEntity>) = dao.upsertAll(players)
    suspend fun archiveAll() = dao.archiveAll()
}
