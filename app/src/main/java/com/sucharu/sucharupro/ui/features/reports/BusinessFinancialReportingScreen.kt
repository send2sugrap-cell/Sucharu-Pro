package com.sucharu.sucharupro.ui.features.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.businessfinancialreporting.*
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

enum class ReportingTab(val title: String, val icon: ImageVector) {
    OVERVIEW("Overview", Icons.Default.Dashboard),
    EXPENSES("Expenses", Icons.Default.ReceiptLong),
    PAYABLES("Payables", Icons.Default.AccountBalanceWallet),
    LEDGER("Ledger", Icons.Default.MenuBook),
    COST_CENTERS("Cost Centers", Icons.Default.Category),
    PROJECT_COSTS("Job / Projects", Icons.Default.WorkOutline),
    COMMITMENTS("Commitments", Icons.Default.LockClock),
    RECONCILIATION("Reconciliation", Icons.Default.FactCheck),
    ADJUSTMENTS("Adjustments", Icons.Default.Tune),
    PERIOD_END("Period-End", Icons.Default.CalendarToday),
    SNAPSHOTS("Snapshots", Icons.Default.CameraAlt)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessFinancialReportingScreen(
    executiveSummary: BusinessExecutiveFinancialSummaryDto? = null,
    expenseReport: BusinessExpenseAnalyticsReportDto? = null,
    payableReport: VendorPayableAnalyticsReportDto? = null,
    ledgerReport: BusinessLedgerReportDto? = null,
    costCenterReport: BusinessCostCenterReportDto? = null,
    projectCostReport: JobProjectCostReportDto? = null,
    commitmentReport: CommitmentAccrualReportDto? = null,
    reconciliationReport: BusinessReconciliationReportDto? = null,
    adjustmentReport: BusinessFinancialAdjustmentReportDto? = null,
    periodEndReport: BusinessPeriodEndReadinessReportDto? = null,
    snapshots: List<BusinessFinancialReportSnapshotDto> = emptyList(),
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRefresh: () -> Unit = {},
    onFilterChange: (String?, String) -> Unit = { _, _ -> },
    onCreateSnapshot: (String, String?) -> Unit = { _, _ -> },
    onExportReport: (String, String) -> Unit = { _, _ -> }
) {
    var selectedTab by remember { mutableStateOf(ReportingTab.OVERVIEW) }
    var selectedCurrency by remember { mutableStateOf("BDT") }
    var selectedPeriodId by remember { mutableStateOf<String?>(null) }
    var showSnapshotDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    val bgDark = Color(0xFF090E17)
    val cardBg = Color(0xFF131D2E)
    val cardBorder = Color(0xFF1E2A3E)
    val accentCyan = Color(0xFF00E5FF)
    val accentGreen = Color(0xFF00E676)
    val accentRed = Color(0xFFFF5252)
    val accentAmber = Color(0xFFFFD600)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF90A4AE)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Reporting Icon",
                            tint = accentCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Financial Reporting & Intelligence",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = "Canonical Multi-Module Financial Analytics Engine",
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.Download, contentDescription = "Export Report", tint = accentCyan)
                    }
                    IconButton(onClick = { showSnapshotDialog = true }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Take Snapshot", tint = accentAmber)
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Data", tint = textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D1424))
            )
        },
        containerColor = bgDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter Bar
            Surface(
                color = Color(0xFF0D1424),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Currency: ", fontSize = 12.sp, color = textSecondary)
                        AssistChip(
                            onClick = {
                                selectedCurrency = if (selectedCurrency == "BDT") "USD" else "BDT"
                                onFilterChange(selectedPeriodId, selectedCurrency)
                            },
                            label = { Text(selectedCurrency, color = accentCyan, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = accentCyan, modifier = Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = cardBg)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Period: ", fontSize = 12.sp, color = textSecondary)
                        AssistChip(
                            onClick = {
                                selectedPeriodId = if (selectedPeriodId == null) "PER-2026-08" else null
                                onFilterChange(selectedPeriodId, selectedCurrency)
                            },
                            label = { Text(selectedPeriodId ?: "All Active", color = accentGreen, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = accentGreen, modifier = Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = cardBg)
                        )
                    }
                }
            }

            // Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color(0xFF0D1424),
                contentColor = accentCyan,
                edgePadding = 12.dp,
                divider = { HorizontalDivider(color = cardBorder) }
            ) {
                ReportingTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(tab.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tab.title, fontSize = 13.sp, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal)
                            }
                        },
                        selectedContentColor = accentCyan,
                        unselectedContentColor = textSecondary
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentCyan)
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = accentRed, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = errorMessage, color = accentRed, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRefresh, colors = ButtonDefaults.buttonColors(containerColor = cardBg)) {
                            Text("Retry", color = textPrimary)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (selectedTab) {
                        ReportingTab.OVERVIEW -> {
                            if (executiveSummary != null) {
                                item { ExecutiveSummaryView(summary = executiveSummary, accentCyan = accentCyan, accentGreen = accentGreen, accentRed = accentRed, accentAmber = accentAmber, cardBg = cardBg, cardBorder = cardBorder) }
                            } else {
                                item { EmptyStateCard("No Executive Summary Data Available") }
                            }
                        }
                        ReportingTab.EXPENSES -> {
                            if (expenseReport != null) {
                                item { ExpenseAnalyticsView(report = expenseReport, accentCyan = accentCyan, accentGreen = accentGreen, cardBg = cardBg, cardBorder = cardBorder) }
                            } else {
                                item { EmptyStateCard("No Expense Analytics Available") }
                            }
                        }
                        ReportingTab.PAYABLES -> {
                            if (payableReport != null) {
                                item { VendorPayablesView(report = payableReport, accentCyan = accentCyan, accentRed = accentRed, cardBg = cardBg, cardBorder = cardBorder) }
                            } else {
                                item { EmptyStateCard("No Vendor Payable Analytics Available") }
                            }
                        }
                        ReportingTab.LEDGER -> {
                            if (ledgerReport != null) {
                                item { BusinessLedgerView(report = ledgerReport, accentCyan = accentCyan, accentGreen = accentGreen, cardBg = cardBg, cardBorder = cardBorder) }
                            } else {
                                item { EmptyStateCard("No Business Ledger Report Available") }
                            }
                        }
                        ReportingTab.COST_CENTERS -> {
                            if (costCenterReport != null) {
                                item { CostCenterReportView(report = costCenterReport, accentCyan = accentCyan, cardBg = cardBg, cardBorder = cardBorder) }
                            } else {
                                item { EmptyStateCard("No Cost Center Data Available") }
                            }
                        }
                        ReportingTab.PROJECT_COSTS -> {
                            if (projectCostReport != null) {
                                item { ProjectCostReportView(report = projectCostReport, accentCyan = accentCyan, accentGreen = accentGreen, cardBg = cardBg, cardBorder = cardBorder) }
                            } else {
                                item { EmptyStateCard("No Project / Job Cost Data Available") }
                            }
                        }
                        ReportingTab.COMMITMENTS -> {
                            if (commitmentReport != null) {
                                item { CommitmentAccrualView(report = commitmentReport, accentCyan = accentCyan, accentAmber = accentAmber, cardBg = cardBg, cardBorder = cardBorder) }
                            } else {
                                item { EmptyStateCard("No Commitment & Accrual Data Available") }
                            }
                        }
                        ReportingTab.RECONCILIATION -> {
                            if (reconciliationReport != null) {
                                item { ReconciliationReportView(report = reconciliationReport, accentCyan = accentCyan, accentGreen = accentGreen, accentRed = accentRed, cardBg = cardBg, cardBorder = cardBorder) }
                            } else {
                                item { EmptyStateCard("No Reconciliation Report Available") }
                            }
                        }
                        ReportingTab.ADJUSTMENTS -> {
                            if (adjustmentReport != null) {
                                item { AdjustmentReportView(report = adjustmentReport, accentCyan = accentCyan, accentAmber = accentAmber, cardBg = cardBg, cardBorder = cardBorder) }
                            } else {
                                item { EmptyStateCard("No Adjustment Report Available") }
                            }
                        }
                        ReportingTab.PERIOD_END -> {
                            if (periodEndReport != null) {
                                item { PeriodEndReadinessView(report = periodEndReport, accentCyan = accentCyan, accentGreen = accentGreen, accentRed = accentRed, cardBg = cardBg, cardBorder = cardBorder) }
                            } else {
                                item { EmptyStateCard("Select a Financial Period to view Readiness Diagnostics") }
                            }
                        }
                        ReportingTab.SNAPSHOTS -> {
                            item { SnapshotsListView(snapshots = snapshots, accentCyan = accentCyan, accentAmber = accentAmber, cardBg = cardBg, cardBorder = cardBorder) }
                        }
                    }
                }
            }
        }
    }

    // Snapshot Dialog
    if (showSnapshotDialog) {
        AlertDialog(
            onDismissRequest = { showSnapshotDialog = false },
            title = { Text("Take Report Snapshot", color = textPrimary) },
            text = {
                Text(
                    "This will create an immutable, tamper-evident snapshot of the current '${selectedTab.title}' report with a cryptographic SHA-256 integrity seal.",
                    color = textSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateSnapshot(selectedTab.name, selectedPeriodId)
                        showSnapshotDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentCyan)
                ) {
                    Text("Capture Snapshot", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSnapshotDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            },
            containerColor = cardBg
        )
    }

    // Export Dialog
    if (showExportDialog) {
        var exportFormat by remember { mutableStateOf("CSV") }
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Financial Report", color = textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select output format for '${selectedTab.title}':", color = textSecondary, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = exportFormat == "CSV",
                            onClick = { exportFormat = "CSV" },
                            label = { Text("CSV (Spreadsheet)") }
                        )
                        FilterChip(
                            selected = exportFormat == "JSON",
                            onClick = { exportFormat = "JSON" },
                            label = { Text("JSON") }
                        )
                        FilterChip(
                            selected = exportFormat == "PDF_TEXT",
                            onClick = { exportFormat = "PDF_TEXT" },
                            label = { Text("Text Document") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onExportReport(selectedTab.name, exportFormat)
                        showExportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentGreen)
                ) {
                    Text("Export Document", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            },
            containerColor = cardBg
        )
    }
}

