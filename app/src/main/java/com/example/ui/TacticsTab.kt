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

fun TacticsTab(viewModel: GameViewModel) {
    val tacticsObj = viewModel.tactics ?: return
    var style by remember { mutableStateOf(tacticsObj.style) }
    var pace by remember { mutableStateOf(tacticsObj.pace.toFloat()) }
    var defPressure by remember { mutableStateOf(tacticsObj.defensivePressure.toFloat()) }
    var offReb by remember { mutableStateOf(tacticsObj.offensiveRebound.toFloat()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 64.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ESTILO DE JOGO",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val playStyles = PlayStyle.entries
                    playStyles.forEach { pStyle ->
                        val isSelected = style == pStyle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) BasketOrange else Color.Transparent)
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
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = when (pStyle) {
                                        PlayStyle.FAST_BREAK -> "Fast Break (Contra-Ataque Veloz)"
                                        PlayStyle.HALF_COURT -> "Half Court (Ataque Cadenciado)"
                                        PlayStyle.DEFENSIVE -> "Defensive (Foco Defensivo)"
                                        PlayStyle.BALANCED -> "Balanced (Equilibrado)"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else TextWhite
                                )
                                Text(
                                    text = when (pStyle) {
                                        PlayStyle.FAST_BREAK -> "Bônus Ataque: +10% • Defesa: -20%"
                                        PlayStyle.HALF_COURT -> "Bônus Ataque: -10% • Defesa: Estável"
                                        PlayStyle.DEFENSIVE -> "Bônus Ataque: -20% • Defesa: +20%"
                                        PlayStyle.BALANCED -> "Sem bônus/penalidades aplicados"
                                    },
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextGray
                                )
                            }
                            if (isSelected) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Selecionado", tint = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PARÂMETROS TÁTICOS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Pace slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Ritmo (Pace): ${pace.toInt()}", color = TextWhite, fontWeight = FontWeight.Bold)
                            Text("Rápido", color = TextGray, fontSize = 12.sp)
                        }
                        Slider(
                            value = pace,
                            onValueChange = {
                                pace = it
                                tacticsObj.pace = it.toInt()
                                viewModel.saveGame()
                            },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(thumbColor = BasketOrange, activeTrackColor = BasketOrange)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Press slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Pressão Defensiva: ${defPressure.toInt()}", color = TextWhite, fontWeight = FontWeight.Bold)
                            Text("Intensa", color = TextGray, fontSize = 12.sp)
                        }
                        Slider(
                            value = defPressure,
                            onValueChange = {
                                defPressure = it
                                tacticsObj.defensivePressure = it.toInt()
                                viewModel.saveGame()
                            },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(thumbColor = BasketOrange, activeTrackColor = BasketOrange)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Rebounds slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Rebote Ofensivo: ${offReb.toInt()}", color = TextWhite, fontWeight = FontWeight.Bold)
                            Text("Agressivo", color = TextGray, fontSize = 12.sp)
                        }
                        Slider(
                            value = offReb,
                            onValueChange = {
                                offReb = it
                                tacticsObj.offensiveRebound = it.toInt()
                                viewModel.saveGame()
                            },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(thumbColor = BasketOrange, activeTrackColor = BasketOrange)
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SUBSTITUIÇÕES E ROTAÇÃO 🔄",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CourtLightSlate)
                            .clickable {
                                viewModel.autoSubstitutionsEnabled = !viewModel.autoSubstitutionsEnabled
                                viewModel.saveGame()
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Substituições Automáticas",
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "O treinador faz rodízio automático de jogadores no 2º e 3º quartos para descansar os titulares para o final da partida.",
                                fontSize = 11.sp,
                                color = TextGray
                            )
                        }
                        Switch(
                            checked = viewModel.autoSubstitutionsEnabled,
                            onCheckedChange = {
                                viewModel.autoSubstitutionsEnabled = it
                                viewModel.saveGame()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = BasketOrange, checkedTrackColor = BasketOrange.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
