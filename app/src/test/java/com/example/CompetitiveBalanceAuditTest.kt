package com.example

import com.example.data.NbaDataGenerator
import com.example.domain.contract.ContractManager
import com.example.domain.season.OffseasonManager
import com.example.domain.season.SeasonManager
import com.example.models.NbaTeam
import com.example.models.PlayerContract
import com.example.models.Season
import com.example.models.Tactics
import com.example.simulator.GameSimulator
import com.example.simulator.SimulationRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt
import kotlin.random.Random

class CompetitiveBalanceAuditTest {

    @Test
    fun leagueRemainsCompetitivelyHealthyAcrossOneHundredSeasons() {
        val seedTeams = NbaDataGenerator.getAllTeams()
        var season = Season(
            teams = seedTeams,
            nextPlayerId = seedTeams.flatMap { it.players }.maxOf { it.id } + 1
        )
        val contractManager = ContractManager()
        val offseasonManager = OffseasonManager(contractManager)
        val seasonManager = SeasonManager()
        var contracts: Map<Int, PlayerContract> = seedTeams.flatMap { team ->
            team.players.map { player ->
                player.id to contractManager.create(player, team.abbreviation, contractManager.recommendedOffer(player))
            }
        }.toMap()
        var freeAgents = emptyList<com.example.models.Player>()

        val titles = linkedMapOf<String, Int>()
        val playoffAppearances = seedTeams.associate { it.name to 0 }.toMutableMap()
        val conferenceTitles = mutableMapOf("East" to 0, "West" to 0)
        val leagueWinRateStdDev = mutableListOf<Double>()
        var maxConsecutiveTitles = 0
        var currentStreak = 0
        var previousChampion: String? = null
        var bestSeasonWins = 0
        var worstSeasonWins = 82

        repeat(100) { seasonIndex ->
            simulateRegularSeason(season, seasonManager)

            val allRecords = season.teams.map { team -> season.standings.getValue(team.name) }
            bestSeasonWins = maxOf(bestSeasonWins, allRecords.maxOf { it.wins })
            worstSeasonWins = minOf(worstSeasonWins, allRecords.minOf { it.wins })
            leagueWinRateStdDev += stdDev(allRecords.map { it.winRate.toDouble() })

            val east = season.getStandings("East").take(8).map { (name, _) -> season.teams.first { it.name == name } }
            val west = season.getStandings("West").take(8).map { (name, _) -> season.teams.first { it.name == name } }
            (east + west).forEach { playoffAppearances[it.name] = playoffAppearances.getValue(it.name) + 1 }

            val eastChampion = simulateConference(east, season.seasonNumber, 11)
            val westChampion = simulateConference(west, season.seasonNumber, 29)
            val champion = simulateSeries(eastChampion, westChampion, season.seasonNumber * 997 + 71)
            titles[champion.name] = titles.getOrDefault(champion.name, 0) + 1
            conferenceTitles[champion.conference] = conferenceTitles.getValue(champion.conference) + 1

            if (previousChampion == champion.name) {
                currentStreak++
            } else {
                previousChampion = champion.name
                currentStreak = 1
            }
            maxConsecutiveTitles = maxOf(maxConsecutiveTitles, currentStreak)

            val transition = offseasonManager.advance(season, contracts, freeAgents)
            season = transition.season
            contracts = transition.contracts
            freeAgents = transition.freeAgents

            if ((seasonIndex + 1) % 10 == 0) {
                println(
                    "COMPETITIVE_AUDIT|season=${seasonIndex + 1}" +
                        "|distinctChampions=${titles.size}" +
                        "|topTitles=${titles.values.maxOrNull() ?: 0}" +
                        "|maxStreak=$maxConsecutiveTitles" +
                        "|bestWins=$bestSeasonWins" +
                        "|worstWins=$worstSeasonWins"
                )
            }
        }

        val distinctChampions = titles.size
        val mostTitles = titles.values.maxOrNull() ?: 0
        val fewestPlayoffs = playoffAppearances.values.minOrNull() ?: 0
        val mostPlayoffs = playoffAppearances.values.maxOrNull() ?: 0
        val playoffSpread = mostPlayoffs - fewestPlayoffs
        val eastTitles = conferenceTitles.getValue("East")
        val westTitles = conferenceTitles.getValue("West")
        val avgWinRateStdDev = leagueWinRateStdDev.average()

        println(
            "COMPETITIVE_AUDIT_SUMMARY" +
                "|seasons=100" +
                "|distinctChampions=$distinctChampions" +
                "|mostTitles=$mostTitles" +
                "|maxConsecutiveTitles=$maxConsecutiveTitles" +
                "|fewestPlayoffs=$fewestPlayoffs" +
                "|mostPlayoffs=$mostPlayoffs" +
                "|playoffSpread=$playoffSpread" +
                "|eastTitles=$eastTitles" +
                "|westTitles=$westTitles" +
                "|bestSeasonWins=$bestSeasonWins" +
                "|worstSeasonWins=$worstSeasonWins" +
                "|avgWinRateStdDev=${"%.4f".format(avgWinRateStdDev)}"
        )

        assertTrue("too few distinct champions: $distinctChampions", distinctChampions >= 8)
        assertTrue("one franchise won too many titles: $mostTitles", mostTitles <= 24)
        assertTrue("dynasty streak is excessive: $maxConsecutiveTitles", maxConsecutiveTitles <= 5)
        assertTrue("a franchise was permanently excluded from playoffs: $fewestPlayoffs", fewestPlayoffs >= 4)
        assertTrue("playoff opportunity spread is excessive: $playoffSpread", playoffSpread <= 70)
        assertTrue("conference title distribution is excessively one-sided: East=$eastTitles West=$westTitles", eastTitles in 25..75)
        assertEquals(100, eastTitles + westTitles)
        assertTrue("best regular-season record is implausibly dominant: $bestSeasonWins", bestSeasonWins <= 76)
        assertTrue("worst regular-season record is implausibly weak: $worstSeasonWins", worstSeasonWins >= 4)
        assertTrue("regular-season parity collapsed: $avgWinRateStdDev", avgWinRateStdDev in 0.03..0.22)
    }

