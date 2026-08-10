package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Upsert

@Entity(tableName = "sponsors", primaryKeys = ["financeId", "name"])
data class SponsorEntity(
    val financeId: Int = 1,
    val name: String,
    val amountPerYear: Int,
    val yearsRemaining: Int
)

@Entity(tableName = "finance_expenses", primaryKeys = ["financeId", "description", "date"])
data class ExpenseEntity(
    val financeId: Int = 1,
    val description: String,
    val amount: Int,
    val date: String
)

@Dao
interface SponsorDao {
    @Query("SELECT * FROM sponsors ORDER BY name") suspend fun all(): List<SponsorEntity>
    @Upsert suspend fun upsertAll(items: List<SponsorEntity>)
    @Query("DELETE FROM sponsors") suspend fun clear()
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM finance_expenses ORDER BY date, description") suspend fun all(): List<ExpenseEntity>
    @Upsert suspend fun upsertAll(items: List<ExpenseEntity>)
    @Query("DELETE FROM finance_expenses") suspend fun clear()
}
