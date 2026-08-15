package com.example.ui.screens

import android.widget.Toast
import com.example.utils.ToastUtils
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.GameViewModel
import com.example.models.AssistantCoachNotification
import com.example.ui.theme.*

enum class NotificationFilter(val label: String) {
    ALL("Todas"),
    UNREAD("Não Lidas"),
    TACTICAL("Tático"),
    DEFENSE("Defesa"),
    DEVELOPMENT("Desenvolvimento")
}

@Composable
fun NotificationsTab(viewModel: GameViewModel) {
    val context = LocalContext.current
    val notifications = viewModel.assistantNotifications

    var selectedFilter by remember { mutableStateOf(NotificationFilter.ALL) }

    val unreadCount = notifications.count { !it.isRead }

    // Filtered notifications list
    val filteredNotifications = notifications.filter { note ->
        when (selectedFilter) {
            NotificationFilter.ALL -> true
            NotificationFilter.UNREAD -> !note.isRead
            NotificationFilter.TACTICAL -> note.coachRole.contains("Tático", ignoreCase = true)
            NotificationFilter.DEFENSE -> note.coachRole.contains("Defensivo", ignoreCase = true)
            NotificationFilter.DEVELOPMENT -> note.coachRole.contains("Desenvolvimento", ignoreCase = true)
        }
    }.sortedByDescending { it.timestamp }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 60.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(BasketOrange, ChampionshipGold)),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = BasketOrange.copy(alpha = 0.2f),
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Comissão",
                                    tint = BasketOrange,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "FEEDBACK DA COMISSÃO TÉCNICA",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = BasketOrange,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Relatórios Pós-Jogo e Desenvolvimento",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            }
                        }

                        if (unreadCount > 0) {
                            Surface(
                                color = ErrorRed,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "$unreadCount NOVAS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (notifications.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    viewModel.markAllNotificationsAsRead()
                                    ToastUtils.showToast(context, "Todas as notificações marcadas como lidas!")
                                }
                            ) {
                                Icon(imageVector = Icons.Default.DoneAll, contentDescription = "Lidas", tint = ElectricCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Marcar todas como lidas", fontSize = 12.sp, color = ElectricCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 2. Filter Chips Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NotificationFilter.entries.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BasketOrange,
                            selectedLabelColor = Color.White,
                            containerColor = CourtLightSlate,
                            labelColor = TextGray
                        )
                    )
                }
            }
        }

        // 3. Empty State or Notifications List
        if (filteredNotifications.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MarkChatRead,
                            contentDescription = "Vazio",
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nenhum relatório encontrado",
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "A comissão técnica enviará feedbacks detalhados após cada partida disputada.",
                            fontSize = 12.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(filteredNotifications) { notification ->
                CoachNotificationCard(
                    notification = notification,
                    onRead = { viewModel.markNotificationAsRead(notification.id) },
                    onApplyBonus = {
                        viewModel.applyCoachRecommendation(notification, context)
                    }
                )
            }
        }
    }
}

@Composable
fun CoachNotificationCard(
    notification: AssistantCoachNotification,
    onRead: () -> Unit,
    onApplyBonus: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val roleIcon = when {
        notification.coachRole.contains("Tático", ignoreCase = true) -> "🧠"
        notification.coachRole.contains("Defensivo", ignoreCase = true) -> "🛡️"
        notification.coachRole.contains("Desenvolvimento", ignoreCase = true) -> "🌱"
        else -> "🎯"
    }

    val cardBorderColor = if (!notification.isRead) BasketOrange else CourtBorder

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (!notification.isRead) onRead()
                expanded = !expanded
            },
        colors = CardDefaults.cardColors(
            containerColor = if (!notification.isRead) CourtDeepSlate else CourtLightSlate.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (!notification.isRead) 1.5.dp else 1.dp, cardBorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Role & Match Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = roleIcon, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = notification.coachName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = notification.coachRole,
                            fontSize = 11.sp,
                            color = ChampionshipGold,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (notification.isWin) SuccessGreen.copy(alpha = 0.2f) else ErrorRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "vs ${notification.opponentName} (${notification.userScore}x${notification.opponentScore})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (notification.isWin) SuccessGreen else ErrorRed,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (!notification.isRead) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(BasketOrange, CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Executive Summary
            Text(
                text = notification.summary,
                fontSize = 13.sp,
                color = TextWhite,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Expanded content details
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = CourtBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Strengths
                    if (notification.keyStrengths.isNotEmpty()) {
                        Text(
                            text = "✅ PONTOS FORTES DA EQUIPE:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                        notification.keyStrengths.forEach { item ->
                            Text(
                                text = "• $item",
                                fontSize = 12.sp,
                                color = TextWhite,
                                modifier = Modifier.padding(start = 6.dp, top = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Weaknesses / Areas to improve
                    if (notification.areasToImprove.isNotEmpty()) {
                        Text(
                            text = "⚠️ ÁREAS A MELHORAR:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRed
                        )
                        notification.areasToImprove.forEach { item ->
                            Text(
                                text = "• $item",
                                fontSize = 12.sp,
                                color = TextWhite,
                                modifier = Modifier.padding(start = 6.dp, top = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Highlights
                    if (notification.playerHighlights.isNotEmpty()) {
                        Text(
                            text = "🌟 DESTAQUES INDIVIDUAIS:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChampionshipGold
                        )
                        notification.playerHighlights.forEach { item ->
                            Text(
                                text = "• $item",
                                fontSize = 12.sp,
                                color = TextWhite,
                                modifier = Modifier.padding(start = 6.dp, top = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Tactical Advice
                    if (notification.tacticalAdvice.isNotEmpty()) {
                        Surface(
                            color = ElectricCyan.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "💡 RECOMENDAÇÃO DO AUXILIAR:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan
                                )
                                Text(
                                    text = notification.tacticalAdvice,
                                    fontSize = 12.sp,
                                    color = TextWhite,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Apply Bonus Action Button
                    if (notification.recommendedBonusLabel != null) {
                        Button(
                            onClick = onApplyBonus,
                            enabled = !notification.isBonusApplied,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!notification.isBonusApplied) BasketOrange else CourtLightSlate,
                                disabledContainerColor = CourtLightSlate
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = if (notification.isBonusApplied) Icons.Default.CheckCircle else Icons.Default.FlashOn,
                                contentDescription = "Bonus",
                                tint = if (notification.isBonusApplied) SuccessGreen else Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (notification.isBonusApplied) "AJUSTE JÁ APLICADO!" else notification.recommendedBonusLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (notification.isBonusApplied) SuccessGreen else Color.White
                            )
                        }
                    }
                }
            }

            // Expand/Collapse Hint Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "Recolher detalhes ▲" else "Expandir relatório completo ▼",
                    fontSize = 11.sp,
                    color = BasketOrange,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
