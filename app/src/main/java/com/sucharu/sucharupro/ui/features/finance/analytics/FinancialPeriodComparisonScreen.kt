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
fun FinancialPeriodComparisonScreen(
    viewModel: FinanceAnalyticsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val comp = state.periodComparison

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Period Comparison Analytics", color = Color(0xFFF8FAFC), fontSize = 18.sp, fontWeight = FontWeight.Bold) },
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
        if (comp == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No comparison data available.", color = Color(0xFF94A3B8))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("PERIOD COMPARISON OVERVIEW", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${comp.periodALabel}  vs  ${comp.periodBLabel}", color = Color(0xFFF8FAFC), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ComparisonMetricRow("Gross Revenue", "${comp.revenueA.formatted()} BDT", "${comp.revenueB.formatted()} BDT", "${comp.revenueVariance.formatted()} BDT (${comp.revenueVariancePercent ?: 0.0}%)")
                            ComparisonMetricRow("Operating Expenses", "${comp.expensesA.formatted()} BDT", "${comp.expensesB.formatted()} BDT", "${comp.expensesVariance.formatted()} BDT (${comp.expensesVariancePercent ?: 0.0}%)")
                            ComparisonMetricRow("Net Financial Profit", "${comp.netProfitA.formatted()} BDT", "${comp.netProfitB.formatted()} BDT", "${comp.netProfitVariance.formatted()} BDT (${comp.netProfitVariancePercent ?: 0.0}%)")
                            ComparisonMetricRow("Customer Collections", "${comp.cashInA.formatted()} BDT", "${comp.cashInB.formatted()} BDT", "${comp.cashInVariance.formatted()} BDT (${comp.cashInVariancePercent ?: 0.0}%)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComparisonMetricRow(metric: String, valA: String, valB: String, variance: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(metric, color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Period A: $valA", color = Color(0xFF94A3B8), fontSize = 11.sp)
            Text("Period B: $valB", color = Color(0xFF94A3B8), fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text("Variance: $variance", color = Color(0xFFF8FAFC), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Divider(color = Color(0xFF334155).copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(top = 8.dp))
    }
}
