package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class ManagerTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Painel", Icons.Default.Dashboard),
    ROSTER("Elenco", Icons.Default.Group),
    TACTICS("Táticas", Icons.Default.Settings),
    STATS("Estatísticas", Icons.Default.BarChart),
    NOTIFICATIONS("Comissão", Icons.Default.Notifications),
    FINANCES("Finanças", Icons.Default.AttachMoney),
    STANDINGS("Classificação", Icons.Default.Leaderboard),
    HISTORY("Histórico", Icons.Default.History)
}
