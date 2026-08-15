package com.example.simulator

import kotlin.random.Random

/** Pure injury-rate and recovery rules shared by simulation and tests. */
object InjuryRules {
    const val CPU_INJURY_PER_THOUSAND = 8
    const val MIN_MEDICAL_LEVEL = 1
    const val MAX_MEDICAL_LEVEL = 5

    fun probabilityPerThousand(isManagedPlayer: Boolean, medicalStaffLevel: Int): Int {
        if (!isManagedPlayer) return CPU_INJURY_PER_THOUSAND
        val level = medicalStaffLevel.coerceIn(MIN_MEDICAL_LEVEL, MAX_MEDICAL_LEVEL)
        return (CPU_INJURY_PER_THOUSAND - (level - 1)).coerceAtLeast(3)
    }

    fun shouldInjure(random: Random, probabilityPerThousand: Int): Boolean {
        require(probabilityPerThousand in 0..1000)
        return random.nextInt(1000) < probabilityPerThousand
    }

    fun daysOut(random: Random, isManagedPlayer: Boolean, medicalStaffLevel: Int): Int {
        val raw = random.nextInt(2, 9)
        if (!isManagedPlayer) return raw
        val level = medicalStaffLevel.coerceIn(MIN_MEDICAL_LEVEL, MAX_MEDICAL_LEVEL)
        return (raw - (level - 1)).coerceAtLeast(1)
    }
}
