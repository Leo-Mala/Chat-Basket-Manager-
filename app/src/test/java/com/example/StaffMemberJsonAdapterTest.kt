package com.example

import com.example.models.AssistantCoachStaff
import com.example.models.HeadCoachStaff
import com.example.models.ScoutStaff
import com.example.models.StaffMember
import com.example.models.StrengthCoach
import com.example.models.TeamDoctor
import com.example.utils.StaffMemberJsonAdapter
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StaffMemberJsonAdapterTest {
    private val type = object : TypeToken<List<StaffMember>>() {}.type
    private val gson = GsonBuilder()
        .registerTypeAdapter(StaffMember::class.java, StaffMemberJsonAdapter())
        .create()

    @Test
    fun legacyStaffMarketWithoutTypeDiscriminatorRestoresConcreteSubtypes() {
        val legacyJson = """
            [
              {"contractYears":3,"defensiveSkill":79,"experience":13,"gameManagement":89,"id":91915,"level":83,"motivationalSkill":86,"name":"Rick Finch","offensiveSkill":94,"playerDevelopment":66,"preferredStyle":"HALF_COURT","reputation":70,"salary":6667653,"specialty":"Tático & Liderança"},
              {"contractYears":2,"id":50765,"level":65,"name":"Nick Hardy","salary":1254082,"specialty":"Fundamentos de Arremesso"},
              {"contractYears":2,"id":69948,"level":78,"name":"Rick Hardy","salary":1073641,"specialty":"Condicionamento Físico"},
              {"contractYears":2,"id":24374,"level":79,"name":"Nick Spoelstra","salary":991600,"specialty":"Talentos da Europa"},
              {"contractYears":3,"id":88974,"level":73,"name":"Dr. Tyronn Popovich","salary":958530,"specialty":"Ortopedia Esportiva"}
            ]
        """.trimIndent()

        val restored: List<StaffMember> = gson.fromJson(legacyJson, type)

        assertEquals(5, restored.size)
        assertTrue(restored[0] is HeadCoachStaff)
        assertTrue(restored[1] is AssistantCoachStaff)
        assertTrue(restored[2] is StrengthCoach)
        assertTrue(restored[3] is ScoutStaff)
        assertTrue(restored[4] is TeamDoctor)
    }

    @Test
    fun newStaffMarketRoundTripWritesDiscriminatorAndRestoresTypes() {
        val staff: List<StaffMember> = listOf(
            AssistantCoachStaff(1, "Assist", 70, 800_000, 2, "Ataque Posicionado"),
            StrengthCoach(2, "Strength", 75, 700_000, 2, "Condicionamento Físico"),
            ScoutStaff(3, "Scout", 80, 650_000, 2, "Talentos da Europa"),
            TeamDoctor(4, "Dr. Test", 82, 900_000, 3, "Ortopedia Esportiva")
        )

        val json = gson.toJson(staff, type)
        val restored: List<StaffMember> = gson.fromJson(json, type)

        assertTrue(json.contains("_staffType"))
        assertTrue(restored[0] is AssistantCoachStaff)
        assertTrue(restored[1] is StrengthCoach)
        assertTrue(restored[2] is ScoutStaff)
        assertTrue(restored[3] is TeamDoctor)
    }
}
