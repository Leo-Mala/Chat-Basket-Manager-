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

fun CelebrationScreen(viewModel: GameViewModel) {
    val playoff = viewModel.playoffResult ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(CourtMidnight, BasketDarkOrange.copy(alpha = 0.3f), CourtMidnight)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Championship Trophy",
                tint = ChampionshipGold,
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "TEMOS UM CAMPEÃO!",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ChampionshipGold
            )
            Text(
                text = playoff.nbaChampion.name.uppercase(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = BasketOrange,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))

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
                        text = "Destaques Finais do Playoffs",
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Campeão Leste:", color = TextGray)
                        Text(playoff.eastChampion.name, color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Campeão Oeste:", color = TextGray)
                        Text(playoff.westChampion.name, color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("MVP das Finais:", color = TextGray)
                        Text(playoff.mvp?.name ?: "N/A", color = ChampionshipGold, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    viewModel.startDraftPhase()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BasketOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "IR PARA O DRAFT DE NOVATOS 🎓",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun SettingsDialog(viewModel: GameViewModel, onExitToMainMenu: (() -> Unit)? = null, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var difficulty by remember { mutableStateOf(viewModel.difficulty) }
    var injuriesEnabled by remember { mutableStateOf(viewModel.injuriesEnabled) }

    Dialog(onDismissRequest = onDismiss) {
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
                    text = "CONFIGURAÇÕES DO JOGO",
                    fontWeight = FontWeight.Bold,
                    color = BasketOrange,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Dificuldade
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Dificuldade", fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    DifficultyLevel.entries.forEach { level ->
                        val valDiff = level.value
                        val textDiff = level.label
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { difficulty = valDiff }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = difficulty == valDiff,
                                onClick = { difficulty = valDiff },
                                colors = RadioButtonDefaults.colors(selectedColor = BasketOrange, unselectedColor = TextGray)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(textDiff, color = TextWhite, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Lesões
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { injuriesEnabled = !injuriesEnabled }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Lesões Ativas", fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 14.sp)
                        Text("Jogadores podem se lesionar", color = TextGray, fontSize = 11.sp)
                    }
                    Switch(
                        checked = injuriesEnabled,
                        onCheckedChange = { injuriesEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = BasketOrange, checkedTrackColor = BasketOrange.copy(alpha = 0.5f))
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Salvar Configurações
                Button(
                    onClick = {
                        viewModel.difficulty = difficulty
                        viewModel.injuriesEnabled = injuriesEnabled
                        viewModel.saveGame()
                        ToastUtils.showToast(context, "Configurações salvas com sucesso!")
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BasketOrange)
                ) {
                    Text("SALVAR CONFIGURAÇÕES", color = Color.White, fontWeight = FontWeight.Bold)
                }

                if (onExitToMainMenu != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onDismiss()
                            onExitToMainMenu()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold)
                    ) {
                        Text("SAIR PARA O MENU INICIAL", color = CourtMidnight, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Reset de dados
                Button(
                    onClick = {
                        val builder = android.app.AlertDialog.Builder(context)
                        builder.setTitle("Resetar todos os dados?")
                        builder.setMessage("Isso apagará permanentemente o time atual, temporada, finanças e histórico. Deseja prosseguir?")
                        builder.setPositiveButton("Resetar") { _, _ ->
                            viewModel.clearSavedGame(context)
                            onDismiss()
                        }
                        builder.setNegativeButton("Cancelar", null)
                        builder.show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("RESETAR JOGO (APAGAR TUDO)", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onDismiss) {
                    Text("FECHAR", color = TextGray)
                }
            }
        }
    }
}

@Composable
fun DraftScreen(viewModel: GameViewModel) {
    val context = LocalContext.current
    var selectedRookie by remember { mutableStateOf<Player?>(null) }
    
    val scoutLvl = viewModel.finances?.scoutingLevel ?: 1
    val margin = 5 - scoutLvl
    
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
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Icon(
                    imageVector = Icons.Default.SportsBasketball,
                    contentDescription = "Draft Icon",
                    tint = BasketOrange,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "DRAFT DE NOVATOS",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = BasketOrange,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Temporada ${viewModel.season?.seasonNumber ?: 1} • Escolha o futuro do seu time!",
                    fontSize = 14.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center
                )
                if (margin > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "🔬 Precisão dos Olheiros: Nível $scoutLvl/5 (Margem de erro: ±$margin)",
                        fontSize = 11.sp,
                        color = ChampionshipGold,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "🔬 Precisão dos Olheiros: Nível 5/5 (Atributos Revelados!)",
                        fontSize = 11.sp,
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(viewModel.draftRookies) { rookie ->
                val isSelected = selectedRookie?.id == rookie.id
                
                val displayOvr = if (margin == 0) "OVR ${rookie.overall}" else "Est. OVR ${rookie.overall - margin}-${rookie.overall + margin}"
                
                // For attributes, show range or exact
                val dispArr = if (margin == 0) "${rookie.shooting}" else "${rookie.shooting - margin}-${rookie.shooting + margin}"
                val dispDef = if (margin == 0) "${rookie.defense}" else "${rookie.defense - margin}-${rookie.defense + margin}"
                val dispReb = if (margin == 0) "${rookie.rebound}" else "${rookie.rebound - margin}-${rookie.rebound + margin}"
                val dispPas = if (margin == 0) "${rookie.passing}" else "${rookie.passing - margin}-${rookie.passing + margin}"
                val dispFis = if (margin == 0) "${rookie.athleticism}" else "${rookie.athleticism - margin}-${rookie.athleticism + margin}"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedRookie = rookie },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) BasketOrange.copy(alpha = 0.2f) else CourtLightSlate
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) BasketOrange else CourtLightSlate.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(BasketOrange),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = rookie.position,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = rookie.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                    Text(
                                        text = "Idade: ${rookie.age} anos",
                                        fontSize = 12.sp,
                                        color = TextGray
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CourtMidnight)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = displayOvr,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = ChampionshipGold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Attributes row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ARRE", fontSize = 11.sp, color = TextGray)
                                Text(dispArr, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("DEF", fontSize = 11.sp, color = TextGray)
                                Text(dispDef, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("REB", fontSize = 11.sp, color = TextGray)
                                Text(dispReb, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("PAS", fontSize = 11.sp, color = TextGray)
                                Text(dispPas, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("FIS", fontSize = 11.sp, color = TextGray)
                                Text(dispFis, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val rookie = selectedRookie
                        if (rookie != null) {
                            viewModel.draftRookie(rookie, context)
                        }
                    },
                    enabled = selectedRookie != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BasketOrange,
                        disabledContainerColor = CourtLightSlate.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (selectedRookie != null) "DRAFTAR ${selectedRookie!!.name.uppercase()}" else "SELECIONE UM JOGADOR",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = if (selectedRookie != null) Color.White else TextGray
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
