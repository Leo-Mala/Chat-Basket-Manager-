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

fun SetupScreen(viewModel: GameViewModel) {
    var selectedTeamName by remember { mutableStateOf("Los Angeles Lakers") }
    var coachName by remember { mutableStateOf("") }
    var offSkill by remember { mutableStateOf(65f) }
    var defSkill by remember { mutableStateOf(65f) }
    var motSkill by remember { mutableStateOf(65f) }
    var selectedDifficulty by remember { mutableStateOf(viewModel.difficulty) }

    val teams = remember { NbaDataGenerator.getAllTeams() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(CourtMidnight, Color(0xFF0D1527), CourtMidnight)
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(BasketOrange.copy(alpha = 0.3f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsBasketball,
                        contentDescription = "Basketball Icon",
                        tint = BasketOrange,
                        modifier = Modifier.size(56.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "NOVA CARREIRA",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = TextWhite,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Configure sua franquia e o perfil da comissão técnica",
                    fontSize = 13.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CourtBorder, RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "1️⃣ ", fontSize = 18.sp)
                            Text(
                                text = "Escolha sua Franquia",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextWhite
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = CourtLightSlate,
                                    contentColor = TextWhite
                                )
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = selectedTeamName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextWhite)
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Seta", tint = ElectricCyan)
                                }
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .background(CourtDeepSlate)
                                    .border(1.dp, CourtBorder)
                            ) {
                                teams.forEach { team ->
                                    DropdownMenuItem(
                                        text = { Text(text = team.name, color = TextWhite, fontWeight = FontWeight.Medium) },
                                        onClick = {
                                            selectedTeamName = team.name
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CourtBorder, RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "2️⃣ ", fontSize = 18.sp)
                            Text(
                                text = "Perfil do Treinador",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextWhite
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        OutlinedTextField(
                            value = coachName,
                            onValueChange = { coachName = it },
                            label = { Text("Nome do Treinador", color = TextMuted) },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextWhite, fontWeight = FontWeight.Bold),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BasketOrange,
                                unfocusedBorderColor = CourtBorder,
                                focusedContainerColor = CourtLightSlate,
                                unfocusedContainerColor = CourtLightSlate
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Atributos Iniciais (Até 99):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = ChampionshipGold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Offensive skill slider
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("🔥 Ataque", color = TextGray, fontSize = 13.sp)
                                Text("${offSkill.toInt()}", fontWeight = FontWeight.Bold, color = BasketOrange)
                            }
                            Slider(
                                value = offSkill,
                                onValueChange = { offSkill = it },
                                valueRange = 30f..99f,
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(thumbColor = BasketOrange, activeTrackColor = BasketOrange, inactiveTrackColor = CourtLightSlate)
                            )
                        }

                        // Defensive skill slider
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("🛡️ Defesa", color = TextGray, fontSize = 13.sp)
                                Text("${defSkill.toInt()}", fontWeight = FontWeight.Bold, color = ElectricCyan)
                            }
                            Slider(
                                value = defSkill,
                                onValueChange = { defSkill = it },
                                valueRange = 30f..99f,
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan, inactiveTrackColor = CourtLightSlate)
                            )
                        }

                        // Motivation skill slider
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("⚡ Motivação", color = TextGray, fontSize = 13.sp)
                                Text("${motSkill.toInt()}", fontWeight = FontWeight.Bold, color = ChampionshipGold)
                            }
                            Slider(
                                value = motSkill,
                                onValueChange = { motSkill = it },
                                valueRange = 30f..99f,
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(thumbColor = ChampionshipGold, activeTrackColor = ChampionshipGold, inactiveTrackColor = CourtLightSlate)
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CourtBorder, RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "3️⃣ ", fontSize = 18.sp)
                            Text(
                                text = "Nível de Dificuldade",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextWhite
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        listOf(
                            0 to ("Fácil" to "Maior vantagem em simulações e facilidade para evoluir a franquia."),
                            1 to ("Normal / Médio" to "Desafio equilibrado e realista para a temporada."),
                            2 to ("Difícil" to "Adversários mais competitivos e menor margem para erros.")
                        ).forEach { (diffVal, info) ->
                            val isSelected = selectedDifficulty == diffVal
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedDifficulty = diffVal },
                                color = if (isSelected) BasketOrange.copy(alpha = 0.15f) else CourtLightSlate,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) BasketOrange else CourtBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedDifficulty = diffVal },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = BasketOrange,
                                            unselectedColor = TextGray
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = info.first,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) BasketOrange else TextWhite,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = info.second,
                                            color = TextGray,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                GameButton(
                    text = "INICIAR TEMPORADA",
                    icon = "🏀",
                    onClick = {
                        val finalCoachName = if (coachName.isBlank()) "Coach Wilson" else coachName
                        viewModel.startNewGame(
                            selectedTeamName = selectedTeamName,
                            coachName = finalCoachName,
                            offSkill = offSkill.toInt(),
                            defSkill = defSkill.toInt(),
                            motSkill = motSkill.toInt(),
                            selectedDifficulty = selectedDifficulty
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    gradient = Brush.horizontalGradient(listOf(BasketOrange, BasketDarkOrange))
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
