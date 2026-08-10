package com.example.models

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import java.io.Serializable

@Immutable
@Stable
data class Facility(
    val type: FacilityType,
    val name: String,
    val level: Int = 1, // 1 - 10
    val maxLevel: Int = 10,
    val baseUpgradeCost: Int = 1_500_000
) : Serializable {
    val currentUpgradeCost: Int
        get() = baseUpgradeCost * level * level

    val bonusPercent: Int
        get() = level * 5 // e.g. level 5 = 25% bonus
}

enum class FacilityType(val label: String, val description: String) {
    ARENA("Arena Principal", "Aumenta capacidade de público e receita de bilheteria."),
    TRAINING_FACILITY("Centro de Treinamento", "Acelera a evolução de atributos e química do time."),
    MEDICAL_CENTER("Centro Médico", "Reduz risco de lesões e acelera recuperação de atletas."),
    SCOUTING_DEPT("Depto. de Scouting", "Melhora scouting do Draft e informações dos adversários.")
}

@Immutable
@Stable
data class TeamFacilities(
    val arena: Facility = Facility(FacilityType.ARENA, "Arena Oficial", level = 1, baseUpgradeCost = 3_000_000),
    val training: Facility = Facility(FacilityType.TRAINING_FACILITY, "Academia de Alta Performance", level = 1, baseUpgradeCost = 1_500_000),
    val medical: Facility = Facility(FacilityType.MEDICAL_CENTER, "Centro Clínico de Fisioterapia", level = 1, baseUpgradeCost = 1_200_000),
    val scouting: Facility = Facility(FacilityType.SCOUTING_DEPT, "Rede de Observadores", level = 1, baseUpgradeCost = 1_000_000)
) : Serializable {
    fun getTotalMaintenanceCost(): Int {
        return (arena.level * 200_000) + (training.level * 100_000) + (medical.level * 80_000) + (scouting.level * 70_000)
    }
}

