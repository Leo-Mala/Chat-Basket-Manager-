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

fun HistoricalPerformanceChart(history: List<SeasonHistory>, teamName: String, currentWins: Int, currentSeasonNum: Int) {
    val dataPoints = remember(history, currentWins, currentSeasonNum) {
        val pts = mutableListOf<Pair<String, Float>>()
        if (history.isEmpty()) {
            pts.add("S0" to 41f)
            pts.add("S1 (At)" to currentWins.toFloat())
        } else {
            history.forEach { h ->
                val wins = h.teamWins[teamName] ?: if (h.champion == teamName) 55 else 35
                pts.add("S${h.seasonNumber}" to wins.toFloat())
            }
            pts.add("S$currentSeasonNum (At)" to currentWins.toFloat())
        }
        pts
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "GRÁFICO DE EVOLUÇÃO TEMPORAL 📈",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ChampionshipGold
            )
            Text(
                text = "Vitórias por Temporada (Regular + Playoffs)",
                fontSize = 11.sp,
                color = TextGray
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(horizontal = 8.dp)
            ) {
                val width = size.width
                val height = size.height
                val padding = 20f
                
                val chartWidth = width - padding * 2
                val chartHeight = height - padding * 2
                
                val maxVal = 82f
                val minVal = 0f
                
                val stepX = chartWidth / (dataPoints.size - 1).coerceAtLeast(1)
                
                val points = dataPoints.mapIndexed { idx, pair ->
                    val x = padding + idx * stepX
                    val ratio = (pair.second - minVal) / (maxVal - minVal)
                    val y = padding + chartHeight - (ratio * chartHeight)
                    androidx.compose.ui.geometry.Offset(x, y)
                }
                
                val gridLines = 3
                for (i in 0..gridLines) {
                    val yRatio = i.toFloat() / gridLines.toFloat()
                    val y = padding + yRatio * chartHeight
                    drawLine(
                        color = CourtLightSlate.copy(alpha = 0.3f),
                        start = androidx.compose.ui.geometry.Offset(padding, y),
                        end = androidx.compose.ui.geometry.Offset(width - padding, y),
                        strokeWidth = 2f
                    )
                }
                
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = BasketOrange,
                        start = points[i],
                        end = points[i+1],
                        strokeWidth = 6f
                    )
                }
                
                points.forEachIndexed { idx, offset ->
                    drawCircle(
                        color = ChampionshipGold,
                        radius = 8f,
                        center = offset
                    )
                    drawCircle(
                        color = CourtMidnight,
                        radius = 4f,
                        center = offset
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dataPoints.forEach { pair ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(pair.first, fontSize = 9.sp, color = TextWhite, fontWeight = FontWeight.Bold)
                        Text("${pair.second.toInt()} V", fontSize = 9.sp, color = BasketOrange)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryTab(viewModel: GameViewModel) {
    val history = viewModel.historyManager.seasons
    var subTabIdx by remember { mutableStateOf(0) } // 0: Champions, 1: Season Matches
    val teamName = viewModel.managedTeam?.name ?: ""
    val currentWins = viewModel.season?.standings?.get(teamName)?.wins ?: 0
    val currentSeasonNum = viewModel.season?.seasonNumber ?: 1
    var selectedSeasonForStats by remember { mutableStateOf<SeasonHistory?>(null) }

    fun formatSeasonYear(num: Int): String {
        val start = 24 + num
        val end = start + 1
        return "$start/$end"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = subTabIdx,
            containerColor = CourtDeepSlate,
            contentColor = BasketOrange
        ) {
            Tab(
                selected = subTabIdx == 0,
                onClick = { subTabIdx = 0 },
                text = { Text("Campeões da Liga 🏆", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = subTabIdx == 1,
                onClick = { subTabIdx = 1 },
                text = { Text("Jogos da Temporada 🏀", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (subTabIdx == 0) {
            HistoricalPerformanceChart(
                history = history,
                teamName = teamName,
                currentWins = currentWins,
                currentSeasonNum = currentSeasonNum
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = CourtDeepSlate)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "LIGA HALL OF FAME",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChampionshipGold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Histórico de Campeões. Toque em uma temporada para ver as estatísticas dos seus jogadores!",
                        color = TextWhite,
                        fontSize = 13.sp
                    )
                }
            }

            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = "Trophy", tint = TextGray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nenhuma temporada concluída ainda.",
                            color = TextGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 64.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(history.reversed()) { h ->
                        val yr = formatSeasonYear(h.seasonNumber)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSeasonForStats = h },
                            colors = CardDefaults.cardColors(containerColor = CourtDeepSlate)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Temporada #${h.seasonNumber} ($yr)",
                                        fontWeight = FontWeight.Black,
                                        color = ChampionshipGold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(CourtLightSlate, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Ver Estatísticas 📊",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextWhite
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Campeão: ${h.champion}",
                                        fontWeight = FontWeight.Bold,
                                        color = BasketOrange,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("MVP dos Playoffs: ${h.mvp}", color = TextWhite, fontSize = 12.sp)
                                Text("Cestinha Geral: ${h.topScorer} (${h.topScorerPoints.toInt()} pts)", color = TextWhite, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        } else {
            val s = viewModel.season
            val gamesHistory = s?.history ?: emptyList()
            
            if (gamesHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.SportsBasketball, contentDescription = "History", tint = TextGray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nenhuma partida disputada nesta temporada ainda.",
                            color = TextGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 64.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(gamesHistory.reversed()) { res ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CourtDeepSlate)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = res.homeTeam.name,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${res.homeScore} - ${res.awayScore}",
                                        fontWeight = FontWeight.Black,
                                        color = BasketOrange,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    Text(
                                        text = res.awayTeam.name,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.End
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Arena: ${res.homeTeam.arena.name} • Público: ${String.format("%,d", res.attendance).replace(',', '.')} torcedores",
                                    fontSize = 11.sp,
                                    color = TextGray
                                )
                                
                                // Calculate Top Performer of the game
                                val allStats = res.homeStats + res.awayStats
                                val bestPair = allStats.maxByOrNull { it.value.points + it.value.rebounds * 1.2 + it.value.assists * 1.5 }
                                if (bestPair != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Destaque: ${bestPair.key.name} (${bestPair.value.points} pts, ${bestPair.value.rebounds} reb, ${bestPair.value.assists} ast)",
                                        fontSize = 11.sp,
                                        color = ChampionshipGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedSeasonForStats != null) {
        val h = selectedSeasonForStats!!
        val yr = formatSeasonYear(h.seasonNumber)
        Dialog(onDismissRequest = { selectedSeasonForStats = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ESTATÍSTICAS TEMP. $yr",
                            fontWeight = FontWeight.Bold,
                            color = BasketOrange,
                            fontSize = 16.sp
                        )
                        IconButton(onClick = { selectedSeasonForStats = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint = TextWhite
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val myWins = h.teamWins[teamName] ?: 0
                    val myLosses = 82 - myWins
                    Text(
                        text = "Campanha do seu time: $myWins Vitórias - $myLosses Derrotas",
                        color = ChampionshipGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (h.playerStats.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Estatísticas dos jogadores não gravadas nesta temporada.\n(Dados só ficam salvos para novas temporadas concluídas)",
                                color = TextGray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            text = "ESTATÍSTICAS DO SEU ELENCO",
                            fontSize = 11.sp,
                            color = TextGray,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(h.playerStats.sortedByDescending { it.seasonPoints }) { p ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = CourtLightSlate)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            when (p.position) {
                                                                "PG" -> BasketOrange
                                                                "SG" -> SuccessGreen
                                                                "SF" -> LightAccent
                                                                "PF" -> ChampionshipGold
                                                                "C" -> ErrorRed
                                                                else -> BasketOrange
                                                            },
                                                            CircleShape
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = p.position,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 9.sp,
                                                        color = Color.White
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = p.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = TextWhite
                                                )
                                            }
                                            Text(
                                                text = "OVR ${p.overall}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = ChampionshipGold
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(CourtMidnight.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                                .padding(6.dp),
                                            horizontalArrangement = Arrangement.SpaceAround
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Jogos", fontSize = 9.sp, color = TextGray)
                                                Text("${p.seasonGames}", fontSize = 11.sp, color = TextWhite, fontWeight = FontWeight.Bold)
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("PPG", fontSize = 9.sp, color = TextGray)
                                                Text(
                                                    text = String.format("%.1f", if (p.seasonGames == 0) 0f else p.seasonPoints.toFloat() / p.seasonGames),
                                                    fontSize = 11.sp,
                                                    color = TextWhite,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("RPG", fontSize = 9.sp, color = TextGray)
                                                Text(
                                                    text = String.format("%.1f", if (p.seasonGames == 0) 0f else p.seasonRebounds.toFloat() / p.seasonGames),
                                                    fontSize = 11.sp,
                                                    color = TextWhite,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("APG", fontSize = 9.sp, color = TextGray)
                                                Text(
                                                    text = String.format("%.1f", if (p.seasonGames == 0) 0f else p.seasonAssists.toFloat() / p.seasonGames),
                                                    fontSize = 11.sp,
                                                    color = TextWhite,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("SPG", fontSize = 9.sp, color = TextGray)
                                                Text(
                                                    text = String.format("%.1f", if (p.seasonGames == 0) 0f else p.seasonSteals.toFloat() / p.seasonGames),
                                                    fontSize = 11.sp,
                                                    color = TextWhite,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("BPG", fontSize = 9.sp, color = TextGray)
                                                Text(
                                                    text = String.format("%.1f", if (p.seasonGames == 0) 0f else p.seasonBlocks.toFloat() / p.seasonGames),
                                                    fontSize = 11.sp,
                                                    color = TextWhite,
                                                    fontWeight = FontWeight.Bold
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
        }
    }
}

@Composable
