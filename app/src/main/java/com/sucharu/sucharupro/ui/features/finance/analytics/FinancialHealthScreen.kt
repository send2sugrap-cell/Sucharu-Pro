package com.sucharu.sucharupro.ui.features.finance.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun FinancialHealthScreen(
    viewModel: FinanceAnalyticsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val health = state.healthScore

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Financial Health Analytics", color = Color(0xFFF8FAFC), fontSize = 18.sp, fontWeight = FontWeight.Bold) },
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
        if (health == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No health data available.", color = Color(0xFF94A3B8))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { HealthScoreCard(healthScore = health) }

                item {
                    Text("Dimension Scores Breakdown", color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            DimensionRow("Liquidity & Cash Runway", health.liquidityScore)
                            DimensionRow("Profitability & Margins", health.profitabilityScore)
                            DimensionRow("Receivable Collection Health", health.receivableHealthScore)
                            DimensionRow("Payable Settlement Discipline", health.payableHealthScore)
                            DimensionRow("Operational Expense Control", health.expenseControlScore)
                            DimensionRow("Reconciliation & Ledger Health", health.reconciliationHealthScore)
                            DimensionRow("Governance Invariants & Balance", health.governanceControlScore)
                        }
                    }
                }

                if (health.criticalIndicators.isNotEmpty()) {
                    item {
                        Text("Critical Alerts (${health.criticalIndicators.size})", color = Color(0xFFF87171), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    items(health.criticalIndicators) { alert ->
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFF87171).copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Text(alert, color = Color(0xFFFCA5A5), fontSize = 12.sp, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DimensionRow(name: String, score: Int) {
    val color = when {
        score >= 75 -> Color(0xFF34D399)
        score >= 50 -> Color(0xFFFBBF24)
        else -> Color(0xFFF87171)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, color = Color(0xFFF8FAFC), fontSize = 12.sp)
        Text("$score / 100", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
