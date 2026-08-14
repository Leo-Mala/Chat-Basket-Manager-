package com.example

import com.example.data.NbaDataGenerator
import com.example.domain.contract.ContractManager
import com.example.domain.rules.FreeAgencyRules
import com.example.domain.season.OffseasonManager
import com.example.domain.season.SeasonManager
import com.example.models.PlayerContract
import com.example.models.Season
import com.example.simulator.GameSimulator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max

/**
 * Quantitative 100-season ecosystem audit.
 *
 * This deliberately goes beyond "the career did not crash". It measures whether CPU franchises
 * keep playable rosters, whether talent remains reasonably distributed, whether the star population
 * stays bounded, how much roster turnover occurs, how large free agency becomes, and how frequently
 * the CPU trade/free-agency/draft systems actually act.
 */
class LeagueBalanceAuditTest {

    private data class Snapshot(
        val seasonNumber: Int,
        val leagueAverageOverall: Double,
        val teamAverageOverallSpread: Double,
        val averageAge: Double,
        val star90Share: Double,
        val star95Share: Double,
        val averageRosterSize: Double,
        val minRosterSize: Int,
        val maxRosterSize: Int,
        val freeAgentCount: Int,
        val averageTurnover: Double,
        val cpuTrades: Int,
        val cpuFreeAgentSignings: Int,
        val cpuDraftPicks: Int
    )

    @Test
    fun cpuLeagueRemainsQuantitativelyBalancedForOneHundredSeasons() {
        val seedTeams = NbaDataGenerator.getAllTeams()
        val userTeamName = seedTeams.first().name
        var season = Season(
            teams = seedTeams,
            seasonNumber = 1,
            nextPlayerId = seedTeams.asSequence().flatMap { it.players.asSequence() }.maxOf { it.id } + 1
        ).apply {
            this.userTeamName = userTeamName
        }

        val contractManager = ContractManager()
        val seasonManager = SeasonManager()
        val offseasonManager = OffseasonManager(contractManager)
        var contracts: Map<Int, PlayerContract> = seedTeams.flatMap { team ->
            team.players.map { player ->
                player.id to contractManager.create(
                    player,
                    team.abbreviation,
                    contractManager.recommendedOffer(player)
                )
            }
        }.toMap()
        var freeAgents = emptyList<com.example.models.Player>()
        var previousCpuRosters = cpuRosterIds(season, userTeamName)
        val snapshots = mutableListOf<Snapshot>()

        repeat(100) {
            simulateRegularSeason(season, seasonManager)

            val transition = offseasonManager.advance(season, contracts, freeAgents)
            season = transition.season
            contracts = transition.contracts
            freeAgents = transition.freeAgents

            val currentCpuRosters = cpuRosterIds(season, userTeamName)
            val turnover = currentCpuRosters.keys.map { teamName ->
                jaccardTurnover(previousCpuRosters[teamName].orEmpty(), currentCpuRosters[teamName].orEmpty())
            }.average()
            previousCpuRosters = currentCpuRosters

            val snapshot = snapshot(
                season = season,
                userTeamName = userTeamName,
                freeAgentCount = freeAgents.size,
                averageTurnover = turnover,
                activity = transition.activity
            )
            snapshots += snapshot

            if (snapshot.seasonNumber % 10 == 1 || snapshot.seasonNumber == 101) {
                println(
                    "BALANCE_AUDIT|season=${snapshot.seasonNumber}" +
                        "|avgOvr=${"%.2f".format(snapshot.leagueAverageOverall)}" +
                        "|teamSpread=${"%.2f".format(snapshot.teamAverageOverallSpread)}" +
                        "|avgAge=${"%.2f".format(snapshot.averageAge)}" +
                        "|star90=${"%.4f".format(snapshot.star90Share)}" +
                        "|star95=${"%.4f".format(snapshot.star95Share)}" +
                        "|avgRoster=${"%.2f".format(snapshot.averageRosterSize)}" +
                        "|minRoster=${snapshot.minRosterSize}" +
                        "|freeAgents=${snapshot.freeAgentCount}" +
                        "|turnover=${"%.4f".format(snapshot.averageTurnover)}" +
                        "|trades=${snapshot.cpuTrades}" +
                        "|faSignings=${snapshot.cpuFreeAgentSignings}" +
                        "|draftPicks=${snapshot.cpuDraftPicks}"
                )
            }
        }

        val averageOverall = snapshots.map { it.leagueAverageOverall }.average()
        val peakSpread = snapshots.maxOf { it.teamAverageOverallSpread }
        val averageAge = snapshots.map { it.averageAge }.average()
        val peakStar90Share = snapshots.maxOf { it.star90Share }
        val peakStar95Share = snapshots.maxOf { it.star95Share }
        val matureSnapshots = snapshots.filter { it.seasonNumber >= 11 }
        val matureAverageStar90Share = matureSnapshots.map { it.star90Share }.average()
        val maturePeakStar90Share = matureSnapshots.maxOf { it.star90Share }
        val averageRoster = snapshots.map { it.averageRosterSize }.average()
        val minimumRoster = snapshots.minOf { it.minRosterSize }
        val maximumRoster = snapshots.maxOf { it.maxRosterSize }
        val maximumFreeAgents = snapshots.maxOf { it.freeAgentCount }
        val averageTurnover = snapshots.map { it.averageTurnover }.average()
        val totalTrades = snapshots.sumOf { it.cpuTrades }
        val totalFreeAgentSignings = snapshots.sumOf { it.cpuFreeAgentSignings }
        val totalDraftPicks = snapshots.sumOf { it.cpuDraftPicks }
        val cpuTeamCount = seedTeams.count { it.name != userTeamName }

        println(
            "BALANCE_AUDIT_SUMMARY" +
                "|seasons=100" +
                "|avgOvr=${"%.2f".format(averageOverall)}" +
                "|peakTeamSpread=${"%.2f".format(peakSpread)}" +
                "|avgAge=${"%.2f".format(averageAge)}" +
                "|peakStar90=${"%.4f".format(peakStar90Share)}" +
                "|peakStar95=${"%.4f".format(peakStar95Share)}" +
                "|matureAvgStar90=${"%.4f".format(matureAverageStar90Share)}" +
                "|maturePeakStar90=${"%.4f".format(maturePeakStar90Share)}" +
                "|avgRoster=${"%.2f".format(averageRoster)}" +
                "|minRoster=$minimumRoster" +
                "|maxRoster=$maximumRoster" +
                "|maxFreeAgents=$maximumFreeAgents" +
                "|avgTurnover=${"%.4f".format(averageTurnover)}" +
                "|totalTrades=$totalTrades" +
                "|totalFaSignings=$totalFreeAgentSignings" +
                "|totalDraftPicks=$totalDraftPicks"
        )

        // Hard ecosystem guardrails. These are intentionally broad enough to permit natural
        // variation while still catching structural imbalance, roster collapse or star extinction.
        assertTrue("CPU franchise dropped below a playable five-man roster", minimumRoster >= 5)
        assertTrue("CPU franchise exceeded supported roster size", maximumRoster <= 18)
        assertTrue("average CPU roster size is structurally too small or too large", averageRoster in 7.0..15.0)
        assertTrue("league-wide average OVR escaped a plausible range", averageOverall in 60.0..92.0)
        assertTrue("talent distribution between CPU franchises became excessively uneven", peakSpread <= 18.0)
        assertTrue("CPU league became saturated with 90+ OVR players", peakStar90Share <= 0.35)
        assertTrue("CPU league became saturated with 95+ OVR players", peakStar95Share <= 0.15)
        assertTrue("90+ talent pipeline disappeared after the initial stars aged out", maturePeakStar90Share > 0.0)
        assertTrue("90+ talent pipeline is too sparse to sustain league stars", matureAverageStar90Share >= 0.002)
        assertTrue("free-agent market exceeded the configured cap", maximumFreeAgents <= FreeAgencyRules.MAX_MARKET_SIZE)
        assertTrue("annual CPU roster churn is implausibly low", averageTurnover >= 0.05)
        assertTrue("annual CPU roster churn is implausibly high", averageTurnover <= 0.40)
        assertTrue("CPU trade engine exceeded its three-trade offseason cap", totalTrades <= 300)
        assertTrue("CPU free-agency engine exceeded its one-signing-per-team cap", totalFreeAgentSignings <= cpuTeamCount * 100)
        assertEquals("every CPU franchise should receive exactly one draft pick per offseason", cpuTeamCount * 100, totalDraftPicks)
    }

