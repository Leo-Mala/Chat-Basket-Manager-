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
import androidx.compose.runtime.saveable.rememberSaveable
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

fun ActiveManagerScreen(viewModel: GameViewModel, onExitToMainMenu: () -> Unit) {
    var selectedTab by rememberSaveable { mutableStateOf(ManagerTab.DASHBOARD) }
    val currentSeason = viewModel.season ?: return
    val team = viewModel.managedTeam ?: return
    var showSettingsDialog by remember { mutableStateOf(false) }

    if (showSettingsDialog) {
        SettingsDialog(
            viewModel = viewModel,
            onExitToMainMenu = onExitToMainMenu,
            onDismiss = { showSettingsDialog = false }
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = CourtDeepSlate,
                tonalElevation = 8.dp
            ) {
                val unreadNotifications = viewModel.getUnreadNotificationCount()
                ManagerTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            if (tab == ManagerTab.NOTIFICATIONS && unreadNotifications > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = ErrorRed) {
                                            Text(
                                                text = if (unreadNotifications > 9) "9+" else "$unreadNotifications",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                ) {
                                    Icon(imageVector = tab.icon, contentDescription = tab.title)
                                }
                            } else {
                                Icon(imageVector = tab.icon, contentDescription = tab.title)
                            }
                        },
                        label = { Text(text = tab.title, fontSize = 9.sp, overflow = TextOverflow.Ellipsis, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BasketOrange,
                            selectedTextColor = BasketOrange,
                            unselectedIconColor = TextGray,
                            unselectedTextColor = TextGray,
                            indicatorColor = CourtLightSlate
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CourtMidnight)
        ) {
            // Elegant Dashboard Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CourtDeepSlate)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = team.name.uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = BasketOrange
                    )
                    val monthName = when (currentSeason.currentMonth) {
                        1 -> "Janeiro"
                        2 -> "Fevereiro"
                        3 -> "Março"
                        4 -> "Abril"
                        5 -> "Maio"
                        6 -> "Junho"
                        7 -> "Julho"
                        8 -> "Agosto"
                        9 -> "Setembro"
                        10 -> "Outubro"
                        11 -> "Novembro"
                        12 -> "Dezembro"
                        else -> "Outubro"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "$monthName ${currentSeason.currentYear} • Jogo ${(currentSeason.currentDay + 1).coerceAtMost(82)}/82",
                            fontSize = 12.sp,
                            color = ChampionshipGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (currentSeason.currentDay.toFloat() / 82f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = BasketOrange,
                        trackColor = CourtLightSlate
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CourtLightSlate)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.AttachMoney, contentDescription = "Orçamento", tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            val b = viewModel.finances?.budget ?: 10000000
                            val bMillions = String.format(java.util.Locale.US, "%.0f", b / 1000000.0)
                            Text(
                                text = if (b < 0) "-$${bMillions.replace("-", "")}M" else "$${bMillions}M",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (b >= 0) SuccessGreen else ErrorRed
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Configurações", tint = TextWhite)
                    }
                }
            }

            // Tab Views Switcher
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val initialIndex = initialState.ordinal
                    val targetIndex = targetState.ordinal
                    if (targetIndex > initialIndex) {
                        (slideInHorizontally(animationSpec = tween(240, easing = FastOutSlowInEasing)) { width -> width / 5 } + fadeIn(animationSpec = tween(240)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(240, easing = FastOutSlowInEasing)) { width -> -width / 5 } + fadeOut(animationSpec = tween(240)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(240, easing = FastOutSlowInEasing)) { width -> -width / 5 } + fadeIn(animationSpec = tween(240)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(240, easing = FastOutSlowInEasing)) { width -> width / 5 } + fadeOut(animationSpec = tween(240)))
                    }
                },
                label = "TabSwitcherAnimation",
                modifier = Modifier.weight(1f)
            ) { tab ->
                when (tab) {
                    ManagerTab.DASHBOARD -> DashboardTab(viewModel)
                    ManagerTab.ROSTER -> RosterTab(viewModel)
                    ManagerTab.TACTICS -> TacticsTab(viewModel)
                    ManagerTab.STATS -> StatsTab(viewModel)
                    ManagerTab.NOTIFICATIONS -> NotificationsTab(viewModel)
                    ManagerTab.FINANCES -> FinancesTab(viewModel)
                    ManagerTab.STANDINGS -> StandingsTab(viewModel)
                    ManagerTab.HISTORY -> HistoryTab(viewModel)
                }
            }

            // Advanced Modules Modal Screens
            if (viewModel.showStaffScreen) {
                Dialog(
                    onDismissRequest = { viewModel.showStaffScreen = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(modifier = Modifier.fillMaxSize(), color = CourtMidnight) {
                        com.example.ui.screens.StaffManagementScreen(
                            teamStaff = viewModel.teamStaff,
                            availableMarket = viewModel.availableStaffMarket,
                            budget = viewModel.finances?.budget ?: 0,
                            onHireStaff = { viewModel.hireStaff(it) },
                            onFireStaff = { viewModel.fireStaff(it) },
                            onDismiss = { viewModel.showStaffScreen = false }
                        )
                    }
                }
            }

            if (viewModel.showFacilitiesScreen) {
                Dialog(
                    onDismissRequest = { viewModel.showFacilitiesScreen = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(modifier = Modifier.fillMaxSize(), color = CourtMidnight) {
                        com.example.ui.screens.FacilitiesScreen(
                            facilities = viewModel.teamFacilities,
                            budget = viewModel.finances?.budget ?: 0,
                            onUpgradeFacility = { viewModel.upgradeFacility(it) },
                            onDismiss = { viewModel.showFacilitiesScreen = false }
                        )
                    }
                }
            }

            if (viewModel.showFinanceAdvancedScreen) {
                Dialog(
                    onDismissRequest = { viewModel.showFinanceAdvancedScreen = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(modifier = Modifier.fillMaxSize(), color = CourtMidnight) {
                        com.example.ui.screens.FinanceAdvancedScreen(
                            financeAdv = viewModel.financeAdvanced,
                            totalPlayerSalaries = (viewModel.managedTeam?.players?.sumOf { it.calculateSalary().toLong() } ?: 0L).toInt(),
                            staffSalaries = viewModel.teamStaff.getTotalStaffSalaries(),
                            facilityMaintenance = viewModel.teamFacilities.getTotalMaintenanceCost(),
                            arenaCapacity = viewModel.managedTeam?.arena?.capacity ?: 20000,
                            currentBudget = viewModel.finances?.budget ?: 0,
                            onUpdateTicketPrice = { viewModel.updateTicketPrice(it) },
                            onDismiss = { viewModel.showFinanceAdvancedScreen = false }
                        )
                    }
                }
            }

            if (viewModel.showNewsFeedScreen) {
                Dialog(
                    onDismissRequest = { viewModel.showNewsFeedScreen = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(modifier = Modifier.fillMaxSize(), color = CourtMidnight) {
                        com.example.ui.screens.NewsFeedScreen(
                            newsList = viewModel.newsFeed,
                            onDismiss = { viewModel.showNewsFeedScreen = false }
                        )
                    }
                }
            }

            if (viewModel.showBoxScoreScreen && viewModel.latestBoxScore != null) {
                Dialog(
                    onDismissRequest = { viewModel.showBoxScoreScreen = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(modifier = Modifier.fillMaxSize(), color = CourtMidnight) {
                        com.example.ui.screens.BoxScoreScreen(
                            boxScore = viewModel.latestBoxScore!!,
                            onDismiss = { viewModel.showBoxScoreScreen = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
