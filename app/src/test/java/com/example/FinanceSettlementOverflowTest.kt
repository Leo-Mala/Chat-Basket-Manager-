package com.example

import com.example.domain.finance.FinanceManager
import com.example.models.Finance
import com.example.models.Sponsor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceSettlementOverflowTest {
    private val manager = FinanceManager()

    @Test
    fun offseasonSettlementNetsCreditsAndSalaryBeforeClamping() {
        val start = Int.MAX_VALUE - 10
        val result = manager.applyOffseasonSettlement(
            finance = Finance(budget = start, coachSalaryPaid = false),
            coachSalary = 100_000_000,
            nextSeasonNumber = 2,
            completedSeasonNumber = 1,
            tvRights = 85_000_000
        )

        assertEquals(start - 15_000_000, result.budget)
        assertFalse(result.coachSalaryPaid)
        assertTrue(result.expenses.any { it.description == "Cota Direitos de TV & Liga" })
        assertTrue(result.expenses.any { it.description == "Salário Anual do Técnico" })
    }

    @Test
    fun offseasonSettlementClampsPositiveOverflowInsteadOfWrappingNegative() {
        val result = manager.applyOffseasonSettlement(
            finance = Finance(budget = Int.MAX_VALUE - 10, coachSalaryPaid = true),
            coachSalary = 350_000,
            nextSeasonNumber = 2,
            completedSeasonNumber = 1
        )

        assertEquals(Int.MAX_VALUE, result.budget)
    }

    @Test
    fun championRewardsClampAfterPrizeAndSponsorBonus() {
        val finance = Finance(
            budget = Int.MAX_VALUE - 5,
            sponsors = listOf(
                Sponsor("A", 1_000_000_000, 2),
                Sponsor("B", 1_000_000_000, 2)
            )
        )

        val result = manager.applyPlayoffRewards(
            finance = finance,
            prize = 35_000_000,
            label = "Prêmio Campeão da NBA 🏆",
            champion = true,
            seasonNumber = 1
        )

        assertEquals(Int.MAX_VALUE, result.budget)
        assertEquals("Bônus Patrocinador (Título 🏆)", result.expenses.first().description)
        assertEquals(1_000_000_000, result.expenses.first().amount)
        assertTrue(result.expenses.any { it.description == "Prêmio Campeão da NBA 🏆" })
    }
}
