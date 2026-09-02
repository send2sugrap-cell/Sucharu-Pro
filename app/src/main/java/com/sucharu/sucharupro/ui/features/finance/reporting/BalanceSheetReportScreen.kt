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
fun BalanceSheetReportScreen(
    viewModel: FinancialReportingViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val report = state.balanceSheetReport

    LaunchedEffect(Unit) {
        viewModel.selectReportType(FinancialReportType.BALANCE_SHEET)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Balance Sheet Statement", color = Color(0xFFF8FAFC), fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFFF8FAFC))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.createSnapshot(FinancialReportType.BALANCE_SHEET) }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Snapshot", tint = Color(0xFF34D399))
                    }
                    IconButton(onClick = { viewModel.requestExport(FinancialReportType.BALANCE_SHEET, FinancialReportExportFormat.PDF) }) {
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
                if (report.controlExceptions.isNotEmpty()) {
                    item {
                        ControlExceptionBanner(exceptions = report.controlExceptions)
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Equation Status: Assets = Liab + Equity", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                ReportStatusBadge(status = report.status)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Total Assets", color = Color(0xFF6EE7B7), fontSize = 11.sp)
                                    Text("${report.totalAssets.formatted()} BDT", color = Color(0xFFF8FAFC), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Total Liabilities", color = Color(0xFFFCA5A5), fontSize = 11.sp)
                                    Text("${report.totalLiabilities.formatted()} BDT", color = Color(0xFFF8FAFC), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Total Equity", color = Color(0xFF38BDF8), fontSize = 11.sp)
                                    Text("${report.totalEquity.formatted()} BDT", color = Color(0xFFF8FAFC), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item { Text("Assets", color = Color(0xFF6EE7B7), fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                items(report.assetLines) { line -> ReportLineRow(line = line) }

                item { Text("Liabilities", color = Color(0xFFFCA5A5), fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                items(report.liabilityLines) { line -> ReportLineRow(line = line) }

                item { Text("Equity / Capital", color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                items(report.equityLines) { line -> ReportLineRow(line = line) }
            }
        }
    }
}
