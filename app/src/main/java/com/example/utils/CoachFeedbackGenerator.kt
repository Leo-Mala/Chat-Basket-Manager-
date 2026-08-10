package com.example.utils

import com.example.models.AssistantCoachNotification
import com.example.models.NbaTeam
import com.example.models.Player
import com.example.simulator.GameSimulator

object CoachFeedbackGenerator {

    fun generatePostMatchFeedback(
        gameResult: GameSimulator.GameResult,
        managedTeam: NbaTeam,
        currentDay: Int,
        seasonNumber: Int
    ): List<AssistantCoachNotification> {
        val isHome = (gameResult.homeTeam.name == managedTeam.name)
        val userScore = if (isHome) gameResult.homeScore else gameResult.awayScore
        val oppScore = if (isHome) gameResult.awayScore else gameResult.homeScore
        val opponentName = if (isHome) gameResult.awayTeam.name else gameResult.homeTeam.name
        val isWin = userScore > oppScore
        val userStatsMap = if (isHome) gameResult.homeStats else gameResult.awayStats

        val notifications = mutableListOf<AssistantCoachNotification>()

        // Find top user performers from the match
        val topScorerEntry = userStatsMap.maxByOrNull { it.value.points }
        val topScorer = topScorerEntry?.key
        val topScorerPts = topScorerEntry?.value?.points ?: 0

        val topPasserEntry = userStatsMap.maxByOrNull { it.value.assists }
        val topPasser = topPasserEntry?.key
        val topPasserAssists = topPasserEntry?.value?.assists ?: 0

        val topRebounderEntry = userStatsMap.maxByOrNull { it.value.rebounds }
        val topRebounder = topRebounderEntry?.key
        val topRebounderRebounds = topRebounderEntry?.value?.rebounds ?: 0

        val totalAssists = userStatsMap.values.sumOf { it.assists }
        val totalTurnovers = userStatsMap.values.sumOf { it.turnovers }

        // 1. Tático & Ataque (Tactical Assistant Coach)
        val tacticalStrengths = mutableListOf<String>()
        val tacticalWeaknesses = mutableListOf<String>()
        val tacticalHighlights = mutableListOf<String>()

        if (userScore >= 115) {
            tacticalStrengths.add("Ataque de alta rotação: time superou os 115 pontos com excelente ritmo.")
        } else {
            tacticalStrengths.add("Execução consistente de jogadas ensaiadas no meio de quadra.")
        }

        if (totalAssists >= 22) {
            tacticalStrengths.add("Circulação de bola rápida gerou $totalAssists assistências no total.")
        } else {
            tacticalWeaknesses.add("Ataque individualizado em excesso: apenas $totalAssists assistências da equipe.")
        }

        if (totalTurnovers >= 14) {
            tacticalWeaknesses.add("Atenção aos turnovers: $totalTurnovers perdas de bola custaram posses valiosas.")
        } else {
            tacticalStrengths.add("Ótimo controle de bola: apenas $totalTurnovers erros no jogo inteiro.")
        }

        if (topScorer != null && topScorerPts >= 25) {
            tacticalHighlights.add("${topScorer.name} liderou a pontuação com $topScorerPts pontos decisivos.")
        }
        if (topPasser != null && topPasserAssists >= 8) {
            tacticalHighlights.add("${topPasser.name} orquestrou o ataque com $topPasserAssists assistências brilhantes.")
        }

        val tacticalSummary = if (isWin) {
            "Vitória por $userScore x $oppScore contra $opponentName! O plano tático ofensivo produziu ótimos arremessos nos momentos chave."
        } else {
            "Derrota por $userScore x $oppScore contra $opponentName. Precisamos reajustar nossa movimentação de bola e evitar desperdícios de posse."
        }

        notifications.add(
            AssistantCoachNotification(
                gameDay = currentDay,
                seasonNumber = seasonNumber,
                opponentName = opponentName,
                isWin = isWin,
                userScore = userScore,
                opponentScore = oppScore,
                coachName = "Coach Dave Miller",
                coachRole = "Tático & Ataque",
                summary = tacticalSummary,
                keyStrengths = tacticalStrengths,
                areasToImprove = tacticalWeaknesses,
                playerHighlights = tacticalHighlights,
                tacticalAdvice = "Recomendo focar nos treinos de movimentação sem bola para maximizar o espaçamento no próximo confronto.",
                recommendedBonusType = "ATTACK_BOOST",
                recommendedBonusLabel = "⚡ +4% Bônus de Eficiência Ofensiva Tática"
            )
        )

        // 2. Especialista Defensivo (Defensive Coach)
        val defStrengths = mutableListOf<String>()
        val defWeaknesses = mutableListOf<String>()
        val defHighlights = mutableListOf<String>()

        if (oppScore <= 100) {
            defStrengths.add("Defesa sufocante: limitou $opponentName a apenas $oppScore pontos!")
        } else {
            defWeaknesses.add("Concedemos $oppScore pontos: a transição defensiva ficou abaixo do padrão.")
        }

        if (topRebounder != null) {
            defStrengths.add("${topRebounder.name} dominou a tábua de rebotes com $topRebounderRebounds rebotes.")
            defHighlights.add("Garrafão protegido: ${topRebounder.name} evitou segundas chances do adversário.")
        }

        val totalStealsAndBlocks = userStatsMap.values.sumOf { it.steals + it.blocks }
        if (totalStealsAndBlocks >= 10) {
            defStrengths.add("Pressão sobre a bola: $totalStealsAndBlocks roubos/tocos combinados.")
        } else {
            defWeaknesses.add("Pouca agressividade na linha de passe: apenas $totalStealsAndBlocks roubos/tocos.")
        }

        val defSummary = if (oppScore <= 102) {
            "Excelente atuação defensiva! Fechamos o garrafão e pressionamos o perímetro do $opponentName."
        } else {
            "O adversário encontrou muitos caminhos para a cesta ($oppScore pts). Precisamos reforçar a comunicação defensiva."
        }

        notifications.add(
            AssistantCoachNotification(
                gameDay = currentDay,
                seasonNumber = seasonNumber,
                opponentName = opponentName,
                isWin = isWin,
                userScore = userScore,
                opponentScore = oppScore,
                coachName = "Coach Marcus Vance",
                coachRole = "Especialista Defensivo",
                summary = defSummary,
                keyStrengths = defStrengths,
                areasToImprove = defWeaknesses,
                playerHighlights = defHighlights,
                tacticalAdvice = "Trabalhar dobra na marcação e contestação de arremessos no perímetro para limitar o próximo adversário.",
                recommendedBonusType = "DEFENSE_BOOST",
                recommendedBonusLabel = "🛡️ +4% Bônus de Intensidade Defensiva"
            )
        )

        // 3. Desenvolvimento de Atletas (Player Development Coach)
        val devStrengths = mutableListOf<String>()
        val devWeaknesses = mutableListOf<String>()
        val devHighlights = mutableListOf<String>()

        // Check young prospect performance
        val youngPlayers = managedTeam.players.filter { it.age <= 22 }
        if (youngPlayers.isNotEmpty()) {
            val bestYoung = youngPlayers.maxByOrNull { userStatsMap[it]?.points ?: 0 }
            val youngPts = userStatsMap[bestYoung]?.points ?: 0
            if (bestYoung != null && youngPts > 0) {
                devHighlights.add("Jovem Promessa: ${bestYoung.name} (${bestYoung.age} anos) atuou com muita maturidade e somou $youngPts pts.")
                devStrengths.add("${bestYoung.name} mostrou evolução constante na tomada de decisão.")
            }
        }

        if (topScorer != null) {
            devHighlights.add("Líder em Quadra: ${topScorer.name} somou $topScorerPts pontos e manteve a moral da equipe elevada.")
        }

        devStrengths.add("O elenco manteve boa integridade física e alto nível de energia no 4º quarto.")
        devWeaknesses.add("Ajustar a rotação de minutos dos titulares para evitar desgaste acumulado na temporada.")

        notifications.add(
            AssistantCoachNotification(
                gameDay = currentDay,
                seasonNumber = seasonNumber,
                opponentName = opponentName,
                isWin = isWin,
                userScore = userScore,
                opponentScore = oppScore,
                coachName = "Coach Sarah Jenkins",
                coachRole = "Desenvolvimento de Atletas",
                summary = "Análise de evolução individual após o confronto com $opponentName. Nossos jogadores continuam ganhando bagagem e experiência em quadra.",
                keyStrengths = devStrengths,
                areasToImprove = devWeaknesses,
                playerHighlights = devHighlights,
                tacticalAdvice = "Aplicar treinamento especial de condicionamento para acelerar o ganho de XP do nosso destaque individual.",
                recommendedBonusType = "XP_BOOST",
                recommendedBonusLabel = "🌱 +25 XP de Treino Extra para o Destaque da Partida"
            )
        )

        return notifications
    }
}
