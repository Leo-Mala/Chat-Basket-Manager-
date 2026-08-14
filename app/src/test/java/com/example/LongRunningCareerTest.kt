package com.example

import com.example.data.NbaDataGenerator
import com.example.domain.contract.ContractManager
import com.example.domain.season.OffseasonManager
import com.example.domain.season.SeasonManager
import com.example.models.Player
import com.example.models.PlayerContract
import com.example.models.Season
import com.example.simulator.GameSimulator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Long-career endurance coverage.
 *
 * Unlike the original structural-only test, every regular-season matchup is now fed through
 * Season.addResult(), every game day advances the season clock, playoff qualification is checked,
 * and the complete offseason/contract transition is repeated for decades of career state.
 */
class LongRunningCareerTest {

    @Test
    fun careerRemainsValidForFiftyFullyScheduledSeasons() {
        runCareerStress(seasonsToSimulate = 50)
    }

    @Test
    fun careerRemainsValidForOneHundredFullyScheduledSeasons() {
        runCareerStress(seasonsToSimulate = 100)
    }

    private fun runCareerStress(seasonsToSimulate: Int) {
        val seedTeams = NbaDataGenerator.getAllTeams()
        var season = Season(
            teams = seedTeams,
            seasonNumber = 1,
            nextPlayerId = seedTeams.asSequence().flatMap { it.players.asSequence() }.maxOf { it.id } + 1
        )
        season.userTeamName = seedTeams.first().name

        val contractManager = ContractManager()
        val offseasonManager = OffseasonManager(contractManager)
        val seasonManager = SeasonManager()
        var contracts: Map<Int, PlayerContract> = seedTeams.flatMap { team ->
            team.players.map { player ->
                player.id to contractManager.create(
                    player,
                    team.abbreviation,
                    contractManager.recommendedOffer(player)
                )
            }
        }.toMap()
        var freeAgents = emptyList<Player>()

        repeat(seasonsToSimulate) {
            assertCareerIdentityAndContracts(season, contracts, freeAgents)

            val appearances = mutableMapOf<String, Int>()
            val homes = mutableMapOf<String, Int>()

            repeat(82) { day ->
                val matchups = seasonManager.getMatchupsForDay(season, day)
                assertEquals("15 matchups on day $day", 15, matchups.size)

                matchups.forEachIndexed { matchupIndex, (home, away) ->
                    appearances[home.abbreviation] = appearances.getOrDefault(home.abbreviation, 0) + 1
                    appearances[away.abbreviation] = appearances.getOrDefault(away.abbreviation, 0) + 1
                    homes[home.abbreviation] = homes.getOrDefault(home.abbreviation, 0) + 1

                    val homeWins = (season.seasonNumber + day + matchupIndex) % 2 == 0
                    season.addResult(
                        GameSimulator.GameResult(
                            homeTeam = home,
                            awayTeam = away,
                            homeScore = if (homeWins) 110 else 100,
                            awayScore = if (homeWins) 100 else 110,
                            attendance = 18_000,
                            homeStats = emptyMap(),
                            awayStats = emptyMap(),
                            injuries = emptyList(),
                            narration = "stress-test"
                        )
                    )
                }
                season.advanceDay()
            }

            assertEquals("82 days must be completed", 82, season.currentDay)
            assertEquals("30 teams x 82 games / 2", 1_230, season.gamesPlayed)
            assertEquals("user-team history keeps its 82 regular-season games", 82, season.history.size)

            season.teams.forEach { team ->
                assertEquals("82 scheduled games for ${team.abbreviation}", 82, appearances[team.abbreviation])
                assertEquals("41 home games for ${team.abbreviation}", 41, homes[team.abbreviation])
                val record = requireNotNull(season.standings[team.name])
                assertEquals("standings games for ${team.abbreviation}", 82, record.gamesPlayed)
                assertEquals("wins + losses for ${team.abbreviation}", 82, record.wins + record.losses)
            }

            val (eastPlayoffTeams, westPlayoffTeams) = season.getPlayoffTeams()
            assertEquals(8, eastPlayoffTeams.size)
            assertEquals(8, westPlayoffTeams.size)
            assertEquals(
                "playoff field must contain 16 distinct teams",
                16,
                (eastPlayoffTeams + westPlayoffTeams).map { it.abbreviation }.distinct().size
            )

            val transition = offseasonManager.advance(season, contracts, freeAgents)
            season = transition.season
            contracts = transition.contracts
            freeAgents = transition.freeAgents

            assertEquals("next season must reset currentDay", 0, season.currentDay)
            assertEquals("next season must reset gamesPlayed", 0, season.gamesPlayed)
            assertTrue("next season standings must start clean", season.standings.values.all { it.gamesPlayed == 0 })
        }

        assertEquals(seasonsToSimulate + 1, season.seasonNumber)
        assertCareerIdentityAndContracts(season, contracts, freeAgents)
    }

    private fun assertCareerIdentityAndContracts(
        season: Season,
        contracts: Map<Int, PlayerContract>,
        freeAgents: List<Player>
    ) {
        val allPlayers = season.teams.flatMap { it.players }
        val activeIds = allPlayers.map { it.id }
        val freeAgentIds = freeAgents.map { it.id }

        assertEquals("duplicate active player IDs", activeIds.size, activeIds.distinct().size)
        assertEquals("duplicate free-agent IDs", freeAgentIds.size, freeAgentIds.distinct().size)
        assertTrue("active and free-agent pools must not overlap", activeIds.toSet().intersect(freeAgentIds.toSet()).isEmpty())
        assertTrue("nextPlayerId must stay ahead of active IDs", season.nextPlayerId > (activeIds.maxOrNull() ?: 0))
        assertTrue("nextPlayerId must stay ahead of free-agent IDs", season.nextPlayerId > (freeAgentIds.maxOrNull() ?: 0))
        assertTrue("each team must have a valid roster", season.teams.all { it.players.size in 0..18 })
        assertTrue("every roster player must have a contract", allPlayers.all { contracts.containsKey(it.id) })
        assertTrue("expired contracts must not remain active", contracts.values.none { it.yearsRemaining <= 0 })
    }
}
