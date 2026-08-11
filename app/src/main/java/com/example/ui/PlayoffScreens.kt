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

fun PlayoffScreen(viewModel: GameViewModel) {
    val context = LocalContext.current
    val currentSeason = viewModel.season ?: return
    val (east, west) = remember { currentSeason.getPlayoffTeams() }

    var playoffChoice by remember { mutableStateOf<String?>(null) } // null = choose, "AUTO" = auto-sim, "PLAY" = interactive
    var simStatusText by remember { mutableStateOf("Preparando chaveamento dos Playoffs...") }
    var simProgress by remember { mutableStateOf(0.1f) }

    // Interactive step state
    var interactiveStep by remember { mutableStateOf(0) } // 0: Quartas, 1: Semis, 2: Finais Conf, 3: Grande Final
    var quarterResults by remember { mutableStateOf<List<Season.SeriesResult>?>(null) }
    var semiResults by remember { mutableStateOf<List<Season.SeriesResult>?>(null) }
    var confFinalResults by remember { mutableStateOf<List<Season.SeriesResult>?>(null) }

    LaunchedEffect(playoffChoice) {
        if (playoffChoice == "AUTO") {
            delay(1500)
            simStatusText = "Simulando Quartas de Final de Conferência..."
            simProgress = 0.3f
            delay(1500)
            simStatusText = "Simulando Semifinais de Conferência..."
            simProgress = 0.5f
            delay(1500)
            simStatusText = "Simulando Finais de Conferência..."
            simProgress = 0.7f
            delay(1500)
            simStatusText = "Simulando as Grandes Finais da NBA..."
            simProgress = 0.9f
            delay(1500)
            simProgress = 1.0f
            viewModel.simulatePlayoffsInteractive(context)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(CourtMidnight, CourtDeepSlate)
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Trophy Icon",
                    tint = ChampionshipGold,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "PLAYOFFS DA NBA",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = ChampionshipGold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "A temporada regular acabou! Os 8 melhores de cada conferência disputam o título em séries melhor de 7.",
                    fontSize = 13.sp,
                    color = TextWhite,
                    textAlign = TextAlign.Center
                )
            }

            // Choice card: Auto-Simulate vs Play Playoffs
            if (playoffChoice == null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🏆 TEMPORADA REGULAR FINALIZADA!",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChampionshipGold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Como você deseja prosseguir na fase de Playoffs?",
                                fontSize = 13.sp,
                                color = TextWhite,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { playoffChoice = "AUTO" },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(vertical = 12.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(imageVector = Icons.Default.FastForward, contentDescription = "Simular", tint = CourtMidnight)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("CONTINUAR SIMULANDO", color = CourtMidnight, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("Simula até a Final", color = CourtMidnight.copy(alpha = 0.8f), fontSize = 9.sp)
                                    }
                                }

                                Button(
                                    onClick = { playoffChoice = "PLAY" },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = BasketOrange),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(vertical = 12.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(imageVector = Icons.Default.SportsBasketball, contentDescription = "Jogar", tint = Color.White)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("JOGAR OS PLAYOFFS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("Rodada por Rodada", color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Automatic Simulation Loading indicator
            if (playoffChoice == "AUTO") {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🏆 SIMULANDO PLAYOFFS AUTOMATICAMENTE",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChampionshipGold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = simStatusText,
                                fontSize = 13.sp,
                                color = TextWhite,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { simProgress },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = BasketOrange,
                                trackColor = CourtLightSlate
                            )
                        }
                    }
                }
            }

            // Interactive Step-by-step Playoff mode
            if (playoffChoice == "PLAY") {
                item {
                    InteractivePlayoffHub(
                        currentSeason = currentSeason,
                        viewModel = viewModel,
                        east = east,
                        west = west,
                        context = context,
                        onFinishPlayoffs = { res ->
                            viewModel.finishPlayoffsWithResult(res)
                        }
                    )
                }
            }

            // Regular Season Awards Block
            viewModel.currentAwards?.let { awards ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CourtLightSlate),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🏆 PRÊMIOS DA TEMPORADA INDIVIDUAL",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = ChampionshipGold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("MVP da Liga:", color = TextGray)
                                Text("${awards.mvp.name} (OVR ${awards.mvp.overall})", color = BasketOrange, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Melhor Defensor:", color = TextGray)
                                Text("${awards.defensivePlayer.name}", color = TextWhite, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Sexto Homem:", color = TextGray)
                                Text("${awards.sixthMan.name}", color = TextWhite, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Rookie do Ano:", color = TextGray)
                                Text("${awards.rookieOfYear.name}", color = TextWhite, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Jogador que mais evoluiu:", color = TextGray)
                                Text("${awards.mostImproved.name}", color = TextWhite, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Técnico do Ano (COY):", color = TextGray)
                                Text("${awards.coachOfYearName} (${awards.coachOfYearTeam})", color = ChampionshipGold, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CourtLightSlate),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "EQUIPES CLASSIFICADAS PARA O PLAYOFF",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = BasketOrange
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Conferência Leste", color = ChampionshipGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                east.forEachIndexed { i, t ->
                                    Text("${i+1}. ${t.name}", color = TextWhite, fontSize = 12.sp)
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Conferência Oeste", color = ChampionshipGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                west.forEachIndexed { i, t ->
                                    Text("${i+1}. ${t.name}", color = TextWhite, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Immutable
@Stable
data class ActivePlayoffGameData(
    val homeTeam: NbaTeam,
    val awayTeam: NbaTeam,
    val seriesIdx: Int,
    val title: String
)

fun updateSeriesWithGame(
    currentRes: Season.SeriesResult?,
    team1: NbaTeam,
    team2: NbaTeam,
    roundName: String,
    gameResult: GameSimulator.GameResult,
    isFinals: Boolean = false
): Season.SeriesResult {
    val existingGames = currentRes?.games ?: emptyList()
    var wins1 = currentRes?.team1Wins ?: 0
    var wins2 = currentRes?.team2Wins ?: 0

    val newGames = existingGames + gameResult
    val homeIsTeam1 = gameResult.homeTeam.name == team1.name
    val team1Score = if (homeIsTeam1) gameResult.homeScore else gameResult.awayScore
    val team2Score = if (homeIsTeam1) gameResult.awayScore else gameResult.homeScore

    if (team1Score > team2Score) wins1++ else wins2++

    val winner = if (wins1 > wins2) team1 else team2
    val mvp = if (isFinals && (wins1 == 4 || wins2 == 4)) {
        val stats = newGames.flatMap { it.homeStats.entries + it.awayStats.entries }
        val playerPoints = stats.groupBy { it.key }.mapValues { it.value.sumOf { s -> s.value.points } }
        playerPoints.maxByOrNull { it.value }?.key
    } else null

    return Season.SeriesResult(
        winner = winner,
        games = newGames,
        roundName = roundName,
        mvp = mvp,
        team1 = team1,
        team2 = team2,
        team1Wins = wins1,
        team2Wins = wins2
    )
}

@Composable
fun InteractivePlayoffHub(
    currentSeason: Season,
    viewModel: GameViewModel,
    east: List<NbaTeam>,
    west: List<NbaTeam>,
    context: Context,
    onFinishPlayoffs: (Season.PlayoffResult) -> Unit
) {
    var interactiveStep by remember { mutableIntStateOf(0) } // 0: Quartas, 1: Semis, 2: Finais Conf, 3: Grande Final
    var confFilter by remember { mutableStateOf("ALL") } // "ALL", "EAST", "WEST"
    var expandedSeriesSet by remember { mutableStateOf<Set<String>>(emptySet()) }
    var activePlayoffGame by remember { mutableStateOf<ActivePlayoffGameData?>(null) }

    // Results lists for each round (stateful lists so UI recomposes on single series simulation)
    val quarterResults = remember { mutableStateListOf<Season.SeriesResult?>().apply { repeat(8) { add(null) } } }
    val semiResults = remember { mutableStateListOf<Season.SeriesResult?>().apply { repeat(4) { add(null) } } }
    val confFinalResults = remember { mutableStateListOf<Season.SeriesResult?>().apply { repeat(2) { add(null) } } }
    var finalsResult by remember { mutableStateOf<Season.SeriesResult?>(null) }

    // Pairings definition for each round
    val quartasPairings = remember(east, west) {
        listOf(
            Triple(east[0], east[7], "Quartas Leste"),
            Triple(east[1], east[6], "Quartas Leste"),
            Triple(east[2], east[5], "Quartas Leste"),
            Triple(east[3], east[4], "Quartas Leste"),
            Triple(west[0], west[7], "Quartas Oeste"),
            Triple(west[1], west[6], "Quartas Oeste"),
            Triple(west[2], west[5], "Quartas Oeste"),
            Triple(west[3], west[4], "Quartas Oeste")
        )
    }

    val semiPairings = remember(quarterResults.toList()) {
        if (quarterResults.all { it != null && (it.team1Wins == 4 || it.team2Wins == 4) }) {
            listOf(
                Triple(quarterResults[0]!!.winner, quarterResults[1]!!.winner, "Semifinal Leste"),
                Triple(quarterResults[2]!!.winner, quarterResults[3]!!.winner, "Semifinal Leste"),
                Triple(quarterResults[4]!!.winner, quarterResults[5]!!.winner, "Semifinal Oeste"),
                Triple(quarterResults[6]!!.winner, quarterResults[7]!!.winner, "Semifinal Oeste")
            )
        } else emptyList()
    }

    val confFinalPairings = remember(semiResults.toList()) {
        if (semiResults.all { it != null && (it.team1Wins == 4 || it.team2Wins == 4) }) {
            listOf(
                Triple(semiResults[0]!!.winner, semiResults[1]!!.winner, "Final Leste"),
                Triple(semiResults[2]!!.winner, semiResults[3]!!.winner, "Final Oeste")
            )
        } else emptyList()
    }

    val myTeam = viewModel.managedTeam

    // Interactive Match Dialog Modal
    activePlayoffGame?.let { activeGame ->
        PartidaDialog(
            viewModel = viewModel,
            homeTeamOverride = activeGame.homeTeam,
            awayTeamOverride = activeGame.awayTeam,
            gameTitle = activeGame.title,
            onGameFinished = { gameResult ->
                val eastChamp = confFinalResults.getOrNull(0)?.winner
                val westChamp = confFinalResults.getOrNull(1)?.winner

                val pairing = when (interactiveStep) {
                    0 -> quartasPairings[activeGame.seriesIdx]
                    1 -> semiPairings[activeGame.seriesIdx]
                    2 -> confFinalPairings[activeGame.seriesIdx]
                    else -> Triple(eastChamp!!, westChamp!!, "Finais da NBA")
                }
                val currentRes = when (interactiveStep) {
                    0 -> quarterResults[activeGame.seriesIdx]
                    1 -> semiResults[activeGame.seriesIdx]
                    2 -> confFinalResults[activeGame.seriesIdx]
                    else -> finalsResult
                }
                val newRes = updateSeriesWithGame(
                    currentRes = currentRes,
                    team1 = pairing.first,
                    team2 = pairing.second,
                    roundName = pairing.third,
                    gameResult = gameResult,
                    isFinals = (interactiveStep == 3)
                )
                when (interactiveStep) {
                    0 -> quarterResults[activeGame.seriesIdx] = newRes
                    1 -> semiResults[activeGame.seriesIdx] = newRes
                    2 -> confFinalResults[activeGame.seriesIdx] = newRes
                    3 -> finalsResult = newRes
                }
                activePlayoffGame = null
            },
            onDismiss = {
                activePlayoffGame = null
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. PLAYOFF STEPPER HEADER ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "ETAPAS DOS PLAYOFFS 🏆",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChampionshipGold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val steps = listOf("Quartas", "Semis", "Final Conf.", "Final NBA")
                    steps.forEachIndexed { idx, label ->
                        val isDone = idx < interactiveStep
                        val isActive = idx == interactiveStep
                        val bgColor = when {
                            isDone -> SuccessGreen
                            isActive -> BasketOrange
                            else -> CourtLightSlate
                        }
                        val textColor = when {
                            isDone || isActive -> Color.White
                            else -> TextGray
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(bgColor),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDone) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                } else {
                                    Text(
                                        text = "${idx + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = textColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // --- 2. USER TEAM SPOTLIGHT CARD (If playing in current stage) ---
        val currentPairings = when (interactiveStep) {
            0 -> quartasPairings
            1 -> semiPairings
            2 -> confFinalPairings
            else -> emptyList()
        }

        val currentResults = when (interactiveStep) {
            0 -> quarterResults
            1 -> semiResults
            2 -> confFinalResults
            else -> emptyList()
        }

        val mySeriesIdx = currentPairings.indexOfFirst {
            it.first.name == myTeam?.name || it.second.name == myTeam?.name
        }

        if (myTeam != null && mySeriesIdx != -1 && interactiveStep < 3) {
            val pairing = currentPairings[mySeriesIdx]
            val opponent = if (pairing.first.name == myTeam.name) pairing.second else pairing.first
            val result = currentResults.getOrNull(mySeriesIdx)
            val isSeriesFinished = (result != null && (result.team1Wins == 4 || result.team2Wins == 4))
            val currentGameCount = result?.games?.size ?: 0
            val nextGameNum = currentGameCount + 1

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                border = BorderStroke(2.dp, ChampionshipGold),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Stars, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SEU TIME EM QUADRA", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ChampionshipGold)
                        }
                        Box(
                            modifier = Modifier
                                .background(BasketOrange, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("SÉRIE DE PLAYOFFS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val team1Wins = result?.team1Wins ?: 0
                    val team2Wins = result?.team2Wins ?: 0
                    val myWins = if (pairing.first.name == myTeam.name) team1Wins else team2Wins
                    val oppWins = if (pairing.first.name == myTeam.name) team2Wins else team1Wins

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CourtMidnight.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(myTeam.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextWhite)
                            Text("OVR ${myTeam.overall}", fontSize = 10.sp, color = ChampionshipGold)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$myWins x $oppWins", fontSize = 20.sp, fontWeight = FontWeight.Black, color = BasketOrange)
                            if (isSeriesFinished) {
                                val isMyWinner = result?.winner?.name == myTeam.name
                                Text(
                                    if (isMyWinner) "CLASSIFICADO! 🏆" else "ELIMINADO ❌",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMyWinner) SuccessGreen else ErrorRed
                                )
                            } else {
                                Text("JOGO $nextGameNum", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextGray)
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(opponent.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextWhite)
                            Text("OVR ${opponent.overall}", fontSize = 10.sp, color = ChampionshipGold)
                        }
                    }

                    if (!isSeriesFinished) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val homeTeam = if (nextGameNum in listOf(1, 2, 5, 7)) pairing.first else pairing.second
                        val awayTeam = if (homeTeam.name == pairing.first.name) pairing.second else pairing.first

                        Button(
                            onClick = {
                                activePlayoffGame = ActivePlayoffGameData(
                                    homeTeam = homeTeam,
                                    awayTeam = awayTeam,
                                    seriesIdx = mySeriesIdx,
                                    title = "${pairing.third} • Jogo $nextGameNum ($team1Wins x $team2Wins)"
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = BasketOrange),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.SportsBasketball, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("🏀 INICIAR PARTIDA (JOGO $nextGameNum DA SÉRIE)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val sim = GameSimulator(context.applicationContext, viewModel.simulationConfig())
                                    val gameRes = sim.simulate(homeTeam, awayTeam)
                                    val newRes = updateSeriesWithGame(result, pairing.first, pairing.second, pairing.third, gameRes)
                                    when (interactiveStep) {
                                        0 -> quarterResults[mySeriesIdx] = newRes
                                        1 -> semiResults[mySeriesIdx] = newRes
                                        2 -> confFinalResults[mySeriesIdx] = newRes
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, BasketOrange),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = BasketOrange, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Simular 1 Jogo", fontSize = 11.sp, color = BasketOrange, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val res = currentSeason.simulateSeries(pairing.first, pairing.second, pairing.third, context)
                                    when (interactiveStep) {
                                        0 -> quarterResults[mySeriesIdx] = res
                                        1 -> semiResults[mySeriesIdx] = res
                                        2 -> confFinalResults[mySeriesIdx] = res
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = CourtLightSlate),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.FastForward, contentDescription = null, tint = TextWhite, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Simular Série Toda", fontSize = 11.sp, color = TextWhite)
                            }
                        }
                    }
                }
            }
        }

        // --- 3. STAGE TITLE AND FILTER / GLOBAL ACTIONS ---
        if (interactiveStep < 3) {
            val stageTitle = when (interactiveStep) {
                0 -> "QUARTAS DE FINAL (8 CONFRONTOS)"
                1 -> "SEMIFINAIS DE CONFERÊNCIA (4 CONFRONTOS)"
                else -> "FINAIS DE CONFERÊNCIA (2 CONFRONTOS)"
            }

            val isAllStageDone = currentResults.isNotEmpty() && currentResults.all { it != null }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stageTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChampionshipGold,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Global action button
                        if (!isAllStageDone) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    currentPairings.forEachIndexed { idx, p ->
                                        if (currentResults[idx] == null) {
                                            val res = currentSeason.simulateSeries(p.first, p.second, p.third, context)
                                            when (interactiveStep) {
                                                0 -> quarterResults[idx] = res
                                                1 -> semiResults[idx] = res
                                                2 -> confFinalResults[idx] = res
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BasketOrange),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SIMULAR TODAS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Conference filter tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("ALL" to "Todas", "EAST" to "🔵 Leste", "WEST" to "🔴 Oeste").forEach { (code, label) ->
                            val isSel = confFilter == code
                            FilterChip(
                                selected = isSel,
                                onClick = { confFilter = code },
                                label = { Text(label, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BasketOrange,
                                    selectedLabelColor = Color.White,
                                    containerColor = CourtLightSlate,
                                    labelColor = TextGray
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Matchup Cards List
                    currentPairings.forEachIndexed { idx, pairing ->
                        val conf = if (pairing.third.contains("Leste")) "EAST" else "WEST"
                        if (confFilter == "ALL" || confFilter == conf) {
                            val res = currentResults.getOrNull(idx)
                            val seriesKey = "step${interactiveStep}_series$idx"
                            val isExpanded = expandedSeriesSet.contains(seriesKey)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = CourtLightSlate),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(if (conf == "EAST") Color(0xFF1D4ED8) else Color(0xFFB91C1C), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(if (conf == "EAST") "LESTE" else "OESTE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }

                                        if (res != null) {
                                            Text(
                                                text = "🏆 Vencedor: ${res.winner.name}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ChampionshipGold
                                            )
                                        } else {
                                            Text("Série Melhor de 7", fontSize = 10.sp, color = TextGray)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = pairing.first.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (res?.winner?.name == pairing.first.name) SuccessGreen else TextWhite
                                            )
                                            Text("OVR ${pairing.first.overall}", fontSize = 10.sp, color = TextGray)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .background(CourtMidnight, CircleShape)
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            if (res != null) {
                                                Text("${res.team1Wins} x ${res.team2Wins}", fontWeight = FontWeight.Black, fontSize = 12.sp, color = BasketOrange)
                                            } else {
                                                Text("VS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = BasketOrange)
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = pairing.second.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (res?.winner?.name == pairing.second.name) SuccessGreen else TextWhite,
                                                textAlign = TextAlign.End
                                            )
                                            Text("OVR ${pairing.second.overall}", fontSize = 10.sp, color = TextGray)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val isFinished = (res != null && (res.team1Wins == 4 || res.team2Wins == 4))
                                        val gameCount = res?.games?.size ?: 0
                                        val nextGameNum = gameCount + 1

                                        if (!isFinished) {
                                            val homeTeam = if (nextGameNum in listOf(1, 2, 5, 7)) pairing.first else pairing.second
                                            val awayTeam = if (homeTeam.name == pairing.first.name) pairing.second else pairing.first

                                            Button(
                                                onClick = {
                                                    activePlayoffGame = ActivePlayoffGameData(
                                                        homeTeam = homeTeam,
                                                        awayTeam = awayTeam,
                                                        seriesIdx = idx,
                                                        title = "${pairing.third} • Jogo $nextGameNum"
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = BasketOrange),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.SportsBasketball, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Iniciar Jogo $nextGameNum", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }

                                            Spacer(modifier = Modifier.width(6.dp))

                                            OutlinedButton(
                                                onClick = {
                                                    val r = currentSeason.simulateSeries(pairing.first, pairing.second, pairing.third, context)
                                                    when (interactiveStep) {
                                                        0 -> quarterResults[idx] = r
                                                        1 -> semiResults[idx] = r
                                                        2 -> confFinalResults[idx] = r
                                                    }
                                                },
                                                border = BorderStroke(1.dp, BasketOrange),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.FastForward, contentDescription = null, tint = BasketOrange, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Simular Série", fontSize = 10.sp, color = BasketOrange, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            TextButton(
                                                onClick = {
                                                    expandedSeriesSet = if (isExpanded) expandedSeriesSet - seriesKey else expandedSeriesSet + seriesKey
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = null,
                                                    tint = TextGray,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(if (isExpanded) "Ocultar Jogos" else "📊 Ver Jogos (${res?.games?.size ?: 0})", fontSize = 10.sp, color = TextGray)
                                            }
                                        }
                                    }

                                    // Expanded Games List
                                    if (res != null && isExpanded) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(CourtMidnight.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                                .padding(8.dp)
                                        ) {
                                            Text("HISTÓRICO DOS JOGOS:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ChampionshipGold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            res.games.forEachIndexed { gIdx, g ->
                                                val homeScorer = g.homeStats.maxByOrNull { it.value.points }
                                                val awayScorer = g.awayStats.maxByOrNull { it.value.points }
                                                val topScorer = if ((homeScorer?.value?.points ?: 0) >= (awayScorer?.value?.points ?: 0)) homeScorer else awayScorer

                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "Jogo ${gIdx + 1}: ${g.homeTeam.name} ${g.homeScore} x ${g.awayScore} ${g.awayTeam.name}",
                                                        fontSize = 10.sp,
                                                        color = TextWhite
                                                    )
                                                    topScorer?.let {
                                                        Text(
                                                            text = "${it.key.name.split(" ").lastOrNull() ?: ""}: ${it.value.points}pts",
                                                            fontSize = 9.sp,
                                                            color = BasketOrange
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Advancement Button when stage is complete
                    if (isAllStageDone) {
                        Spacer(modifier = Modifier.height(14.dp))
                        val nextLabel = when (interactiveStep) {
                            0 -> "AVANÇAR PARA AS SEMIFINAIS DE CONFERÊNCIA ➡️"
                            1 -> "AVANÇAR PARA AS FINAIS DE CONFERÊNCIA 🏆"
                            else -> "DISPUTAR A GRANDE FINAL DA NBA 👑"
                        }

                        Button(
                            onClick = {
                                interactiveStep++
                                confFilter = "ALL"
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text(nextLabel, color = CourtMidnight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // --- 4. STEP 3: FINALS OF THE NBA ARENA ---
        if (interactiveStep == 3) {
            val eastChamp = confFinalResults[0]?.winner
            val westChamp = confFinalResults[1]?.winner

            if (eastChamp != null && westChamp != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                    border = BorderStroke(2.dp, ChampionshipGold),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("GRANDE FINAL DA NBA 👑", fontSize = 18.sp, fontWeight = FontWeight.Black, color = ChampionshipGold)
                        Text("O Confronto Supremo pelo Troféu Larry O'Brien", fontSize = 11.sp, color = TextGray)

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CourtMidnight.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("🔵 LESTE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF60A5FA))
                                Text(eastChamp.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextWhite, textAlign = TextAlign.Center)
                                Text("OVR ${eastChamp.overall}", fontSize = 10.sp, color = ChampionshipGold)
                            }

                            if (finalsResult != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${finalsResult!!.team1Wins} x ${finalsResult!!.team2Wins}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = BasketOrange)
                                    Text("SÉRIE MD7", fontSize = 9.sp, color = TextGray)
                                }
                            } else {
                                Text("VS", fontSize = 18.sp, fontWeight = FontWeight.Black, color = BasketOrange)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("🔴 OESTE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF87171))
                                Text(westChamp.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextWhite, textAlign = TextAlign.Center)
                                Text("OVR ${westChamp.overall}", fontSize = 10.sp, color = ChampionshipGold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val isFinalsFinished = (finalsResult != null && (finalsResult!!.team1Wins == 4 || finalsResult!!.team2Wins == 4))
                        val finalsGameCount = finalsResult?.games?.size ?: 0
                        val finalsNextGameNum = finalsGameCount + 1

                        if (!isFinalsFinished) {
                            val homeTeam = if (finalsNextGameNum in listOf(1, 2, 5, 7)) eastChamp else westChamp
                            val awayTeam = if (homeTeam.name == eastChamp.name) westChamp else eastChamp

                            Button(
                                onClick = {
                                    activePlayoffGame = ActivePlayoffGameData(
                                        homeTeam = homeTeam,
                                        awayTeam = awayTeam,
                                        seriesIdx = 0,
                                        title = "FINAIS DA NBA • Jogo $finalsNextGameNum (${finalsResult?.team1Wins ?: 0} x ${finalsResult?.team2Wins ?: 0})"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = BasketOrange),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.SportsBasketball, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("👑 INICIAR JOGO DA FINAL (JOGO $finalsNextGameNum)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val sim = GameSimulator(context.applicationContext, viewModel.simulationConfig())
                                        val gameRes = sim.simulate(homeTeam, awayTeam)
                                        val newRes = updateSeriesWithGame(finalsResult, eastChamp, westChamp, "Finais da NBA", gameRes, isFinals = true)
                                        finalsResult = newRes
                                    },
                                    modifier = Modifier.weight(1f),
                                    border = BorderStroke(1.dp, ChampionshipGold),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Simular 1 Jogo", fontSize = 11.sp, color = ChampionshipGold, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        finalsResult = currentSeason.simulateSeries(eastChamp, westChamp, "Finais da NBA", context, isFinals = true)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = CourtLightSlate),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.FastForward, contentDescription = null, tint = TextWhite, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Simular Série Toda", fontSize = 11.sp, color = TextWhite)
                                }
                            }
                        } else {
                            val champion = finalsResult!!.winner
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, SuccessGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("🎉 GRANDE CAMPEÃO DA NBA 🎉", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(champion.name, fontSize = 20.sp, fontWeight = FontWeight.Black, color = TextWhite)
                                    finalsResult!!.mvp?.let { mvp ->
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("🏆 MVP das Finais: ${mvp.name} (OVR ${mvp.overall})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ChampionshipGold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Games list of finals
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CourtMidnight.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text("PLACAR DOS JOGOS DA FINAL:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ChampionshipGold)
                                Spacer(modifier = Modifier.height(4.dp))
                                finalsResult!!.games.forEachIndexed { gIdx, g ->
                                    val topScorer = (g.homeStats.entries + g.awayStats.entries).maxByOrNull { it.value.points }
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        colors = CardDefaults.cardColors(containerColor = CourtLightSlate.copy(alpha = 0.35f)),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Jogo ${gIdx + 1}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ChampionshipGold,
                                                modifier = Modifier.width(48.dp)
                                            )
                                            Text(
                                                text = "${g.homeTeam.name} ${g.homeScore} x ${g.awayScore} ${g.awayTeam.name}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextWhite,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            topScorer?.let {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "⭐ ${it.key.name.split(" ").lastOrNull() ?: ""}: ${it.value.points}p",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BasketOrange
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    val fullPlayoffResult = Season.PlayoffResult(
                                        eastChampion = eastChamp,
                                        westChampion = westChamp,
                                        nbaChampion = champion,
                                        mvp = finalsResult!!.mvp,
                                        seriesResults = quarterResults.filterNotNull() + semiResults.filterNotNull() + confFinalResults.filterNotNull() + listOf(finalsResult!!)
                                    )
                                    onFinishPlayoffs(fullPlayoffResult)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Text("👑 CONCLUIR E IR PARA A CELEBRAÇÃO", color = CourtMidnight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