    private fun simulateRegularSeason(season: Season, seasonManager: SeasonManager) {
        repeat(82) { day ->
            val matchups = seasonManager.getMatchupsForDay(season, day)
            assertEquals(15, matchups.size)
            matchups.forEachIndexed { matchupIndex, (home, away) ->
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
                        narration = "balance-audit"
                    )
                )
            }
            season.advanceDay()
        }
    }

    private fun snapshot(
        season: Season,
        userTeamName: String,
        freeAgentCount: Int,
        averageTurnover: Double,
        activity: OffseasonManager.Activity
    ): Snapshot {
        val cpuTeams = season.teams.filter { it.name != userTeamName }
        val cpuPlayers = cpuTeams.flatMap { it.players }
        require(cpuTeams.isNotEmpty())
        require(cpuPlayers.isNotEmpty())

        val teamAverages = cpuTeams.map { team ->
            if (team.players.isEmpty()) 0.0 else team.players.map { it.overall }.average()
        }
        val spread = (teamAverages.maxOrNull() ?: 0.0) - (teamAverages.minOrNull() ?: 0.0)
        val rosterSizes = cpuTeams.map { it.players.size }
        val playerCount = max(1, cpuPlayers.size)

        return Snapshot(
            seasonNumber = season.seasonNumber,
            leagueAverageOverall = cpuPlayers.map { it.overall }.average(),
            teamAverageOverallSpread = spread,
            averageAge = cpuPlayers.map { it.age }.average(),
            star90Share = cpuPlayers.count { it.overall >= 90 }.toDouble() / playerCount,
            star95Share = cpuPlayers.count { it.overall >= 95 }.toDouble() / playerCount,
            averageRosterSize = rosterSizes.average(),
            minRosterSize = rosterSizes.minOrNull() ?: 0,
            maxRosterSize = rosterSizes.maxOrNull() ?: 0,
            freeAgentCount = freeAgentCount,
            averageTurnover = averageTurnover,
            cpuTrades = activity.cpuTrades,
            cpuFreeAgentSignings = activity.cpuFreeAgentSignings,
            cpuDraftPicks = activity.cpuDraftPicks
        )
    }

    private fun cpuRosterIds(season: Season, userTeamName: String): Map<String, Set<Int>> =
        season.teams
            .filter { it.name != userTeamName }
            .associate { team -> team.name to team.players.map { it.id }.toSet() }

    private fun jaccardTurnover(previous: Set<Int>, current: Set<Int>): Double {
        val union = previous union current
        if (union.isEmpty()) return 0.0
        val retained = previous intersect current
        return 1.0 - retained.size.toDouble() / union.size.toDouble()
    }
}
