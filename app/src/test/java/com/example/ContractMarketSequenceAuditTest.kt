package com.example

import com.example.data.NbaDataGenerator
import com.example.domain.contract.ContractManager
import com.example.domain.contract.ContractOffer
import com.example.domain.season.OffseasonManager
import com.example.models.PlayerContract
import com.example.models.Season
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContractMarketSequenceAuditTest {
    private val contractManager = ContractManager()

    private fun initialSeason(): Season {
        val teams = NbaDataGenerator.getAllTeams()
        return Season(
            teams = teams,
            seasonNumber = 1,
            nextPlayerId = teams.flatMap { it.players }.maxOf { it.id } + 1
        ).apply { userTeamName = teams.first().name }
    }

    private fun initialContracts(season: Season): Map<Int, PlayerContract> =
        season.teams.flatMap { team ->
            team.players.map { player ->
                player.id to contractManager.create(
                    player,
                    team.abbreviation,
                    contractManager.recommendedOffer(player)
                )
            }
        }.toMap()

    @Test
    fun repeatedOffseasonsKeepContractsExactlySynchronizedWithActiveRosters() {
        var season = initialSeason()
        var contracts = initialContracts(season)
        var freeAgents = emptyList<com.example.models.Player>()
        val offseason = OffseasonManager(contractManager = contractManager)

        repeat(8) {
            val result = offseason.advance(season, contracts, freeAgents)
            season = result.season
            contracts = result.contracts
            freeAgents = result.freeAgents

            val activePlayers = season.teams.flatMap { it.players }
            val activeIds = activePlayers.map { it.id }
            assertEquals("active player IDs duplicated after market activity", activeIds.size, activeIds.toSet().size)
            assertEquals("contract table must exactly match active roster IDs", activeIds.toSet(), contracts.keys)

            season.teams.forEach { team ->
                team.players.forEach { player ->
                    val contract = contracts[player.id] ?: error("missing contract for ${player.id}")
                    assertEquals("contract ownership drifted after trade/free agency/draft", team.abbreviation, contract.teamId)
                    assertTrue("invalid remaining term", contract.yearsRemaining in 1..5)
                    assertTrue("invalid active salary", contract.salary > 0L)
                }
            }
            assertTrue("free-agent market duplicated IDs", freeAgents.map { it.id }.let { it.size == it.toSet().size })
            assertTrue("free agent cannot retain an active contract", freeAgents.none { it.id in contracts })
        }
    }

    @Test
    fun userStarExpirationRenewsInsteadOfSilentlyDeletingManagedRoster() {
        val season = initialSeason()
        val userTeam = season.teams.first()
        val expiring = userTeam.players.filter { it.age < com.example.domain.rules.SeasonRules.MAX_PLAYER_AGE }
            .maxByOrNull { it.overall } ?: userTeam.players.first()
        val contracts = initialContracts(season).toMutableMap()
        contracts[expiring.id] = contractManager.create(
            expiring,
            userTeam.abbreviation,
            ContractOffer(expiring.calculateSalary().toLong(), years = 1, noTrade = true)
        )

        val result = OffseasonManager(contractManager = contractManager).advance(season, contracts, emptyList())
        val managedAfter = result.season.teams.first { it.name == userTeam.name }
        val renewed = result.contracts[expiring.id]

        assertTrue("expired managed-team player must stay on roster until a manual renewal UI exists", managedAfter.players.any { it.id == expiring.id })
        assertTrue("managed-team renewal must create a replacement contract", renewed != null && renewed.yearsRemaining in 1..5)
        assertFalse("renewed managed-team player cannot also appear in free agency", result.freeAgents.any { it.id == expiring.id })
    }

    @Test
    fun cpuDraftRookiesReceiveFullRookieContractAfterTheTransition() {
        val season = initialSeason()
        val existingIds = season.teams.flatMap { it.players }.map { it.id }.toSet()
        val result = OffseasonManager(contractManager = contractManager).advance(
            season,
            initialContracts(season),
            emptyList()
        )

        val rookies = result.season.teams
            .filter { it.name != result.season.userTeamName }
            .flatMap { it.players }
            .filter { it.id !in existingIds }

        assertTrue("CPU draft should introduce rookies", rookies.isNotEmpty())
        rookies.forEach { rookie ->
            val owner = result.season.teams.first { team -> team.players.any { it.id == rookie.id } }
            val contract = result.contracts[rookie.id] ?: error("drafted rookie missing contract")
            assertEquals(19, rookie.age)
            assertEquals(4, contract.yearsRemaining)
            assertEquals(owner.abbreviation, contract.teamId)
        }
    }

    @Test
    fun noTradeTermsSurviveASeasonAndTransferPreservesAllOtherTerms() {
        val player = NbaDataGenerator.getAllTeams().first().players.first()
        val original = contractManager.create(
            player,
            "OLD",
            ContractOffer(9_500_000L, years = 3, playerOption = true, noTrade = true)
        )
        val afterSeason = contractManager.advanceSeason(listOf(original)).contracts.getValue(player.id)
        assertTrue(afterSeason.noTrade)
        assertTrue(afterSeason.playerOption)
        assertEquals(2, afterSeason.yearsRemaining)

        val transferred = contractManager.transfer(afterSeason, "NEW")!!
        assertEquals("NEW", transferred.teamId)
        assertEquals(afterSeason.salary, transferred.salary)
        assertEquals(afterSeason.yearsRemaining, transferred.yearsRemaining)
        assertEquals(afterSeason.playerOption, transferred.playerOption)
        assertEquals(afterSeason.noTrade, transferred.noTrade)
    }
}
