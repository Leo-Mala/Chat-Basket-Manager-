package com.example.data.repository

import com.example.data.local.ExpenseDao
import com.example.data.local.ExpenseEntity
import com.example.data.local.FinanceDao
import com.example.data.local.FinanceEntity
import com.example.data.local.SponsorDao
import com.example.data.local.SponsorEntity

class FinanceRepository(
    private val financeDao: FinanceDao,
    private val sponsorDao: SponsorDao,
    private val expenseDao: ExpenseDao
) {
    suspend fun getFinance(): FinanceEntity? = financeDao.get()
    suspend fun saveFinance(finance: FinanceEntity) = financeDao.upsert(finance)
    suspend fun saveSponsors(sponsors: List<SponsorEntity>) = sponsorDao.upsertAll(sponsors)
    suspend fun saveExpenses(expenses: List<ExpenseEntity>) = expenseDao.upsertAll(expenses)
    suspend fun getSponsors(): List<SponsorEntity> = sponsorDao.all()
    suspend fun getExpenses(): List<ExpenseEntity> = expenseDao.all()
}
