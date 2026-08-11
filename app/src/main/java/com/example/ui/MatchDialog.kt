package com.example.ui

import com.example.*

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.data.NbaDataGenerator
import com.example.domain.finance.FinanceManager
import com.example.domain.roster.RosterManager
import com.example.domain.season.SeasonManager
import com.example.domain.trade.TradeManager
import com.example.domain.draft.DraftManager
import com.example.domain.playoff.PlayoffManager
import com.example.models.*
import com.example.simulator.GameSimulator
import com.example.ui.theme.*
import com.example.utils.AwardsCalculator
import com.example.utils.AutoSaveManager
import com.example.utils.ToastUtils
import com.example.utils.CoachFeedbackGenerator
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.StatsTab
import com.example.ui.screens.NotificationsTab
import com.example.ui.components.GameButton
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random


@Composable

fun PartidaDialog(
    viewModel: GameViewModel,
    homeTeamOverride: NbaTeam? = null,
    awayTeamOverride: NbaTeam? = null,
    gameTitle: String? = null,
    onGameFinished: ((GameSimulator.GameResult) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentSeason = viewModel.season ?: return
    val userManagedTeam = viewModel.managedTeam ?: return

    val (defaultOpponent, defaultIsHome) = viewModel.getNextOpponent()
    val homeTeam = homeTeamOverride ?: if (defaultIsHome) userManagedTeam else defaultOpponent
    val awayTeam = awayTeamOverride ?: if (defaultIsHome) defaultOpponent else userManagedTeam

    val isUserGame = (userManagedTeam.name == homeTeam.name || userManagedTeam.name == awayTeam.name)

    val team = if (isUserGame) {
        if (userManagedTeam.name == homeTeam.name) homeTeam else awayTeam
    } else {
        homeTeam
    }

    val opponent = if (isUserGame) {
        if (userManagedTeam.name == homeTeam.name) awayTeam else homeTeam
    } else {
        awayTeam
    }

    val isHome = (team.name == homeTeam.name)

    var userScore by remember { mutableStateOf(0) }
    var oppScore by remember { mutableStateOf(0) }
    var currentQuarter by remember { mutableStateOf(1) }
    var isHalftime by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }
    
    val qUserScores = remember { mutableStateListOf<Int>() }
    val qOppScores = remember { mutableStateListOf<Int>() }
    
    var simResult by remember { mutableStateOf<GameSimulator.GameResult?>(null) }
    var narration by remember { mutableStateOf("Início do jogo! A bola está ao alto!") }

    var isLiveCoachingActive by remember { mutableStateOf(false) }
    var hasUsedLiveCoaching by remember { mutableStateOf(false) }
    var timeoutsRemaining by remember { mutableStateOf(2) }
    var timeoutBoost by remember { mutableStateOf(0.0) }

    val starPlayer = team.players.maxByOrNull { it.overall }
    val starName = starPlayer?.name ?: "Estrela do Time"

    fun executeClutchPlay(playType: String) {
        val baseRoll = (1..100).random() / 100.0 + timeoutBoost
        when (playType) {
            "3PT" -> {
                if (baseRoll > 0.40) {
                    userScore += 3
                    if (qUserScores.isNotEmpty()) qUserScores[qUserScores.lastIndex] = qUserScores.last() + 3
                    narration = "🎯 0:02 - $starName recebe no perímetro, faz o drible de hesitação e lança DE TRÊS...\n\nÉ BOLA NA REDE! CESTA INCRÍVEL DE 3 PONTOS NO ESTOURO DO CRONÔMETRO! 🏀🔥"
                } else {
                    narration = "🎯 0:02 - $starName lança de 3 sob forte marcação... A bola bate no aro e sai! Apito final!"
                }
            }
            "PAINT" -> {
                if (baseRoll > 0.30) {
                    val isAndOne = (1..100).random() < 35
                    val pts = if (isAndOne) 3 else 2
                    userScore += pts
                    if (qUserScores.isNotEmpty()) qUserScores[qUserScores.lastIndex] = qUserScores.last() + pts
                    narration = if (isAndOne) {
                        "💥 0:03 - Infiltração espetacular de $starName no garrafão! ENTERRA COM DUAS MÃOS E SOFRE A FALTA! AND-1 CONVERTIDO! 🔥"
                    } else {
                        "💥 0:03 - Infiltração rápida pelo garrafão! $starName corta a defesa e faz a bandeja decisiva! DOIS PONTOS! 🏀"
                    }
                } else {
                    narration = "💥 0:03 - Infiltração forçada no garrafão! A defesa adversária tranca o espaço e bloqueia o arremesso!"
                }
            }
            "ISO" -> {
                if (baseRoll > 0.35) {
                    userScore += 2
                    if (qUserScores.isNotEmpty()) qUserScores[qUserScores.lastIndex] = qUserScores.last() + 2
                    narration = "🌟 0:04 - ISOLAMENTO TOTAL para $starName! Drible cruzado, cria espaço no x1 e arremessa de meia distância...\n\nSWISH! BOLA LIMPA NA REDE! CESTA DA VITÓRIA NOS SEGUNDOS FINAIS! 🏀🌟"
                } else {
                    narration = "🌟 0:04 - Isolamento para $starName! A marcação dupla fecha rápido e força o arremesso contestado que erra o alvo!"
                }
            }
            "FOUL" -> {
                val oppFt = (1..2).random()
                oppScore += oppFt
                if (qOppScores.isNotEmpty()) qOppScores[qOppScores.lastIndex] = qOppScores.last() + oppFt
                narration = "🛑 0:10 - Falta tática cometida para parar o relógio! ${opponent.name} converte $oppFt lance(s) livre(s).\nPlacar: ${team.name} $userScore x $oppScore ${opponent.name}. Posse de bola final para $starName!"
            }
        }
        hasUsedLiveCoaching = true
        isLiveCoachingActive = false
    }
    
    LaunchedEffect(isHalftime, isFinished, currentQuarter, isLiveCoachingActive, hasUsedLiveCoaching) {
        if (!isHalftime && !isFinished && !isLiveCoachingActive) {
            narration = "${currentQuarter}º Quarto em andamento... As equipes disputam cada posse!"
            delay(2500)

            val tacticsObj = if (isUserGame) (viewModel.tactics ?: Tactics()) else Tactics()
            val coachObj = if (isUserGame) viewModel.coach else null
            val diff = if (isUserGame) viewModel.difficulty else 1

            val userDiffMod = when (diff) {
                0 -> 1.06 // Fácil: +6% pro usuário
                1 -> 0.95 // Normal: -5% pro usuário
                2 -> 0.92 // Difícil: -8% pro usuário
                else -> 0.95
            }
            val oppDiffMod = when (diff) {
                0 -> 0.92 // Fácil: -8% pro oponente
                1 -> 1.08 // Normal: +8% pro oponente
                2 -> 1.10 // Difícil: +10% pro oponente
                else -> 1.08
            }

            val userAvgRating = if (team.players.isNotEmpty()) team.players.map { it.overall }.average() else 75.0
            val oppAvgRating = if (opponent.players.isNotEmpty()) opponent.players.map { it.overall }.average() else 75.0

            val userOff = (userAvgRating / 75.0) * tacticsObj.getOffensiveModifier() * (1 + (coachObj?.getOffensiveBonus() ?: 0.0)) * userDiffMod
            val userDef = (userAvgRating / 75.0) * tacticsObj.getDefensiveModifier() * (1 + (coachObj?.getDefensiveBonus() ?: 0.0)) * userDiffMod

            val oppOff = (oppAvgRating / 75.0) * oppDiffMod
            val oppDef = (oppAvgRating / 75.0) * oppDiffMod

            val homeBonus = if (isHome) 1.5 else 0.0
            val baseU = (24.0 * userOff / oppDef + homeBonus + (tacticsObj.pace - 50) * 0.05).toInt()
            val baseO = (24.0 * oppOff / userDef + (tacticsObj.pace - 50) * 0.05).toInt()

            val uPoints = (baseU + (-4..5).random()).coerceIn(12, 45)
            val oPoints = (baseO + (-4..5).random()).coerceIn(12, 45)

            narration = "${currentQuarter}º Quarto: Troca de cestas e jogadas de alto nível!"
            delay(2500)

            qUserScores.add(uPoints)
            qOppScores.add(oPoints)
            userScore += uPoints
            oppScore += oPoints

            if (currentQuarter <= 2) {
                val subLog = if (isUserGame) viewModel.performAutoSubstitution(currentQuarter) else ""
                val baseMsg = when (currentQuarter) {
                    1 -> "Fim do 1º Quarto! Placar parcial: ${team.name} $userScore x $oppScore ${opponent.name}"
                    else -> "Fim do 2º Quarto! Intervalo de jogo! Placar parcial: ${team.name} $userScore x $oppScore ${opponent.name}"
                }
                narration = if (subLog.isNotEmpty()) "$baseMsg\n$subLog" else baseMsg
                delay(3000)
                
                if (currentQuarter == 2) {
                    isHalftime = true
                } else {
                    currentQuarter++
                }
            } else if (currentQuarter in 3..4) {
                val subLog = if (isUserGame) viewModel.performAutoSubstitution(currentQuarter) else ""
                val baseMsg = when (currentQuarter) {
                    3 -> "Fim do 3º Quarto! Emoção pura! Placar parcial: ${team.name} $userScore x $oppScore ${opponent.name}"
                    else -> "Fim do 4º Quarto! Apito final!"
                }
                narration = if (subLog.isNotEmpty()) "$baseMsg\n$subLog" else baseMsg
                delay(3000)
                
                if (currentQuarter == 4) {
                    if (isUserGame && !hasUsedLiveCoaching) {
                        isLiveCoachingActive = true
                        narration = "⏱️ MODO TÉCNICO EM TEMPO REAL!\nFaltam 15 segundos no 4º Quarto! Placar: ${team.name} $userScore x $oppScore ${opponent.name}.\nO jogo está acirrado! Faça sua chamada tática decisiva na prancheta!"
                        return@LaunchedEffect
                    }

                    isFinished = true
                    val simulator = GameSimulator(context.applicationContext, viewModel.simulationConfig())
                    val baseResult = simulator.simulate(homeTeam, awayTeam)
                    
                    val finalHomeScore = if (isHome) userScore else oppScore
                    val finalAwayScore = if (isHome) oppScore else userScore
                    
                    val finalResult = baseResult.copy(
                        homeScore = finalHomeScore,
                        awayScore = finalAwayScore
                    )
                    
                    simResult = finalResult
                    if (isUserGame) {
                        viewModel.latestResult = finalResult
                        val won = if (isHome) finalResult.homeScore > finalResult.awayScore else finalResult.awayScore > finalResult.homeScore
                        val xpEarned = if (won) 15 else 8
                        team.players.forEach { player ->
                            player.xp += xpEarned
                        }
                    }

                    if (onGameFinished != null) {
                        if (isUserGame) {
                            viewModel.finances?.let { f ->
                                val ticketPrice = when (userManagedTeam.name) {
                                    "Los Angeles Lakers", "Golden State Warriors", "New York Knicks" -> 150
                                    "Chicago Bulls", "Boston Celtics", "Miami Heat" -> 120
                                    "Dallas Mavericks", "Denver Nuggets", "Houston Rockets" -> 100
                                    else -> 80
                                }
                                if (isHome) {
                                    val gateRevenue = finalResult.attendance * ticketPrice
                                    f.budget += gateRevenue
                                    f.expenses.add(Expense("Receita de Ingressos (Playoffs)", gateRevenue, "Playoffs"))
                                }
                            }
                            viewModel.saveGame()
                        }
                        onGameFinished(finalResult)
                    } else {
                        val matchups = viewModel.getMatchupsForDay(currentSeason.currentDay)
                        viewModel.simulateOtherGames(context, matchups, finalResult)
                        
                        currentSeason.advanceDay()
                        
                        viewModel.finances?.let { f ->
                            val ticketPrice = if (viewModel.financeAdvanced.ticketPrice > 0) viewModel.financeAdvanced.ticketPrice else when (userManagedTeam.name) {
                                "Los Angeles Lakers", "Golden State Warriors", "New York Knicks" -> 120
                                "Chicago Bulls", "Boston Celtics", "Miami Heat" -> 100
                                "Dallas Mavericks", "Denver Nuggets", "Houston Rockets" -> 85
                                else -> 70
                            }
                            if (isHome) {
                                val gateRevenue = finalResult.attendance * ticketPrice
                                f.budget += gateRevenue
                                f.expenses.add(Expense("Receita de Ingressos", gateRevenue, "Dia ${currentSeason.currentDay}"))
                            }

                            val dailySponsorRevenue = f.sponsors.sumOf { it.amountPerYear } / 82
                            f.budget += dailySponsorRevenue
                            f.expenses.add(Expense("Receita de Patrocínio", dailySponsorRevenue, "Dia ${currentSeason.currentDay}"))

                            // Share of TV Rights & Merchandise per game
                            val dailyTvMerch = (85_000_000 + 20_000_000) / 82
                            f.budget += dailyTvMerch
                            
                            val playerSalaries = userManagedTeam.players.sumOf { it.calculateSalary() / 82 }
                            f.budget -= playerSalaries
                            f.expenses.add(Expense("Salários dos Jogadores", playerSalaries, "Dia ${currentSeason.currentDay}"))
                            
                            if (currentSeason.currentDay % 5 == 0) {
                                val expAmount = 250000
                                f.budget -= expAmount
                                f.expenses.add(Expense("Despesas e Salários", expAmount, "Dia ${currentSeason.currentDay}"))
                            }

                            // Pay coach salary (once per season)
                            if (!f.coachSalaryPaid && viewModel.coach != null) {
                                val coachSalary = viewModel.coach?.salary ?: 350000
                                f.budget -= coachSalary
                                f.expenses.add(Expense("Salário do Técnico", coachSalary, "Temporada ${currentSeason.seasonNumber}"))
                                f.coachSalaryPaid = true
                            }
                        }
                        
                        viewModel.season = null
                        viewModel.season = currentSeason
                        viewModel.managedTeam = userManagedTeam
                        
                        if (currentSeason.currentDay >= 82) {
                            viewModel.currentAwards = AwardsCalculator.calculateAwards(currentSeason.teams, currentSeason.standings, viewModel.coach?.name ?: "Você", viewModel.managedTeam?.name)
                            viewModel.gameState = GameState.PLAYOFFS
                        }
                        
                        viewModel.saveGame()
                    }
                } else {
                    currentQuarter++
                }
            }
        }
    }

    Dialog(onDismissRequest = { if (isFinished) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = gameTitle ?: if (isFinished) "FIM DE JOGO 🏀" else "SIMULAÇÃO EM TEMPO REAL Q$currentQuarter",
                    fontWeight = FontWeight.Bold,
                    color = ChampionshipGold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Scoreboard comparison
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            text = team.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextWhite,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "$userScore",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black,
                            color = BasketOrange
                        )
                    }
                    Text("VS", fontWeight = FontWeight.Bold, color = TextGray, fontSize = 16.sp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            text = opponent.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextWhite,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "$oppScore",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black,
                            color = TextWhite
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quarters score table
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (q in 1..4) {
                        val uQ = qUserScores.getOrNull(q - 1)
                        val oQ = qOppScores.getOrNull(q - 1)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Q$q", fontSize = 11.sp, color = TextGray, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (uQ != null && oQ != null) "$uQ - $oQ" else "- x -",
                                fontSize = 12.sp,
                                color = if (q == currentQuarter) BasketOrange else TextWhite,
                                fontWeight = if (q == currentQuarter) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CourtLightSlate.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = narration,
                            fontSize = 12.sp,
                            color = TextWhite,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (isLiveCoachingActive) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, ChampionshipGold, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SportsBasketball,
                                    contentDescription = null,
                                    tint = BasketOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "⏱️ MODO TÉCNICO EM TEMPO REAL",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ChampionshipGold
                                )
                            }

                            Text(
                                text = "4º Quarto (0:15) • Chamada Tática Decisiva",
                                fontSize = 11.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                            )

                            // Timeout button
                            Button(
                                onClick = {
                                    if (timeoutsRemaining > 0) {
                                        timeoutsRemaining--
                                        timeoutBoost = 0.20
                                        narration = "⏱️ TIMEOUT SOLICITADO! Sua equipe se reúne no banco de reservas com a prancheta tática.\nFoco e energia restaurados (+20% de precisão na última jogada)!"
                                    }
                                },
                                enabled = timeoutsRemaining > 0,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCyan.copy(alpha = 0.25f),
                                    disabledContainerColor = CourtLightSlate.copy(alpha = 0.3f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (timeoutsRemaining > 0) ElectricCyan else CourtBorder),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = if (timeoutsRemaining > 0) "⏱️ PEDIR TEMPO / TIMEOUT ($timeoutsRemaining RESTANTES)" else "⏱️ TIMEOUTS ESGOTADOS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (timeoutsRemaining > 0) ElectricCyan else TextMuted
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "✍️ DESENHAR ÚLTIMA JOGADA DE ATAQUE:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite,
                                modifier = Modifier.align(Alignment.Start)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // 3-Point Shot Play
                            Button(
                                onClick = { executeClutchPlay("3PT") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = BasketOrange),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("🎯 Arremesso de 3 Pontos (Virada/Empate)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Drive in Paint Play
                            Button(
                                onClick = { executeClutchPlay("PAINT") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = CourtLightSlate),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CourtBorder)
                            ) {
                                Text("💥 Infiltração no Garrafão (Cavar Falta / Bandeja)", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Star Isolation Play
                            Button(
                                onClick = { executeClutchPlay("ISO") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = CourtLightSlate),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.6f))
                            ) {
                                Text("🌟 Isolamento da Estrela ($starName)", color = ChampionshipGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Tactical Foul Button
                            OutlinedButton(
                                onClick = { executeClutchPlay("FOUL") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444))
                            ) {
                                Text("🛑 Falta Tática (Parar o Relógio)", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                if (isHalftime) {
                    if (isUserGame) {
                        val tacticsObj = viewModel.tactics ?: Tactics()
                        var style by remember { mutableStateOf(tacticsObj.style) }
                        var pace by remember { mutableStateOf(tacticsObj.pace.toFloat()) }
                        var defPressure by remember { mutableStateOf(tacticsObj.defensivePressure.toFloat()) }
                        var offReb by remember { mutableStateOf(tacticsObj.offensiveRebound.toFloat()) }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CourtLightSlate),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "⚙️ AJUSTE TÁTICO NO INTERVALO",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ChampionshipGold,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    PlayStyle.entries.forEach { pStyle ->
                                        val isSelected = style == pStyle
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSelected) BasketOrange else CourtDeepSlate)
                                                .clickable {
                                                    style = pStyle
                                                    tacticsObj.style = pStyle
                                                    val (newPace, newDef, newOff) = when (pStyle) {
                                                        PlayStyle.FAST_BREAK -> Triple(85, 60, 70)
                                                        PlayStyle.HALF_COURT -> Triple(30, 50, 40)
                                                        PlayStyle.DEFENSIVE -> Triple(35, 85, 30)
                                                        PlayStyle.BALANCED -> Triple(50, 50, 50)
                                                    }
                                                    pace = newPace.toFloat()
                                                    defPressure = newDef.toFloat()
                                                    offReb = newOff.toFloat()

                                                    tacticsObj.pace = newPace
                                                    tacticsObj.defensivePressure = newDef
                                                    tacticsObj.offensiveRebound = newOff
                                                    viewModel.saveGame()
                                                }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = when(pStyle) {
                                                    PlayStyle.FAST_BREAK -> "Corrida"
                                                    PlayStyle.HALF_COURT -> "Meio-C"
                                                    PlayStyle.BALANCED -> "Equilib"
                                                    PlayStyle.DEFENSIVE -> "Defesa"
                                                },
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Ritmo (Pace): ${pace.toInt()}", color = TextWhite, fontSize = 11.sp)
                                Slider(
                                    value = pace,
                                    onValueChange = {
                                        pace = it
                                        tacticsObj.pace = it.toInt()
                                        viewModel.saveGame()
                                    },
                                    valueRange = 0f..100f,
                                    modifier = Modifier.height(24.dp),
                                    colors = SliderDefaults.colors(thumbColor = BasketOrange, activeTrackColor = BasketOrange)
                                )
                                
                                Text("Pressão Defensiva: ${defPressure.toInt()}%", color = TextWhite, fontSize = 11.sp)
                                Slider(
                                    value = defPressure,
                                    onValueChange = {
                                        defPressure = it
                                        tacticsObj.defensivePressure = it.toInt()
                                        viewModel.saveGame()
                                    },
                                    valueRange = 0f..100f,
                                    modifier = Modifier.height(24.dp),
                                    colors = SliderDefaults.colors(thumbColor = BasketOrange, activeTrackColor = BasketOrange)
                                )

                                Text("Rebote Ofensivo: ${offReb.toInt()}%", color = TextWhite, fontSize = 11.sp)
                                Slider(
                                    value = offReb,
                                    onValueChange = {
                                        offReb = it
                                        tacticsObj.offensiveRebound = it.toInt()
                                        viewModel.saveGame()
                                    },
                                    valueRange = 0f..100f,
                                    modifier = Modifier.height(24.dp),
                                    colors = SliderDefaults.colors(thumbColor = BasketOrange, activeTrackColor = BasketOrange)
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CourtDeepSlate)
                                        .clickable {
                                            viewModel.autoSubstitutionsEnabled = !viewModel.autoSubstitutionsEnabled
                                            viewModel.saveGame()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, tint = BasketOrange, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Substituições Automáticas", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Switch(
                                        checked = viewModel.autoSubstitutionsEnabled,
                                        onCheckedChange = {
                                            viewModel.autoSubstitutionsEnabled = it
                                            viewModel.saveGame()
                                        },
                                        modifier = Modifier.height(20.dp),
                                        colors = SwitchDefaults.colors(checkedThumbColor = BasketOrange, checkedTrackColor = BasketOrange.copy(alpha = 0.5f))
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        isHalftime = false
                                        currentQuarter = 3
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = BasketOrange),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("CONTINUAR JOGO ➡️", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CourtLightSlate),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "INTERVALO DE JOGO ☕",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ChampionshipGold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "As equipes se preparam no vestiário para o 2º tempo.",
                                    fontSize = 11.sp,
                                    color = TextWhite,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        isHalftime = false
                                        currentQuarter = 3
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = BasketOrange),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("CONTINUAR JOGO ➡️", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                if (isFinished) {
                    simResult?.let { result ->
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // MVP Player Row
                        val allStats = result.homeStats.entries + result.awayStats.entries
                        val mvpEntry = allStats.maxByOrNull { it.value.points }
                        if (mvpEntry != null) {
                            val mvpPlayer = mvpEntry.key
                            val mvpStats = mvpEntry.value
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = CourtLightSlate),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("⭐ MVP DO JOGO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ChampionshipGold)
                                    Text(mvpPlayer.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextWhite)
                                    Text("${mvpStats.points} PTS • ${mvpStats.rebounds} REB • ${mvpStats.assists} AST", fontSize = 11.sp, color = BasketOrange)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        val arenaName = if (isHome) team.arena.name else opponent.arena.name
                        Text(
                            text = "Público: ${String.format("%,d", result.attendance).replace(',', '.')} • Arena: $arenaName",
                            fontSize = 10.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center
                        )

                        if (result.injuries.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            result.injuries.forEach { injury ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "🚑 Lesão: ${injury.player.name} fora por ${injury.daysOut} dias!",
                                        color = ErrorRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onDismiss() },
                        colors = ButtonDefaults.buttonColors(containerColor = BasketOrange),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("FECHAR E SALVAR", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

