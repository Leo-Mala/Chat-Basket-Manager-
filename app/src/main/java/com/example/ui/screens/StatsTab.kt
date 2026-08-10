package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.GameViewModel
import com.example.models.Player
import com.example.ui.theme.*
import java.util.Locale

enum class StatSortOption(val label: String) {
    PPG("Pontos (PPG)"),
    RPG("Rebotes (RPG)"),
    APG("Assistências (APG)"),
    SPG("Roubos (SPG)"),
    BPG("Tocos (BPG)"),
    OVR("Geral (OVR)")
}

enum class StatViewMode(val label: String) {
    SEASON_AVG("Médias da Temporada"),
    SEASON_TOTALS("Totais da Temporada"),
    CAREER_TOTALS("Carreira")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsTab(viewModel: GameViewModel) {
    val team = viewModel.managedTeam ?: return
    val season = viewModel.season ?: return
    val players = team.players

    var searchQuery by remember { mutableStateOf("") }
    var selectedPositionFilter by remember { mutableStateOf("TODOS") }
    var selectedSortOption by remember { mutableStateOf(StatSortOption.PPG) }
    var selectedViewMode by remember { mutableStateOf(StatViewMode.SEASON_AVG) }
    var selectedPlayerForDialog by remember { mutableStateOf<Player?>(null) }

    // Helper functions for player averages
    fun getPPG(p: Player): Double = if (p.seasonGames > 0) p.seasonPoints.toDouble() / p.seasonGames else 0.0
    fun getRPG(p: Player): Double = if (p.seasonGames > 0) p.seasonRebounds.toDouble() / p.seasonGames else 0.0
    fun getAPG(p: Player): Double = if (p.seasonGames > 0) p.seasonAssists.toDouble() / p.seasonGames else 0.0
    fun getSPG(p: Player): Double = if (p.seasonGames > 0) p.seasonSteals.toDouble() / p.seasonGames else 0.0
    fun getBPG(p: Player): Double = if (p.seasonGames > 0) p.seasonBlocks.toDouble() / p.seasonGames else 0.0

    // Filter & Sort Players
    val filteredPlayers = remember(players, searchQuery, selectedPositionFilter, selectedSortOption) {
        players.filter { player ->
            val matchesName = player.name.contains(searchQuery, ignoreCase = true)
            val matchesPos = if (selectedPositionFilter == "TODOS") true else player.position == selectedPositionFilter
            matchesName && matchesPos
        }.sortedWith { p1, p2 ->
            when (selectedSortOption) {
                StatSortOption.PPG -> getPPG(p2).compareTo(getPPG(p1))
                StatSortOption.RPG -> getRPG(p2).compareTo(getRPG(p1))
                StatSortOption.APG -> getAPG(p2).compareTo(getAPG(p1))
                StatSortOption.SPG -> getSPG(p2).compareTo(getSPG(p1))
                StatSortOption.BPG -> getBPG(p2).compareTo(getBPG(p1))
                StatSortOption.OVR -> p2.overall.compareTo(p1.overall)
            }
        }
    }

    val topScorer = players.maxByOrNull { getPPG(it) }
    val maxPPG = players.maxOfOrNull { getPPG(it) }?.coerceAtLeast(1.0) ?: 1.0

    // Team season metrics
    val totalGamesPlayed = season.standings[team.name]?.gamesPlayed ?: 0
    val teamTotalPts = players.sumOf { it.seasonPoints }
    val teamAvgPts = if (totalGamesPlayed > 0) teamTotalPts.toDouble() / totalGamesPlayed else 0.0

    // Player Detail Dialog
    selectedPlayerForDialog?.let { player ->
        PlayerStatsDetailDialog(
            player = player,
            onDismiss = { selectedPlayerForDialog = null }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 60.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Banner: Team Overview
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(BasketOrange, ElectricCyan)),
                        RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "MÉDIAS & ESTATÍSTICAS DA TEMPORADA",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = BasketOrange,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${team.name} • $totalGamesPlayed Jogos Disputados",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        }
                        Surface(
                            color = ElectricCyan.copy(alpha = 0.15f),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "Estatísticas",
                                tint = ElectricCyan,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Metrics Summary Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatMetricCard(
                            label = "MÉDIA DE PONTOS",
                            value = String.format(Locale.US, "%.1f", teamAvgPts),
                            subText = "PPG Time",
                            color = BasketOrange,
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            label = "CESTINHA",
                            value = if (topScorer != null) String.format(Locale.US, "%.1f", getPPG(topScorer)) else "0.0",
                            subText = topScorer?.name?.take(10) ?: "-",
                            color = ChampionshipGold,
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            label = "TOTAL PONTOS",
                            value = "$teamTotalPts",
                            subText = "Pts Acumulados",
                            color = ElectricCyan,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. Interactive Chart 1: Top Scorers PPG Bar Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📊 ", fontSize = 18.sp)
                            Text(
                                text = "LÍDERES EM PONTUAÇÃO (PPG)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextWhite
                            )
                        }
                        Text(
                            text = "Top 5 Elenco",
                            fontSize = 12.sp,
                            color = TextGray
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val top5Scorers = players.sortedByDescending { getPPG(it) }.take(5)
                    top5Scorers.forEachIndexed { index, player ->
                        val ppg = getPPG(player)
                        val fraction = (ppg / maxPPG).toFloat().coerceIn(0.05f, 1.0f)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = if (index == 0) ChampionshipGold else CourtLightSlate,
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.padding(end = 6.dp)
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (index == 0) CourtMidnight else TextWhite,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = player.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(${player.position})",
                                        fontSize = 11.sp,
                                        color = TextGray
                                    )
                                }
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", ppg)} PPG",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BasketOrange
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Custom Bar Visualization
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(CourtLightSlate)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = when (index) {
                                                    0 -> listOf(ChampionshipGold, BasketOrange)
                                                    1 -> listOf(BasketOrange, BasketDarkOrange)
                                                    2 -> listOf(ElectricCyan, Color(0xFF009688))
                                                    else -> listOf(CourtBorder, CourtLightSlate)
                                                }
                                            )
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Interactive Chart 2: Triple Threat Multi-Stat Comparison (PPG / RPG / APG)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📈 COMPARAÇÃO TRIO PRINCIPAL (PPG / RPG / APG)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Visualização combinada dos principais titulares",
                        fontSize = 11.sp,
                        color = TextGray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LegendItem(color = BasketOrange, label = "Pontos (PPG)")
                        LegendItem(color = ElectricCyan, label = "Rebotes (RPG)")
                        LegendItem(color = ChampionshipGold, label = "Assistências (APG)")
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val top3AllAround = players.sortedByDescending { getPPG(it) + getRPG(it) + getAPG(it) }.take(3)
                    top3AllAround.forEach { player ->
                        val ppg = getPPG(player)
                        val rpg = getRPG(player)
                        val apg = getAPG(player)

                        val ppgFrac = (ppg / 35.0).toFloat().coerceIn(0.04f, 1f)
                        val rpgFrac = (rpg / 15.0).toFloat().coerceIn(0.04f, 1f)
                        val apgFrac = (apg / 15.0).toFloat().coerceIn(0.04f, 1f)

                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Text(
                                text = "${player.name} (${player.position})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            // Grouped Bars
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // PPG Bar
                                Column(modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(CourtLightSlate)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(ppgFrac)
                                                .background(BasketOrange)
                                        )
                                    }
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", ppg)} pts",
                                        fontSize = 10.sp,
                                        color = BasketOrange,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                // RPG Bar
                                Column(modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(CourtLightSlate)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(rpgFrac)
                                                .background(ElectricCyan)
                                        )
                                    }
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", rpg)} reb",
                                        fontSize = 10.sp,
                                        color = ElectricCyan,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                // APG Bar
                                Column(modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(CourtLightSlate)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(apgFrac)
                                                .background(ChampionshipGold)
                                        )
                                    }
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", apg)} ast",
                                        fontSize = 10.sp,
                                        color = ChampionshipGold,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Search, Filter & Controls Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📋 TABELA COMPLETA DE ESTATÍSTICAS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextWhite
                        )
                        Text(
                            text = "${filteredPlayers.size} Jogadores",
                            fontSize = 12.sp,
                            color = ChampionshipGold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search text field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar jogador por nome...", color = TextMuted, fontSize = 13.sp) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar", tint = TextGray) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BasketOrange,
                            unfocusedBorderColor = CourtBorder,
                            focusedContainerColor = CourtLightSlate,
                            unfocusedContainerColor = CourtLightSlate,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Position Filters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("TODOS", "PG", "SG", "SF", "PF", "C").forEach { pos ->
                            val isSelected = selectedPositionFilter == pos
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPositionFilter = pos },
                                label = { Text(pos, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BasketOrange,
                                    selectedLabelColor = Color.White,
                                    containerColor = CourtLightSlate,
                                    labelColor = TextGray
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sort & View Mode Selectors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Sort Dropdown
                        var sortExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { sortExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, CourtBorder),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = CourtLightSlate)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = selectedSortOption.label,
                                        fontSize = 11.sp,
                                        color = TextWhite,
                                        maxLines = 1
                                    )
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Ord", tint = BasketOrange)
                                }
                            }
                            DropdownMenu(
                                expanded = sortExpanded,
                                onDismissRequest = { sortExpanded = false },
                                modifier = Modifier.background(CourtDeepSlate)
                            ) {
                                StatSortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label, color = TextWhite, fontSize = 12.sp) },
                                        onClick = {
                                            selectedSortOption = option
                                            sortExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // View Mode Dropdown
                        var viewExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { viewExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, CourtBorder),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = CourtLightSlate)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = selectedViewMode.label,
                                        fontSize = 11.sp,
                                        color = TextWhite,
                                        maxLines = 1
                                    )
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Modo", tint = ElectricCyan)
                                }
                            }
                            DropdownMenu(
                                expanded = viewExpanded,
                                onDismissRequest = { viewExpanded = false },
                                modifier = Modifier.background(CourtDeepSlate)
                            ) {
                                StatViewMode.entries.forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(mode.label, color = TextWhite, fontSize = 12.sp) },
                                        onClick = {
                                            selectedViewMode = mode
                                            viewExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Table Rows / Player Cards
        items(filteredPlayers) { player ->
            PlayerStatRowCard(
                player = player,
                viewMode = selectedViewMode,
                onClick = { selectedPlayerForDialog = player }
            )
        }
    }
}

