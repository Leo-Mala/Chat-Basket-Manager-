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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.FinanceAdvanced
import com.example.models.SponsorshipDeal
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceAdvancedScreen(
    financeAdv: FinanceAdvanced,
    totalPlayerSalaries: Int,
    staffSalaries: Int = 0,
    facilityMaintenance: Int = 0,
    arenaCapacity: Int = 20000,
    currentBudget: Int,
    onUpdateTicketPrice: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var ticketPriceState by remember { mutableStateOf(financeAdv.ticketPrice.toFloat()) }

    val luxuryTax = financeAdv.calculateLuxuryTax(totalPlayerSalaries)

    val fillRate = when {
        ticketPriceState <= 75 -> 0.98
        ticketPriceState <= 100 -> 0.92
        ticketPriceState <= 130 -> 0.85
        ticketPriceState <= 160 -> 0.72
        else -> 0.55
    }

    val ticketEst = if (financeAdv.revenues.ticketRevenue > 0) {
        financeAdv.revenues.ticketRevenue
    } else {
        (41 * arenaCapacity * ticketPriceState * fillRate).toInt()
    }

    val sponsorshipEst = if (financeAdv.revenues.sponsorshipRevenue > 0) {
        financeAdv.revenues.sponsorshipRevenue
    } else {
        financeAdv.activeSponsorships.sumOf { it.annualAmount }
    }

    val broadcastingEst = if (financeAdv.revenues.broadcastingRevenue >= 85_000_000) {
        financeAdv.revenues.broadcastingRevenue
    } else {
        85_000_000
    }

    val merchandiseEst = if (financeAdv.revenues.merchandiseRevenue >= 20_000_000) {
        financeAdv.revenues.merchandiseRevenue
    } else {
        20_000_000
    }

    val totalRevEst = ticketEst + sponsorshipEst + broadcastingEst + merchandiseEst

    val staffSalariesEst = if (financeAdv.expenses.staffSalaries > 0) financeAdv.expenses.staffSalaries else staffSalaries
    val facilityMaintEst = if (financeAdv.expenses.facilityMaintenance > 0) financeAdv.expenses.facilityMaintenance else facilityMaintenance
    val travelAndOps = financeAdv.expenses.travelLogistics + financeAdv.expenses.operationalExpenses

    val totalExpEst = totalPlayerSalaries + staffSalariesEst + facilityMaintEst + travelAndOps + luxuryTax

    val netProfitEst = totalRevEst - totalExpEst

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "💰 BALANÇO FINANCEIRO & TETO SALARIAL",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Caixa Atual: $${String.format("%,d", currentBudget).replace(',', '.')}",
                            fontSize = 11.sp,
                            color = ChampionshipGold
                        )
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Salary Cap & Luxury Tax Overview
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, if (totalPlayerSalaries > financeAdv.luxuryTaxThreshold) Color(0xFFEF4444) else ElectricCyan, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🏀 FOLHA SALARIAL x SALARY CAP", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Surface(
                                color = if (totalPlayerSalaries > financeAdv.luxuryTaxThreshold) Color(0xFFEF4444).copy(alpha = 0.2f) else SuccessGreen.copy(alpha = 0.2f),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = if (totalPlayerSalaries > financeAdv.luxuryTaxThreshold) "LUXURY TAX ATIVA" else "DENTRO DO TETO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalPlayerSalaries > financeAdv.luxuryTaxThreshold) Color(0xFFEF4444) else SuccessGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        FinanceRow("Salários dos Jogadores", "$${String.format("%,d", totalPlayerSalaries)}", TextWhite)
                        FinanceRow("Teto Salarial (Salary Cap)", "$${String.format("%,d", financeAdv.salaryCap)}", TextMuted)
                        FinanceRow("Gatilho de Taxa (Luxury Tax)", "$${String.format("%,d", financeAdv.luxuryTaxThreshold)}", TextMuted)

                        if (luxuryTax > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            FinanceRow("Multa de Luxury Tax (1.5x)", "-$${String.format("%,d", luxuryTax)}", Color(0xFFEF4444))
                        }
                    }
                }
            }

            // Ticket Price Controller
            item {
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
                            Text(text = "🎟️ Preço dos Ingressos (Bilheteria)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ChampionshipGold)
                            Text(text = "$${ticketPriceState.toInt()} / ingresso", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = BasketOrange)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = ticketPriceState,
                            onValueChange = {
                                ticketPriceState = it
                                onUpdateTicketPrice(it.toInt())
                            },
                            valueRange = 40f..200f,
                            steps = 32,
                            colors = SliderDefaults.colors(
                                thumbColor = BasketOrange,
                                activeTrackColor = BasketOrange
                            )
                        )

                        Text(
                            text = if (ticketPriceState > 140) "⚠️ Preço muito alto pode reduzir o público na arena e afetar a satisfação da torcida."
                            else if (ticketPriceState < 70) "💡 Preço acessível atrai ginásio lotado, mas pode deixar receita potencial na mesa."
                            else "Ajuste equilibrado para maximizar a receita mantendo a arena cheia.",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            // Annual Income Statement (Receitas x Despesas)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CourtBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "📊 PROJEÇÃO ANUAL (DRE)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(text = "🟢 Receitas Estimadas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                        FinanceRow("Bilheteria (Ingressos)", "+$${String.format("%,d", ticketEst)}")
                        FinanceRow("Patrocínios & Naming Rights", "+$${String.format("%,d", sponsorshipEst)}")
                        FinanceRow("Direitos de Transmissão TV", "+$${String.format("%,d", broadcastingEst)}")
                        FinanceRow("Merchandising & Produtos", "+$${String.format("%,d", merchandiseEst)}")

                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = CourtBorder)

                        Text(text = "🔴 Despesas Estimadas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        FinanceRow("Folha Salarial Jogadores", "-$${String.format("%,d", totalPlayerSalaries)}")
                        FinanceRow("Folha da Comissão (Staff)", "-$${String.format("%,d", staffSalariesEst)}")
                        FinanceRow("Manutenção de Instalações", "-$${String.format("%,d", facilityMaintEst)}")
                        FinanceRow("Viagens e Operacional", "-$${String.format("%,d", travelAndOps)}")
                        if (luxuryTax > 0) {
                            FinanceRow("Multa de Luxury Tax", "-$${String.format("%,d", luxuryTax)}")
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = CourtBorder)

                        FinanceRow(
                            label = "LUCRO/PREJUÍZO LÍQUIDO",
                            value = "${if (netProfitEst >= 0) "+" else ""}$${String.format("%,d", netProfitEst)}",
                            color = if (netProfitEst >= 0) SuccessGreen else Color(0xFFEF4444)
                        )
                    }
                }
            }

            // Active Sponsorship Deals
            item {
                Text(text = "🤝 CONTRATOS DE PATROCÍNIO", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ChampionshipGold)
            }

            items(financeAdv.activeSponsorships) { sponsor ->
                SponsorshipCard(sponsor)
            }
        }
    }
}

@Composable
private fun FinanceRow(label: String, value: String, color: Color = TextMuted) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = color)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun SponsorshipCard(sponsor: SponsorshipDeal) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CourtDeepSlate),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CourtBorder, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = sponsor.brandName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                Surface(
                    color = BasketOrange.copy(alpha = 0.2f),
                    shape = CircleShape
                ) {
                    Text(
                        text = sponsor.type,
                        fontSize = 10.sp,
                        color = BasketOrange,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Valor Anual: $${String.format("%,d", sponsor.annualAmount)}", fontSize = 12.sp, color = ChampionshipGold)
                Text(text = "Restante: ${sponsor.yearsRemaining} ano(s)", fontSize = 12.sp, color = TextMuted)
            }

            sponsor.goalDescription?.let { goal ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "🎯 Meta de Bônus: $goal (+$${String.format("%,d", sponsor.goalBonus)})", fontSize = 11.sp, color = ElectricCyan)
            }
        }
    }
}
