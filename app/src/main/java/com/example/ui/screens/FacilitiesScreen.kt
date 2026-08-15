package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.Facility
import com.example.models.FacilityType
import com.example.models.TeamFacilities
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacilitiesScreen(
    facilities: TeamFacilities,
    budget: Int,
    onUpgradeFacility: (FacilityType) -> Unit,
    onDismiss: () -> Unit
) {
    var facilityToUpgrade by remember { mutableStateOf<Facility?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "🏛️ INSTALAÇÕES & INFRAESTRUTURA",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Saldo: $${String.format("%,d", budget).replace(',', '.')}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                            Text(
                                text = "•  Manutenção: $${String.format("%,d", facilities.getTotalMaintenanceCost()).replace(',', '.')}/ano",
                                fontSize = 11.sp,
                                color = ChampionshipGold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CourtMidnight)
            )
        },
        containerColor = CourtMidnight
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, SuccessGreen, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SALDO BANCÁRIO DO CLUBE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                            Text(
                                text = if (budget < 0) "-$${String.format("%,d", kotlin.math.abs(budget)).replace(',', '.')}" else "$${String.format("%,d", budget).replace(',', '.')}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = if (budget >= 0) SuccessGreen else ErrorRed
                            )
                        }
                        Surface(
                            color = SuccessGreen.copy(alpha = 0.15f),
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen)
                        ) {
                            Text(
                                text = "💰 CAIXA ATUAL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BasketOrange.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "ℹ️ Impacto da Infraestrutura", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ChampionshipGold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Instalações de alto nível garantem maior bilheteria, aceleram a evolução de promessas, encurtam o tempo de lesões e refinam a precisão do recrutamento no Draft.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            item {
                FacilityCard(
                    facility = facilities.arena,
                    budget = budget,
                    icon = "🏟️",
                    bonusDescription = "+${facilities.arena.bonusPercent}% na receita de ingressos e maior bilheteria em jogos em casa.",
                    onUpgradeClick = { facilityToUpgrade = facilities.arena }
                )
            }

            item {
                FacilityCard(
                    facility = facilities.training,
                    budget = budget,
                    icon = "🏋️",
                    bonusDescription = "+${facilities.training.bonusPercent}% de aceleração na evolução do OVR e química dos jogadores.",
                    onUpgradeClick = { facilityToUpgrade = facilities.training }
                )
            }

            item {
                FacilityCard(
                    facility = facilities.medical,
                    budget = budget,
                    icon = "🩺",
                    bonusDescription = "-${facilities.medical.bonusPercent}% no risco de lesões e tempo de recuperação acelerado.",
                    onUpgradeClick = { facilityToUpgrade = facilities.medical }
                )
            }

            item {
                FacilityCard(
                    facility = facilities.scouting,
                    budget = budget,
                    icon = "🔭",
                    bonusDescription = "+${facilities.scouting.bonusPercent}% de precisão na avaliação de rookies e bônus no Draft.",
                    onUpgradeClick = { facilityToUpgrade = facilities.scouting }
                )
            }
        }
    }

    facilityToUpgrade?.let { facility ->
        AlertDialog(
            onDismissRequest = { facilityToUpgrade = null },
            title = { Text("Evoluir ${facility.name}?", fontWeight = FontWeight.Bold, color = TextWhite) },
            text = {
                Text(
                    text = "Avançar para o Nível ${facility.level + 1} por $${String.format("%,d", facility.currentUpgradeCost).replace(',', '.')}?\n\n${facility.type.description}",
                    color = TextMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpgradeFacility(facility.type)
                        facilityToUpgrade = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BasketOrange)
                ) {
                    Text("Confirmar Upgrade", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { facilityToUpgrade = null }) {
                    Text("Cancelar", color = TextMuted)
                }
            },
            containerColor = CourtDeepSlate
        )
    }
}

@Composable
private fun FacilityCard(
    facility: Facility,
    budget: Int,
    icon: String,
    bonusDescription: String,
    onUpgradeClick: () -> Unit
) {
    val isMaxLevel = facility.level >= facility.maxLevel
    val canAfford = budget >= facility.currentUpgradeCost && !isMaxLevel

    Card(
        colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CourtBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = icon, fontSize = 24.sp)
                    Column {
                        Text(text = facility.type.label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text(text = facility.name, fontSize = 12.sp, color = TextMuted)
                    }
                }

                Surface(
                    color = ElectricCyan.copy(alpha = 0.2f),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan)
                ) {
                    Text(
                        text = "NÍVEL ${facility.level}/${facility.maxLevel}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ElectricCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = bonusDescription,
                fontSize = 12.sp,
                color = SuccessGreen,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isMaxLevel) {
                    Text(
                        text = "Upgrade: $${String.format("%,d", facility.currentUpgradeCost).replace(',', '.')}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChampionshipGold
                    )

                    Button(
                        onClick = onUpgradeClick,
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BasketOrange,
                            disabledContainerColor = CourtBorder
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Evoluir (+1)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    Text(
                        text = "⭐ NÍVEL MÁXIMO ATINGIDO",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChampionshipGold
                    )
                }
            }
        }
    }
}
