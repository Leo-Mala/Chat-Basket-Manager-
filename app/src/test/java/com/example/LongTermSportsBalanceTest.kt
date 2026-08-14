package com.example

import com.example.data.NbaDataGenerator
import com.example.domain.contract.ContractManager
import com.example.domain.draft.DraftManager
import com.example.domain.roster.RosterManager
import com.example.domain.rules.FreeAgencyRules
import com.example.domain.rules.SeasonRules
import com.example.domain.season.OffseasonManager
import com.example.models.Player
import com.example.models.PlayerContract
import com.example.models.Season
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LongTermSportsBalanceTest {

    @Test
    fun ageThirtyEightPlayerRetiresBeforeNextSeasonRosterIsFinalized() {
        val teams = NbaDataGenerator.getAllTeams()
        val retiring = teams.first().players.first()
        retiring.age = SeasonRules.MAX_PLAYER_AGE
        val retiringId = retiring.id
        val season = Season(teams, nextPlayerId = nextId(teams))

        val advanced = season.advanceSeason()

        assertFalse(advanced.teams.flatMap { it.players }.any { it.id == retiringId })
        assertTrue(advanced.teams.flatMap { it.players }.all { it.age <= SeasonRules.MAX_PLAYER_AGE })
        assertTrue(advanced.teams.all { it.players.size >= 12 })
    }

    @Test
    fun freeAgentsAgeRetireAndMarketCannotGrowWithoutBound() {
        val teams = NbaDataGenerator.getAllTeams()
        val season = Season(teams, nextPlayerId = nextId(teams))
        val retiringId = 100_000
        val market = List(250) { index ->
            testPlayer(
                id = retiringId + index,
                overall = 70 + (index % 10),
                age = if (index == 0) SeasonRules.MAX_PLAYER_AGE else 20 + (index % 10)
            )
        }

        val result = OffseasonManager().advance(season, emptyMap(), market)

        assertTrue(result.freeAgents.size <= FreeAgencyRules.MAX_MARKET_SIZE)
        assertFalse(result.freeAgents.any { it.id == retiringId })
        assertTrue(result.freeAgents.all { it.age <= SeasonRules.MAX_PLAYER_AGE })
    }

    @Test
    fun scoutingLevelDoesNotManufactureStrongerDraftClasses() {
        val teamsA = NbaDataGenerator.getAllTeams()
        val teamsB = NbaDataGenerator.getAllTeams()
        val seasonA = Season(teamsA, nextPlayerId = nextId(teamsA))
        val seasonB = Season(teamsB, nextPlayerId = nextId(teamsB))
        val manager = DraftManager()

        val lowScouting = manager.generateClass(seasonA, emptyList(), scoutingLevel = 1, size = 20)
        val maxScouting = manager.generateClass(seasonB, emptyList(), scoutingLevel = 5, size = 20)

        assertEquals(lowScouting.map { it.overall }, maxScouting.map { it.overall })
        assertEquals(lowScouting.map { it.id }, maxScouting.map { it.id })
    }

    @Test
    fun generatedDraftAndFreeAgentOverallMatchesTheirAttributes() {
        val teams = NbaDataGenerator.getAllTeams()
        val season = Season(teams, nextPlayerId = nextId(teams))
        val draft = DraftManager().generateClass(season, emptyList(), scoutingLevel = 3, size = 12)
        val freeAgents = RosterManager().generateFreeAgents(season, draft).players

        (draft + freeAgents).forEach { player ->
            assertEquals(player.calculateOverall(), player.overall)
        }
    }

    @Test
    fun automaticDevelopmentIsDeterministicAndDoesNotTurnNormalProspectIntoNinetyNine() {
        val first = testPlayer(200_001, overall = 75, age = 19)
        val second = testPlayer(200_001, overall = 75, age = 19)

        repeat(6) {
            playFullSeason(first)
            playFullSeason(second)
            first.advanceSeason()
            second.advanceSeason()
            first.resetSeasonStats()
            second.resetSeasonStats()
        }

        assertEquals(first.overall, second.overall)
        assertEquals(first.shooting, second.shooting)
        assertEquals(first.defense, second.defense)
        assertEquals(first.rebound, second.rebound)
        assertEquals(first.passing, second.passing)
        assertEquals(first.athleticism, second.athleticism)
        assertTrue("Normal prospect inflated to ${first.overall}", first.overall <= 90)
    }

    @Test
    fun centuryOfOffseasonsKeepsTalentAndFreeAgentPoolBounded() {
        val seedTeams = NbaDataGenerator.getAllTeams()
        var season = Season(seedTeams, nextPlayerId = nextId(seedTeams))
        val contractManager = ContractManager()
        val offseasonManager = OffseasonManager(contractManager)
        var contracts: Map<Int, PlayerContract> = seedTeams.flatMap { team ->
            team.players.map { player ->
                player.id to contractManager.create(player, team.abbreviation, contractManager.recommendedOffer(player))
            }
        }.toMap()
        var freeAgents = emptyList<Player>()

        repeat(100) {
            val transition = offseasonManager.advance(season, contracts, freeAgents)
            season = transition.season
            contracts = transition.contracts
            freeAgents = transition.freeAgents

            val active = season.teams.flatMap { team -> team.players }
            val averageOverall = active.map { it.overall }.average()
            val eliteShare = active.count { it.overall >= 90 }.toDouble() / active.size.coerceAtLeast(1)

            assertTrue("Free-agent market grew to ${freeAgents.size}", freeAgents.size <= FreeAgencyRules.MAX_MARKET_SIZE)
            assertTrue("Active player older than retirement limit", active.all { it.age <= SeasonRules.MAX_PLAYER_AGE })
            assertTrue("League average OVR inflated to $averageOverall", averageOverall <= 88.0)
            assertTrue("Elite-player share inflated to $eliteShare", eliteShare <= 0.30)
            assertTrue("Roster fell below playable size", season.teams.all { it.players.size >= 12 })
        }
    }

    private fun playFullSeason(player: Player) {
        repeat(82) {
            player.seasonGames++
            player.seasonPoints += 16
            player.evolveInSeason(16)
        }
    }

    private fun testPlayer(id: Int, overall: Int, age: Int): Player =
        Player(id, "Balance-$id", "PG", overall, overall, overall, overall, overall, overall, age)

    private fun nextId(teams: List<com.example.models.NbaTeam>): Int =
        teams.flatMap { it.players }.maxOf { it.id } + 1
}
