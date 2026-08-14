package com.example.domain.season

import com.example.models.*

class SeasonManager {
    fun getMatchupsForDay(season: Season, day: Int): List<Pair<NbaTeam, NbaTeam>> {
        val n = season.teams.size
        if (n < 2 || n % 2 != 0 || day < 0) return emptyList()
        val rounds = n - 1
        val round = day % rounds
        val cycle = day / rounds
        val basePairs = baseRound(season.teams, round)

        return when (cycle) {
            0 -> basePairs
            1 -> basePairs.map { (home, away) -> away to home }
            else -> orientExtraRounds(season.teams, round, basePairs)
        }
    }

    private fun baseRound(teams: List<NbaTeam>, round: Int): List<Pair<NbaTeam, NbaTeam>> {
        val n = teams.size
        val active = mutableListOf<NbaTeam>().apply {
            add(teams[0])
            for (i in 1 until n) add(teams[(i + round - 1) % (n - 1) + 1])
        }
        return (0 until n / 2).map { active[it] to active[n - 1 - it] }
    }

    /**
     * NBA has 82 games with 30 teams. Two complete round-robin cycles account for
     * 58 games (29 home + 29 away for every team). The remaining 24 rounds form a
     * 24-regular undirected graph. Because every vertex has even degree, orienting
     * an Euler circuit gives every team exactly 12 home and 12 away games.
     */
    private fun orientExtraRounds(
        teams: List<NbaTeam>,
        round: Int,
        basePairs: List<Pair<NbaTeam, NbaTeam>>
    ): List<Pair<NbaTeam, NbaTeam>> {
        val extraRounds = minOf(24, teams.size - 1)
        if (round >= extraRounds) return basePairs

        data class Edge(val a: Int, val b: Int)
        val edges = mutableListOf<Edge>()
        val edgeIdByPair = mutableMapOf<Pair<Int, Int>, Int>()
        val adjacency = List(teams.size) { mutableListOf<Int>() }
        val indexByTeam = teams.withIndex().associate { it.value.abbreviation to it.index }

        repeat(extraRounds) { r ->
            baseRound(teams, r).forEach { (first, second) ->
                val a = indexByTeam.getValue(first.abbreviation)
                val b = indexByTeam.getValue(second.abbreviation)
                val edgeId = edges.size
                edges += Edge(a, b)
                adjacency[a] += edgeId
                adjacency[b] += edgeId
                edgeIdByPair[minOf(a, b) to maxOf(a, b)] = edgeId
            }
        }

        val used = BooleanArray(edges.size)
        val cursor = IntArray(teams.size)
        val orientation = arrayOfNulls<Pair<Int, Int>>(edges.size)

        for (start in teams.indices) {
            if (adjacency[start].none { !used[it] }) continue
            val vertexStack = mutableListOf(start)
            val edgeStack = mutableListOf<Int>()
            val circuitVertices = mutableListOf<Int>()
            val circuitEdges = mutableListOf<Int>()

            while (vertexStack.isNotEmpty()) {
                val vertex = vertexStack.last()
                while (cursor[vertex] < adjacency[vertex].size && used[adjacency[vertex][cursor[vertex]]]) {
                    cursor[vertex]++
                }
                if (cursor[vertex] >= adjacency[vertex].size) {
                    circuitVertices += vertex
                    vertexStack.removeAt(vertexStack.lastIndex)
                    if (edgeStack.isNotEmpty()) circuitEdges += edgeStack.removeAt(edgeStack.lastIndex)
                } else {
                    val edgeId = adjacency[vertex][cursor[vertex]]
                    used[edgeId] = true
                    val edge = edges[edgeId]
                    val next = if (edge.a == vertex) edge.b else edge.a
                    vertexStack += next
                    edgeStack += edgeId
                }
            }

            val vertices = circuitVertices.asReversed()
            val circuit = circuitEdges.asReversed()
            circuit.forEachIndexed { index, edgeId ->
                orientation[edgeId] = vertices[index] to vertices[index + 1]
            }
        }

        return basePairs.map { (first, second) ->
            val a = indexByTeam.getValue(first.abbreviation)
            val b = indexByTeam.getValue(second.abbreviation)
            val edgeId = edgeIdByPair.getValue(minOf(a, b) to maxOf(a, b))
            val (homeIndex, awayIndex) = requireNotNull(orientation[edgeId])
            teams[homeIndex] to teams[awayIndex]
        }
    }

    fun nextOpponent(season: Season, managedTeam: NbaTeam): Pair<NbaTeam, Boolean>? {
        val matchup = getMatchupsForDay(season, season.currentDay)
            .firstOrNull { it.first.name == managedTeam.name || it.second.name == managedTeam.name }
            ?: return null
        val home = matchup.first.name == managedTeam.name
        return (if (home) matchup.second else matchup.first) to home
    }

    fun advanceSeason(season: Season): Season = season.advanceSeason()
}
