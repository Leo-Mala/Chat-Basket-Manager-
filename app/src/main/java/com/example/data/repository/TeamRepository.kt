package com.example.data.repository

import com.example.data.local.TeamDao
import com.example.data.local.TeamEntity

class TeamRepository(private val dao: TeamDao) {
    suspend fun getAll(): List<TeamEntity> = dao.all()
    suspend fun upsertAll(teams: List<TeamEntity>) = dao.upsertAll(teams)
    suspend fun clear() = dao.clear()
}
