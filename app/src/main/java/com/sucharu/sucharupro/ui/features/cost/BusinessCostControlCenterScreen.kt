package com.sucharu.sucharupro.ui.features.cost

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
import com.sucharu.sucharupro.data.api.model.businesscostcontrol.*
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

enum class CostControlTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Control Center", Icons.Default.Dashboard),
    COMMITMENTS("Commitments", Icons.Default.Assignment),
    ACCRUALS("Accruals", Icons.Default.AccountBalance),
    PERIODS("Period Controls", Icons.Default.DateRange),
    RECONCILIATION("Reconciliation", Icons.Default.CompareArrows),
    AUDIT("Audit Trail", Icons.Default.History)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessCostControlCenterScreen(
    dashboard: BusinessCostControlDashboardResponse? = null,
    commitments: List<BusinessCostCommitmentResponse> = emptyList(),
    accruals: List<BusinessCostAccrualResponse> = emptyList(),
    periods: List<BusinessFinancialPeriodResponse> = emptyList(),
    reconciliation: BusinessCostReconciliationSummaryResponse? = null,
    auditEvents: List<BusinessCostControlAuditEventResponse> = emptyList(),
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRefresh: () -> Unit = {},
    onCreateCommitment: (CreateCostCommitmentRequest) -> Unit = {},
    onApproveCommitment: (String) -> Unit = {},
    onConsumeCommitment: (String, String, String) -> Unit = { _, _, _ -> },
    onCreateAccrual: (CreateCostAccrualRequest) -> Unit = {},
    onPostAccrual: (String) -> Unit = {},
    onReverseAccrual: (String, String, String) -> Unit = { _, _, _ -> },
    onCreatePeriod: (CreateFinancialPeriodRequest) -> Unit = {},
    onClosePeriod: (String, String) -> Unit = { _, _ -> }
) {
    var selectedTab by remember { mutableStateOf(CostControlTab.DASHBOARD) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Cost Control & Financial Governance",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Module 15 — Step 05: Cost Commitments, Accruals & Period-End Controls",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Data")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs Bar
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                CostControlTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.title) },
                        icon = { Icon(tab.icon, contentDescription = tab.title) }
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                when (selectedTab) {
                    CostControlTab.DASHBOARD -> DashboardContent(dashboard, reconciliation)
                    CostControlTab.COMMITMENTS -> CommitmentsContent(commitments, onApproveCommitment)
                    CostControlTab.ACCRUALS -> AccrualsContent(accruals, onPostAccrual)
                    CostControlTab.PERIODS -> PeriodsContent(periods, onClosePeriod)
                    CostControlTab.RECONCILIATION -> ReconciliationContent(reconciliation)
                    CostControlTab.AUDIT -> AuditTrailContent(auditEvents)
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(
    dashboard: BusinessCostControlDashboardResponse?,
    reconciliation: BusinessCostReconciliationSummaryResponse?
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Executive Cost Control Summary",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "Total Commitments",
                    value = "${dashboard?.totalCommitments ?: "0.00"} ${dashboard?.currency ?: "BDT"}",
                    subtitle = "${dashboard?.totalCommitmentCount ?: 0} Total Orders",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primaryContainer
                )
                KpiCard(
                    title = "Active Commitments",
                    value = "${dashboard?.activeCommitments ?: "0.00"} ${dashboard?.currency ?: "BDT"}",
                    subtitle = "${dashboard?.activeCommitmentCount ?: 0} Active",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondaryContainer
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "Consumed Amount",
                    value = "${dashboard?.consumedCommitments ?: "0.00"} ${dashboard?.currency ?: "BDT"}",
                    subtitle = "Utilized Against POs",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                )
                KpiCard(
                    title = "Remaining Amount",
                    value = "${dashboard?.remainingCommitments ?: "0.00"} ${dashboard?.currency ?: "BDT"}",
                    subtitle = "Open Budget Liability",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "Posted Accruals",
                    value = "${dashboard?.accruedCosts ?: "0.00"} ${dashboard?.currency ?: "BDT"}",
                    subtitle = "Unbilled Expenses Incurred",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.errorContainer
                )
                KpiCard(
                    title = "Pending Approvals",
                    value = "${dashboard?.pendingAccrualCount ?: 0}",
                    subtitle = "${dashboard?.exceptionCount ?: 0} Exceptions Flagged",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Period-End & Accrual Health Indicators",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "• Separation of Duties (SoD) is strictly enforced between commitment creators, approving managers, and ledger posters.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Accrual reversals post compensating entries directly to the canonical Business Ledger (Module 15 Step 03).",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Closed periods prevent unauthorized backdated recognition or unbilled adjustments.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun CommitmentsContent(
    commitments: List<BusinessCostCommitmentResponse>,
    onApproveCommitment: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Purchase & Service Cost Commitments (${commitments.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (commitments.isEmpty()) {
            item {
                Text(
                    text = "No cost commitments found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(commitments) { c ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = c.commitmentNumber,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            StatusBadge(status = c.status)
                        }

                        Text(
                            text = c.description,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Committed: ${c.committedAmount} ${c.currency}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Consumed: ${c.consumedAmount} ${c.currency}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Remaining: ${c.remainingAmount} ${c.currency}",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary)
                            )
                        }

                        if (c.status == "SUBMITTED" || c.status == "DRAFT") {
                            Button(
                                onClick = { onApproveCommitment(c.id) },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Approve Commitment")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccrualsContent(
    accruals: List<BusinessCostAccrualResponse>,
    onPostAccrual: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Business Cost Accruals & Unbilled Incurred Costs (${accruals.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (accruals.isEmpty()) {
            item {
                Text(
                    text = "No cost accruals recorded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(accruals) { a ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = a.accrualNumber,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            StatusBadge(status = a.status)
                        }

                        Text(
                            text = a.description,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Accrual: ${a.accrualAmount} ${a.currency}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Reversed: ${a.reversedAmount} ${a.currency}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Net: ${a.netAccrualAmount} ${a.currency}",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary)
                            )
                        }

                        if (a.ledgerPostingId != null) {
                            Text(
                                text = "Canonical Ledger Posting: ${a.ledgerPostingId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        if (a.status == "APPROVED") {
                            Button(
                                onClick = { onPostAccrual(a.id) },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Post to Canonical Ledger")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodsContent(
    periods: List<BusinessFinancialPeriodResponse>,
    onClosePeriod: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Financial Accounting Periods (${periods.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (periods.isEmpty()) {
            item {
                Text(
                    text = "No financial periods configured.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(periods) { p ->
                val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val startStr = df.format(Date(p.startDate))
                val endStr = df.format(Date(p.endDate))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${p.periodCode} — ${p.periodName}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            StatusBadge(status = p.status)
                        }

                        Text(
                            text = "Date Range: $startStr – $endStr",
                            style = MaterialTheme.typography.bodySmall
                        )

                        if (p.closeReason != null) {
                            Text(
                                text = "Closing Note: ${p.closeReason}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (p.status == "OPEN" || p.status == "SOFT_CLOSED") {
                            Button(
                                onClick = { onClosePeriod(p.id, "Standard period-end closing") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Execute Period-End Close")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReconciliationContent(
    reconciliation: BusinessCostReconciliationSummaryResponse?
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Three-Way Cost & Liability Reconciliation",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReconciliationRow("Total Commitments", "${reconciliation?.commitmentAmount ?: "0.00"} ${reconciliation?.currency ?: "BDT"}")
                    ReconciliationRow("Consumed Commitments", "${reconciliation?.consumedAmount ?: "0.00"} ${reconciliation?.currency ?: "BDT"}")
                    ReconciliationRow("Incurred Accruals (Unbilled)", "${reconciliation?.accruedAmount ?: "0.00"} ${reconciliation?.currency ?: "BDT"}")
                    ReconciliationRow("Invoiced Payables", "${reconciliation?.payableAmount ?: "0.00"} ${reconciliation?.currency ?: "BDT"}")
                    ReconciliationRow("Settled / Paid Amount", "${reconciliation?.paidAmount ?: "0.00"} ${reconciliation?.currency ?: "BDT"}")
                    Divider()
                    ReconciliationRow(
                        label = "Unreconciled Variance",
                        value = "${reconciliation?.unreconciledAmount ?: "0.00"} ${reconciliation?.currency ?: "BDT"}",
                        isHighlighted = true
                    )
                }
            }
        }

        item {
            Text(
                text = "Control Exceptions & Discrepancies (${reconciliation?.exceptions?.size ?: 0})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        val exceptions = reconciliation?.exceptions ?: emptyList()
        if (exceptions.isEmpty()) {
            item {
                Text(
                    text = "No reconciliation discrepancies detected. Financial records are balanced.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            items(exceptions) { ex ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (ex.severity == "CRITICAL") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = ex.exceptionType,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = ex.severity,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Text(
                            text = ex.description,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditTrailContent(
    auditEvents: List<BusinessCostControlAuditEventResponse>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Financial Governance Audit Events (${auditEvents.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (auditEvents.isEmpty()) {
            item {
                Text(
                    text = "No audit events logged.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(auditEvents) { ev ->
                val df = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())
                val timeStr = df.format(Date(ev.timestamp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${ev.entityType}: ${ev.eventType}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "Actor: ${ev.actorUserId} (${ev.actorRole})",
                            style = MaterialTheme.typography.bodySmall
                        )

                        if (ev.reason != null) {
                            Text(
                                text = "Reason: ${ev.reason}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (ev.newState != null) {
                            Text(
                                text = "State: ${ev.newState}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Supporting UI Components ---

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val bgColor = when (status) {
        "ACTIVE", "APPROVED", "OPEN", "POSTED" -> MaterialTheme.colorScheme.primaryContainer
        "FULLY_CONSUMED", "CLOSED" -> MaterialTheme.colorScheme.secondaryContainer
        "PARTIALLY_CONSUMED", "REVIEWED", "SOFT_CLOSED" -> MaterialTheme.colorScheme.tertiaryContainer
        "CANCELLED", "REVERSED" -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when (status) {
        "ACTIVE", "APPROVED", "OPEN", "POSTED" -> MaterialTheme.colorScheme.onPrimaryContainer
        "FULLY_CONSUMED", "CLOSED" -> MaterialTheme.colorScheme.onSecondaryContainer
        "PARTIALLY_CONSUMED", "REVIEWED", "SOFT_CLOSED" -> MaterialTheme.colorScheme.onTertiaryContainer
        "CANCELLED", "REVERSED" -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor
        )
    }
}

@Composable
private fun ReconciliationRow(
    label: String,
    value: String,
    isHighlighted: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isHighlighted) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodySmall
        )
        Text(
            text = value,
            style = if (isHighlighted) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) else MaterialTheme.typography.bodySmall
        )
    }
}
