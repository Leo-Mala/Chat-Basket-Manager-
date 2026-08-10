package com.example.data.repository

import com.example.data.local.SeasonDao
import com.example.data.local.StandingDao
import com.example.data.local.SeasonEntity
import com.example.data.local.StandingEntity

class SeasonRepository(
    private val seasonDao: SeasonDao,
    private val standingDao: StandingDao
) {
    suspend fun getSeasons(): List<SeasonEntity> = seasonDao.all()
    suspend fun saveSeason(season: SeasonEntity) = seasonDao.upsert(season)
    suspend fun saveStandings(rows: List<StandingEntity>) = standingDao.upsertAll(rows)
    suspend fun getStandings(): List<StandingEntity> = standingDao.all()
}
