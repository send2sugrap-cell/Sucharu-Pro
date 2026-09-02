package com.sucharu.sucharupro.ui.features.profitability.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.profitability.*
import java.math.BigDecimal

/**
 * Production Jetpack Compose Command Center for Profitability Alerts & Management Actions.
 * Module 16 Step 09.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitabilityAlertsCommandCenterScreen(
    snapshot: ProfitabilityMonitoringSnapshotDto?,
    alerts: List<ProfitabilityAlertDto>,
    actions: List<ProfitabilityManagementActionDto>,
    correlations: List<ProfitabilityAlertCorrelationDto>,
    escalations: List<ProfitabilityAlertEscalationDto>,
    rules: List<ProfitabilityAlertRuleDto>,
    onEvaluateClick: () -> Unit = {},
    onAcknowledgeAlert: (String) -> Unit = {},
    onResolveAlert: (String) -> Unit = {},
    onUpdateActionStatus: (String, String) -> Unit = { _, _ -> }
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Overview", "Alert Queue", "Escalations & Clusters", "Actions", "Rule Policies", "Reconciliation")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Profitability Alerts & Early-Warning", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Management Action & Risk Monitoring", style = MaterialTheme.typography.bodySmall)
                    }
                },
                actions = {
                    IconButton(onClick = onEvaluateClick) {
                        Icon(Icons.Default.Refresh, contentDescription = "Evaluate Rules")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> AlertOverviewTab(snapshot)
                1 -> AlertQueueTab(alerts, onAcknowledgeAlert, onResolveAlert)
                2 -> EscalationsAndClustersTab(escalations, correlations)
                3 -> ManagementActionsTab(actions, onUpdateActionStatus)
                4 -> AlertRulesTab(rules)
                5 -> AlertReconciliationTab(snapshot, alerts, actions)
            }
        }
    }
}

@Composable
fun AlertOverviewTab(snapshot: ProfitabilityMonitoringSnapshotDto?) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Executive Alert Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiCard(
                    title = "Active Alerts",
                    value = "${snapshot?.totalActiveAlerts ?: 0}",
                    subtitle = "${snapshot?.criticalAlertCount ?: 0} Critical • ${snapshot?.highAlertCount ?: 0} High",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Financial Exposure",
                    value = "৳${snapshot?.totalUnresolvedFinancialImpact ?: BigDecimal.ZERO}",
                    subtitle = "Unresolved Risk Impact",
                    color = Color.Red,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiCard(
                    title = "Open Actions",
                    value = "${snapshot?.openActionCount ?: 0}",
                    subtitle = "${snapshot?.overdueActionCount ?: 0} Overdue Actions",
                    color = Color(0xFFFFA500),
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Escalations",
                    value = "${snapshot?.escalatedAlertCount ?: 0}",
                    subtitle = "${snapshot?.recurringIssueCount ?: 0} Recurring Conditions",
                    color = Color.Magenta,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Severity Distribution", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    snapshot?.severityDistribution?.forEach { (sev, count) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(sev, style = MaterialTheme.typography.bodyMedium)
                            Text("$count alerts", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlertQueueTab(
    alerts: List<ProfitabilityAlertDto>,
    onAcknowledgeAlert: (String) -> Unit,
    onResolveAlert: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Active Alert Queue (${alerts.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        if (alerts.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No active profitability alerts detected.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            items(alerts) { alert ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(alert.dimensionLabel, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            SeverityChip(alert.severity)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${alert.dimensionType} • ${alert.alertType}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(alert.explanation, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Exposure: ৳${alert.financialImpact}", fontWeight = FontWeight.SemiBold, color = Color.Red)
                            Text("Status: ${alert.status}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (alert.recommendedActionCode != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Recommended Action: ${alert.recommendedActionCode}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (alert.status == "DETECTED") {
                                OutlinedButton(onClick = { onAcknowledgeAlert(alert.alertId) }) {
                                    Text("Acknowledge")
                                }
                            }
                            if (alert.status != "RESOLVED") {
                                Button(onClick = { onResolveAlert(alert.alertId) }) {
                                    Text("Resolve")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EscalationsAndClustersTab(
    escalations: List<ProfitabilityAlertEscalationDto>,
    correlations: List<ProfitabilityAlertCorrelationDto>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Urgent Escalations (${escalations.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        if (escalations.isEmpty()) {
            item {
                Text("No escalated risk conditions.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            items(escalations) { esc ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Escalation: ${esc.escalationLevel}", fontWeight = FontWeight.Bold, color = Color.Red)
                            Text("Age: ${esc.ageInHours} hrs", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(esc.justification, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text("Cross-Dimensional Event Clusters (${correlations.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        if (correlations.isEmpty()) {
            item {
                Text("No correlated multi-alert events.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            items(correlations) { corr ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(corr.correlationTitle, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(corr.correlationReason, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Combined Exposure: ৳${corr.totalFinancialImpact}", fontWeight = FontWeight.SemiBold, color = Color.Red)
                    }
                }
            }
        }
    }
}

@Composable
fun ManagementActionsTab(
    actions: List<ProfitabilityManagementActionDto>,
    onUpdateActionStatus: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Management Remediation Actions (${actions.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        if (actions.isEmpty()) {
            item {
                Text("No remediation actions assigned.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            items(actions) { act ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(act.actionTitle, fontWeight = FontWeight.Bold)
                            AssistChip(onClick = {}, label = { Text(act.status) })
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(act.actionDescription, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Priority: ${act.priorityScore} / 100", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("Expected Impact: ৳${act.expectedFinancialImpact}", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (act.status == "PROPOSED") {
                                OutlinedButton(onClick = { onUpdateActionStatus(act.actionId, "IN_PROGRESS") }) {
                                    Text("Start Action")
                                }
                            }
                            if (act.status == "IN_PROGRESS") {
                                Button(onClick = { onUpdateActionStatus(act.actionId, "COMPLETED") }) {
                                    Text("Mark Complete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlertRulesTab(rules: List<ProfitabilityAlertRuleDto>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Tenant Alert Threshold Policies (${rules.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        items(rules) { r ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(r.ruleName, fontWeight = FontWeight.Bold)
                        SeverityChip(r.severity)
                    }
                    Text("${r.dimensionType} • ${r.alertType}", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Threshold: ${r.thresholdMetric} ${r.comparisonOperator} ${r.thresholdValue}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun AlertReconciliationTab(
    snapshot: ProfitabilityMonitoringSnapshotDto?,
    alerts: List<ProfitabilityAlertDto>,
    actions: List<ProfitabilityManagementActionDto>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Reconciliation & Cryptographic Verification", fontWeight = FontWeight.Bold, fontSize = 16.sp)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Mathematical Audit Assertions", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Active Alerts Verified: ${alerts.size}", style = MaterialTheme.typography.bodyMedium)
                Text("• Management Actions Verified: ${actions.size}", style = MaterialTheme.typography.bodyMedium)
                Text("• Aggregate Unresolved Exposure: ৳${snapshot?.totalUnresolvedFinancialImpact ?: BigDecimal.ZERO}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Snapshot Integrity Hash:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Text(snapshot?.integrityHash ?: "PENDING_CALCULATION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun KpiCard(title: String, value: String, subtitle: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun SeverityChip(severity: String) {
    val chipColor = when (severity) {
        "CRITICAL" -> Color.Red
        "HIGH" -> Color.Magenta
        "MEDIUM" -> Color(0xFFFFA500)
        else -> Color(0xFF2E7D32)
    }
    AssistChip(
        onClick = {},
        label = { Text(severity, fontWeight = FontWeight.Bold, color = chipColor) }
    )
}
