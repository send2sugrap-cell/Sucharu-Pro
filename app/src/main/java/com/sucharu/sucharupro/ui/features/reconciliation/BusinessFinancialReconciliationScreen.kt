package com.sucharu.sucharupro.ui.features.reconciliation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.sucharu.sucharupro.data.api.model.businessreconciliation.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

enum class ReconciliationTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Control Hub", Icons.Default.Dashboard),
    RUNS("Reconciliation Runs", Icons.Default.PlayCircleOutline),
    DISCREPANCIES("Discrepancy Queue", Icons.Default.WarningAmber),
    PERIOD_READINESS("Period Readiness", Icons.Default.FactCheck),
    AUDIT("Audit Trail", Icons.Default.History)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessFinancialReconciliationScreen(
    dashboard: ReconciliationDashboardSummaryResponse? = null,
    runs: List<BusinessFinancialReconciliationRunResponse> = emptyList(),
    discrepancies: List<BusinessFinancialReconciliationDiscrepancyResponse> = emptyList(),
    periodReadiness: PeriodCloseReadinessResponse? = null,
    auditEvents: List<BusinessFinancialReconciliationAuditEventResponse> = emptyList(),
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRefresh: () -> Unit = {},
    onCreateRun: (CreateReconciliationRunRequest) -> Unit = {},
    onExecuteRun: (String) -> Unit = {},
    onApproveRun: (String, String?) -> Unit = { _, _ -> },
    onAssignDiscrepancy: (String, String) -> Unit = { _, _ -> },
    onResolveDiscrepancy: (String, String) -> Unit = { _, _ -> },
    onWaiveDiscrepancy: (String, String) -> Unit = { _, _ -> },
    onRejectDiscrepancy: (String, String) -> Unit = { _, _ -> },
    onLinkCorrection: (String, String, String, String?) -> Unit = { _, _, _, _ -> }
) {
    var selectedTab by remember { mutableStateOf(ReconciliationTab.DASHBOARD) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "FINANCIAL RECONCILIATION & SETTLEMENT CONTROL",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF00E5FF)
                        )
                        Text(
                            text = "Canonical Multi-Tier Discrepancy Detection & Period Closure Gate",
                            fontSize = 12.sp,
                            color = Color(0xFF88A0C0)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color(0xFF00E5FF)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D1424)
                )
            )
        },
        containerColor = Color(0xFF080D1A)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Navigation Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color(0xFF0D1829),
                contentColor = Color(0xFF00E5FF),
                edgePadding = 12.dp
            ) {
                ReconciliationTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.title, fontWeight = FontWeight.Medium) },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        selectedContentColor = Color(0xFF00E5FF),
                        unselectedContentColor = Color(0xFF6A7F9D)
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF00E5FF))
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(Color(0xFF2A0808), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Text(text = "Error: $errorMessage", color = Color(0xFFFF5252))
                }
            } else {
                when (selectedTab) {
                    ReconciliationTab.DASHBOARD -> ReconciliationDashboardView(dashboard, runs, discrepancies)
                    ReconciliationTab.RUNS -> ReconciliationRunsView(runs, onExecuteRun, onApproveRun)
                    ReconciliationTab.DISCREPANCIES -> DiscrepanciesQueueView(discrepancies, onAssignDiscrepancy, onResolveDiscrepancy, onWaiveDiscrepancy)
                    ReconciliationTab.PERIOD_READINESS -> PeriodReadinessView(periodReadiness)
                    ReconciliationTab.AUDIT -> ReconciliationAuditView(auditEvents)
                }
            }
        }
    }
}

@Composable
fun ReconciliationDashboardView(
    dashboard: ReconciliationDashboardSummaryResponse?,
    runs: List<BusinessFinancialReconciliationRunResponse>,
    discrepancies: List<BusinessFinancialReconciliationDiscrepancyResponse>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "RECONCILIATION SUMMARY KPIs",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF88A0C0)
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ReconKpiCard(
                    title = "Total Runs",
                    value = "${dashboard?.totalRuns ?: runs.size}",
                    color = Color(0xFF00E5FF),
                    modifier = Modifier.weight(1f)
                )
                ReconKpiCard(
                    title = "Approved Runs",
                    value = "${dashboard?.approvedRuns ?: runs.count { it.status == "APPROVED" }}",
                    color = Color(0xFF00E676),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ReconKpiCard(
                    title = "Open Issues",
                    value = "${dashboard?.openDiscrepancies ?: discrepancies.count { it.status == "OPEN" || it.status == "INVESTIGATING" }}",
                    color = Color(0xFFFFD600),
                    modifier = Modifier.weight(1f)
                )
                ReconKpiCard(
                    title = "Critical Issues",
                    value = "${dashboard?.criticalDiscrepancies ?: discrepancies.count { it.severity == "CRITICAL" && (it.status == "OPEN" || it.status == "INVESTIGATING") }}",
                    color = Color(0xFFFF1744),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ReconKpiCard(
                    title = "Records Checked",
                    value = "${dashboard?.totalRecordsChecked ?: runs.sumOf { it.totalRecordsChecked }}",
                    color = Color(0xFF7C4DFF),
                    modifier = Modifier.weight(1f)
                )
                ReconKpiCard(
                    title = "Matched Records",
                    value = "${dashboard?.totalMatchedRecords ?: runs.sumOf { it.matchedRecords }}",
                    color = Color(0xFF2979FF),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "RECENT RECONCILIATION RUNS",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF88A0C0)
            )
        }

        items(runs.take(5)) { run ->
            ReconRunCard(run = run)
        }
    }
}

