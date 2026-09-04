package com.example

import com.example.data.NbaDataGenerator
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NbaRosterSeptember2026Test {

    @Test
    fun ownerSuppliedSeptember2026RosterIsLoadedExactly() {
        val teams = NbaDataGenerator.getAllTeams()

        assertEquals(30, teams.size)
        assertEquals(15, teams.count { it.conference == "East" })
        assertEquals(15, teams.count { it.conference == "West" })
        assertTrue(teams.all { it.players.size == 18 })

        val players = teams.flatMap { it.players }
        assertEquals(540, players.size)
        assertEquals(540, players.map { it.id }.toSet().size)

        val canonical = teams.flatMap { team ->
            team.players.map { player ->
                listOf(
                    team.name,
                    team.conference,
                    player.name,
                    player.position,
                    player.age.toString(),
                    player.overall.toString(),
                    player.shooting.toString(),
                    player.defense.toString(),
                    player.rebound.toString(),
                    player.passing.toString(),
                    player.athleticism.toString()
                ).joinToString("|")
            }
        }.joinToString("\n")

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }

        assertEquals(NbaDataGenerator.ROSTER_CANONICAL_SHA256, digest)
        assertEquals("599317bc0c2254eb313c1d29594f5ee2a5139e0eeef5ce41da20975b521e0ded", digest)
    }

    @Test
    fun seedOverallIsNotRecalculatedAwayFromOwnerSuppliedValue() {
        val teams = NbaDataGenerator.getAllTeams()
        val hawks = teams.single { it.name == "Atlanta Hawks" }
        val trae = hawks.players.single { it.name == "Trae Young" }

        assertEquals(93, trae.overall)
        assertEquals(91, trae.shooting)
        assertEquals(45, trae.defense)
        assertEquals(35, trae.rebound)
        assertEquals(95, trae.passing)
        assertEquals(85, trae.athleticism)
        assertEquals(26, trae.age)
    }
}
