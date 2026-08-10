package com.example.models

import java.io.Serializable

data class FinanceAdvanced(
    var salaryCap: Int = 140_000_000,
    var luxuryTaxThreshold: Int = 170_000_000,
    var ticketPrice: Int = 85, // $ por ingresso
    var ticketPriceMultiplier: Double = 1.0,
    var fanSatisfaction: Int = 75, // 0-100
    var ownerSatisfaction: Int = 80, // 0-100
    var ownerObjective: String = "Vencer 45+ jogos e manter orçamento sob controle.",
    val revenues: AdvancedRevenues = AdvancedRevenues(),
    val expenses: AdvancedExpenses = AdvancedExpenses(),
    val activeSponsorships: MutableList<SponsorshipDeal> = mutableListOf()
) : Serializable {

    fun calculateLuxuryTax(totalPlayerSalaries: Int): Int {
        if (totalPlayerSalaries <= luxuryTaxThreshold) return 0
        val excess = totalPlayerSalaries - luxuryTaxThreshold
        return (excess * 1.5).toInt()
    }

    fun calculateNetProfitLoss(): Int {
        return revenues.totalRevenue() - expenses.totalExpenses()
    }
}

data class AdvancedRevenues(
    var ticketRevenue: Int = 0,
    var sponsorshipRevenue: Int = 0,
    var merchandiseRevenue: Int = 20_000_000,
    var broadcastingRevenue: Int = 85_000_000,
    var playoffRevenue: Int = 0
) : Serializable {
    fun totalRevenue(): Int = ticketRevenue + sponsorshipRevenue + merchandiseRevenue + broadcastingRevenue + playoffRevenue
}

data class AdvancedExpenses(
    var playerSalaries: Int = 0,
    var staffSalaries: Int = 0,
    var facilityMaintenance: Int = 0,
    var travelLogistics: Int = 1_500_000,
    var operationalExpenses: Int = 2_000_000,
    var luxuryTaxPaid: Int = 0
) : Serializable {
    fun totalExpenses(): Int = playerSalaries + staffSalaries + facilityMaintenance + travelLogistics + operationalExpenses + luxuryTaxPaid
}

data class SponsorshipDeal(
    val brandName: String,
    val type: String, // Regional, Nacional, Global
    val annualAmount: Int,
    var yearsRemaining: Int,
    val goalDescription: String? = null,
    val goalBonus: Int = 0
) : Serializable