@Composable
fun ReconKpiCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101C30)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontSize = 12.sp, color = Color(0xFF88A0C0))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun ReconRunCard(run: BusinessFinancialReconciliationRunResponse, onExecute: (String) -> Unit = {}, onApprove: (String, String?) -> Unit = { _, _ -> }) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111E36)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = run.runNumber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                ReconStatusBadge(status = run.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Domain: ${run.runType} | Period: ${run.periodId}", fontSize = 13.sp, color = Color(0xFF88A0C0))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Checked: ${run.totalRecordsChecked} | Matched: ${run.matchedRecords} | Discrepancies: ${run.discrepancyCount} (${run.criticalDiscrepancyCount} Critical)",
                fontSize = 12.sp,
                color = if (run.criticalDiscrepancyCount > 0) Color(0xFFFF5252) else Color(0xFF69F0AE)
            )
        }
    }
}

@Composable
fun ReconStatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        "APPROVED" -> Color(0xFF1B5E20) to Color(0xFF69F0AE)
        "COMPLETED" -> Color(0xFF0D47A1) to Color(0xFF82B1FF)
        "UNDER_REVIEW" -> Color(0xFFE65100) to Color(0xFFFFD180)
        "RUNNING" -> Color(0xFF4A148C) to Color(0xFFEA80FC)
        "FAILED" -> Color(0xFFB71C1C) to Color(0xFFFF8A80)
        else -> Color(0xFF263238) to Color(0xFFCFD8DC)
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = status, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ReconciliationRunsView(
    runs: List<BusinessFinancialReconciliationRunResponse>,
    onExecuteRun: (String) -> Unit,
    onApproveRun: (String, String?) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "ALL RECONCILIATION RUNS (${runs.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF88A0C0)
            )
        }

        if (runs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No reconciliation runs executed yet.", color = Color(0xFF88A0C0))
                }
            }
        } else {
            items(runs) { run ->
                ReconRunCard(run = run, onExecute = onExecuteRun, onApprove = onApproveRun)
            }
        }
    }
}

@Composable
fun DiscrepanciesQueueView(
    discrepancies: List<BusinessFinancialReconciliationDiscrepancyResponse>,
    onAssign: (String, String) -> Unit,
    onResolve: (String, String) -> Unit,
    onWaive: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "DISCREPANCY QUEUE (${discrepancies.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF88A0C0)
            )
        }

        if (discrepancies.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No discrepancies detected. 100% financial consistency!", color = Color(0xFF69F0AE))
                }
            }
        } else {
            items(discrepancies) { disc ->
                DiscrepancyCard(disc = disc)
            }
        }
    }
}

@Composable
fun DiscrepancyCard(disc: BusinessFinancialReconciliationDiscrepancyResponse) {
    val borderColor = when (disc.severity) {
        "CRITICAL" -> Color(0xFFFF1744)
        "WARNING" -> Color(0xFFFFD600)
        else -> Color(0xFF00E5FF)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131D33)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = disc.discrepancyType,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = borderColor
                )
                ReconStatusBadge(status = disc.status)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = disc.description, fontSize = 13.sp, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Expected: ${disc.expectedAmount} ${disc.currency}", fontSize = 12.sp, color = Color(0xFF88A0C0))
                Text(text = "Actual: ${disc.actualAmount} ${disc.currency}", fontSize = 12.sp, color = Color(0xFF88A0C0))
                Text(text = "Diff: ${disc.differenceAmount} ${disc.currency}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = borderColor)
            }
        }
    }
}

@Composable
fun PeriodReadinessView(readiness: PeriodCloseReadinessResponse?) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "PERIOD-END CLOSURE READINESS GATE",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF88A0C0)
            )
        }

        if (readiness == null) {
            item {
                Text("Select a period to evaluate close readiness.", color = Color(0xFF88A0C0))
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (readiness.isReady) Color(0xFF0F2E1E) else Color(0xFF331418)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (readiness.isReady) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (readiness.isReady) Color(0xFF69F0AE) else Color(0xFFFF5252),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (readiness.isReady) "READY FOR HARD CLOSE" else "BLOCKED FROM CLOSURE",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (readiness.isReady) Color(0xFF69F0AE) else Color(0xFFFF5252)
                                )
                                Text(
                                    text = "Period: ${readiness.periodId}",
                                    fontSize = 13.sp,
                                    color = Color(0xFFB0BEC5)
                                )
                            }
                        }
                    }
                }
            }

            if (readiness.blockingIssues.isNotEmpty()) {
                item {
                    Text(
                        text = "BLOCKING ISSUES (${readiness.blockingIssues.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFFFF5252)
                    )
                }
                items(readiness.blockingIssues) { issue ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1015))
                    ) {
                        Text(
                            text = "• $issue",
                            modifier = Modifier.padding(12.dp),
                            color = Color(0xFFFF8A80),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReconciliationAuditView(events: List<BusinessFinancialReconciliationAuditEventResponse>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "APPEND-ONLY RECONCILIATION AUDIT LOG (${events.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF88A0C0)
            )
        }

        items(events) { evt ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1828)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = evt.eventType, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF00E5FF))
                        Text(
                            text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(evt.timestamp)),
                            fontSize = 11.sp,
                            color = Color(0xFF88A0C0)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Actor: ${evt.actorId} (${evt.actorRole})", fontSize = 12.sp, color = Color(0xFFB0BEC5))
                    if (!evt.reason.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "Reason: ${evt.reason}", fontSize = 12.sp, color = Color(0xFFCFD8DC))
                    }
                }
            }
        }
    }
}
