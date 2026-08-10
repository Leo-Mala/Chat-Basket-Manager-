package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.News
import com.example.models.NewsType
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsFeedScreen(
    newsList: List<News>,
    onDismiss: () -> Unit
) {
    var selectedTypeFilter by remember { mutableStateOf<NewsType?>(null) }

    val filteredNews = if (selectedTypeFilter == null) {
        newsList
    } else {
        newsList.filter { it.type == selectedTypeFilter }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📰 FEED DE NOTÍCIAS DA LIGA", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite) },
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
            // Filter chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 10.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedTypeFilter == null,
                        onClick = { selectedTypeFilter = null },
                        label = { Text("Todas (${newsList.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BasketOrange,
                            selectedLabelColor = Color.White,
                            containerColor = CourtDeepSlate,
                            labelColor = TextMuted
                        )
                    )
                }

                items(NewsType.values()) { type ->
                    val count = newsList.count { it.type == type }
                    FilterChip(
                        selected = selectedTypeFilter == type,
                        onClick = { selectedTypeFilter = if (selectedTypeFilter == type) null else type },
                        label = { Text("${type.emoji} ${type.label} ($count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BasketOrange,
                            selectedLabelColor = Color.White,
                            containerColor = CourtDeepSlate,
                            labelColor = TextMuted
                        )
                    )
                }
            }

            if (filteredNews.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📰", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Nenhuma notícia nesta categoria ainda.", color = TextMuted, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 56.dp)
                ) {
                    items(filteredNews) { item ->
                        NewsCard(news = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsCard(news: News) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CourtBorder, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ElectricCyan.copy(alpha = 0.2f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "${news.type.emoji} ${news.type.label.uppercase()}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(text = news.dateString, fontSize = 11.sp, color = TextMuted)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = news.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = news.content,
                fontSize = 12.sp,
                color = TextMuted,
                lineHeight = 17.sp
            )
        }
    }
}