@Composable
fun StatMetricCard(
    label: String,
    value: String,
    subText: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CourtLightSlate),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextGray
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = subText,
                fontSize = 10.sp,
                color = TextWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, color = TextGray)
    }
}

@Composable
fun PlayerStatRowCard(
    player: Player,
    viewMode: StatViewMode,
    onClick: () -> Unit
) {
    val ppg = if (player.seasonGames > 0) player.seasonPoints.toDouble() / player.seasonGames else 0.0
    val rpg = if (player.seasonGames > 0) player.seasonRebounds.toDouble() / player.seasonGames else 0.0
    val apg = if (player.seasonGames > 0) player.seasonAssists.toDouble() / player.seasonGames else 0.0
    val spg = if (player.seasonGames > 0) player.seasonSteals.toDouble() / player.seasonGames else 0.0
    val bpg = if (player.seasonGames > 0) player.seasonBlocks.toDouble() / player.seasonGames else 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CourtBorder)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1.2f)
            ) {
                Surface(
                    color = BasketOrange.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = player.position,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BasketOrange,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = player.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "OVR ${player.overall} • ${player.seasonGames} Jogos",
                        fontSize = 11.sp,
                        color = TextGray
                    )
                }
            }

            // Stat Box based on ViewMode
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (viewMode) {
                    StatViewMode.SEASON_AVG -> {
                        StatBoxItem(label = "PTS", value = String.format(Locale.US, "%.1f", ppg), color = BasketOrange)
                        StatBoxItem(label = "REB", value = String.format(Locale.US, "%.1f", rpg), color = ElectricCyan)
                        StatBoxItem(label = "AST", value = String.format(Locale.US, "%.1f", apg), color = ChampionshipGold)
                    }
                    StatViewMode.SEASON_TOTALS -> {
                        StatBoxItem(label = "PTS", value = "${player.seasonPoints}", color = BasketOrange)
                        StatBoxItem(label = "REB", value = "${player.seasonRebounds}", color = ElectricCyan)
                        StatBoxItem(label = "AST", value = "${player.seasonAssists}", color = ChampionshipGold)
                    }
                    StatViewMode.CAREER_TOTALS -> {
                        StatBoxItem(label = "PTS", value = "${player.careerPoints}", color = BasketOrange)
                        StatBoxItem(label = "REB", value = "${player.careerRebounds}", color = ElectricCyan)
                        StatBoxItem(label = "AST", value = "${player.careerAssists}", color = ChampionshipGold)
                    }
                }
            }
        }
    }
}

