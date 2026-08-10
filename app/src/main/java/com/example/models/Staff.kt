package com.example.models

import java.io.Serializable

sealed class StaffMember : Serializable {
    abstract val id: Int
    abstract val name: String
    abstract val level: Int // 0-100
    abstract val salary: Int
    abstract val contractYears: Int
    abstract val specialty: String
}

data class HeadCoachStaff(
    override val id: Int,
    override val name: String,
    override val level: Int,
    override val salary: Int,
    override val contractYears: Int,
    override val specialty: String = "Tático & Liderança",
    val offensiveSkill: Int,
    val defensiveSkill: Int,
    val motivationalSkill: Int,
    val experience: Int, // anos
    val reputation: Int, // 0-100
    val preferredStyle: PlayStyle,
    val playerDevelopment: Int, // 0-100
    val gameManagement: Int // 0-100
) : StaffMember()

data class AssistantCoachStaff(
    override val id: Int,
    override val name: String,
    override val level: Int,
    override val salary: Int,
    override val contractYears: Int,
    override val specialty: String // Offense, Defense, Fitness, Scouting, Youth
) : StaffMember()

data class StrengthCoach(
    override val id: Int,
    override val name: String,
    override val level: Int,
    override val salary: Int,
    override val contractYears: Int,
    override val specialty: String // Strength, Speed, Endurance, Flexibility
) : StaffMember()

data class ScoutStaff(
    override val id: Int,
    override val name: String,
    override val level: Int,
    override val salary: Int,
    override val contractYears: Int,
    override val specialty: String // High School, College, International, G-League
) : StaffMember()

data class TeamDoctor(
    override val id: Int,
    override val name: String,
    override val level: Int,
    override val salary: Int,
    override val contractYears: Int,
    override val specialty: String // Prevenção, Reabilitação, Tratamento
) : StaffMember()

data class ExecutiveStaff(
    override val id: Int,
    override val name: String,
    override val level: Int,
    override val salary: Int,
    override val contractYears: Int,
    override val specialty: String, // General Manager, Operations, Financial, Marketing
    val roleTitle: String
) : StaffMember()

data class TeamStaff(
    var headCoach: HeadCoachStaff? = null,
    val assistants: MutableList<AssistantCoachStaff> = mutableListOf(),
    var strengthCoach: StrengthCoach? = null,
    var scout: ScoutStaff? = null,
    var teamDoctor: TeamDoctor? = null,
    val executives: MutableList<ExecutiveStaff> = mutableListOf()
) : Serializable {
    fun getTotalStaffSalaries(): Int {
        var total = (headCoach?.salary ?: 0) + (strengthCoach?.salary ?: 0) + (scout?.salary ?: 0) + (teamDoctor?.salary ?: 0)
        total += assistants.sumOf { it.salary }
        total += executives.sumOf { it.salary }
        return total
    }
}
