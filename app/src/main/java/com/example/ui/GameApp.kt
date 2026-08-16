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
import androidx.compose.ui.graphics.Brush
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
import com.example.domain.rules.SavedGameLoadState
import com.example.domain.season.CareerResumeRules
import com.example.models.*
import com.example.simulator.GameSimulator
import com.example.ui.theme.*
import com.example.utils.AwardsCalculator
import com.example.utils.AutoSaveManager
import com.example.utils.ToastUtils
import com.example.utils.CoachFeedbackGenerator
import com.example.utils.SaveSlotManager
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

fun BasketManagerGameApp() {
    val context = LocalContext.current
    val viewModel: GameViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(context.applicationContext as android.app.Application) as T
        }
    })

    var showMainMenu by remember { mutableStateOf(true) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val menuScope = rememberCoroutineScope()
    var saveSlots by remember { mutableStateOf(SaveSlotManager.getSlots(context)) }
    var activeSlotId by remember { mutableIntStateOf(SaveSlotManager.getActiveSlot(context)) }
    var pendingContinueSlot by remember { mutableStateOf<Int?>(null) }

    androidx.activity.compose.BackHandler(enabled = !showMainMenu) {
        if (viewModel.gameState == GameState.SETUP) {
            SaveSlotManager.clearPendingNewSlot(context)
        }
        showMainMenu = true
    }

    LaunchedEffect(showMainMenu, viewModel.savedGameLoadState) {
        if (showMainMenu && viewModel.savedGameLoadState == SavedGameLoadState.READY) {
            val currentTeam = viewModel.managedTeam
            val currentSeason = viewModel.season
            if (currentTeam != null && currentSeason != null) {
                SaveSlotManager.updateSlot(
                    context = context,
                    slotId = SaveSlotManager.getActiveSlot(context),
                    team = currentTeam,
                    season = currentSeason,
                    finance = viewModel.finances,
                    difficulty = viewModel.difficulty
                )
            }
        }
        activeSlotId = SaveSlotManager.getActiveSlot(context)
        saveSlots = SaveSlotManager.getSlots(context)
    }

    LaunchedEffect(viewModel.savedGameLoadState, pendingContinueSlot, activeSlotId) {
        val pendingSlot = pendingContinueSlot ?: return@LaunchedEffect
        if (pendingSlot != activeSlotId) return@LaunchedEffect
        when (viewModel.savedGameLoadState) {
            SavedGameLoadState.READY -> {
                pendingContinueSlot = null
                showMainMenu = false
            }
            SavedGameLoadState.EMPTY, SavedGameLoadState.ERROR -> pendingContinueSlot = null
            SavedGameLoadState.LOADING -> Unit
        }
    }

    AnimatedContent(
        targetState = showMainMenu,
        transitionSpec = {
            if (targetState) {
                (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.96f, animationSpec = tween(300)))
                    .togetherWith(fadeOut(animationSpec = tween(220)) + scaleOut(targetScale = 1.04f, animationSpec = tween(220)))
            } else {
                (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 1.04f, animationSpec = tween(300)))
                    .togetherWith(fadeOut(animationSpec = tween(220)) + scaleOut(targetScale = 0.96f, animationSpec = tween(220)))
            }
        },
        label = "MainMenuToGameTransition"
    ) { isMainMenu ->
        if (isMainMenu) {
            when (viewModel.savedGameLoadState) {
                SavedGameLoadState.LOADING -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(color = BasketOrange)
                        Text("CARREGANDO CARREIRA SALVA...", color = TextWhite, fontWeight = FontWeight.Bold)
                        Text("Não feche nem inicie uma nova carreira enquanto o save é verificado.", color = TextGray, textAlign = TextAlign.Center)
                    }
                }

                SavedGameLoadState.ERROR -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CourtDeepSlate)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("SAVE ENCONTRADO, MAS NÃO FOI POSSÍVEL CARREGAR", color = ChampionshipGold, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                            Text(viewModel.loadErrorMessage ?: "Erro desconhecido", color = TextWhite, textAlign = TextAlign.Center)
                            Button(onClick = { viewModel.retryLoadSavedGame() }) {
                                Text("TENTAR CARREGAR NOVAMENTE")
                            }
                            Text("O save não será apagado por esta tela.", color = TextGray, textAlign = TextAlign.Center)
                        }
                    }
                }

                SavedGameLoadState.EMPTY, SavedGameLoadState.READY -> {
                    val hasSavedGame = saveSlots.any { it.occupied }
                    val teamName = if (viewModel.savedGameLoadState == SavedGameLoadState.READY) viewModel.managedTeam?.name.orEmpty() else ""
                    val budget = if (viewModel.savedGameLoadState == SavedGameLoadState.READY) viewModel.finances?.budget ?: 0 else 0
                    val wins = viewModel.season?.standings?.get(teamName)?.wins ?: 0
                    val losses = viewModel.season?.standings?.get(teamName)?.losses ?: 0

                    val resumeLoadedCareer: () -> Unit = {
                        val currentSeason = viewModel.season
                        if (viewModel.managedTeam != null && currentSeason != null) {
                            viewModel.gameState = CareerResumeRules.resolve(
                                currentDay = currentSeason.currentDay,
                                hasPlayoffResult = viewModel.playoffResult != null,
                                hasDraftClass = viewModel.draftRookies.isNotEmpty()
                            )
                            showMainMenu = false
                        }
                    }

                    MainMenuScreen(
                        onContinue = resumeLoadedCareer,
                        onNewCareer = {
                            val targetSlot = saveSlots.firstOrNull { !it.occupied }?.slotId ?: activeSlotId
                            SaveSlotManager.setPendingNewSlot(context, targetSlot)
                            viewModel.gameState = CareerResumeRules.newCareerState()
                            showMainMenu = false
                        },
                        onContinueSlot = { slotId ->
                            if (slotId == activeSlotId &&
                                viewModel.savedGameLoadState == SavedGameLoadState.READY &&
                                viewModel.managedTeam != null
                            ) {
                                resumeLoadedCareer()
                            } else {
                                menuScope.launch {
                                    if (viewModel.savedGameLoadState == SavedGameLoadState.READY && viewModel.managedTeam != null) {
                                        viewModel.saveGame()?.join()
                                    }
                                    SaveSlotManager.clearPendingNewSlot(context)
                                    SaveSlotManager.setActiveSlot(context, slotId)
                                    activeSlotId = slotId
                                    pendingContinueSlot = slotId
                                    viewModel.retryLoadSavedGame()
                                }
                            }
                        },
                        onNewCareerSlot = { slotId ->
                            SaveSlotManager.setPendingNewSlot(context, slotId)
                            viewModel.gameState = CareerResumeRules.newCareerState()
                            showMainMenu = false
                        },
                        onSettings = {
                            showSettingsDialog = true
                        },
                        hasSavedGame = hasSavedGame,
                        teamName = teamName,
                        budget = budget,
                        wins = wins,
                        losses = losses,
                        saveSlots = saveSlots,
                        activeSlotId = activeSlotId
                    )

                    if (showSettingsDialog) {
                        SettingsDialog(
                            viewModel = viewModel,
                            onExitToMainMenu = null,
                            onDismiss = { showSettingsDialog = false }
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            ) {
                AnimatedContent(
                    modifier = Modifier.fillMaxSize(),
                    targetState = viewModel.gameState,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(280)) + slideInVertically(animationSpec = tween(280)) { height -> height / 12 })
                        .togetherWith(fadeOut(animationSpec = tween(200)) + slideOutVertically(animationSpec = tween(200)) { height -> -height / 12 })
                },
                label = "GameStateTransition"
            ) { state ->
                when (state) {
                    GameState.SETUP -> SetupScreen(viewModel)
                    GameState.LOAD_ERROR -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Não foi possível carregar a carreira", style = MaterialTheme.typography.titleLarge)
                            Text(viewModel.loadErrorMessage ?: "Erro desconhecido", textAlign = TextAlign.Center)
                            Button(onClick = { viewModel.retryLoadSavedGame() }) { Text("Tentar novamente") }
                        }
                    }
                    GameState.ACTIVE -> ActiveManagerScreen(viewModel = viewModel, onExitToMainMenu = { showMainMenu = true })
                    GameState.PLAYOFFS -> PlayoffScreen(viewModel)
                    GameState.CHAMPIONSHIP_CELEBRATION -> CelebrationScreen(viewModel)
                    GameState.DRAFT -> DraftScreen(viewModel)
                    }
                }
            }
        }
    }
}
