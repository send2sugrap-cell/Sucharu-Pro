package com.sucharu.sucharupro.ui.features.finance.reporting

import androidx.compose.foundation.background
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
fun GeneralLedgerReportScreen(
    viewModel: FinancialReportingViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val report = state.generalLedgerReport

    LaunchedEffect(Unit) {
        viewModel.selectReportType(FinancialReportType.GENERAL_LEDGER)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("General Ledger Report", color = Color(0xFFF8FAFC), fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFFF8FAFC))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.createSnapshot(FinancialReportType.GENERAL_LEDGER) }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Snapshot", tint = Color(0xFF34D399))
                    }
                    IconButton(onClick = { viewModel.requestExport(FinancialReportType.GENERAL_LEDGER, FinancialReportExportFormat.PDF) }) {
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
                            Text("Consolidated General Ledger", color = Color(0xFFF8FAFC), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Total Debit", color = Color(0xFF6EE7B7), fontSize = 11.sp)
                                    Text("${report.totalDebitPosted.formatted()} BDT", color = Color(0xFFF8FAFC), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Total Credit", color = Color(0xFF38BDF8), fontSize = 11.sp)
                                    Text("${report.totalCreditPosted.formatted()} BDT", color = Color(0xFFF8FAFC), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Closing Balance", color = Color(0xFFFDE68A), fontSize = 11.sp)
                                    Text("${report.closingBalance.formatted()} BDT", color = Color(0xFFF8FAFC), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    Text("Chronological Ledger Entries (${report.entries.size})", color = Color(0xFF94A3B8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                items(report.entries) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(entry.accountHead, color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(entry.referenceType.defaultLabel, color = Color(0xFF94A3B8), fontSize = 10.sp)
                            }
                            Text(entry.narration, color = Color(0xFFF8FAFC), fontSize = 12.sp, maxLines = 2)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                if (!entry.debit.isZero()) Text("Dr: ${entry.debit.formatted()}", color = Color(0xFF6EE7B7), fontSize = 11.sp)
                                if (!entry.credit.isZero()) Text("Cr: ${entry.credit.formatted()}", color = Color(0xFFFCA5A5), fontSize = 11.sp)
                                Text("Running: ${entry.runningBalance.formatted()} BDT", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
