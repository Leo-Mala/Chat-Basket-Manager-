package com.example

import com.example.data.StaffAndFacilitiesGenerator
import com.example.models.StaffMember
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

    @Test
    fun generatedInitialStaffAlwaysUsesUniqueIds() {
        repeat(1_000) {
            val staff = StaffAndFacilitiesGenerator.generateInitialStaff("Integrity Test Team")
            val members = buildList<StaffMember> {
                staff.headCoach?.let(::add)
                addAll(staff.assistants)
                staff.strengthCoach?.let(::add)
                staff.scout?.let(::add)
                staff.teamDoctor?.let(::add)
                addAll(staff.executives)
            }
            assertEquals(members.size, members.map { member -> member.id }.toSet().size)
        }
    }
}
