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

fun FinancesTab(viewModel: GameViewModel) {
    val financeObj = viewModel.finances ?: return
    val context = LocalContext.current
    var showSponsorDialog by remember { mutableStateOf(false) }

    val availableSponsors = remember {
        listOf(
            Sponsor("Nike Global", 1200000, 3),
            Sponsor("Kia Motors Arena", 1500000, 2),
            Sponsor("Tissot Timekeeper", 800000, 1),
            Sponsor("State Farm Protection", 1000000, 4)
        )
    }

    if (showSponsorDialog) {
        Dialog(onDismissRequest = { showSponsorDialog = false }) {
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
                        text = "ASSINAR NOVO PATROCÍNIO",
                        fontWeight = FontWeight.Bold,
                        color = BasketOrange,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    availableSponsors.forEach { sponsor ->
                        val alreadyHas = financeObj.sponsors.any { it.name == sponsor.name }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable(enabled = !alreadyHas) {
                                    viewModel.signSponsor(sponsor)
                                    showSponsorDialog = false
                                    Toast
                                        .makeText(
                                            context,
                                            "Patrocínio de ${sponsor.name} assinado!",
                                            Toast.LENGTH_SHORT
                                        )
                                        .show()
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (alreadyHas) CourtLightSlate.copy(alpha = 0.5f) else CourtLightSlate
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(sponsor.name, fontWeight = FontWeight.Bold, color = if (alreadyHas) TextGray else TextWhite)
                                    if (alreadyHas) {
                                        Text("JÁ ATIVO", color = ChampionshipGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(
                                    "Financiamento: $${sponsor.amountPerYear / 1000}k / ano",
                                    color = if (alreadyHas) SuccessGreen.copy(alpha = 0.5f) else SuccessGreen,
                                    fontSize = 13.sp
                                )
                                Text("Duração do Contrato: ${sponsor.yearsRemaining} anos", color = TextGray, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { showSponsorDialog = false }) {
                        Text("CANCELAR", color = TextGray)
                    }
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("SALDO BANCÁRIO DO CLUBE", fontSize = 12.sp, color = TextGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    val budgetMillions = String.format(java.util.Locale.US, "%.1f", financeObj.budget / 1000000.0)
                    Text(
                        text = if (financeObj.budget < 0) "-$${budgetMillions.replace("-", "")} Milhões" else "$${budgetMillions} Milhões",
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp,
                        color = if (financeObj.budget >= 0) SuccessGreen else ErrorRed
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showSponsorDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = BasketOrange),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Assinar", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PATROCÍNIO", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.showFinanceAdvancedScreen = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = CourtLightSlate),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("📊 RELATÓRIO DRE", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "INSTALAÇÕES E INFRAESTRUTURA 🏛️",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BasketOrange
                        )
                        TextButton(onClick = { viewModel.showFacilitiesScreen = true }) {
                            Text("GERENCIAR (+1)", color = ElectricCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val fac = viewModel.teamFacilities
                    val itemsList = listOf(
                        Triple("🏟️ Arena Principal", "Nível ${fac.arena.level}/${fac.arena.maxLevel}", fac.arena.currentUpgradeCost),
                        Triple("🏋️ Centro de Treinamento", "Nível ${fac.training.level}/${fac.training.maxLevel}", fac.training.currentUpgradeCost),
                        Triple("🏥 Centro Médico", "Nível ${fac.medical.level}/${fac.medical.maxLevel}", fac.medical.currentUpgradeCost),
                        Triple("🔍 Rede de Olheiros", "Nível ${fac.scouting.level}/${fac.scouting.maxLevel}", fac.scouting.currentUpgradeCost)
                    )

                    itemsList.forEachIndexed { idx, itemData ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = itemData.first, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextWhite)
                            Surface(
                                color = CourtLightSlate,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = itemData.second,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (idx < itemsList.size - 1) {
                            HorizontalDivider(color = CourtLightSlate.copy(alpha = 0.3f), thickness = 1.dp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.showFacilitiesScreen = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BasketDarkOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("EVOLUIR E GERENCIAR INSTALAÇÕES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        item {
            Text(
                text = "PATROCINADORES ATIVOS (${financeObj.sponsors.size})",
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            financeObj.sponsors.forEach { sponsor ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = CourtDeepSlate)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(sponsor.name, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text("Prazo Restante: ${sponsor.yearsRemaining} anos", color = TextGray, fontSize = 12.sp)
                        }
                        Text(
                            text = "+$${sponsor.amountPerYear / 1000}k/ano",
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "HISTÓRICO FINANCEIRO RECENTE",
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(financeObj.expenses.reversed()) { exp ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                colors = CardDefaults.cardColors(containerColor = CourtDeepSlate)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(exp.description, fontWeight = FontWeight.Medium, color = TextWhite)
                        Text(exp.date, color = TextGray, fontSize = 11.sp)
                    }
                    val isIncome = exp.description.contains("Receita") || exp.description.contains("Patrocínio") || exp.description.contains("Ingressos")
                    Text(
                        text = if(isIncome) "+$${exp.amount / 1000}k" else "-$${exp.amount / 1000}k",
                        fontWeight = FontWeight.Bold,
                        color = if(isIncome) SuccessGreen else ErrorRed
                    )
                }
            }
        }
    }
}

@Composable
