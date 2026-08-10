package com.example.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface ContractDao {
    @Query("SELECT * FROM contracts ORDER BY playerId") suspend fun all(): List<ContractEntity>
    @Query("SELECT * FROM contracts WHERE playerId = :playerId LIMIT 1") suspend fun find(playerId: Int): ContractEntity?
    @Query("SELECT * FROM contracts WHERE teamId = :teamId") suspend fun forTeam(teamId: String): List<ContractEntity>
    @Upsert suspend fun upsert(contract: ContractEntity)
    @Upsert suspend fun upsertAll(contracts: List<ContractEntity>)
    @Query("DELETE FROM contracts WHERE playerId = :playerId") suspend fun delete(playerId: Int)
    @Query("DELETE FROM contracts") suspend fun clear()
}
