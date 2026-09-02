package com.sucharu.sucharupro.ui.features.finance.analytics

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitabilityAnalyticsScreen(
    viewModel: FinanceAnalyticsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val prof = state.profitability

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profitability Analytics", color = Color(0xFFF8FAFC), fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFFF8FAFC))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A),
        modifier = modifier
    ) { padding ->
        if (prof == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No profitability data available.", color = Color(0xFF94A3B8))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF34D399).copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("NET PROFITABILITY STATUS", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(prof.status.defaultLabel, color = Color(0xFF34D399), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Net Profit Margin", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    Text("${prof.netProfitMarginPercent ?: 0.0}%", color = Color(0xFFF8FAFC), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Expense / Revenue", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    Text("${prof.expenseToRevenueRatioPercent ?: 0.0}%", color = Color(0xFFF8FAFC), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Trend", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    Text(prof.trend.defaultLabel, color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ExecutiveKpiCard("Gross Revenue", prof.totalRevenue, accentColor = Color(0xFF38BDF8), modifier = Modifier.weight(1f))
                        ExecutiveKpiCard("Operating Expenses", prof.totalExpenses, accentColor = Color(0xFFFCA5A5), modifier = Modifier.weight(1f))
                    }
                }

                item {
                    ExecutiveKpiCard(
                        "Net Financial Result",
                        prof.netProfit,
                        subtitle = "Surplus / Deficit after operating expenses",
                        accentColor = if (prof.netProfit.isPositive()) Color(0xFF34D399) else Color(0xFFF87171),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
