package com.example.models

import java.io.Serializable

data class Finance(
    var budget: Int = 100000000,
    var sponsors: List<Sponsor> = emptyList(),
    var expenses: MutableList<Expense> = mutableListOf(),
    var coachSalaryPaid: Boolean = false,
    var arenaSeatsLevel: Int = 1,
    var medicalStaffLevel: Int = 1,
    var scoutingLevel: Int = 1
) : Serializable {
    fun getArenaCapacity(baseCapacity: Int): Int {
        val lvl = if (arenaSeatsLevel < 1) 1 else arenaSeatsLevel
        return baseCapacity + (lvl - 1) * 2000
    }
}

data class Sponsor(
    val name: String,
    val amountPerYear: Int,
    val yearsRemaining: Int
) : Serializable

data class Expense(
    val description: String,
    val amount: Int,
    val date: String
) : Serializable
