package com.sucharu.sucharupro.ui.features.finance.reporting

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialReportExportFormat
import com.sucharu.sucharupro.domain.model.finance.FinancialReportType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodComparisonScreen(
    viewModel: FinancialReportingViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val comp = state.periodComparisonResult

    LaunchedEffect(Unit) {
        viewModel.selectReportType(FinancialReportType.PERIOD_COMPARISON)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Period-over-Period Comparison", color = Color(0xFFF8FAFC), fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFFF8FAFC))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.createSnapshot(FinancialReportType.PERIOD_COMPARISON) }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Snapshot", tint = Color(0xFF34D399))
                    }
                    IconButton(onClick = { viewModel.requestExport(FinancialReportType.PERIOD_COMPARISON, FinancialReportExportFormat.PDF) }) {
                        Icon(Icons.Default.Download, contentDescription = "Export PDF", tint = Color(0xFF60A5FA))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A),
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (comp != null) {
                item {
                    Text(
                        "${comp.periodALabel} vs ${comp.periodBLabel}",
                        color = Color(0xFF38BDF8),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    ComparisonCard("Revenue", comp.revenueA, comp.revenueB, comp.revenueAbsoluteChange, comp.revenuePercentageChange)
                }
                item {
                    ComparisonCard("Operating Expenses", comp.expensesA, comp.expensesB, comp.expensesAbsoluteChange, comp.expensesPercentageChange)
                }
                item {
                    ComparisonCard("Net Profit", comp.profitA, comp.profitB, comp.profitAbsoluteChange, comp.profitPercentageChange)
                }
                item {
                    ComparisonCard("Collections", comp.collectionsA, comp.collectionsB, comp.collectionsAbsoluteChange, comp.collectionsPercentageChange)
                }
                item {
                    ComparisonCard("Supplier Payments", comp.supplierPaymentsA, comp.supplierPaymentsB, comp.supplierPaymentsAbsoluteChange, comp.supplierPaymentsPercentageChange)
                }
            }
        }
    }
}

@Composable
private fun ComparisonCard(
    title: String,
    valA: Money,
    valB: Money,
    diff: Money,
    pct: Double?
) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, color = Color(0xFF94A3B8), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                if (pct != null) {
                    val isPos = pct >= 0
                    Surface(
                        color = if (isPos) Color(0xFF064E3B) else Color(0xFF7F1D1D),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${if (isPos) "+" else ""}${String.format("%.1f", pct)}%",
                            color = if (isPos) Color(0xFF6EE7B7) else Color(0xFFFCA5A5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Period A", color = Color(0xFF64748B), fontSize = 10.sp)
                    Text("${valA.formatted()} BDT", color = Color(0xFFF8FAFC), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Period B", color = Color(0xFF64748B), fontSize = 10.sp)
                    Text("${valB.formatted()} BDT", color = Color(0xFFF8FAFC), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Variance", color = Color(0xFF64748B), fontSize = 10.sp)
                    Text("${diff.formatted()} BDT", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