// --- Subviews ---

@Composable
private fun ExecutiveSummaryView(
    summary: BusinessExecutiveFinancialSummaryDto,
    accentCyan: Color,
    accentGreen: Color,
    accentRed: Color,
    accentAmber: Color,
    cardBg: Color,
    cardBorder: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Status Readiness Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = if (summary.periodReadinessStatus == "READY") Color(0xFF0F3822) else Color(0xFF381515)),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (summary.periodReadinessStatus == "READY") accentGreen else accentRed),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (summary.periodReadinessStatus == "READY") Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (summary.periodReadinessStatus == "READY") accentGreen else accentRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Period-End Readiness: ${summary.periodReadinessStatus}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (summary.periodReadinessStatus == "READY") "All financial reconciliations and period-end checks passed." else "${summary.periodClosureBlockerCount} blockers preventing closing.",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Primary KPI Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiCard(
                title = "Total Expenses",
                value = "${formatCurrency(summary.totalExpenseAmount)} ${summary.currency}",
                subtitle = "${summary.expenseCount} expenses (${formatCurrency(summary.approvedExpenseAmount)} approved)",
                icon = Icons.Default.ReceiptLong,
                accentColor = accentCyan,
                modifier = Modifier.weight(1f),
                cardBg = cardBg,
                cardBorder = cardBorder
            )
            KpiCard(
                title = "Outstanding Payables",
                value = "${formatCurrency(summary.outstandingPayableAmount)} ${summary.currency}",
                subtitle = "Overdue: ${formatCurrency(summary.overduePayableAmount)} ${summary.currency}",
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = accentRed,
                modifier = Modifier.weight(1f),
                cardBg = cardBg,
                cardBorder = cardBorder
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiCard(
                title = "Ledger Net Movement",
                value = "${formatCurrency(summary.netLedgerMovement)} ${summary.currency}",
                subtitle = "Debit: ${formatCurrency(summary.totalLedgerDebit)} | Credit: ${formatCurrency(summary.totalLedgerCredit)}",
                icon = Icons.Default.MenuBook,
                accentColor = accentGreen,
                modifier = Modifier.weight(1f),
                cardBg = cardBg,
                cardBorder = cardBorder
            )
            KpiCard(
                title = "Allocated Cost",
                value = "${formatCurrency(summary.totalAllocatedCost)} ${summary.currency}",
                subtitle = "Unallocated: ${formatCurrency(summary.totalUnallocatedCost)} ${summary.currency}",
                icon = Icons.Default.Category,
                accentColor = accentAmber,
                modifier = Modifier.weight(1f),
                cardBg = cardBg,
                cardBorder = cardBorder
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiCard(
                title = "Active Commitments",
                value = "${formatCurrency(summary.remainingCommitmentAmount)} ${summary.currency}",
                subtitle = "${summary.activeCommitmentCount} active commitments",
                icon = Icons.Default.LockClock,
                accentColor = accentCyan,
                modifier = Modifier.weight(1f),
                cardBg = cardBg,
                cardBorder = cardBorder
            )
            KpiCard(
                title = "Outstanding Accruals",
                value = "${formatCurrency(summary.outstandingAccrualAmount)} ${summary.currency}",
                subtitle = "${summary.activeAccrualCount} active accruals",
                icon = Icons.Default.Savings,
                accentColor = accentAmber,
                modifier = Modifier.weight(1f),
                cardBg = cardBg,
                cardBorder = cardBorder
            )
        }
    }
}

