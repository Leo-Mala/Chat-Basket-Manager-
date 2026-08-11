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

fun RosterTab(viewModel: GameViewModel) {
    val team = viewModel.managedTeam ?: return
    val context = LocalContext.current
    var selectedPlayerForTrain by remember { mutableStateOf<Player?>(null) }
    
    // Estados reativos para novos diálogos e funcionalidades
    var rosterTabState by remember { mutableStateOf(0) } // 0: Elenco, 1: Agentes Livres
    var showLineupDialog by remember { mutableStateOf(false) }
    var showCompareDialog by remember { mutableStateOf(false) }
    var showTradeOfferDialog by remember { mutableStateOf(false) }
    var tradeOfferedPlayer by remember { mutableStateOf<Player?>(null) }
    var tradeOfferedPlayerTeam by remember { mutableStateOf("") }

    // 1. DIÁLOGO DE TREINAMENTO E AÇÃO DE TROCA
    if (selectedPlayerForTrain != null) {
        val player = selectedPlayerForTrain!!
        Dialog(onDismissRequest = { selectedPlayerForTrain = null }) {
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
                        text = "TREINAMENTO & MERCADO",
                        fontWeight = FontWeight.Bold,
                        color = BasketOrange,
                        fontSize = 14.sp
                    )
                    Text(
                        text = player.name,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = TextWhite
                    )
                    Text(
                        text = "${player.position} • OVR ${player.overall}",
                        color = ChampionshipGold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Pontos de XP disponíveis: ${player.xp}",
                        fontWeight = FontWeight.Bold,
                        color = LightAccent,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Custo por melhoria: 10 XP (+1 ponto no atributo)",
                        fontSize = 11.sp,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // career & season statistics section
                    Text(
                        text = "ESTATÍSTICAS & HISTÓRICO 📊",
                        fontWeight = FontWeight.Bold,
                        color = BasketOrange,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )

                    // Season Stats vs Career Stats Table
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CourtMidnight.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("TEMPORADA ATUAL", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Jogos: ${player.seasonGames}", fontSize = 11.sp, color = TextWhite)
                            Text("PPG: ${String.format("%.1f", if(player.seasonGames == 0) 0f else player.seasonPoints.toFloat() / player.seasonGames)}", fontSize = 11.sp, color = TextWhite)
                            Text("RPG: ${String.format("%.1f", if(player.seasonGames == 0) 0f else player.seasonRebounds.toFloat() / player.seasonGames)}", fontSize = 11.sp, color = TextWhite)
                            Text("APG: ${String.format("%.1f", if(player.seasonGames == 0) 0f else player.seasonAssists.toFloat() / player.seasonGames)}", fontSize = 11.sp, color = TextWhite)
                        }

                        Box(modifier = Modifier.width(1.dp).height(65.dp).background(CourtLightSlate.copy(alpha = 0.3f)).align(Alignment.CenterVertically))

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("CARREIRA ACUMULADA", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Jogos: ${player.careerGames}", fontSize = 11.sp, color = TextWhite)
                            Text("PPG: ${String.format("%.1f", if(player.careerGames == 0) 0f else player.careerPoints.toFloat() / player.careerGames)}", fontSize = 11.sp, color = TextWhite)
                            Text("RPG: ${String.format("%.1f", if(player.careerGames == 0) 0f else player.careerRebounds.toFloat() / player.careerGames)}", fontSize = 11.sp, color = TextWhite)
                            Text("APG: ${String.format("%.1f", if(player.careerGames == 0) 0f else player.careerAssists.toFloat() / player.careerGames)}", fontSize = 11.sp, color = TextWhite)
                        }
                    }

                    if (player.championships > 0 || player.mvps > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (player.championships > 0) {
                                Text("🏆 x${player.championships} Campeão", fontSize = 10.sp, color = ChampionshipGold, fontWeight = FontWeight.Bold)
                                if (player.mvps > 0) Spacer(modifier = Modifier.width(12.dp))
                            }
                            if (player.mvps > 0) {
                                Text("⭐ x${player.mvps} MVP", fontSize = 10.sp, color = BasketOrange, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = CourtLightSlate.copy(alpha = 0.3f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    val attributes = listOf(
                        "shooting" to "Arremesso (${player.shooting})",
                        "defense" to "Defesa (${player.defense})",
                        "rebound" to "Rebote (${player.rebound})",
                        "passing" to "Passe (${player.passing})",
                        "athleticism" to "Atletismo (${player.athleticism})"
                    )

                    attributes.forEach { (attrKey, attrLabel) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(attrLabel, color = TextWhite, fontSize = 14.sp)
                            Button(
                                onClick = {
                                    val success = viewModel.trainPlayer(player, attrKey)
                                    if (!success) {
                                        // Trigger updates
                                    }
                                },
                                enabled = player.xp >= 10,
                                colors = ButtonDefaults.buttonColors(containerColor = BasketOrange, disabledContainerColor = CourtLightSlate)
                            ) {
                                Text("+1 Attr", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.proposePlayerTrade(player, context) { candidate, teamName ->
                                    tradeOfferedPlayer = candidate
                                    tradeOfferedPlayerTeam = teamName
                                    showTradeOfferDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PROPOR TROCA", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = { selectedPlayerForTrain = null },
                            colors = ButtonDefaults.buttonColors(containerColor = CourtLightSlate),
                            modifier = Modifier.weight(0.8f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("FECHAR", fontSize = 11.sp, color = TextWhite)
                        }
                    }
                }
            }
        }
    }

    // 2. DIÁLOGO DO QUINTETO INICIAL (Starting Five)
    if (showLineupDialog) {
        Dialog(onDismissRequest = { showLineupDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎯 QUINTETO INICIAL",
                        fontWeight = FontWeight.Bold,
                        color = BasketOrange,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Selecione exatamente 5 titulares para compor a escalação principal da equipe. Escalados: ${viewModel.startingFive.size}/5",
                        color = TextGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(team.players) { player ->
                            val isSelected = viewModel.startingFive.any { it.id == player.id }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val success = viewModel.toggleStartingPlayer(player)
                                        if (!success) {
                                            ToastUtils.showToast(context, "Limite de 5 titulares já atingido!")
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) BasketOrange.copy(alpha = 0.2f) else CourtLightSlate
                                ),
                                border = if (isSelected) BorderStroke(1.dp, BasketOrange) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(player.name, fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 14.sp)
                                        Text("${player.position} • OVR ${player.overall}", color = TextGray, fontSize = 12.sp)
                                    }
                                    if (isSelected) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = "Escalado", tint = BasketOrange, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val ok = viewModel.autoSelectBestLineup()
                                if (ok) {
                                    ToastUtils.showToast(context, "Escalação automática aplicada! Os 5 melhores disponíveis foram selecionados.")
                                }
                            },
                            border = BorderStroke(1.dp, ChampionshipGold),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AUTO ESCALAR", color = ChampionshipGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                        Button(
                            onClick = { showLineupDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = BasketOrange),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("CONCLUIR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // 3. DIÁLOGO DO COMPARADOR DE JOGADORES (Player Comparison)
    if (showCompareDialog) {
        Dialog(onDismissRequest = { showCompareDialog = false }) {
            var selectedUserPlayerIdx by remember { mutableStateOf(0) }
            var selectedOpponentPlayerIdx by remember { mutableStateOf(0) }
            
            val userPlayers = team.players
            val allOpponentPlayers = remember {
                viewModel.season?.teams?.flatMap { it.players } ?: emptyList()
            }
            
            var expandedUserSpinner by remember { mutableStateOf(false) }
            var expandedOpponentSpinner by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⚔️ COMPARADOR DE ATRIBUTOS",
                        fontWeight = FontWeight.Bold,
                        color = BasketOrange,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Seletores (Dropdowns)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Jogador 1 (Nosso time)
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { expandedUserSpinner = true },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(6.dp),
                                border = BorderStroke(1.dp, BasketOrange)
                            ) {
                                Text(
                                    userPlayers.getOrNull(selectedUserPlayerIdx)?.name ?: "Jogador 1",
                                    color = TextWhite,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            DropdownMenu(
                                expanded = expandedUserSpinner,
                                onDismissRequest = { expandedUserSpinner = false },
                                modifier = Modifier.background(CourtLightSlate).heightIn(max = 240.dp)
                            ) {
                                userPlayers.forEachIndexed { idx, p ->
                                    DropdownMenuItem(
                                        text = { Text("${p.name} (OVR ${p.overall})", color = TextWhite, fontSize = 12.sp) },
                                        onClick = {
                                            selectedUserPlayerIdx = idx
                                            expandedUserSpinner = false
                                        }
                                    )
                                }
                            }
                        }

                        // Jogador 2 (Toda a liga)
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { expandedOpponentSpinner = true },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(6.dp),
                                border = BorderStroke(1.dp, ChampionshipGold)
                            ) {
                                Text(
                                    allOpponentPlayers.getOrNull(selectedOpponentPlayerIdx)?.name ?: "Jogador 2",
                                    color = TextWhite,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            DropdownMenu(
                                expanded = expandedOpponentSpinner,
                                onDismissRequest = { expandedOpponentSpinner = false },
                                modifier = Modifier.background(CourtLightSlate).heightIn(max = 240.dp)
                            ) {
                                allOpponentPlayers.forEachIndexed { idx, p ->
                                    DropdownMenuItem(
                                        text = { Text("${p.name} (OVR ${p.overall})", color = TextWhite, fontSize = 12.sp) },
                                        onClick = {
                                            selectedOpponentPlayerIdx = idx
                                            expandedOpponentSpinner = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Exibir Comparação de Atributos
                    val p1 = userPlayers.getOrNull(selectedUserPlayerIdx)
                    val p2 = allOpponentPlayers.getOrNull(selectedOpponentPlayerIdx)

                    if (p1 != null && p2 != null) {
                        val statsToCompare = listOf(
                            Triple("OVERALL", p1.overall, p2.overall),
                            Triple("Arremesso", p1.shooting, p2.shooting),
                            Triple("Defesa", p1.defense, p2.defense),
                            Triple("Rebote", p1.rebound, p2.rebound),
                            Triple("Passe", p1.passing, p2.passing),
                            Triple("Atletismo", p1.athleticism, p2.athleticism),
                            Triple("Idade (Anos)", p1.age, p2.age)
                        )

                        statsToCompare.forEach { (label, v1, v2) ->
                            val is1Better = if (label == "Idade (Anos)") v1 < v2 else v1 > v2
                            val is2Better = if (label == "Idade (Anos)") v2 < v1 else v2 > v1

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$v1",
                                    color = if (is1Better) SuccessGreen else if (is2Better) ErrorRed else TextWhite,
                                    fontWeight = if (is1Better) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Start,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = label,
                                    color = TextGray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1.5f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "$v2",
                                    color = if (is2Better) SuccessGreen else if (is1Better) ErrorRed else TextWhite,
                                    fontWeight = if (is2Better) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    TextButton(onClick = { showCompareDialog = false }) {
                        Text("FECHAR COMPARADOR", color = BasketOrange, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 4. DIÁLOGO DE CONFIRMAÇÃO DE PROPOSTA DE TROCA (Trade Offer Confirmation)
    if (showTradeOfferDialog && tradeOfferedPlayer != null && selectedPlayerForTrain != null) {
        val myPlayer = selectedPlayerForTrain!!
        val offered = tradeOfferedPlayer!!
        Dialog(onDismissRequest = { showTradeOfferDialog = false }) {
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
                        text = "🤝 PROPOSTA DE MERCADO",
                        fontWeight = FontWeight.Bold,
                        color = BasketOrange,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "O time ${tradeOfferedPlayerTeam} aceita a sua oferta de troca se ela for de comum acordo bilateral:",
                        color = TextWhite,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Você Oferece", color = TextGray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(myPlayer.name, fontWeight = FontWeight.Bold, color = TextWhite, textAlign = TextAlign.Center, fontSize = 14.sp)
                            Text("OVR ${myPlayer.overall}", color = BasketOrange, fontWeight = FontWeight.Black)
                        }
                        Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = "Trocar", tint = ChampionshipGold, modifier = Modifier.size(32.dp))
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Você Recebe", color = TextGray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(offered.name, fontWeight = FontWeight.Bold, color = TextWhite, textAlign = TextAlign.Center, fontSize = 14.sp)
                            Text("OVR ${offered.overall}", color = SuccessGreen, fontWeight = FontWeight.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                showTradeOfferDialog = false
                                selectedPlayerForTrain = null
                                viewModel.executePlayerTrade(myPlayer, offered)
                                ToastUtils.showToast(context, "Troca Efetuada! ${offered.name} agora joga no ${team.name}!", Toast.LENGTH_LONG)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ACEITAR", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showTradeOfferDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("RECUSAR", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // LISTAGEM E ESTATÍSTICAS PRINCIPAIS
    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Selector Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(CourtDeepSlate, RoundedCornerShape(8.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (rosterTabState == 0) BasketOrange else Color.Transparent)
                    .clickable { rosterTabState = 0 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ELENCO ATIVO",
                    color = if (rosterTabState == 0) TextWhite else TextGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (rosterTabState == 1) BasketOrange else Color.Transparent)
                    .clickable { rosterTabState = 1 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "AGENTES LIVRES",
                        color = if (rosterTabState == 1) TextWhite else TextGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    if (viewModel.freeAgents.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(ChampionshipGold, CircleShape)
                                .size(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${viewModel.freeAgents.size}",
                                color = CourtMidnight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        if (rosterTabState == 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = CourtDeepSlate)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ESTATÍSTICAS DA EQUIPE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("OVR Geral Médio", color = TextGray, fontSize = 12.sp)
                            Text("${team.getAverageOverall().toInt()}", fontWeight = FontWeight.Black, fontSize = 24.sp, color = BasketOrange)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Jogador Estrela", color = TextGray, fontSize = 12.sp)
                            Text(team.getBestPlayer()?.name ?: "N/A", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ChampionshipGold)
                            Text("OVR ${team.getBestPlayer()?.overall ?: 0}", color = TextGray, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Botoes de Ação Rápida no Topo da Lista
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { showLineupDialog = true },
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(containerColor = BasketOrange),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("TITULARES (${viewModel.startingFive.size}/5)", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Button(
                    onClick = {
                        val ok = viewModel.autoSelectBestLineup()
                        if (ok) {
                            ToastUtils.showToast(context, "Escalação automática aplicada! Os 5 melhores disponíveis foram escalados.")
                        } else {
                            ToastUtils.showToast(context, "Não há jogadores disponíveis!")
                        }
                    },
                    modifier = Modifier.weight(1.1f),
                    colors = ButtonDefaults.buttonColors(containerColor = CourtLightSlate),
                    border = BorderStroke(1.dp, ChampionshipGold),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("AUTO ESCALAR", fontSize = 10.sp, color = ChampionshipGold, fontWeight = FontWeight.Bold)
                    }
                }
                Button(
                    onClick = { showCompareDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = CourtDeepSlate),
                    border = BorderStroke(1.dp, BasketOrange),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, tint = BasketOrange, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("COMPARAR", fontSize = 10.sp, color = BasketOrange, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text(
                text = "ELENCO ATIVO",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 64.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(team.players) { player ->
                    val isStarting = viewModel.startingFive.any { it.id == player.id }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPlayerForTrain = player },
                        colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                        border = if (isStarting) BorderStroke(1.dp, BasketOrange.copy(alpha = 0.5f)) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                when (player.position) {
                                                    "PG" -> BasketOrange
                                                    "SG" -> SuccessGreen
                                                    "SF" -> LightAccent
                                                    "PF" -> ChampionshipGold
                                                    "C" -> ErrorRed
                                                    else -> BasketOrange
                                                },
                                                CircleShape
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        val icon = when (player.position) {
                                            "PG" -> "🎯 PG"
                                            "SG" -> "🏹 SG"
                                            "SF" -> "⚡ SF"
                                            "PF" -> "💪 PF"
                                            "C" -> "🛡️ C"
                                            else -> player.position
                                        }
                                        Text(
                                            text = icon,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = Color.White
                                        )
                                    }
                                    if (isStarting) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(ChampionshipGold, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "TITULAR 🏀",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 8.sp,
                                                color = CourtMidnight
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = player.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = TextWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Idade: ${player.age} anos • XP Acumulado: ${player.xp}",
                                    fontSize = 12.sp,
                                    color = TextGray
                                )
                                if (player.injured) {
                                    Text(
                                        text = "LESIONADO (${player.injuryDays} dias restantes)",
                                        color = ErrorRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${player.overall}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 24.sp,
                                    color = BasketOrange
                                )
                                Text(
                                    text = "OVR",
                                    fontSize = 10.sp,
                                    color = TextGray
                                )
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        } else {
            // Free agency view
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "MERCADO DE AGENTES LIVRES 🏀",
                        fontWeight = FontWeight.Bold,
                        color = BasketOrange,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Contrate astros sem contrato pagando bônus do seu orçamento. Limite do elenco: 12 jogadores (se ultrapassar, o jogador com pior OVR será dispensado).",
                        fontSize = 11.sp,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Seu Orçamento:", fontSize = 12.sp, color = TextWhite)
                        val faBudget = viewModel.finances?.budget ?: 0
                        val faMillions = String.format(java.util.Locale.US, "%.1f", faBudget / 1000000.0)
                        Text(
                            text = if (faBudget < 0) "-$${faMillions.replace("-", "")}M" else "$${faMillions}M",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = if (faBudget >= 0) SuccessGreen else ErrorRed
                        )
                    }
                }
            }

            if (viewModel.freeAgents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhum agente livre no mercado neste momento.", color = TextGray, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 64.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(viewModel.freeAgents) { agent ->
                        val cost = agent.overall * 120000
                        val canAfford = (viewModel.finances?.budget ?: 0) >= cost
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CourtLightSlate.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    when (agent.position) {
                                                        "PG" -> BasketOrange
                                                        "SG" -> SuccessGreen
                                                        "SF" -> LightAccent
                                                        "PF" -> ChampionshipGold
                                                        "C" -> ErrorRed
                                                        else -> BasketOrange
                                                    },
                                                    CircleShape
                                                )
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = agent.position,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = Color.White
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = agent.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = TextWhite
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${agent.overall}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 20.sp,
                                            color = ChampionshipGold
                                        )
                                        Text("OVR", fontSize = 9.sp, color = TextGray)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Idade: ${agent.age} anos", fontSize = 11.sp, color = TextGray)
                                        Text("Custo: $${cost / 1000000.0}M", fontSize = 11.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.signFreeAgent(agent, context)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SuccessGreen,
                                            disabledContainerColor = CourtLightSlate.copy(alpha = 0.3f)
                                        ),
                                        enabled = canAfford,
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("CONTRATAR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                // Quick tiny attributes row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("ARR: ${agent.shooting}", fontSize = 10.sp, color = TextGray)
                                    Text("DEF: ${agent.defense}", fontSize = 10.sp, color = TextGray)
                                    Text("REB: ${agent.rebound}", fontSize = 10.sp, color = TextGray)
                                    Text("PAS: ${agent.passing}", fontSize = 10.sp, color = TextGray)
                                    Text("ATL: ${agent.athleticism}", fontSize = 10.sp, color = TextGray)
                                }
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

