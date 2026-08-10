package com.example

import com.example.data.NbaDataGenerator
import com.example.domain.draft.DraftManager
import com.example.domain.roster.RosterManager
import com.example.models.Season
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OffseasonIdentityTest {
    @Test
    fun draftAndFreeAgencyConsumeDisjointGlobalIds() {
        val teams = NbaDataGenerator.getAllTeams()
        val next = teams.flatMap { it.players }.maxOf { it.id } + 1
        val season = Season(teams, nextPlayerId = next)
        val draft = DraftManager().generateClass(season, emptyList(), scoutingLevel = 2)
        val freeAgents = RosterManager().generateFreeAgents(season, draft).players

        val ids = draft.map { it.id } + freeAgents.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
        assertTrue(ids.all { it >= next })
        assertEquals(next + ids.size, season.nextPlayerId)
    }

    @Test
    fun draftClassIsDeterministicFromAllocatedIds() {
        val teams = NbaDataGenerator.getAllTeams()
        val next = teams.flatMap { it.players }.maxOf { it.id } + 1
        val seasonA = Season(teams, nextPlayerId = next)
        val seasonB = Season(NbaDataGenerator.getAllTeams(), nextPlayerId = next)
        val a = DraftManager().generateClass(seasonA, emptyList(), 2)
        val b = DraftManager().generateClass(seasonB, emptyList(), 2)
        assertEquals(a, b)
    }
}
