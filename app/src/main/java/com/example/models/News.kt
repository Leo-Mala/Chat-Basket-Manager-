package com.example.models

import java.io.Serializable

enum class NewsType(val label: String, val emoji: String) {
    INJURY("Lesão", "🏥"),
    TRADE_RUMOR("Especulação de Troca", "🔄"),
    RECORD("Recorde Histórico", "🔥"),
    MEDIA_REACTION("Imprensa & Mídia", "📰"),
    GAME_RESULT("Resultado do Jogo", "🏀"),
    STANDINGS_UPDATE("Classificação", "📊"),
    CONTRACT_SIGNING("Contratação / Staff", "✍️")
}

data class News(
    val id: Long = System.currentTimeMillis() + (1..10000).random(),
    val title: String,
    val content: String,
    val dateString: String,
    val type: NewsType,
    val relatedPlayerName: String? = null,
    val relatedTeamName: String? = null,
    var isRead: Boolean = false
) : Serializable
