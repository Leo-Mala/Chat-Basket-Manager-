package com.example

import com.example.data.NbaDataGenerator
import com.example.domain.contract.ContractManager
import com.example.domain.contract.ContractOffer
import com.example.domain.finance.FinanceManager
import com.example.domain.roster.RosterManager
import com.example.domain.rules.ContractRules
import com.example.domain.trade.TradeManager
import com.example.models.Finance
import com.example.models.PlayerContract
import com.example.models.Season
import com.example.simulator.GameSimulator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class EconomySportsIntegrityTest {

    @Test
    fun contractEvaluationUsesDocumentedMinimumThreshold() {
        val player = NbaDataGenerator.getAllTeams().first().players.first()
        val manager = ContractManager()
        val fair = ContractRules.annualSalary(player)

        val acceptable = manager.evaluate(player, ContractOffer((fair * 0.90).toLong(), 3))
        val tooLow = manager.evaluate(player, ContractOffer((fair * 0.80).toLong(), 3))

        assertTrue("90% of fair value should be inside the documented 82% acceptance band", acceptable.accepted)
        assertFalse("80% of fair value must remain below the minimum", tooLow.accepted)
    }

    @Test
    fun freeAgentSigningChargesOnlyExplicitSigningBonusUpfront() {
        val teams = NbaDataGenerator.getAllTeams()
        val team = teams.first().copy(players = teams.first().players.take(10))
        val season = Season(teams, nextPlayerId = teams.flatMap { it.players }.maxOf { it.id } + 1)
        val player = RosterManager().generateFreeAgents(season, emptyList()).players.first()
        val finance = Finance(100_000_000)
        val expectedBonus = ContractRules.signingBonus(player).toInt()

        val result = RosterManager().signFreeAgent(team, finance, player, day = 1)

        requireNotNull(result)
        assertEquals(100_000_000 - expectedBonus, result.finance.budget)
        assertEquals(expectedBonus, result.finance.expenses.last().amount)
        assertTrue(result.finance.expenses.last().description.startsWith("Bônus de assinatura:"))
        assertTrue("signing bonus must be lower than the annual salary", expectedBonus < ContractRules.annualSalary(player))
    }

    @Test
    fun regularSeasonFinanceUsesContractPayrollWithoutHiddenTvCredit() {
        val team = NbaDataGenerator.getAllTeams().first()
        val opponent = NbaDataGenerator.getAllTeams()[1]
        val finance = Finance(100_000_000)
        val result = GameSimulator.GameResult(
            homeTeam = opponent,
            awayTeam = team,
            homeScore = 100,
            awayScore = 101,
            attendance = 0,
            homeStats = emptyMap(),
            awayStats = emptyMap(),
            injuries = emptyList(),
            narration = "economy-test"
        )

        val updated = FinanceManager().applyRegularSeasonGame(
            finance = finance,
            team = team,
            coach = null,
            result = result,
            isHome = false,
            day = 1,
            annualPlayerPayroll = 82_000_000L
        )

        assertEquals("82M annual payroll must charge exactly 1M for one of 82 games", 99_000_000, updated.budget)
        assertEquals(1_000_000, updated.expenses.single { it.description == "Salários dos Jogadores" }.amount)
        assertTrue("TV rights must not be credited inside a regular-season game", updated.expenses.none { it.description.contains("TV", ignoreCase = true) })
    }

    @Test
    fun incomingNoTradeClauseBlocksTrade() {
        val teams = NbaDataGenerator.getAllTeams()
        val managed = teams.first()
        val mine = managed.players.first()
        val opponent = teams.drop(1)
            .flatMap { team -> team.players.map { player -> team to player } }
            .minByOrNull { (_, player) -> abs(player.overall - mine.overall) }
            ?: error("No trade opponent")
        val incoming = opponent.second
        val season = Season(teams)
        val outgoingContract = PlayerContract(mine.id, managed.abbreviation, 5_000_000, 2, noTrade = false)
        val incomingContract = PlayerContract(incoming.id, opponent.first.abbreviation, 5_000_000, 2, noTrade = true)

        val trade = TradeManager().execute(
            season,
            managed,
            mine,
            incoming,
            outgoingContract,
            incomingContract
        )

        assertNull(trade)
    }
}
