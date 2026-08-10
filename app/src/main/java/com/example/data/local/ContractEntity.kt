package com.example.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(tableName = "contracts", indices = [Index(value = ["teamId"])])
data class ContractEntity(
    @androidx.room.PrimaryKey val playerId: Int,
    val teamId: String?,
    val salary: Long,
    val yearsRemaining: Int,
    val playerOption: Boolean = false,
    val noTrade: Boolean = false
)
