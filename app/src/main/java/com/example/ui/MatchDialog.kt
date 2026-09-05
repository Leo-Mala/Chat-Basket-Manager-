package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.GameViewModel
import com.example.domain.rules.LiveMatchRules
import com.example.domain.rules.LiveScoringSide
import com.example.domain.rules.LiveScoringTimeline
import com.example.models.Expense
import com.example.models.GameState
import com.example.models.NbaTeam
import com.example.models.PlayStyle
import com.example.models.Player
import com.example.models.Tactics
import com.example.simulator.GameSimulator
import com.example.simulator.LiveLineupRules
import com.example.ui.theme.BasketOrange
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.CourtBorder
import com.example.ui.theme.CourtDeepSlate
import com.example.ui.theme.CourtLightSlate
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.utils.AwardsCalculator
import kotlinx.coroutines.delay
import kotlin.math.min

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

    val initialMatchup = remember(homeTeamOverride?.name, awayTeamOverride?.name) {
        val (defaultOpponent, defaultIsHome) = viewModel.getNextOpponent()
        val initialHome = homeTeamOverride ?: if (defaultIsHome) userManagedTeam else defaultOpponent
        val initialAway = awayTeamOverride ?: if (defaultIsHome) defaultOpponent else userManagedTeam
        initialHome to initialAway
    }
    val homeTeam = initialMatchup.first
    val awayTeam = initialMatchup.second
    val isUserGame = userManagedTeam.name == homeTeam.name || userManagedTeam.name == awayTeam.name
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
    val isHome = team.name == homeTeam.name

    val initialLiveLineup = remember(team.name) {
        LiveLineupRules.initialLineup(
            roster = team.players,
            preferred = if (isUserGame) viewModel.startingFive else emptyList()
        )
    }
    val activeLineup = remember(team.name) {
        mutableStateListOf<Player>().apply { addAll(initialLiveLineup) }
    }

    var userScore by remember { mutableStateOf(0) }
    var oppScore by remember { mutableStateOf(0) }
    var currentQuarter by remember { mutableStateOf(1) }
    var quarterClock by remember { mutableStateOf("12:00") }
    var isPaused by remember { mutableStateOf(false) }
    var isHalftime by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }
    val qUserScores = remember { mutableStateListOf<Int>() }
    val qOppScores = remember { mutableStateListOf<Int>() }
    var simResult by remember { mutableStateOf<GameSimulator.GameResult?>(null) }
    var narration by remember { mutableStateOf("Início do jogo! A bola está ao alto!") }

    var selectedPlayerOutId by remember { mutableStateOf<Int?>(null) }
    var selectedPlayerInId by remember { mutableStateOf<Int?>(null) }

    var isLiveCoachingActive by remember { mutableStateOf(false) }
    var hasUsedLiveCoaching by remember { mutableStateOf(false) }
    var timeoutsRemaining by remember { mutableStateOf(2) }
    var timeoutBoost by remember { mutableStateOf(0.0) }

    val starPlayer = team.players.maxByOrNull { it.overall }
    val starName = starPlayer?.name ?: "Estrela do Time"

    fun replaceActiveLineup(updated: List<Player>) {
        if (updated.size != LiveLineupRules.PLAYERS_ON_COURT) return
        activeLineup.clear()
        activeLineup.addAll(updated)
    }

    fun applyAutomaticRotation(completedQuarter: Int): String {
        if (!isUserGame || !viewModel.autoSubstitutionsEnabled) return ""
        return when (completedQuarter) {
            2 -> {
                val bench = LiveLineupRules.bench(team.players, activeLineup)
                val playerOut = activeLineup.minByOrNull { it.overall }
                val playerIn = bench.maxByOrNull { it.overall }
                if (playerOut == null || playerIn == null) {
                    ""
                } else {
                    val substitution = LiveLineupRules.substitute(
                        roster = team.players,
                        activeLineup = activeLineup,
                        playerOutId = playerOut.id,
                        playerInId = playerIn.id
                    )
                    if (substitution == null) {
                        ""
                    } else {
                        replaceActiveLineup(substitution.lineup)
                        "🔄 Rotação automática: ${substitution.playerIn.name} entra no lugar de ${substitution.playerOut.name}."
                    }
                }
            }
            3 -> {
                val restored = LiveLineupRules.initialLineup(team.players, initialLiveLineup)
                if (restored.size == LiveLineupRules.PLAYERS_ON_COURT) {
                    replaceActiveLineup(restored)
                    "🔄 Rotação automática: titulares principais retornam para o 4º quarto."
                } else {
                    ""
                }
            }
            else -> ""
        }
    }

    fun finishGame() {
        if (isFinished) return
        isPaused = false
        isFinished = true
        quarterClock = "00:00"

        val simulator = GameSimulator(context.applicationContext, viewModel.simulationConfig())
        val baseResult = simulator.simulate(homeTeam, awayTeam)

        val finalUserScore = LiveMatchRules.scoreFromQuarters(qUserScores)
        val finalOpponentScore = LiveMatchRules.scoreFromQuarters(qOppScores)
        userScore = finalUserScore
        oppScore = finalOpponentScore
        val finalHomeScore = if (isHome) finalUserScore else finalOpponentScore
        val finalAwayScore = if (isHome) finalOpponentScore else finalUserScore
        val finalResult = baseResult.copy(homeScore = finalHomeScore, awayScore = finalAwayScore)
        simResult = finalResult

        if (isUserGame) {
            viewModel.latestResult = finalResult
            val won = if (isHome) {
                finalResult.homeScore > finalResult.awayScore
            } else {
                finalResult.awayScore > finalResult.homeScore
            }
            val xpEarned = if (won) 15 else 8
            team.players.forEach { it.addXpSafely(xpEarned) }
        }

        if (onGameFinished != null) {
            if (isUserGame) {
                viewModel.finances?.let { finance ->
                    val ticketPrice = when (userManagedTeam.name) {
                        "Los Angeles Lakers", "Golden State Warriors", "New York Knicks" -> 150
                        "Chicago Bulls", "Boston Celtics", "Miami Heat" -> 120
                        "Dallas Mavericks", "Denver Nuggets", "Houston Rockets" -> 100
                        else -> 80
                    }
                    if (isHome) {
                        val gateRevenue = finalResult.attendance * ticketPrice
                        finance.budget += gateRevenue
                        finance.expenses.add(Expense("Receita de Ingressos (Playoffs)", gateRevenue, "Playoffs"))
                    }
                }
                viewModel.saveGame()
            }
            onGameFinished(finalResult)
            return
        }

        val matchups = viewModel.getMatchupsForDay(currentSeason.currentDay)
        viewModel.simulateOtherGames(context, matchups, finalResult)
        currentSeason.advanceDay()

        viewModel.finances?.let { finance ->
            val ticketPrice = if (viewModel.financeAdvanced.ticketPrice > 0) {
                viewModel.financeAdvanced.ticketPrice
            } else {
                when (userManagedTeam.name) {
                    "Los Angeles Lakers", "Golden State Warriors", "New York Knicks" -> 120
                    "Chicago Bulls", "Boston Celtics", "Miami Heat" -> 100
                    "Dallas Mavericks", "Denver Nuggets", "Houston Rockets" -> 85
                    else -> 70
                }
            }
            if (isHome) {
                val gateRevenue = finalResult.attendance * ticketPrice
                finance.budget += gateRevenue
                finance.expenses.add(Expense("Receita de Ingressos", gateRevenue, "Dia ${currentSeason.currentDay}"))
            }

            val dailySponsorRevenue = finance.sponsors.sumOf { it.amountPerYear } / 82
            finance.budget += dailySponsorRevenue
            finance.expenses.add(Expense("Receita de Patrocínio", dailySponsorRevenue, "Dia ${currentSeason.currentDay}"))

            val dailyTvMerch = (85_000_000 + 20_000_000) / 82
            finance.budget += dailyTvMerch

            val playerSalaries = userManagedTeam.players.sumOf { it.calculateSalary() / 82 }
            finance.budget -= playerSalaries
            finance.expenses.add(Expense("Salários dos Jogadores", playerSalaries, "Dia ${currentSeason.currentDay}"))

            if (currentSeason.currentDay % 5 == 0) {
                val expenseAmount = 250_000
                finance.budget -= expenseAmount
                finance.expenses.add(Expense("Despesas e Salários", expenseAmount, "Dia ${currentSeason.currentDay}"))
            }

            if (!finance.coachSalaryPaid && viewModel.coach != null) {
                val coachSalary = viewModel.coach?.salary ?: 350_000
                finance.budget -= coachSalary
                finance.expenses.add(Expense("Salário do Técnico", coachSalary, "Temporada ${currentSeason.seasonNumber}"))
                finance.coachSalaryPaid = true
            }
        }

        viewModel.season = null
        viewModel.season = currentSeason
        viewModel.managedTeam = userManagedTeam

        if (currentSeason.currentDay >= 82) {
            viewModel.currentAwards = AwardsCalculator.calculateAwards(
                currentSeason.teams,
                currentSeason.standings,
                viewModel.coach?.name ?: "Você",
                viewModel.managedTeam?.name
            )
            viewModel.gameState = GameState.PLAYOFFS
        }
        viewModel.saveGame()
    }

    fun executeClutchPlay(playType: String) {
        if (!isLiveCoachingActive || isFinished) return
        val baseRoll = (1..100).random() / 100.0 + timeoutBoost
        when (playType) {
            "3PT" -> {
                if (baseRoll > 0.40) {
                    userScore += 3
                    if (qUserScores.isNotEmpty()) qUserScores[qUserScores.lastIndex] = qUserScores.last() + 3
                    narration = "🎯 0:02 - $starName recebe no perímetro e lança de três... É BOLA NA REDE! +3!"
                } else {
                    narration = "🎯 0:02 - $starName lança de três sob pressão... bate no aro e sai!"
                }
            }
            "PAINT" -> {
                if (baseRoll > 0.30) {
                    val andOne = (1..100).random() < 35
                    val points = if (andOne) 3 else 2
                    userScore += points
                    if (qUserScores.isNotEmpty()) qUserScores[qUserScores.lastIndex] = qUserScores.last() + points
                    narration = if (andOne) {
                        "💥 0:03 - $starName ataca o garrafão, converte e sofre a falta! AND-1! +3!"
                    } else {
                        "💥 0:03 - $starName infiltra e converte a bandeja! +2!"
                    }
                } else {
                    narration = "💥 0:03 - A defesa fecha o garrafão e bloqueia a tentativa!"
                }
            }
            "ISO" -> {
                if (baseRoll > 0.35) {
                    userScore += 2
                    if (qUserScores.isNotEmpty()) qUserScores[qUserScores.lastIndex] = qUserScores.last() + 2
                    narration = "🌟 0:04 - Isolamento para $starName, arremesso de média distância... CESTA! +2!"
                } else {
                    narration = "🌟 0:04 - Isolamento para $starName, mas a marcação força o erro!"
                }
            }
            "FOUL" -> {
                val opponentFreeThrows = (1..2).random()
                oppScore += opponentFreeThrows
                if (qOppScores.isNotEmpty()) qOppScores[qOppScores.lastIndex] = qOppScores.last() + opponentFreeThrows
                narration = "🛑 0:10 - Falta tática. ${opponent.name} converte $opponentFreeThrows lance(s) livre(s)."
            }
        }
        hasUsedLiveCoaching = true
        isLiveCoachingActive = false
        timeoutBoost = 0.0
    }

    LaunchedEffect(isHalftime, isFinished, currentQuarter, isLiveCoachingActive, hasUsedLiveCoaching) {
        if (isHalftime || isFinished || isLiveCoachingActive) return@LaunchedEffect

        val resolvingClutch = currentQuarter == 4 && hasUsedLiveCoaching
        if (resolvingClutch) {
            finishGame()
            return@LaunchedEffect
        }

        isPaused = false
        selectedPlayerOutId = null
        selectedPlayerInId = null
        quarterClock = "12:00"
        narration = "${currentQuarter}º quarto em andamento. O relógio acelerado começou!"

        val tactics = if (isUserGame) viewModel.tactics ?: Tactics() else Tactics()
        val coach = if (isUserGame) viewModel.coach else null
        val difficulty = if (isUserGame) viewModel.difficulty else 1
        val userDiffMod = when (difficulty) {
            0 -> 1.06
            1 -> 0.95
            2 -> 0.92
            else -> 0.95
        }
        val opponentDiffMod = when (difficulty) {
            0 -> 0.92
            1 -> 1.08
            2 -> 1.10
            else -> 1.08
        }

        // The current five is match-local. Manual substitutions therefore influence the next
        // quarter calculation without overwriting the saved pre-game starting five.
        val userRatingSource = if (isUserGame && activeLineup.size == LiveLineupRules.PLAYERS_ON_COURT) {
            activeLineup.toList()
        } else {
            team.players
        }
        val userAverage = if (userRatingSource.isNotEmpty()) userRatingSource.map { it.overall }.average() else 75.0
        val opponentAverage = if (opponent.players.isNotEmpty()) opponent.players.map { it.overall }.average() else 75.0
        val userOffense = (userAverage / 75.0) * tactics.getOffensiveModifier() *
            (1 + (coach?.getOffensiveBonus() ?: 0.0)) * userDiffMod
        val userDefense = (userAverage / 75.0) * tactics.getDefensiveModifier() *
            (1 + (coach?.getDefensiveBonus() ?: 0.0)) * userDiffMod
        val opponentOffense = (opponentAverage / 75.0) * opponentDiffMod
        val opponentDefense = (opponentAverage / 75.0) * opponentDiffMod
        val homeBonus = if (isHome) 1.5 else 0.0
        val baseUser = (24.0 * userOffense / opponentDefense + homeBonus + (tactics.pace - 50) * 0.05).toInt()
        val baseOpponent = (24.0 * opponentOffense / userDefense + (tactics.pace - 50) * 0.05).toInt()
        val quarterUserPoints = (baseUser + (-4..5).random()).coerceIn(12, 45)
        val quarterOpponentPoints = (baseOpponent + (-4..5).random()).coerceIn(12, 45)

        qUserScores.add(0)
        qOppScores.add(0)
        val timeline = LiveScoringTimeline.build(quarterUserPoints, quarterOpponentPoints)
        var elapsedMillis = 0L
        var nextEvent = 0

        while (elapsedMillis < LiveScoringTimeline.QUARTER_REAL_DURATION_MS) {
            delay(LiveScoringTimeline.UI_TICK_MS)
            if (isPaused) continue

            elapsedMillis = min(
                LiveScoringTimeline.QUARTER_REAL_DURATION_MS,
                elapsedMillis + LiveScoringTimeline.UI_TICK_MS
            )
            quarterClock = LiveScoringTimeline.clockForElapsed(elapsedMillis)

            while (nextEvent < timeline.size && timeline[nextEvent].elapsedMillis <= elapsedMillis) {
                val scoringEvent = timeline[nextEvent++]
                when (scoringEvent.side) {
                    LiveScoringSide.USER -> {
                        userScore += scoringEvent.points
                        qUserScores[qUserScores.lastIndex] = qUserScores.last() + scoringEvent.points
                        narration = when (scoringEvent.points) {
                            1 -> "⏱️ $quarterClock • ${team.name} converte 1 lance livre. Placar $userScore x $oppScore."
                            2 -> "🏀 $quarterClock • Cesta de 2 de ${team.name}! Placar $userScore x $oppScore."
                            else -> "🎯 $quarterClock • Bola de 3 de ${team.name}! Placar $userScore x $oppScore."
                        }
                    }
                    LiveScoringSide.OPPONENT -> {
                        oppScore += scoringEvent.points
                        qOppScores[qOppScores.lastIndex] = qOppScores.last() + scoringEvent.points
                        narration = when (scoringEvent.points) {
                            1 -> "⏱️ $quarterClock • ${opponent.name} converte 1 lance livre. Placar $userScore x $oppScore."
                            2 -> "🏀 $quarterClock • Cesta de 2 de ${opponent.name}. Placar $userScore x $oppScore."
                            else -> "🎯 $quarterClock • Bola de 3 de ${opponent.name}. Placar $userScore x $oppScore."
                        }
                    }
                }
            }
        }

        quarterClock = "00:00"
        val automaticRotationLog = applyAutomaticRotation(currentQuarter)
        val baseMessage = when (currentQuarter) {
            1 -> "Fim do 1º quarto! ${team.name} $userScore x $oppScore ${opponent.name}."
            2 -> "Fim do 2º quarto! Intervalo: ${team.name} $userScore x $oppScore ${opponent.name}."
            3 -> "Fim do 3º quarto! ${team.name} $userScore x $oppScore ${opponent.name}."
            else -> "Fim do 4º quarto!"
        }
        narration = if (automaticRotationLog.isBlank()) baseMessage else "$baseMessage\n$automaticRotationLog"
        delay(1_000)

        when (currentQuarter) {
            1 -> currentQuarter = 2
            2 -> isHalftime = true
            3 -> currentQuarter = 4
            4 -> {
                val shouldOfferClutch = LiveMatchRules.shouldOfferClutch(
                    isUserGame = isUserGame,
                    hasUsedLiveCoaching = hasUsedLiveCoaching,
                    userScore = userScore,
                    opponentScore = oppScore
                )
                if (shouldOfferClutch) {
                    isLiveCoachingActive = true
                    narration = "⏱️ MODO TÉCNICO! Restam 15 segundos. ${team.name} $userScore x $oppScore ${opponent.name}. Faça a chamada decisiva."
                } else {
                    finishGame()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = { if (isFinished) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .padding(horizontal = 12.dp, vertical = 18.dp),
            colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = gameTitle ?: if (isFinished) "FIM DE JOGO 🏀" else "PARTIDA AO VIVO",
                    fontWeight = FontWeight.Bold,
                    color = ChampionshipGold,
                    fontSize = 14.sp
                )
                if (!isFinished) {
                    Text(
                        text = "Q$currentQuarter • $quarterClock  ·  1 quarto = 1 min",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isPaused) ChampionshipGold else ElectricCyan
                    )
                    if (isPaused) {
                        Text("PAUSADO", color = ChampionshipGold, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(team.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextWhite, textAlign = TextAlign.Center)
                        Text("$userScore", fontSize = 40.sp, fontWeight = FontWeight.Black, color = BasketOrange)
                    }
                    Text("VS", fontWeight = FontWeight.Bold, color = TextGray, fontSize = 16.sp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(opponent.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextWhite, textAlign = TextAlign.Center)
                        Text("$oppScore", fontSize = 40.sp, fontWeight = FontWeight.Black, color = TextWhite)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (quarter in 1..4) {
                        val userQuarter = qUserScores.getOrNull(quarter - 1)
                        val opponentQuarter = qOppScores.getOrNull(quarter - 1)
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("Q$quarter", fontSize = 11.sp, color = TextGray, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (userQuarter != null && opponentQuarter != null) "$userQuarter - $opponentQuarter" else "- x -",
                                fontSize = 12.sp,
                                color = if (quarter == currentQuarter && !isFinished) BasketOrange else TextWhite,
                                fontWeight = if (quarter == currentQuarter && !isFinished) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CourtLightSlate.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                        Text(narration, fontSize = 12.sp, color = TextWhite, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                    }
                }

                if (isUserGame && !isFinished) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Em quadra: ${activeLineup.joinToString(" • ") { it.name }}",
                        color = TextMuted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }

                if (!isFinished && !isHalftime && !isLiveCoachingActive) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            isPaused = !isPaused
                            narration = if (isPaused) {
                                "⏸️ Jogo pausado em Q$currentQuarter $quarterClock. Você pode ajustar a rotação."
                            } else {
                                "▶️ Jogo retomado em Q$currentQuarter $quarterClock."
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isPaused) ElectricCyan else CourtLightSlate),
                        border = BorderStroke(1.dp, if (isPaused) ElectricCyan else CourtBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isPaused) "▶️ CONTINUAR JOGO" else "⏸️ PAUSAR", color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                }

                if (isUserGame && isPaused && !isHalftime && !isFinished && !isLiveCoachingActive) {
                    Spacer(modifier = Modifier.height(10.dp))
                    ManualSubstitutionPanel(
                        roster = team.players,
                        activeLineup = activeLineup,
                        selectedOutId = selectedPlayerOutId,
                        selectedInId = selectedPlayerInId,
                        onSelectOut = { selectedPlayerOutId = it },
                        onSelectIn = { selectedPlayerInId = it },
                        onConfirm = {
                            val playerOutId = selectedPlayerOutId
                            val playerInId = selectedPlayerInId
                            if (playerOutId != null && playerInId != null) {
                                val substitution = LiveLineupRules.substitute(
                                    roster = team.players,
                                    activeLineup = activeLineup,
                                    playerOutId = playerOutId,
                                    playerInId = playerInId
                                )
                                if (substitution != null) {
                                    replaceActiveLineup(substitution.lineup)
                                    narration = "🔄 Substituição: ${substitution.playerIn.name} entra no lugar de ${substitution.playerOut.name}."
                                    selectedPlayerOutId = null
                                    selectedPlayerInId = null
                                }
                            }
                        }
                    )
                }

                if (isLiveCoachingActive) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.5.dp, ChampionshipGold, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.SportsBasketball, contentDescription = null, tint = BasketOrange, modifier = Modifier.size(18.dp))
                                Text("⏱️ MODO TÉCNICO EM TEMPO REAL", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = ChampionshipGold)
                            }
                            Text("4º quarto (0:15) • Chamada tática decisiva", fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))

                            Button(
                                onClick = {
                                    if (timeoutsRemaining > 0) {
                                        timeoutsRemaining--
                                        timeoutBoost = 0.20
                                        narration = "⏱️ TIMEOUT! Sua equipe ganha +20% de precisão na chamada decisiva."
                                    }
                                },
                                enabled = timeoutsRemaining > 0,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan.copy(alpha = 0.25f)),
                                border = BorderStroke(1.dp, if (timeoutsRemaining > 0) ElectricCyan else CourtBorder),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(if (timeoutsRemaining > 0) "⏱️ TIMEOUT ($timeoutsRemaining restantes)" else "⏱️ TIMEOUTS ESGOTADOS", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { executeClutchPlay("3PT") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = BasketOrange)) {
                                Text("🎯 Arremesso de 3 pontos", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(onClick = { executeClutchPlay("PAINT") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = CourtLightSlate)) {
                                Text("💥 Infiltração no garrafão", color = TextWhite, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(onClick = { executeClutchPlay("ISO") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = CourtLightSlate)) {
                                Text("🌟 Isolamento da estrela ($starName)", color = ChampionshipGold, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(onClick = { executeClutchPlay("FOUL") }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, ErrorRed)) {
                                Text("🛑 Falta tática", color = ErrorRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (isHalftime) {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (isUserGame) {
                        val tactics = viewModel.tactics ?: Tactics()
                        var style by remember { mutableStateOf(tactics.style) }
                        var pace by remember { mutableStateOf(tactics.pace.toFloat()) }
                        var defensivePressure by remember { mutableStateOf(tactics.defensivePressure.toFloat()) }
                        var offensiveRebound by remember { mutableStateOf(tactics.offensiveRebound.toFloat()) }

                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CourtLightSlate), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("⚙️ AJUSTE TÁTICO NO INTERVALO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ChampionshipGold, modifier = Modifier.align(Alignment.CenterHorizontally))
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    PlayStyle.entries.forEach { playStyle ->
                                        val selected = style == playStyle
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (selected) BasketOrange else CourtDeepSlate)
                                                .clickable {
                                                    style = playStyle
                                                    tactics.style = playStyle
                                                    val values = when (playStyle) {
                                                        PlayStyle.FAST_BREAK -> Triple(85, 60, 70)
                                                        PlayStyle.HALF_COURT -> Triple(30, 50, 40)
                                                        PlayStyle.DEFENSIVE -> Triple(35, 85, 30)
                                                        PlayStyle.BALANCED -> Triple(50, 50, 50)
                                                    }
                                                    pace = values.first.toFloat()
                                                    defensivePressure = values.second.toFloat()
                                                    offensiveRebound = values.third.toFloat()
                                                    tactics.pace = values.first
                                                    tactics.defensivePressure = values.second
                                                    tactics.offensiveRebound = values.third
                                                    viewModel.saveGame()
                                                }
                                                .padding(vertical = 7.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = when (playStyle) {
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
                                Text("Ritmo: ${pace.toInt()}", color = TextWhite, fontSize = 11.sp)
                                Slider(value = pace, onValueChange = { pace = it; tactics.pace = it.toInt() }, valueRange = 0f..100f, colors = SliderDefaults.colors(thumbColor = BasketOrange, activeTrackColor = BasketOrange))
                                Text("Pressão defensiva: ${defensivePressure.toInt()}%", color = TextWhite, fontSize = 11.sp)
                                Slider(value = defensivePressure, onValueChange = { defensivePressure = it; tactics.defensivePressure = it.toInt() }, valueRange = 0f..100f, colors = SliderDefaults.colors(thumbColor = BasketOrange, activeTrackColor = BasketOrange))
                                Text("Rebote ofensivo: ${offensiveRebound.toInt()}%", color = TextWhite, fontSize = 11.sp)
                                Slider(value = offensiveRebound, onValueChange = { offensiveRebound = it; tactics.offensiveRebound = it.toInt() }, valueRange = 0f..100f, colors = SliderDefaults.colors(thumbColor = BasketOrange, activeTrackColor = BasketOrange))

                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(CourtDeepSlate).padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = BasketOrange, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Substituições automáticas", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Switch(
                                        checked = viewModel.autoSubstitutionsEnabled,
                                        onCheckedChange = { viewModel.autoSubstitutionsEnabled = it; viewModel.saveGame() },
                                        colors = SwitchDefaults.colors(checkedThumbColor = BasketOrange, checkedTrackColor = BasketOrange.copy(alpha = 0.5f))
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        viewModel.saveGame()
                                        isHalftime = false
                                        currentQuarter = 3
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = BasketOrange)
                                ) {
                                    Text("CONTINUAR JOGO ➡️", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CourtLightSlate)) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("INTERVALO DE JOGO ☕", fontWeight = FontWeight.Bold, color = ChampionshipGold)
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(onClick = { isHalftime = false; currentQuarter = 3 }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = BasketOrange)) {
                                    Text("CONTINUAR JOGO ➡️", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (isFinished) {
                    simResult?.let { result ->
                        Spacer(modifier = Modifier.height(12.dp))
                        val allStats = result.homeStats.entries + result.awayStats.entries
                        val mvpEntry = allStats.maxByOrNull { it.value.points }
                        if (mvpEntry != null) {
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CourtLightSlate), shape = RoundedCornerShape(8.dp)) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("⭐ MVP DO JOGO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ChampionshipGold)
                                    Text(mvpEntry.key.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextWhite)
                                    Text("${mvpEntry.value.points} PTS • ${mvpEntry.value.rebounds} REB • ${mvpEntry.value.assists} AST", fontSize = 11.sp, color = BasketOrange)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val arenaName = if (isHome) team.arena.name else opponent.arena.name
                        Text("Público: ${String.format("%,d", result.attendance).replace(',', '.')} • Arena: $arenaName", fontSize = 10.sp, color = TextGray, textAlign = TextAlign.Center)
                        if (result.injuries.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            result.injuries.forEach { injury ->
                                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.15f))) {
                                    Text("🚑 Lesão: ${injury.player.name} fora por ${injury.daysOut} dias!", color = ErrorRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = BasketOrange), modifier = Modifier.fillMaxWidth()) {
                        Text("FECHAR E SALVAR", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualSubstitutionPanel(
    roster: List<Player>,
    activeLineup: List<Player>,
    selectedOutId: Int?,
    selectedInId: Int?,
    onSelectOut: (Int) -> Unit,
    onSelectIn: (Int) -> Unit,
    onConfirm: () -> Unit
) {
    val bench = LiveLineupRules.bench(roster, activeLineup)
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, BasketOrange, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CourtLightSlate),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("🔄 SUBSTITUIÇÃO MANUAL", color = ChampionshipGold, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            Text("Escolha quem sai e quem entra. A troca vale apenas para esta partida.", color = TextMuted, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("SAI DA QUADRA", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            activeLineup.forEach { player ->
                PlayerChoiceRow(player = player, selected = selectedOutId == player.id, onClick = { onSelectOut(player.id) })
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("ENTRA EM QUADRA", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            if (bench.isEmpty()) {
                Text("Nenhum reserva disponível.", color = TextMuted, fontSize = 10.sp)
            } else {
                bench.forEach { player ->
                    PlayerChoiceRow(player = player, selected = selectedInId == player.id, onClick = { onSelectIn(player.id) })
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onConfirm,
                enabled = selectedOutId != null && selectedInId != null,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BasketOrange),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("CONFIRMAR TROCA", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PlayerChoiceRow(
    player: Player,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (selected) BasketOrange.copy(alpha = 0.28f) else CourtDeepSlate)
            .border(1.dp, if (selected) BasketOrange else CourtBorder, RoundedCornerShape(7.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(player.name, color = TextWhite, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
        Text("${player.position} • OVR ${player.overall}", color = if (selected) ChampionshipGold else TextMuted, fontSize = 10.sp)
    }
}
