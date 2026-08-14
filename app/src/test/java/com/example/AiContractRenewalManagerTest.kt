package com.example

import com.example.data.NbaDataGenerator
import com.example.domain.contract.AiContractRenewalManager
import com.example.domain.contract.ContractManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiContractRenewalManagerTest {

    @Test
    fun cpuCorePlayerIsRenewedButUserExpirationRemainsManual() {
        val teams = NbaDataGenerator.getAllTeams()
        val user = teams[0]
        val cpu = teams[1]
        val userStar = user.players.maxByOrNull { it.overall }!!
        val cpuStar = cpu.players
            .filter { it.age <= 33 }
            .maxByOrNull { it.overall }!!

        val result = AiContractRenewalManager(ContractManager()).renewExpiring(
            teams = teams,
            continuingContracts = emptyMap(),
            expiredPlayerIds = setOf(userStar.id, cpuStar.id),
            userTeamName = user.name
        )

        assertTrue(result.renewedPlayerIds.contains(cpuStar.id))
        assertTrue(result.contracts.containsKey(cpuStar.id))
        assertFalse(result.renewedPlayerIds.contains(userStar.id))
        assertTrue(result.unrenewedExpiredPlayerIds.contains(userStar.id))
        assertFalse(result.contracts.containsKey(userStar.id))
    }

    @Test
    fun cpuDoesNotAutomaticallyRenewEveryExpiringPlayer() {
        val teams = NbaDataGenerator.getAllTeams()
        val cpu = teams[1]
        val expiring = cpu.players
            .filter { it.age <= 33 }
            .sortedByDescending { it.overall }
            .take(6)
        val expiringIds = expiring.map { it.id }.toSet()

        val result = AiContractRenewalManager().renewExpiring(
            teams = teams,
            continuingContracts = emptyMap(),
            expiredPlayerIds = expiringIds,
            userTeamName = teams[0].name,
            maxRenewalsPerTeam = 3
        )

        assertTrue(result.renewedPlayerIds.size <= 3)
        assertTrue(result.unrenewedExpiredPlayerIds.isNotEmpty())
        assertTrue(result.renewedPlayerIds.intersect(result.unrenewedExpiredPlayerIds).isEmpty())
    }
}
