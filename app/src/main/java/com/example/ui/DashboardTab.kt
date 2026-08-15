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

fun DashboardTab(viewModel: GameViewModel) {
    val context = LocalContext.current
    val currentSeason = viewModel.season ?: return
    val team = viewModel.managedTeam ?: return
    val (opponent, isHome) = viewModel.getNextOpponent()

    var showLiveDialog by remember { mutableStateOf(false) }

    // Whole Season Simulation Progress Dialog collected via StateFlow with WhileSubscribed(5000)
    val seasonSimProgress by viewModel.seasonSimulationProgressFlow.collectAsStateWithLifecycle()
    if (seasonSimProgress != null) {
        val progressState = seasonSimProgress!!
        val currentDay = progressState.first
        val totalDays = progressState.second
        val pct = currentDay.toFloat() / totalDays.toFloat()
        
        val record = currentSeason.standings[team.name]
        val wins = record?.wins ?: 0
        val losses = record?.losses ?: 0
        
        Dialog(onDismissRequest = { /* Don't dismiss until finished */ }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Simula",
                        tint = ChampionshipGold,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SIMULANDO TEMPORADA",
                        fontWeight = FontWeight.Bold,
                        color = BasketOrange,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val dayIdx = (currentDay - 1).coerceIn(0, 81)
                    val matchups = viewModel.getMatchupsForDay(dayIdx)
                    val userMatchup = matchups.find { it.first.name == team.name || it.second.name == team.name }
                    val matchupText = if (userMatchup != null) {
                        "${userMatchup.first.name} vs ${userMatchup.second.name}"
                    } else {
                        "Simulando rodada..."
                    }
                    val phaseText = when {
                        currentDay <= 20 -> "Temporada Regular - Fase Inicial 🏁"
                        currentDay <= 45 -> "Temporada Regular - Fase Intermediária 🏀"
                        currentDay <= 70 -> "Temporada Regular - Reta Final ⚡"
                        currentDay <= 82 -> "Temporada Regular - Rodadas Decisivas 🔥"
                        else -> "Temporada Regular Concluída 🏆"
                    }

                    Text(
                        text = phaseText,
                        color = ChampionshipGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Rodada $currentDay de $totalDays",
                        color = TextWhite,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Partida: $matchupText",
                        color = TextGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LinearProgressIndicator(
                        progress = { pct },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = ChampionshipGold,
                        trackColor = CourtLightSlate
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Estatísticas de Campanha do ${team.name}:",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$wins Vitórias",
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text("  •  ", color = TextGray, fontSize = 16.sp)
                        Text(
                            text = "$losses Derrotas",
                            color = ErrorRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Orçamento Atual: $${String.format("%,d", viewModel.finances?.budget ?: 0).replace(',', '.')}",
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 64.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Live Simulation Dialog Trigger
            if (showLiveDialog) {
                PartidaDialog(viewModel = viewModel, onDismiss = { showLiveDialog = false })
            }
        }

        item {
            // Quick Management Central Grid
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "CENTRAL DE COMANDO DA FRANQUIA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BasketOrange
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.showStaffScreen = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = CourtLightSlate),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("👔", fontSize = 18.sp)
                                Text("Comissão", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            }
                        }

                        Button(
                            onClick = { viewModel.showFacilitiesScreen = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = CourtLightSlate),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🏛️", fontSize = 18.sp)
                                Text("Instalações", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            }
                        }

                        Button(
                            onClick = { viewModel.showFinanceAdvancedScreen = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = CourtLightSlate),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("💰", fontSize = 18.sp)
                                Text("Finanças", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.showNewsFeedScreen = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = CourtLightSlate),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📰", fontSize = 18.sp)
                                Text("Notícias", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            }
                        }

                        Button(
                            onClick = {
                                if (viewModel.latestBoxScore != null) {
                                    viewModel.showBoxScoreScreen = true
                                } else {
                                    ToastUtils.showToast(context, "Box Score estará disponível após a 1ª partida!")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = CourtLightSlate),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📊", fontSize = 18.sp)
                                Text("Box Score", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            }
                        }
                    }
                }
            }
        }

        item {
            // Next Match Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PRÓXIMO COMPROMISSO",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "vs ${opponent.name}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = TextWhite
                            )
                            Text(
                                text = if (isHome) "Em Casa • ${team.arena.name}" else "Fora • ${opponent.arena.name}",
                                fontSize = 13.sp,
                                color = TextGray
                            )
                            Text(
                                text = "Oponente OVR Médio: ${opponent.getAverageOverall().toInt()}",
                                fontSize = 13.sp,
                                color = ChampionshipGold,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.SportsBasketball,
                            contentDescription = "Basket",
                            tint = BasketOrange,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Match Simulation Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                showLiveDialog = true
                            },
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(containerColor = BasketOrange)
                        ) {
                            Icon(imageVector = Icons.Default.SportsBasketball, contentDescription = "Simular", tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SIMULAR PARTIDA", color = Color.White, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.simulateDayInstant(context)
                                ToastUtils.showToast(context, "Jogo simulado instantaneamente!")
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = CourtLightSlate)
                        ) {
                            Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Instant", tint = TextWhite)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("FINALIZAR", color = TextWhite, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.simulateSeasonRemaining(context)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = seasonSimProgress == null,
                        colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FastForward, contentDescription = "Season", tint = CourtMidnight)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SIMULAR ATÉ FIM DA TEMPORADA 🏆", color = CourtMidnight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Coach info & managed stats
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PERFIL DE TREINADOR",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = viewModel.coach?.name ?: "Técnico Wilson",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Ataque", color = TextGray, fontSize = 11.sp)
                            Text("${viewModel.coach?.offensiveSkill ?: 50}", fontWeight = FontWeight.Bold, color = BasketOrange, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    val ok = viewModel.upgradeCoachSkill("offensive")
                                    if (ok) ToastUtils.showToast(context, "Ataque do Treinador +1!")
                                    else ToastUtils.showToast(context, "Saldo insuficiente ($500k necessário) ou máximo atingido!")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BasketOrange),
                                contentPadding = PaddingValues(4.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("+1 ($500k)", fontSize = 9.sp, color = Color.White)
                            }
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Defesa", color = TextGray, fontSize = 11.sp)
                            Text("${viewModel.coach?.defensiveSkill ?: 50}", fontWeight = FontWeight.Bold, color = BasketOrange, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    val ok = viewModel.upgradeCoachSkill("defensive")
                                    if (ok) ToastUtils.showToast(context, "Defesa do Treinador +1!")
                                    else ToastUtils.showToast(context, "Saldo insuficiente ($500k necessário) ou máximo atingido!")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BasketOrange),
                                contentPadding = PaddingValues(4.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("+1 ($500k)", fontSize = 9.sp, color = Color.White)
                            }
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Motivação", color = TextGray, fontSize = 11.sp)
                            Text("${viewModel.coach?.motivationalSkill ?: 50}", fontWeight = FontWeight.Bold, color = BasketOrange, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    val ok = viewModel.upgradeCoachSkill("motivational")
                                    if (ok) ToastUtils.showToast(context, "Motivação do Treinador +1!")
                                    else ToastUtils.showToast(context, "Saldo insuficiente ($500k necessário) ou máximo atingido!")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BasketOrange),
                                contentPadding = PaddingValues(4.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("+1 ($500k)", fontSize = 9.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Latest Match Result
        viewModel.latestResult?.let { res ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ÚLTIMA PARTIDA",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChampionshipGold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(res.homeTeam.name, fontWeight = FontWeight.Bold, color = TextWhite, modifier = Modifier.weight(1f))
                            Text(
                                text = "${res.homeScore} - ${res.awayScore}",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = BasketOrange,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Text(res.awayTeam.name, fontWeight = FontWeight.Bold, color = TextWhite, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Público: ${res.attendance} torcedores • Arena: ${res.homeTeam.arena.name}",
                            fontSize = 11.sp,
                            color = TextGray
                        )
                        
                        if (res.injuries.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Lesões: " + res.injuries.joinToString { "${it.player.name} (${it.daysOut} dias)" },
                                color = ErrorRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.clearSavedGame(context) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                border = BorderStroke(1.dp, ErrorRed)
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("RESETAR CARREIRA / NOVO JOGO")
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

