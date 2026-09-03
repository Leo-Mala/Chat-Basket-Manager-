package com.example.models

import android.content.Context
import com.example.simulator.GameSimulator
import com.example.simulator.SimulationConfig
import com.example.domain.rules.PlayerGenerationRules
import com.example.domain.rules.SeasonRules
import java.io.Serializable

class Season(
    var teams: List<NbaTeam>,
    var currentDay: Int = 0,
    var gamesPlayed: Int = 0,
    var seasonNumber: Int = 1,
    var currentMonth: Int = 10,
    var currentYear: Int = 2025,
    /** Monotonic allocator for player identities. Never derive new IDs from active rosters. */
    var nextPlayerId: Int = 1
) : Serializable {

    var userTeamName: String? = null

    val standings = mutableMapOf<String, SeasonRecord>()
    val history = mutableListOf<GameSimulator.GameResult>()

    init {
        teams.forEach { standings[it.name] = SeasonRecord() }
    }

    fun addResult(result: GameSimulator.GameResult) = synchronized(this) {
        val utn = userTeamName
        if (utn == null || result.homeTeam.name == utn || result.awayTeam.name == utn) {
            history.add(result)
        }
        gamesPlayed++
        updateRecord(result.homeTeam.name, result.homeScore, result.awayScore)
        updateRecord(result.awayTeam.name, result.awayScore, result.homeScore)
    }

    private fun updateRecord(teamName: String, scored: Int, conceded: Int) {
        val record = standings[teamName] ?: return
        record.gamesPlayed++
        record.totalPointsScored += scored
        record.totalPointsConceded += conceded
        if (scored > conceded) record.wins++
        else record.losses++
    }

    fun getStandings(conference: String? = null): List<Pair<String, SeasonRecord>> {
        val filtered = if (conference == null) standings.entries
        else standings.entries.filter { teams.find { team -> team.name == it.key }?.conference == conference }
        return filtered.sortedWith(
            compareByDescending<Map.Entry<String, SeasonRecord>> { it.value.wins }
                .thenByDescending { it.value.winRate }
                .thenByDescending { it.value.pointDifference }
        ).map { it.key to it.value }
    }

    fun getPlayoffTeams(): Pair<List<NbaTeam>, List<NbaTeam>> {
        val east = getStandings("East").take(8).map { teams.find { team -> team.name == it.first }!! }
        val west = getStandings("West").take(8).map { teams.find { team -> team.name == it.first }!! }
        return east to west
    }

    fun simulatePlayoffs(context: Context, simulationConfig: SimulationConfig = SimulationConfig()): PlayoffResult {
        val (eastTeams, westTeams) = getPlayoffTeams()
        val (eastChampion, eastSeries) = simulateConference(eastTeams, "Leste", context, simulationConfig)
        val (westChampion, westSeries) = simulateConference(westTeams, "Oeste", context, simulationConfig)
        val finalsResult = simulateFinals(eastChampion, westChampion, context, simulationConfig)
        return PlayoffResult(
            eastChampion = eastChampion,
            westChampion = westChampion,
            nbaChampion = finalsResult.winner,
            mvp = finalsResult.mvp,
            seriesResults = eastSeries + westSeries + finalsResult
        )
    }

    private fun simulateConference(teams: List<NbaTeam>, conferenceName: String, context: Context, simulationConfig: SimulationConfig): Pair<NbaTeam, List<SeriesResult>> {
        val quarterPairs = listOf(
            teams[0] to teams[7],
            teams[1] to teams[6],
            teams[2] to teams[5],
            teams[3] to teams[4]
        )
        val quarterResults = quarterPairs.map { (t1, t2) ->
            simulateSeries(t1, t2, "Quartas $conferenceName", context, simulationConfig = simulationConfig)
        }
        val semiResults = listOf(
            simulateSeries(quarterResults[0].winner, quarterResults[1].winner, "Semifinal $conferenceName", context, simulationConfig = simulationConfig),
            simulateSeries(quarterResults[2].winner, quarterResults[3].winner, "Semifinal $conferenceName", context, simulationConfig = simulationConfig)
        )
        val conferenceFinal = simulateSeries(semiResults[0].winner, semiResults[1].winner, "Final $conferenceName", context, simulationConfig = simulationConfig)
        return conferenceFinal.winner to (quarterResults + semiResults + conferenceFinal)
    }

    private fun simulateFinals(east: NbaTeam, west: NbaTeam, context: Context, simulationConfig: SimulationConfig): SeriesResult {
        val eastRecord = standings[east.name]!!
        val westRecord = standings[west.name]!!
        val homeTeam = if (eastRecord.wins > westRecord.wins) east else west
        val awayTeam = if (homeTeam == east) west else east
        return simulateSeries(homeTeam, awayTeam, "Finais da NBA", context, isFinals = true, simulationConfig = simulationConfig)
    }

    private fun recordPostseasonResult(result: GameSimulator.GameResult) = synchronized(this) {
        val utn = userTeamName
        if (utn == null || result.homeTeam.name == utn || result.awayTeam.name == utn) {
            history.add(result)
        }
    }

    fun simulateSeries(
        team1: NbaTeam,
        team2: NbaTeam,
        roundName: String,
        context: Context,
        isFinals: Boolean = false,
        simulationConfig: SimulationConfig = SimulationConfig()
    ): SeriesResult {
        var wins1 = 0
        var wins2 = 0
        var gameCount = 0
        val games = mutableListOf<GameSimulator.GameResult>()
        val simulator = GameSimulator(context.applicationContext, simulationConfig)
        try {
            while (wins1 < SeasonRules.PLAYOFF_SERIES_WINS && wins2 < SeasonRules.PLAYOFF_SERIES_WINS) {
                gameCount++
                val homeTeam = if (SeasonRules.homeTeamIsHigherSeedGame(gameCount)) team1 else team2
                val awayTeam = if (homeTeam == team1) team2 else team1
                val result = simulator.simulate(homeTeam, awayTeam)
                games.add(result)
                recordPostseasonResult(result)

                val homeScore = if (homeTeam == team1) result.homeScore else result.awayScore
                val awayScore = if (homeTeam == team1) result.awayScore else result.homeScore
                if (homeScore > awayScore) {
                    if (homeTeam == team1) wins1++ else wins2++
                } else {
                    if (homeTeam == team1) wins2++ else wins1++
                }
            }
        } finally {
            simulator.release()
        }
        val champion = if (wins1 > wins2) team1 else team2
        val mvp = if (isFinals) {
            val stats = games.flatMap { it.homeStats.entries + it.awayStats.entries }
            val playerPoints = stats.groupBy { it.key }.mapValues { it.value.sumOf { s -> s.value.points } }
            playerPoints.maxByOrNull { it.value }?.key
        } else null

        return SeriesResult(
            winner = champion,
            games = games,
            roundName = roundName,
            mvp = mvp,
            team1 = team1,
            team2 = team2,
            team1Wins = wins1,
            team2Wins = wins2
        )
    }

    fun advanceDay() {
        currentDay++
        if (currentDay % 12 == 0) {
            currentMonth++
            if (currentMonth > 12) {
                currentMonth = 1
                currentYear++
            }
        }
        teams.forEach { team ->
            team.players.forEach { it.advanceDay() }
        }
    }

    fun allocatePlayerIds(count: Int): IntRange {
        require(count >= 0) { "count must be non-negative" }
        if (count == 0) return IntRange.EMPTY
        val start = nextPlayerId.coerceAtLeast(1)
        val requiredCapacity = Math.addExact(count, PlayerGenerationRules.FREE_AGENT_BATCH_SIZE)
        Math.addExact(start, requiredCapacity)
        nextPlayerId = Math.addExact(start, count)
        return start until nextPlayerId
    }

    fun advanceSeason(): Season {
        val newTeams = teams.map { team ->
            // Existing roster players age/develop first. Retirement is evaluated after aging,
            // so an age-38 player cannot survive as an active 39-year-old for another season.
            val keptPlayers = team.players
                .onEach { player ->
                    player.advanceSeason()
                    player.injured = false
                    player.injuryDays = 0
                    player.resetSeasonStats()
                }
                .filter { it.age <= SeasonRules.MAX_PLAYER_AGE }
                .toMutableList()

            // Replenishment happens only after development/retirement. New rookies therefore
            // enter the next season at their generated age and rating, without a free offseason boost.
            val needed = (12 - keptPlayers.size).coerceAtLeast(0)
            if (needed > 0) {
                val rookieIds = allocatePlayerIds(needed)
                keptPlayers.addAll(generateRookies(needed, rookieIds.first))
            }
            team.copy(players = keptPlayers)
        }
        val next = Season(
            teams = newTeams,
            currentDay = 0,
            gamesPlayed = 0,
            seasonNumber = this.seasonNumber + 1,
            currentMonth = 10,
            currentYear = 2025 + this.seasonNumber,
            nextPlayerId = this.nextPlayerId
        )
        next.userTeamName = this.userTeamName
        return next
    }

    private fun generateRookies(count: Int, startingId: Int): List<Player> {
        val firstNames = listOf("Jayden", "Aiden", "Ethan", "Liam", "Noah", "Oliver", "Elijah", "James", "Benjamin", "Lucas")
        val lastNames = listOf("Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez")
        val positions = listOf("PG", "SG", "SF", "PF", "C")
        return List(count) { index ->
            val id = startingId + index
            val random = kotlin.random.Random(id)
            PlayerGenerationRules.createBalancedPlayer(
                id = id,
                name = "${firstNames[random.nextInt(firstNames.size)]} ${lastNames[random.nextInt(lastNames.size)]}",
                position = positions[random.nextInt(positions.size)],
                targetOverall = random.nextInt(62, 80),
                age = 19 + random.nextInt(0, 3),
                random = random
            )
        }
    }

    data class SeriesResult(
        val winner: NbaTeam,
        val games: List<GameSimulator.GameResult>,
        val roundName: String,
        val mvp: Player? = null,
        val team1: NbaTeam? = null,
        val team2: NbaTeam? = null,
        val team1Wins: Int = 0,
        val team2Wins: Int = 0
    ) : Serializable

    data class PlayoffResult(
        val eastChampion: NbaTeam,
        val westChampion: NbaTeam,
        val nbaChampion: NbaTeam,
        val mvp: Player?,
        val seriesResults: List<SeriesResult>
    ) : Serializable

    data class SeasonRecord(
        var wins: Int = 0,
        var losses: Int = 0,
        var gamesPlayed: Int = 0,
        var totalPointsScored: Int = 0,
        var totalPointsConceded: Int = 0
    ) : Serializable {
        val winRate: Float get() = if (gamesPlayed == 0) 0f else wins.toFloat() / gamesPlayed
        val pointDifference: Int get() = totalPointsScored - totalPointsConceded
    }
}
