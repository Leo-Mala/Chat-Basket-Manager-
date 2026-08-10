package com.example

import com.example.domain.contract.ContractManager
import com.example.domain.rules.ContractRules
import com.example.models.Player
import org.junit.Assert.*
import org.junit.Test

class ContractRulesTest {
    private fun player(overall: Int, age: Int) = Player(9000 + overall + age, "Test", "PG", overall, overall, overall, overall, overall, overall, age)

    @Test fun `market value increases with overall`() {
        assertTrue(ContractRules.marketValue(player(85, 25)) > ContractRules.marketValue(player(75, 25)))
    }

    @Test fun `roster limits are enforced`() {
        assertTrue(ContractRules.canSign(14))
        assertFalse(ContractRules.canSign(15))
        assertTrue(ContractRules.mustReleaseForStandardRoster(12))
    }

    @Test fun `recommended offer respects age`() {
        val young = ContractManager().recommendedOffer(player(80, 22))
        val veteran = ContractManager().recommendedOffer(player(80, 32))
        assertEquals(4, young.years)
        assertEquals(2, veteran.years)
    }
}
