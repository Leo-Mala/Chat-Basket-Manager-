package com.example

import com.example.domain.draft.AiDraftManager
import com.example.domain.rules.FreeAgencyRules
import com.example.domain.season.OffseasonManager
import com.example.models.Arena
import com.example.models.NbaTeam
import com.example.models.Player
import com.example.models.Season
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiDraftManagerTest {

    @Test
    fun worstCpuTeamPicksBestRookieFirstAndUserTeamIsUntouched() {
        val user = team("User", "USR", 1, 80)
        val bad = team("Bad", "BAD", 100, 70)
        val good = team("Good", "GOD", 200, 76)
        val userIds = user.players.map { it.id }
        val badRelease = FreeAgencyRules.releaseCandidate(bad.players)!!
        val goodRelease = FreeAgencyRules.releaseCandidate(good.players)!!
        val best = player(9_001, "PG", 90, 19)
        val second = player(9_002, "C", 85, 19)

        val result = AiDraftManager().draftForCpu(
            teams = listOf(user, bad, good),
            rookies = listOf(second, best),
            userTeamName = user.name,
            priorityTeamNames = listOf(bad.name, good.name, user.name)
        )

        assertEquals(userIds, result.teams.first { it.name == user.name }.players.map { it.id })
        assertEquals(2, result.picks.size)
        assertEquals(bad.name, result.picks[0].teamName)
        assertEquals(best.id, result.picks[0].rookieId)
        assertEquals(badRelease.id, result.picks[0].releasedPlayerId)
        assertEquals(good.name, result.picks[1].teamName)
        assertEquals(second.id, result.picks[1].rookieId)
        assertEquals(goodRelease.id, result.picks[1].releasedPlayerId)
        assertTrue(result.undraftedRookies.isEmpty())
        assertEquals(setOf(badRelease.id, goodRelease.id), result.releasedPlayers.map { it.id }.toSet())
    }

    @Test
    fun eachCpuGetsAtMostOneUniqueRookieAndExtrasRemainUndrafted() {
        val user = team("User", "USR", 1, 80)
        val cpu1 = team("CPU1", "C01", 100, 74)
        val cpu2 = team("CPU2", "C02", 200, 75)
        val rookies = listOf(
            player(9_101, "PG", 84, 19),
            player(9_102, "SG", 82, 19),
            player(9_103, "C", 80, 19)
        )

        val result = AiDraftManager().draftForCpu(
            teams = listOf(user, cpu1, cpu2),
            rookies = rookies,
            userTeamName = user.name,
            priorityTeamNames = listOf(cpu1.name, cpu2.name)
        )

        assertEquals(2, result.picks.size)
        assertEquals(2, result.picks.map { it.rookieId }.distinct().size)
        assertEquals(1, result.undraftedRookies.size)
        assertFalse(result.picks.any { it.teamName == user.name })
    }

    @Test
    fun offseasonDraftsOneAge19RookiePerCpuAndCreatesContracts() {
        val user = team("User", "USR", 1, 80)
        val bad = team("Bad", "BAD", 100, 72)
        val good = team("Good", "GOD", 200, 78)
        val firstGeneratedId = 1_000
        val season = Season(
            teams = listOf(user, bad, good),
            nextPlayerId = firstGeneratedId
        ).apply {
            userTeamName = user.name
            standings.getValue(user.name).wins = 40
            standings.getValue(bad.name).wins = 10
            standings.getValue(good.name).wins = 50
        }
        val originalCpuIds = (bad.players + good.players).map { it.id }.toSet()

        val result = OffseasonManager().advance(
            currentSeason = season,
            currentContracts = emptyMap(),
            currentFreeAgents = emptyList()
        )

        val nextUser = result.season.teams.first { it.name == user.name }
        val nextBad = result.season.teams.first { it.name == bad.name }
        val nextGood = result.season.teams.first { it.name == good.name }
        val userNew = nextUser.players.filter { it.id >= firstGeneratedId }
        val badNew = nextBad.players.filter { it.id >= firstGeneratedId }
        val goodNew = nextGood.players.filter { it.id >= firstGeneratedId }

        assertTrue(userNew.isEmpty())
        assertEquals(1, badNew.size)
        assertEquals(1, goodNew.size)
        assertEquals(19, badNew.single().age)
        assertEquals(19, goodNew.single().age)
        assertEquals(firstGeneratedId + 2, result.season.nextPlayerId)

        val badContract = result.contracts[badNew.single().id]
        val goodContract = result.contracts[goodNew.single().id]
        assertNotNull(badContract)
        assertNotNull(goodContract)
        assertEquals(bad.abbreviation, badContract!!.teamId)
        assertEquals(good.abbreviation, goodContract!!.teamId)

        val activeIds = result.season.teams.flatMap { it.players }.map { it.id }.toSet()
        val releasedCpuIds = originalCpuIds - activeIds
        assertEquals(2, releasedCpuIds.size)
        assertTrue(releasedCpuIds.all { id -> result.freeAgents.any { it.id == id } })
        assertTrue(activeIds.size == result.season.teams.sumOf { it.players.size })
    }

    private fun team(name: String, abbreviation: String, startId: Int, baseOverall: Int): NbaTeam =
        NbaTeam(
            name = name,
            city = name,
            abbreviation = abbreviation,
            conference = "East",
            arena = Arena("$name Arena", name, 20_000, 2000),
            players = List(12) { index ->
                player(
                    id = startId + index,
                    position = listOf("PG", "SG", "SF", "PF", "C")[index % 5],
                    overall = baseOverall + (index % 3),
                    age = 25
                )
            }
        )

    private fun player(id: Int, position: String, overall: Int, age: Int): Player =
        Player(
            id = id,
            name = "P-$id",
            position = position,
            overall = overall,
            shooting = overall,
            defense = overall,
            rebound = overall,
            passing = overall,
            athleticism = overall,
            age = age
        )
}
