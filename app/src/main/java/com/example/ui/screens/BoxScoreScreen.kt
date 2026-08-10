package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.MatchBoxScore
import com.example.models.PlayerBoxScore
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxScoreScreen(
    boxScore: MatchBoxScore,
    onDismiss: () -> Unit
) {
    var selectedTeamTab by remember { mutableStateOf(0) } // 0: Home, 1: Away

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 BOX SCORE OFICIAL", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CourtMidnight)
            )
        },
        containerColor = CourtMidnight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp)
        ) {
            // Match Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BasketOrange.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = boxScore.dateString, fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = boxScore.homeTeamName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text(text = "${boxScore.homeScore}", fontSize = 32.sp, fontWeight = FontWeight.Black, color = BasketOrange)
                        }

                        Text(text = "VS", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextMuted)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = boxScore.awayTeamName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text(text = "${boxScore.awayScore}", fontSize = 32.sp, fontWeight = FontWeight.Black, color = ElectricCyan)
                        }
                    }

                    boxScore.mvpPlayerName?.let { mvp ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = ChampionshipGold.copy(alpha = 0.2f),
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ChampionshipGold)
                        ) {
                            Text(
                                text = "⭐ MVP: $mvp",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChampionshipGold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Team Tab selection
            TabRow(
                selectedTabIndex = selectedTeamTab,
                containerColor = CourtDeepSlate,
                contentColor = BasketOrange,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTeamTab == 0,
                    onClick = { selectedTeamTab = 0 },
                    text = { Text(boxScore.homeTeamName, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTeamTab == 1,
                    onClick = { selectedTeamTab = 1 },
                    text = { Text(boxScore.awayTeamName, fontWeight = FontWeight.Bold) }
                )
            }

            // Table Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CourtLightSlate.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "JOGADOR", modifier = Modifier.weight(2.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Text(text = "MIN", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, textAlign = TextAlign.Center)
                Text(text = "PTS", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ChampionshipGold, textAlign = TextAlign.Center)
                Text(text = "REB", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, textAlign = TextAlign.Center)
                Text(text = "AST", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, textAlign = TextAlign.Center)
                Text(text = "STL", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, textAlign = TextAlign.Center)
                Text(text = "BLK", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, textAlign = TextAlign.Center)
                Text(text = "FG", modifier = Modifier.weight(1.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, textAlign = TextAlign.Center)
            }

            val playersList = if (selectedTeamTab == 0) boxScore.homePlayers else boxScore.awayPlayers

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(top = 6.dp, bottom = 56.dp)
            ) {
                items(playersList) { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CourtDeepSlate.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(2.5f)) {
                            Text(text = p.playerName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text(text = p.position, fontSize = 9.sp, color = ElectricCyan)
                        }
                        Text(text = "${p.minutesPlayed}'", modifier = Modifier.weight(1f), fontSize = 11.sp, color = TextMuted, textAlign = TextAlign.Center)
                        Text(text = "${p.points}", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ChampionshipGold, textAlign = TextAlign.Center)
                        Text(text = "${p.rebounds}", modifier = Modifier.weight(1f), fontSize = 11.sp, color = TextWhite, textAlign = TextAlign.Center)
                        Text(text = "${p.assists}", modifier = Modifier.weight(1f), fontSize = 11.sp, color = TextWhite, textAlign = TextAlign.Center)
                        Text(text = "${p.steals}", modifier = Modifier.weight(1f), fontSize = 11.sp, color = TextMuted, textAlign = TextAlign.Center)
                        Text(text = "${p.blocks}", modifier = Modifier.weight(1f), fontSize = 11.sp, color = TextMuted, textAlign = TextAlign.Center)
                        Text(text = "${p.fgMade}/${p.fgAttempted}", modifier = Modifier.weight(1.5f), fontSize = 11.sp, color = TextWhite, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
