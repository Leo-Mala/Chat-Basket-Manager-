package com.example.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StaffAndFacilitiesGenerator
import com.example.models.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffManagementScreen(
    teamStaff: TeamStaff,
    availableMarket: List<StaffMember>,
    budget: Int,
    onHireStaff: (StaffMember) -> Unit,
    onFireStaff: (StaffMember) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSubTab by remember { mutableStateOf(0) } // 0: Atual, 1: Mercado de Contratação
    var memberToHire by remember { mutableStateOf<StaffMember?>(null) }
    var memberToFire by remember { mutableStateOf<StaffMember?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "👔 GESTÃO DE STAFF & COMISSÃO",
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
                                text = "•  Folha: $${String.format("%,d", teamStaff.getTotalStaffSalaries()).replace(',', '.')}/ano",
                                fontSize = 11.sp,
                                color = ChampionshipGold
                            )
                        }
                    }
                },
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
                .padding(horizontal = 16.dp)
        ) {
            // Sub-tabs
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = CourtDeepSlate,
                contentColor = BasketOrange,
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = { Text("Comissão Atual", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = { Text("Mercado (${availableMarket.size})", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedSubTab == 0) {
                // Current Staff View
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 56.dp)
                ) {
                    // Head Coach
                    item {
                        SectionHeader("👔 Técnico Principal (Head Coach)")
                        val hc = teamStaff.headCoach
                        if (hc != null) {
                            HeadCoachCard(headCoach = hc, onFire = { memberToFire = hc })
                        } else {
                            EmptyStaffSlot("Nenhum Técnico Principal contratado!", onGoToMarket = { selectedSubTab = 1 })
                        }
                    }

                    // Strength & Doctor
                    item {
                        SectionHeader("🏥 Saúde & Preparação Física")
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            teamStaff.strengthCoach?.let {
                                GenericStaffCard(staff = it, title = "Preparador Físico", onFire = { memberToFire = it })
                            } ?: EmptyStaffSlot("Sem Preparador Físico", onGoToMarket = { selectedSubTab = 1 })

                            teamStaff.teamDoctor?.let {
                                GenericStaffCard(staff = it, title = "Médico do Time", onFire = { memberToFire = it })
                            } ?: EmptyStaffSlot("Sem Médico do Time", onGoToMarket = { selectedSubTab = 1 })
                        }
                    }

                    // Scout & Assistants
                    item {
                        SectionHeader("🔍 Scouting & Assistentes")
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            teamStaff.scout?.let {
                                GenericStaffCard(staff = it, title = "Olheiro Chefe (Scout)", onFire = { memberToFire = it })
                            } ?: EmptyStaffSlot("Sem Olheiro Chefe", onGoToMarket = { selectedSubTab = 1 })

                            teamStaff.assistants.forEach { assistant ->
                                GenericStaffCard(staff = assistant, title = "Assistente Técnico", onFire = { memberToFire = assistant })
                            }
                        }
                    }

                    // Executives
                    item {
                        SectionHeader("💼 Diretoria Executiva")
                        teamStaff.executives.forEach { exec ->
                            GenericStaffCard(staff = exec, title = exec.roleTitle, onFire = { memberToFire = exec })
                        }
                    }
                }
            } else {
                // Market Candidates
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 56.dp)
                ) {
                    items(availableMarket) { candidate ->
                        MarketStaffCard(
                            staff = candidate,
                            canAfford = budget >= candidate.salary,
                            onHire = { memberToHire = candidate }
                        )
                    }
                }
            }
        }
    }

    // Hire Confirmation Dialog
    memberToHire?.let { candidate ->
        AlertDialog(
            onDismissRequest = { memberToHire = null },
            title = { Text("Contratar ${candidate.name}?", fontWeight = FontWeight.Bold, color = TextWhite) },
            text = {
                Text(
                    text = "Salário: $${String.format("%,d", candidate.salary)}/ano (${candidate.contractYears} anos).\nNível: ${candidate.level}/100 • Especialidade: ${candidate.specialty}",
                    color = TextMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onHireStaff(candidate)
                        memberToHire = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BasketOrange)
                ) {
                    Text("Confirmar Contratação", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToHire = null }) {
                    Text("Cancelar", color = TextMuted)
                }
            },
            containerColor = CourtDeepSlate
        )
    }

    // Fire Confirmation Dialog
    memberToFire?.let { staff ->
        AlertDialog(
            onDismissRequest = { memberToFire = null },
            title = { Text("Demitir ${staff.name}?", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444)) },
            text = {
                Text("A demissão é imediata. A vaga na comissão ficará aberta para novas contratações.", color = TextMuted)
            },
            confirmButton = {
                Button(
                    onClick = {
                        onFireStaff(staff)
                        memberToFire = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Demitir", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToFire = null }) {
                    Text("Cancelar", color = TextMuted)
                }
            },
            containerColor = CourtDeepSlate
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = ChampionshipGold,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
private fun HeadCoachCard(headCoach: HeadCoachStaff, onFire: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, BasketOrange, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = headCoach.name, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextWhite)
                    Text(text = "Estilo: ${headCoach.preferredStyle.name} • Exp: ${headCoach.experience} anos", fontSize = 12.sp, color = ElectricCyan)
                }
                Surface(
                    color = BasketOrange.copy(alpha = 0.2f),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BasketOrange)
                ) {
                    Text(
                        text = "Lvl ${headCoach.level}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BasketOrange,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBadge("Ataque", "${headCoach.offensiveSkill}")
                StatBadge("Defesa", "${headCoach.defensiveSkill}")
                StatBadge("Motivação", "${headCoach.motivationalSkill}")
                StatBadge("Evolução", "${headCoach.playerDevelopment}")
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Salário: $${String.format("%,d", headCoach.salary)}/ano",
                    fontSize = 12.sp,
                    color = ChampionshipGold,
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(
                    onClick = onFire,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Demitir", color = Color(0xFFEF4444), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun GenericStaffCard(staff: StaffMember, title: String, onFire: () -> Unit) {
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Text(text = staff.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    Text(text = "Especialidade: ${staff.specialty}", fontSize = 11.sp, color = ElectricCyan)
                }

                Surface(
                    color = ElectricCyan.copy(alpha = 0.2f),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "Lvl ${staff.level}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Salário: $${String.format("%,d", staff.salary)}/ano",
                    fontSize = 12.sp,
                    color = ChampionshipGold,
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(
                    onClick = onFire,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    Text("Demitir", color = Color(0xFFEF4444), fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun EmptyStaffSlot(message: String, onGoToMarket: () -> Unit) {
    Surface(
        color = CourtDeepSlate.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CourtBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onGoToMarket() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = message, fontSize = 12.sp, color = TextMuted)
            Text(text = "➕ Contratar", fontSize = 12.sp, color = BasketOrange, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MarketStaffCard(staff: StaffMember, canAfford: Boolean, onHire: () -> Unit) {
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when(staff) {
                            is HeadCoachStaff -> "Técnico Principal"
                            is AssistantCoachStaff -> "Assistente Técnico"
                            is StrengthCoach -> "Preparador Físico"
                            is ScoutStaff -> "Olheiro (Scout)"
                            is TeamDoctor -> "Médico do Time"
                            is ExecutiveStaff -> staff.roleTitle
                        },
                        fontSize = 11.sp,
                        color = BasketOrange,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = staff.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    Text(text = "Especialidade: ${staff.specialty}", fontSize = 12.sp, color = ElectricCyan)
                }

                Surface(
                    color = ElectricCyan.copy(alpha = 0.2f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "Lvl ${staff.level}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$${String.format("%,d", staff.salary)} / ano (${staff.contractYears}a)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChampionshipGold
                )

                Button(
                    onClick = onHire,
                    enabled = canAfford,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BasketOrange,
                        disabledContainerColor = CourtBorder
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Contratar", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StatBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, color = TextMuted)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
    }
}
