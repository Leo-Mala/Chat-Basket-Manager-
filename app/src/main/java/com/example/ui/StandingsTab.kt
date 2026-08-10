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

fun StandingsTab(viewModel: GameViewModel) {
    val s = viewModel.season ?: return
    var selectedTabIdx by remember { mutableStateOf(0) } // 0: East, 1: West, 2: Leaders
    var selectedStatTab by remember { mutableStateOf(0) } // 0: Points, 1: Rebounds, 2: Assists

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTabIdx,
            containerColor = CourtDeepSlate,
            contentColor = BasketOrange
        ) {
            Tab(
                selected = selectedTabIdx == 0,
                onClick = { selectedTabIdx = 0 },
                text = { Text("Conferência Leste", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
            )
            Tab(
                selected = selectedTabIdx == 1,
                onClick = { selectedTabIdx = 1 },
                text = { Text("Conferência Oeste", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
            )
            Tab(
                selected = selectedTabIdx == 2,
                onClick = { selectedTabIdx = 2 },
                text = { Text("Líderes da Liga 🌟", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
            )
        }

        AnimatedContent(
            targetState = selectedTabIdx,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { width -> width / 4 } + fadeIn(animationSpec = tween(220)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { width -> -width / 4 } + fadeOut(animationSpec = tween(220)))
                } else {
                    (slideInHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { width -> -width / 4 } + fadeIn(animationSpec = tween(220)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { width -> width / 4 } + fadeOut(animationSpec = tween(220)))
                }
            },
            label = "StandingsTabTransition",
            modifier = Modifier.fillMaxSize()
        ) { tabIdx ->
            if (tabIdx == 0 || tabIdx == 1) {
                // Classificação da Conferência
                val selectedConf = if (tabIdx == 0) "East" else "West"
                val standingsList = remember(s.standings, selectedConf, s.gamesPlayed) {
                    s.getStandings(selectedConf)
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Equipe", color = TextGray, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), fontSize = 12.sp)
                        Text("V - D", color = TextGray, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp)
                        Text("% Vit", color = TextGray, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp)
                        Text("Saldo", color = TextGray, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontSize = 12.sp)
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 64.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(standingsList.zip(1..standingsList.size)) { (pair, rank) ->
                            val teamName = pair.first
                            val record = pair.second
                            val isUserTeam = teamName == viewModel.managedTeam?.name

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUserTeam) BasketOrange.copy(alpha = 0.25f) else CourtDeepSlate
                                ),
                                shape = RoundedCornerShape(8.dp),
                                border = if (isUserTeam) BorderStroke(1.dp, BasketOrange) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$rank.",
                                        color = if (rank <= 8) ChampionshipGold else TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    Text(
                                        text = teamName,
                                        color = TextWhite,
                                        fontWeight = if (isUserTeam) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1.5f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${record.wins} - ${record.losses}",
                                        color = TextWhite,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = String.format("%.3f", record.winRate),
                                        color = TextWhite,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = (if(record.pointDifference > 0) "+" else "") + "${record.pointDifference}",
                                        color = if (record.pointDifference >= 0) SuccessGreen else ErrorRed,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            } else {
                // Líderes Estatísticos de toda a liga (Top 10)
                val allPlayers = remember(s.teams, s.gamesPlayed) {
                    s.teams.flatMap { t -> t.players.map { p -> p to t.name } }
                }

                val rankedList = remember(allPlayers, selectedStatTab) {
                    when (selectedStatTab) {
                        0 -> allPlayers.sortedByDescending { it.first.seasonPoints }.take(10)
                        1 -> allPlayers.sortedByDescending { it.first.seasonRebounds }.take(10)
                        else -> allPlayers.sortedByDescending { it.first.seasonAssists }.take(10)
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Seletores de Estatística (Sub-abas)
                    TabRow(
                        selectedTabIndex = selectedStatTab,
                        containerColor = CourtMidnight,
                        contentColor = ChampionshipGold,
                        indicator = {} // Sem indicador de linha grossa
                    ) {
                        Tab(
                            selected = selectedStatTab == 0,
                            onClick = { selectedStatTab = 0 },
                            text = { Text("🏀 Pontos", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if(selectedStatTab == 0) ChampionshipGold else TextGray) }
                        )
                        Tab(
                            selected = selectedStatTab == 1,
                            onClick = { selectedStatTab = 1 },
                            text = { Text("🛡️ Rebotes", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if(selectedStatTab == 1) ChampionshipGold else TextGray) }
                        )
                        Tab(
                            selected = selectedStatTab == 2,
                            onClick = { selectedStatTab = 2 },
                            text = { Text("🤝 Assistências", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if(selectedStatTab == 2) ChampionshipGold else TextGray) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Jogador / Equipe", color = TextGray, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f), fontSize = 12.sp)
                        Text("Pos", color = TextGray, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center, fontSize = 12.sp)
                        Text("Total", color = TextGray, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontSize = 12.sp)
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 64.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(rankedList) { index, pairItem ->
                            val player = pairItem.first
                            val teamName = pairItem.second
                            val isOurPlayer = teamName == viewModel.managedTeam?.name
                            val statValue = when (selectedStatTab) {
                                0 -> player.seasonPoints
                                1 -> player.seasonRebounds
                                else -> player.seasonAssists
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isOurPlayer) BasketOrange.copy(alpha = 0.2f) else CourtDeepSlate
                                ),
                                shape = RoundedCornerShape(8.dp),
                                border = if (isOurPlayer) BorderStroke(1.dp, BasketOrange) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}.",
                                        color = if (index < 3) ChampionshipGold else TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(24.dp),
                                        fontSize = 14.sp
                                    )
                                    Column(modifier = Modifier.weight(2f)) {
                                        Text(
                                            text = player.name,
                                            color = TextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = teamName,
                                            color = TextGray,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = player.position,
                                        color = BasketOrange,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(0.5f),
                                        textAlign = TextAlign.Center,
                                        fontSize = 12.sp
                                    )
                                    val gCount = player.seasonGames.coerceAtLeast(1)
                                    val perGame = String.format("%.1f", statValue.toFloat() / gCount)
                                    Text(
                                        text = "$statValue ($perGame/j)",
                                        color = ChampionshipGold,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.weight(1.3f),
                                        textAlign = TextAlign.End,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
