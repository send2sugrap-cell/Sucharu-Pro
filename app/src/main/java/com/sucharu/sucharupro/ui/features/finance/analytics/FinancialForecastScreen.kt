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
fun FinancialForecastScreen(
    viewModel: FinanceAnalyticsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val fc = state.forecast

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Management Financial Forecast", color = Color(0xFFF8FAFC), fontSize = 18.sp, fontWeight = FontWeight.Bold) },
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
        if (fc == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No forecast projection available.", color = Color(0xFF94A3B8))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFBBF24).copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "NOTE: Management forecast estimates are deterministic projections based on historical moving averages. They are NOT posted financial facts.",
                            color = Color(0xFFFDE68A),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ExecutiveKpiCard("Projected Revenue", fc.projectedRevenue, accentColor = Color(0xFF38BDF8), modifier = Modifier.weight(1f))
                        ExecutiveKpiCard("Projected Expenses", fc.projectedExpenses, accentColor = Color(0xFFFCA5A5), modifier = Modifier.weight(1f))
                    }
                }

                item {
                    ExecutiveKpiCard(
                        "Projected Net Profit / Operating Surplus",
                        fc.projectedNetProfit,
                        subtitle = "Method: ${fc.method.defaultLabel} (${fc.confidenceLevel})",
                        accentColor = if (fc.projectedNetProfit.isPositive()) Color(0xFF34D399) else Color(0xFFF87171),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