    private fun simulateRegularSeason(season: Season, manager: SeasonManager) {
        repeat(82) { day ->
            val matchups = manager.getMatchupsForDay(season, day)
            assertEquals(15, matchups.size)
            matchups.forEachIndexed { index, (home, away) ->
                val (homeScore, awayScore) = score(home, away, seed = season.seasonNumber * 1_000_003 + day * 101 + index)
                season.addResult(
                    GameSimulator.GameResult(
                        homeTeam = home,
                        awayTeam = away,
                        homeScore = homeScore,
                        awayScore = awayScore,
                        attendance = 18_000,
                        homeStats = emptyMap(),
                        awayStats = emptyMap(),
                        injuries = emptyList(),
                        narration = "competitive-audit"
                    )
                )
            }
            season.advanceDay()
        }
    }

    private fun simulateConference(seeds: List<NbaTeam>, seasonNumber: Int, salt: Int): NbaTeam {
        require(seeds.size == 8)
        val round1 = listOf(
            simulateSeries(seeds[0], seeds[7], seasonNumber * 10_000 + salt + 1),
            simulateSeries(seeds[1], seeds[6], seasonNumber * 10_000 + salt + 2),
            simulateSeries(seeds[2], seeds[5], seasonNumber * 10_000 + salt + 3),
            simulateSeries(seeds[3], seeds[4], seasonNumber * 10_000 + salt + 4)
        )
        val semis = listOf(
            simulateSeries(round1[0], round1[3], seasonNumber * 10_000 + salt + 5),
            simulateSeries(round1[1], round1[2], seasonNumber * 10_000 + salt + 6)
        )
        return simulateSeries(semis[0], semis[1], seasonNumber * 10_000 + salt + 7)
    }

    private fun simulateSeries(higherSeed: NbaTeam, lowerSeed: NbaTeam, seed: Int): NbaTeam {
        var higherWins = 0
        var lowerWins = 0
        var game = 0
        while (higherWins < 4 && lowerWins < 4) {
            game++
            val higherHome = game in setOf(1, 2, 5, 7)
            val home = if (higherHome) higherSeed else lowerSeed
            val away = if (higherHome) lowerSeed else higherSeed
            val (homeScore, awayScore) = score(home, away, seed * 31 + game)
            if (homeScore > awayScore) {
                if (home === higherSeed) higherWins++ else lowerWins++
            } else {
                if (away === higherSeed) higherWins++ else lowerWins++
            }
        }
        return if (higherWins > lowerWins) higherSeed else lowerSeed
    }

    private fun score(home: NbaTeam, away: NbaTeam, seed: Int): Pair<Int, Int> {
        val rng = Random(seed)
        val tactics = Tactics()
        val homeProfile = SimulationRules.profile(home, tactics, null, home = true)
        val awayProfile = SimulationRules.profile(away, tactics, null, home = false)
        var homeScore = SimulationRules.expectedScore(homeProfile, awayProfile, rng)
        var awayScore = SimulationRules.expectedScore(awayProfile, homeProfile, rng)
        if (homeScore == awayScore) {
            if (rng.nextBoolean()) homeScore += 2 else awayScore += 2
        }
        return homeScore to awayScore
    }

    private fun stdDev(values: List<Double>): Double {
        val mean = values.average()
        return sqrt(values.sumOf { (it - mean) * (it - mean) } / values.size)
    }
}