@Composable
private fun ExpenseAnalyticsView(
    report: BusinessExpenseAnalyticsReportDto,
    accentCyan: Color,
    accentGreen: Color,
    cardBg: Color,
    cardBorder: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Expense Breakdown by Category", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))
                report.categoryBreakdown.forEach { cat ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(cat.category, color = Color.LightGray, fontSize = 12.sp)
                        Text("${formatCurrency(cat.totalAmount)} (${cat.percentage}%)", color = accentCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun VendorPayablesView(
    report: VendorPayableAnalyticsReportDto,
    accentCyan: Color,
    accentRed: Color,
    cardBg: Color,
    cardBorder: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Payables Aging Analysis", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))
                report.agingBuckets.forEach { bucket ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(bucket.label, color = Color.LightGray, fontSize = 12.sp)
                        Text("${formatCurrency(bucket.amount)} ${bucket.currency} (${bucket.count} bills)", color = if (bucket.bucketType == "CURRENT") accentCyan else accentRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun BusinessLedgerView(
    report: BusinessLedgerReportDto,
    accentCyan: Color,
    accentGreen: Color,
    cardBg: Color,
    cardBorder: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Ledger Postings by Source Type", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))
            report.sourceBreakdowns.forEach { src ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(src.sourceType, color = Color.LightGray, fontSize = 12.sp)
                    Text("Debit: ${formatCurrency(src.debitAmount)} | Credit: ${formatCurrency(src.creditAmount)}", color = accentGreen, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CostCenterReportView(
    report: BusinessCostCenterReportDto,
    accentCyan: Color,
    cardBg: Color,
    cardBorder: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Cost Centers Performance", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))
            report.costCenters.forEach { cc ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${cc.code} - ${cc.name}", color = Color.LightGray, fontSize = 12.sp)
                    Text("Total: ${formatCurrency(cc.totalTrackedAmount)} ${report.currency}", color = accentCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ProjectCostReportView(
    report: JobProjectCostReportDto,
    accentCyan: Color,
    accentGreen: Color,
    cardBg: Color,
    cardBorder: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Job / Project Profitability & Cost Tracking", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))
            report.jobCosts.forEach { j ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Job: ${j.jobId}", color = Color.LightGray, fontSize = 12.sp)
                    Text("Cost: ${formatCurrency(j.totalCost)} | Margin: ${j.marginPercentage?.let { "$it%" } ?: "N/A"}", color = accentGreen, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CommitmentAccrualView(
    report: CommitmentAccrualReportDto,
    accentCyan: Color,
    accentAmber: Color,
    cardBg: Color,
    cardBorder: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Commitment & Accrual Register", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))
            report.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("[${item.type}] ${item.title}", color = Color.LightGray, fontSize = 12.sp)
                    Text("Rem: ${formatCurrency(item.remainingOutstandingAmount)} ${item.currency}", color = accentAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ReconciliationReportView(
    report: BusinessReconciliationReportDto,
    accentCyan: Color,
    accentGreen: Color,
    accentRed: Color,
    cardBg: Color,
    cardBorder: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Financial Reconciliation Status", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text("Reconciled Balance: ${formatCurrency(report.reconciledAmount)} ${report.currency}", color = accentGreen, fontSize = 13.sp)
            Text("Unreconciled Variance: ${formatCurrency(report.unreconciledAmount)} ${report.currency}", color = if (report.unreconciledAmount == BigDecimal.ZERO) accentGreen else accentRed, fontSize = 13.sp)
            Text("Open Discrepancies: ${report.openDiscrepanciesCount}", color = if (report.openDiscrepanciesCount == 0) accentGreen else accentRed, fontSize = 13.sp)
        }
    }
}

@Composable
private fun AdjustmentReportView(
    report: BusinessFinancialAdjustmentReportDto,
    accentCyan: Color,
    accentAmber: Color,
    cardBg: Color,
    cardBorder: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Financial Adjustments & Corrections", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text("Total Adjustments: ${formatCurrency(report.totalAdjustmentAmount)} ${report.currency}", color = accentCyan, fontSize = 13.sp)
            Text("Total Refunds: ${formatCurrency(report.totalRefundAmount)} ${report.currency}", color = accentAmber, fontSize = 13.sp)
            Text("Total Write-Offs: ${formatCurrency(report.totalWriteOffAmount)} ${report.currency}", color = accentAmber, fontSize = 13.sp)
        }
    }
}

@Composable
private fun PeriodEndReadinessView(
    report: BusinessPeriodEndReadinessReportDto,
    accentCyan: Color,
    accentGreen: Color,
    accentRed: Color,
    cardBg: Color,
    cardBorder: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = if (report.readinessStatus == "READY") Color(0xFF0F3822) else Color(0xFF381515)),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (report.readinessStatus == "READY") accentGreen else accentRed),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Period: ${report.periodCode} (${report.periodName})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Readiness Status: ${report.readinessStatus}", color = if (report.readinessStatus == "READY") accentGreen else accentRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        if (report.blockers.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Closure Blockers (${report.blockerCount})", color = accentRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    report.blockers.forEach { b ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Icon(Icons.Default.Cancel, contentDescription = null, tint = accentRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("[${b.code}] ${b.description}", color = Color.LightGray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SnapshotsListView(
    snapshots: List<BusinessFinancialReportSnapshotDto>,
    accentCyan: Color,
    accentAmber: Color,
    cardBg: Color,
    cardBorder: Color
) {
    if (snapshots.isEmpty()) {
        EmptyStateCard("No Report Snapshots captured yet.")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            snapshots.forEach { snap ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(snap.snapshotId, color = accentCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(snap.generatedAt)), color = Color.Gray, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Type: ${snap.reportType} | Period: ${snap.periodId ?: "ALL"}", color = Color.LightGray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("SHA-256: ${snap.integrityHash.take(16)}...", color = accentAmber, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    cardBg: Color,
    cardBorder: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Color.LightGray, fontSize = 11.sp)
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2E)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2A3E)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = message, color = Color.LightGray, fontSize = 13.sp)
        }
    }
}

private fun formatCurrency(amount: BigDecimal?): String {
    if (amount == null) return "0.00"
    val nf = NumberFormat.getNumberInstance(Locale.US)
    nf.minimumFractionDigits = 2
    nf.maximumFractionDigits = 2
    return nf.format(amount)
}
