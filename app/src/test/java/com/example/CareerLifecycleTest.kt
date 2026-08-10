package com.example

import com.example.data.NbaDataGenerator
import com.example.domain.contract.ContractManager
import com.example.domain.contract.ContractOffer
import com.example.domain.draft.DraftManager
import com.example.domain.roster.RosterManager
import com.example.domain.trade.TradeManager
import com.example.models.Season
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Functional domain flow covering the main offseason user journey without Android UI. */
class CareerLifecycleTest {
    @Test
    fun newCareerDraftFreeAgencyTradeAndSeasonAdvanceRemainConsistent() {
        val teams = NbaDataGenerator.getAllTeams()
        val next = teams.flatMap { it.players }.maxOf { it.id } + 1
        var season = Season(teams, nextPlayerId = next)
        season.userTeamName = teams.first().name

        val contractManager = ContractManager()
        var contracts = teams.flatMap { team ->
            team.players.map { player ->
                player.id to contractManager.create(player, team.abbreviation, contractManager.recommendedOffer(player))
            }
        }.toMap()

        val draftClass = DraftManager().generateClass(season, emptyList(), 2)
        assertEquals(6, draftClass.size)
        assertTrue(draftClass.all { it.id >= next })

        val managed = teams.first()
        val (draftedTeam, releasedName) = DraftManager().draft(managed, draftClass.first())
        assertNotNull(draftedTeam.players.firstOrNull { it.id == draftClass.first().id })
        assertTrue(releasedName != null || managed.players.size < 12)

        val freeAgents = RosterManager().generateFreeAgents(season, draftClass).players
        assertEquals(6, freeAgents.size)
        assertTrue(freeAgents.none { player -> draftClass.any { it.id == player.id } })

        val outgoing = managed.players.first()
        val offered = teams[1].players.first()
        val outgoingContract = contracts[outgoing.id]!!.copy(noTrade = true)
        contracts = contracts + (outgoing.id to outgoingContract)
        val blocked = TradeManager().execute(season, managed, outgoing, offered, outgoingContract)
        assertNull(blocked)

        val advancedContracts = contractManager.advanceSeason(contracts.values)
        assertFalse(advancedContracts.contracts.values.any { it.yearsRemaining <= 0 })
        season = season.advanceSeason()
        assertEquals(2, season.seasonNumber)
        assertTrue(season.nextPlayerId >= next)
    }
}
