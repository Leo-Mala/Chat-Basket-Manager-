package com.example.models

import java.io.Serializable

data class AssistantCoachNotification(
    val id: String = java.util.UUID.randomUUID().toString(),
    val gameDay: Int,
    val seasonNumber: Int,
    val opponentName: String,
    val isWin: Boolean,
    val userScore: Int,
    val opponentScore: Int,
    val timestamp: Long = System.currentTimeMillis(),
    var isRead: Boolean = false,
    val coachName: String,
    val coachRole: String, // "Tático & Ataque", "Especialista Defensivo", "Técnico de Arremessos", "Desenvolvimento de Atletas"
    val summary: String,
    val keyStrengths: List<String>,
    val areasToImprove: List<String>,
    val playerHighlights: List<String>,
    val tacticalAdvice: String,
    val recommendedBonusType: String? = null, // "ATTACK_BOOST", "DEFENSE_BOOST", "XP_BOOST", "MOTIVATION_BOOST"
    val recommendedBonusLabel: String? = null,
    var isBonusApplied: Boolean = false
) : Serializable
