package com.example

import com.example.domain.finance.FinanceManager
import com.example.models.Arena
import com.example.models.Finance
import com.example.models.NbaTeam
import com.example.models.Sponsor
import com.example.simulator.GameSimulator
import org.junit.Assert.assertEquals
import org.junit.Test

class FinanceManagerOverflowTest {
    private val manager = FinanceManager()
    private val team = NbaTeam(
        name = "Test Team",
        city = "Test City",
        abbreviation = "TST",
        conference = "East",
        arena = Arena("Test Arena", "Test City", 20_000, 2020),
        players = emptyList()
    )

    @Test
    fun regularSeasonRevenueDoesNotWrapBudgetNegative() {
        val result = GameSimulator.GameResult(
            homeTeam = team,
            awayTeam = team.copy(name = "Away", abbreviation = "AWY"),
            homeScore = 100,
            awayScore = 90,
            attendance = 20_000,
            homeStats = emptyMap(),
            awayStats = emptyMap(),
            injuries = emptyList(),
            narration = ""
        )

        val updated = manager.applyRegularSeasonGame(
            finance = Finance(budget = Int.MAX_VALUE - 10),
            team = team,
            coach = null,
            result = result,
            isHome = true,
            day = 1,
            ticketPriceOverride = 500,
            annualPlayerPayroll = 0L
        )

        assertEquals(Int.MAX_VALUE, updated.budget)
    }

    @Test
    fun sponsorRevenueAggregationDoesNotOverflowBeforeDivision() {
        val finance = Finance(
            budget = 1_000,
            sponsors = listOf(
                Sponsor("A", Int.MAX_VALUE, 1),
                Sponsor("B", Int.MAX_VALUE, 1)
            )
        )
        val result = GameSimulator.GameResult(
            homeTeam = team,
            awayTeam = team.copy(name = "Away", abbreviation = "AWY"),
            homeScore = 100,
            awayScore = 90,
            attendance = 0,
            homeStats = emptyMap(),
            awayStats = emptyMap(),
            injuries = emptyList(),
            narration = ""
        )

        val updated = manager.applyRegularSeasonGame(
            finance = finance,
            team = team,
            coach = null,
            result = result,
            isHome = false,
            day = 1,
            annualPlayerPayroll = 0L
        )

        val expectedSponsorRevenue = ((Int.MAX_VALUE.toLong() * 2L) / 82L).toInt()
        assertEquals(1_000 + expectedSponsorRevenue, updated.budget)
    }
}
