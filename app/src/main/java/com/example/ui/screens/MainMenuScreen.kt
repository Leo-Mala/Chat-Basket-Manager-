package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.ui.components.GameButton
import com.example.ui.theme.*
import com.example.utils.SaveSlotSummary

@Composable
fun MainMenuScreen(
    onContinue: () -> Unit,
    onNewCareer: () -> Unit,
    onSettings: () -> Unit,
    hasSavedGame: Boolean,
    teamName: String,
    budget: Int,
    wins: Int,
    losses: Int,
    saveSlots: List<SaveSlotSummary> = emptyList(),
    activeSlotId: Int = 1,
    onContinueSlot: ((Int) -> Unit)? = null,
    onNewCareerSlot: ((Int) -> Unit)? = null
) {
    var selectedFeatureInfo by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showContinueSlots by remember { mutableStateOf(false) }
    var showNewCareerSlots by remember { mutableStateOf(false) }

    // Infinite breathing glow animation for the main logo
    val infiniteTransition = rememberInfiniteTransition(label = "LogoGlow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF080D1A),
                        CourtMidnight,
                        Color(0xFF0B1220),
                        CourtMidnight
                    )
                )
            )
    ) {
        // Top ambient radial flare
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-50).dp)
                .size(340.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            BasketOrange.copy(alpha = glowAlpha),
                            ChampionshipGold.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Bottom cyan ambient glow
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 60.dp)
                .size(360.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ElectricCyan.copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically)
        ) {
            // Main Logo & Header Title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(140.dp)
                        .scale(pulseScale)
                ) {
                    // Outer glowing aura
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        BasketOrange.copy(alpha = glowAlpha),
                                        ChampionshipGold.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )

                    // Glass ring container
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(118.dp)
                            .shadow(16.dp, CircleShape, spotColor = BasketOrange)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        CourtDeepSlate,
                                        Color(0xFF1E293B)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .border(
                                width = 2.dp,
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        BasketOrange,
                                        ChampionshipGold,
                                        ElectricCyan,
                                        BasketOrange
                                    )
                                ),
                                shape = CircleShape
                            )
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_basketball),
                            contentDescription = "Logo Basket Manager",
                            modifier = Modifier.size(92.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "BASKET MANAGER",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = TextWhite,
                    letterSpacing = 2.5.sp,
                    textAlign = TextAlign.Center
                )

                Surface(
                    color = ElectricCyan.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .clickable {
                            selectedFeatureInfo = "⚡ SIMULADOR NBA 2026" to "O Basket Manager 2026 é um simulador completo de franquias da NBA. Monte sua comissão técnica, ajuste táticas de ataque e defesa, gerencie a folha salarial e acompanhe partidas em tempo real!"
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "⚡ SIMULADOR NBA 2026 ℹ️",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Interactive Feature highlights row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    FeatureChip("🏆 Franchise Mode") {
                        selectedFeatureInfo = "🏆 Franchise Mode" to "Assuma o controle total da sua franquia da NBA! Gerencie contratos de jogadores, finanças, patrocinadores, comissão técnica e dispute 82 jogos na temporada regular rumo aos Playoffs."
                    }
                    FeatureChip("📊 Live Play-by-Play") {
                        selectedFeatureInfo = "📊 Live Play-by-Play" to "Simule partidas com velocidade ajustável ou narração jogada a jogada em tempo real! Acompanhe arremessos, enterradas, faltas e substituições com estatísticas atualizadas na hora."
                    }
                    FeatureChip("💰 Roster & Draft") {
                        selectedFeatureInfo = "💰 Roster & Draft" to "Contrate agentes livres, faça trocas de jogadores e participe do Draft oficial com promessas jovens da liga para construir uma dinastia vitoriosa!"
                    }
                }
            }

            // Saved Game Info Card
            if (teamName.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.5.dp,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    ChampionshipGold.copy(alpha = 0.7f),
                                    BasketOrange.copy(alpha = 0.5f),
                                    ElectricCyan.copy(alpha = 0.4f)
                                )
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .clip(RoundedCornerShape(22.dp)),
                    colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                    elevation = CardDefaults.cardElevation(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = BasketOrange.copy(alpha = 0.2f),
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = "🏀", fontSize = 18.sp)
                                }
                            }

                            Text(
                                text = teamName,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextWhite
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = CourtLightSlate.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "CAMPANHA",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = "$wins V - $losses D",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SuccessGreen
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .height(28.dp)
                                    .width(1.dp)
                                    .background(CourtBorder)
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "ORÇAMENTO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = "$${String.format("%,d", budget).replace(',', '.')}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ChampionshipGold
                                )
                            }
                        }
                    }
                }
            }

            // Main Action Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (hasSavedGame) {
                    GameButton(
                        text = if (saveSlots.count { it.occupied } > 1) "ESCOLHER CARREIRA" else "CONTINUAR CARREIRA",
                        icon = "▶",
                        onClick = {
                            val occupiedSlots = saveSlots.filter { it.occupied }
                            when {
                                onContinueSlot == null -> onContinue()
                                occupiedSlots.size == 1 -> onContinueSlot.invoke(occupiedSlots.first().slotId)
                                occupiedSlots.isNotEmpty() -> showContinueSlots = true
                                else -> onContinue()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        gradient = Brush.horizontalGradient(
                            listOf(BasketOrange, BasketDarkOrange)
                        )
                    )
                }

                GameButton(
                    text = if (hasSavedGame) "NOVA CARREIRA" else "INICIAR NOVA CARREIRA",
                    icon = "🏆",
                    onClick = {
                        if (onNewCareerSlot != null && saveSlots.isNotEmpty()) showNewCareerSlots = true
                        else onNewCareer()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    gradient = if (hasSavedGame)
                        Brush.horizontalGradient(listOf(CourtLightSlate, CourtDeepSlate))
                    else
                        Brush.horizontalGradient(listOf(BasketOrange, BasketDarkOrange))
                )

                GameButton(
                    text = "AJUSTES & CONFIGURAÇÕES",
                    icon = "⚙️",
                    onClick = onSettings,
                    modifier = Modifier.fillMaxWidth(),
                    gradient = Brush.horizontalGradient(listOf(CourtLightSlate, CourtDeepSlate))
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (saveSlots.isNotEmpty()) "💾 ${saveSlots.count { it.occupied }}/${saveSlots.size} slots ocupados • autosave ativo" else "💾 Salvamento automático local ativo",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                }
            }
        }
    }

    if (showContinueSlots) {
        SaveSlotPickerDialog(
            title = "CONTINUAR CARREIRA",
            subtitle = "Escolha qual carreira deseja carregar.",
            slots = saveSlots.filter { it.occupied },
            activeSlotId = activeSlotId,
            newCareerMode = false,
            onDismiss = { showContinueSlots = false },
            onSelect = { slotId ->
                showContinueSlots = false
                onContinueSlot?.invoke(slotId)
            }
        )
    }

    if (showNewCareerSlots) {
        SaveSlotPickerDialog(
            title = "NOVA CARREIRA",
            subtitle = "Escolha um slot. Um slot ocupado só será substituído depois que você confirmar a criação da nova carreira.",
            slots = saveSlots,
            activeSlotId = activeSlotId,
            newCareerMode = true,
            onDismiss = { showNewCareerSlots = false },
            onSelect = { slotId ->
                showNewCareerSlots = false
                onNewCareerSlot?.invoke(slotId) ?: onNewCareer()
            }
        )
    }

    // Feature Detail Dialog
    selectedFeatureInfo?.let { (title, description) ->
        Dialog(onDismissRequest = { selectedFeatureInfo = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, BasketOrange, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChampionshipGold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = description,
                        fontSize = 14.sp,
                        color = TextWhite,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selectedFeatureInfo = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CourtBorder)
                        ) {
                            Text("Fechar", color = TextGray)
                        }

                        Button(
                            onClick = {
                                selectedFeatureInfo = null
                                if (onNewCareerSlot != null && saveSlots.isNotEmpty()) showNewCareerSlots = true
                                else onNewCareer()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = BasketOrange),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Iniciar", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun SaveSlotPickerDialog(
    title: String,
    subtitle: String,
    slots: List<SaveSlotSummary>,
    activeSlotId: Int,
    newCareerMode: Boolean,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, BasketOrange, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(title, color = ChampionshipGold, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(subtitle, color = TextGray, fontSize = 12.sp, lineHeight = 17.sp)

                slots.forEach { slot ->
                    val selectedBorder = if (slot.slotId == activeSlotId && slot.occupied) ElectricCyan else CourtBorder
                    Surface(
                        onClick = { onSelect(slot.slotId) },
                        color = CourtLightSlate.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, selectedBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "SLOT ${slot.slotId}",
                                    color = if (slot.occupied) TextWhite else TextMuted,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    if (slot.occupied) "OCUPADO" else "VAZIO",
                                    color = if (slot.occupied) SuccessGreen else ElectricCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }

                            if (slot.occupied) {
                                Text(slot.teamName ?: "Carreira salva", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                val seasonText = buildString {
                                    append("Temporada ${slot.seasonNumber ?: "-"}")
                                    slot.currentYear?.let { append(" • $it") }
                                    slot.currentDay?.let { append(" • Jogo ${(it + 1).coerceAtMost(82)}/82") }
                                }
                                Text(seasonText, color = TextGray, fontSize = 11.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (slot.wins != null && slot.losses != null) {
                                        Text("${slot.wins}V-${slot.losses}D", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    slot.budget?.let {
                                        Text("$${String.format("%,d", it).replace(',', '.')}", color = ChampionshipGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (newCareerMode) {
                                    Text(
                                        "⚠ Será substituído somente ao confirmar a nova carreira.",
                                        color = BasketOrange,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                Text("Disponível para uma nova carreira", color = TextGray, fontSize = 12.sp)
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CourtBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("CANCELAR", color = TextGray)
                }
            }
        }
    }
}

@Composable
private fun FeatureChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = CourtDeepSlate.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CourtBorder)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}
