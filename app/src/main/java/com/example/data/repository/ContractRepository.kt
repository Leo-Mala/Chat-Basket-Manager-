package com.example.data.repository

import com.example.data.local.ContractDao
import com.example.data.local.ContractEntity

class ContractRepository(private val dao: ContractDao) {
    suspend fun getAll() = dao.all()
    suspend fun find(playerId: Int) = dao.find(playerId)
    suspend fun forTeam(teamId: String) = dao.forTeam(teamId)
    suspend fun save(contract: ContractEntity) = dao.upsert(contract)
    suspend fun saveAll(contracts: List<ContractEntity>) = dao.upsertAll(contracts)
    suspend fun delete(playerId: Int) = dao.delete(playerId)
}
