package com.sucharu.sucharupro.ui.features.finance.analytics

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
fun PayableAnalyticsScreen(
    viewModel: FinanceAnalyticsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val pay = state.payable

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payable & Supplier Intelligence", color = Color(0xFFF8FAFC), fontSize = 18.sp, fontWeight = FontWeight.Bold) },
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
        if (pay == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No payable data available.", color = Color(0xFF94A3B8))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ExecutiveKpiCard("Total Payable Dues", pay.totalPayables, accentColor = Color(0xFFFCA5A5), modifier = Modifier.weight(1f))
                        ExecutiveKpiCard("Overdue Obligations", pay.overduePayables, subtitle = "Exposure: ${pay.overduePercentage ?: 0.0}%", accentColor = Color(0xFFF87171), modifier = Modifier.weight(1f))
                    }
                }

                item {
                    Text("Payable Aging Breakdown", color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AgingRow("Current Obligations", "${pay.currentPayables.formatted()} BDT", Color(0xFF6EE7B7))
                            AgingRow("1–30 Days Overdue", "${pay.overdue1To30.formatted()} BDT", Color(0xFFFDE68A))
                            AgingRow("31–60 Days Overdue", "${pay.overdue31To60.formatted()} BDT", Color(0xFFF97316))
                            AgingRow("61–90 Days Overdue", "${pay.overdue61To90.formatted()} BDT", Color(0xFFEF4444))
                            AgingRow("90+ Days Critical Overdue", "${pay.overdue90Plus.formatted()} BDT", Color(0xFFDC2626))
                        }
                    }
                }
            }
        }
    }
}
