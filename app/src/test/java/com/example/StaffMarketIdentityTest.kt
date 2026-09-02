package com.example

import com.example.data.StaffAndFacilitiesGenerator
import org.junit.Assert.assertEquals
import org.junit.Test

class StaffMarketIdentityTest {
    @Test
    fun generatedStaffMarketAlwaysUsesUniqueIds() {
        repeat(1_000) {
            val market = StaffAndFacilitiesGenerator.generateAvailableStaffMarket()
            assertEquals(market.size, market.map { member -> member.id }.toSet().size)
        }
    }
}