@Composable
fun StatBoxItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 9.sp, color = TextGray, fontWeight = FontWeight.Bold)
        Text(text = value, fontSize = 13.sp, color = color, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun PlayerStatsDetailDialog(
    player: Player,
    onDismiss: () -> Unit
) {
    val ppg = if (player.seasonGames > 0) player.seasonPoints.toDouble() / player.seasonGames else 0.0
    val rpg = if (player.seasonGames > 0) player.seasonRebounds.toDouble() / player.seasonGames else 0.0
    val apg = if (player.seasonGames > 0) player.seasonAssists.toDouble() / player.seasonGames else 0.0
    val spg = if (player.seasonGames > 0) player.seasonSteals.toDouble() / player.seasonGames else 0.0
    val bpg = if (player.seasonGames > 0) player.seasonBlocks.toDouble() / player.seasonGames else 0.0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, BasketOrange)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = player.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = TextWhite
                        )
                        Text(
                            text = "${player.position} • Idade ${player.age} anos • OVR ${player.overall}",
                            fontSize = 12.sp,
                            color = ChampionshipGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", tint = TextWhite)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Season Averages Box
                Text(
                    text = "MÉDIAS NA TEMPORADA ATUAL (${player.seasonGames} JOGOS)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BasketOrange
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatMetricCard("PPG", String.format(Locale.US, "%.1f", ppg), "Pontos", BasketOrange, Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(4.dp))
                    StatMetricCard("RPG", String.format(Locale.US, "%.1f", rpg), "Rebotes", ElectricCyan, Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(4.dp))
                    StatMetricCard("APG", String.format(Locale.US, "%.1f", apg), "Assistências", ChampionshipGold, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatMetricCard("SPG", String.format(Locale.US, "%.1f", spg), "Roubos", SuccessGreen, Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(4.dp))
                    StatMetricCard("BPG", String.format(Locale.US, "%.1f", bpg), "Tocos", TextWhite, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Attributes breakdown
                Text(
                    text = "DISTRIBUIÇÃO DE HABILIDADES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(8.dp))

                AttributeBar("Ataque / Arremesso", player.shooting, BasketOrange)
                AttributeBar("Defesa", player.defense, ElectricCyan)
                AttributeBar("Rebote", player.rebound, ChampionshipGold)
                AttributeBar("Passe / Visão", player.passing, SuccessGreen)
                AttributeBar("Atletismo", player.athleticism, Color(0xFFAB47BC))

                Spacer(modifier = Modifier.height(16.dp))

                // Career totals summary
                Surface(
                    color = CourtLightSlate,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "🏆 MÁRCA NA CARREIRA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChampionshipGold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${player.careerPoints} Pontos • ${player.careerRebounds} Rebotes • ${player.careerAssists} Assistências em ${player.careerGames} Jogos",
                            fontSize = 12.sp,
                            color = TextWhite,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AttributeBar(label: String, valValue: Int, color: Color) {
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 11.sp, color = TextGray)
            Text(text = "$valValue", fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(CourtLightSlate)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((valValue / 99f).coerceIn(0f, 1f))
                    .background(color)
            )
        }
    }
}
