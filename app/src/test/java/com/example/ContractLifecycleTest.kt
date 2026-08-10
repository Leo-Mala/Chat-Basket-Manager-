package com.example

import com.example.data.NbaDataGenerator
import com.example.domain.contract.ContractManager
import com.example.domain.contract.ContractOffer
import com.example.models.PlayerContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContractLifecycleTest {
    private val manager = ContractManager()

    @Test
    fun negotiatedTermsSurviveSeasonAdvanceUntilExpiration() {
        val player = NbaDataGenerator.getAllTeams().first().players.first()
        val contract = manager.create(player, "T1", ContractOffer(9_000_000, 2, playerOption = true, noTrade = true))

        val first = manager.advanceSeason(listOf(contract))
        assertEquals(1, first.contracts[player.id]?.yearsRemaining)
        assertTrue(first.expiredPlayerIds.isEmpty())
        assertTrue(first.contracts[player.id]?.playerOption == true)
        assertTrue(first.contracts[player.id]?.noTrade == true)

        val second = manager.advanceSeason(first.contracts.values)
        assertTrue(second.contracts.isEmpty())
        assertEquals(setOf(player.id), second.expiredPlayerIds)
    }

    @Test
    fun transferOnlyChangesTeamOwnership() {
        val contract = PlayerContract(10, "OLD", 5_000_000, 3, playerOption = true, noTrade = false)
        val transferred = manager.transfer(contract, "NEW")
        assertEquals("NEW", transferred?.teamId)
        assertEquals(contract.salary, transferred?.salary)
        assertEquals(contract.yearsRemaining, transferred?.yearsRemaining)
        assertEquals(contract.playerOption, transferred?.playerOption)
    }
}
