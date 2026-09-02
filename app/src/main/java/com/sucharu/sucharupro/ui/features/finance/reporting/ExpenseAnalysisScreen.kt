package com.sucharu.sucharupro.ui.features.finance.reporting

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.sucharu.sucharupro.domain.model.finance.FinancialReportExportFormat
import com.sucharu.sucharupro.domain.model.finance.FinancialReportType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseAnalysisScreen(
    viewModel: FinancialReportingViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val report = state.expenseAnalysisReport

    LaunchedEffect(Unit) {
        viewModel.selectReportType(FinancialReportType.EXPENSE_ANALYSIS)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Analysis", color = Color(0xFFF8FAFC), fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFFF8FAFC))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.createSnapshot(FinancialReportType.EXPENSE_ANALYSIS) }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Snapshot", tint = Color(0xFF34D399))
                    }
                    IconButton(onClick = { viewModel.requestExport(FinancialReportType.EXPENSE_ANALYSIS, FinancialReportExportFormat.PDF) }) {
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
            item {
                ReportPeriodSelector(
                    selectedPeriod = state.selectedPeriod,
                    onPeriodSelected = { viewModel.selectPeriod(it) }
                )
            }

            if (report != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Expense Overview", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Total Expenses", color = Color(0xFFFCA5A5), fontSize = 11.sp)
                                    Text("${report.totalExpenses.formatted()} BDT", color = Color(0xFFF8FAFC), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Posted", color = Color(0xFF6EE7B7), fontSize = 11.sp)
                                    Text("${report.postedExpenses.formatted()} BDT", color = Color(0xFFF8FAFC), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Pending", color = Color(0xFFFDE68A), fontSize = 11.sp)
                                    Text("${report.pendingExpenses.formatted()} BDT", color = Color(0xFFF8FAFC), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    Text("Category-wise Breakdown", color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                items(report.categoryBreakdown) { cat ->
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(cat.categoryName, color = Color(0xFFF8FAFC), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("${cat.expenseCount} entries • ${String.format("%.1f", cat.percentageOfTotal)}% of total", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                            Text("${cat.totalAmount.formatted()} BDT", color = Color(0xFFFCA5A5), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
