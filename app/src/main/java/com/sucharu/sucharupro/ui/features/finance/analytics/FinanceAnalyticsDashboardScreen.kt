package com.sucharu.sucharupro.ui.features.finance.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.finance.FinancialReportPeriod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceAnalyticsDashboardScreen(
    viewModel: FinanceAnalyticsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Finance Analytics & Governance", color = Color(0xFFF8FAFC), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Module 09 Step 10 • Executive Health", color = Color(0xFF38BDF8), fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFFF8FAFC))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.createSnapshot() }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Save Snapshot", tint = Color(0xFF34D399))
                    }
                    IconButton(onClick = { viewModel.loadDashboard() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF38BDF8))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A),
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Period Selector Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val periods = listOf(
                    FinancialReportPeriod.Today,
                    FinancialReportPeriod.CurrentWeek,
                    FinancialReportPeriod.CurrentMonth,
                    FinancialReportPeriod.PreviousMonth
                )
                items(periods) { p ->
                    val isSelected = state.filter.reportPeriod == p
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B),
                        modifier = Modifier.clickable { viewModel.selectPeriod(p) }
                    ) {
                        Text(
                            p.defaultLabel,
                            color = if (isSelected) Color(0xFF0F172A) else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Tab Selector Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(FinanceAnalyticsTab.values()) { tab ->
                    val isSelected = state.selectedTab == tab
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF1E293B) else Color.Transparent,
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.clickable { viewModel.selectTab(tab) }
                    ) {
                        Text(
                            tab.defaultLabel,
                            color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF38BDF8))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    state.successMessage?.let { msg ->
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF34D399).copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(msg, color = Color(0xFF34D399), fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                            }
                        }
                    }

                    // Health Score Card
                    state.healthScore?.let { health ->
                        item { HealthScoreCard(healthScore = health) }
                    }

                    // Executive Summary Grid
                    state.summary?.let { summary ->
                        item {
                            Text("Executive Financial Positions", color = Color(0xFFF8FAFC), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ExecutiveKpiCard("Total Revenue", summary.totalRevenue, accentColor = Color(0xFF38BDF8), modifier = Modifier.weight(1f))
                                ExecutiveKpiCard("Operating Expenses", summary.totalExpenses, accentColor = Color(0xFFFCA5A5), modifier = Modifier.weight(1f))
                            }
                        }
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ExecutiveKpiCard("Net Profit", summary.netProfit, accentColor = if (summary.netProfit.isPositive()) Color(0xFF34D399) else Color(0xFFF87171), modifier = Modifier.weight(1f))
                                ExecutiveKpiCard("Cash Position", summary.cashPosition, accentColor = Color(0xFFFDE68A), modifier = Modifier.weight(1f))
                            }
                        }
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ExecutiveKpiCard("Receivable Due", summary.totalReceivables, subtitle = "Overdue: ${summary.overdueReceivables.formatted()}", accentColor = Color(0xFF38BDF8), modifier = Modifier.weight(1f))
                                ExecutiveKpiCard("Payable Obligation", summary.totalPayables, subtitle = "Overdue: ${summary.overduePayables.formatted()}", accentColor = Color(0xFFFCA5A5), modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // Top Early Warning Risks
                    if (state.risks.isNotEmpty()) {
                        item {
                            Text("Active Risk Indicators (${state.risks.size})", color = Color(0xFFF87171), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        items(state.risks) { risk ->
                            RiskAlertCard(risk = risk)
                        }
                    }

                    // Governance Checks
                    if (state.governanceControls.isNotEmpty()) {
                        item {
                            Text("Governance Invariant Controls", color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        items(state.governanceControls) { control ->
                            GovernanceControlRow(control = control)
                        }
                    }
                }
            }
        }
    }
}
