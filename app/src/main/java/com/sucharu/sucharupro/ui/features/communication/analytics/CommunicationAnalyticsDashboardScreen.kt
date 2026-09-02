package com.sucharu.sucharupro.ui.features.communication.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationAnalyticsSnapshot
import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationGovernanceStatus
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationSnapshotVerificationResult
import com.sucharu.sucharupro.domain.model.communication.analytics.SnapshotVerificationStatus

@Composable
fun CommunicationAnalyticsDashboardScreen(
    viewModel: CommunicationAnalyticsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var showExportDialog by remember { mutableStateOf(false) }
    var showAuditLogs by remember { mutableStateOf(false) }
    var selectedRiskForGovernance by remember { mutableStateOf<com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationRiskIndicator?>(null) }

    if (showAuditLogs) {
        CommunicationAuditLogScreen(
            auditEvents = uiState.auditEvents,
            onBack = { showAuditLogs = false }
        )
        return
    }

    if (showExportDialog) {
        CommunicationExportDialog(
            isLoading = uiState.isRequestingExport,
            availableSnapshotId = uiState.snapshots.firstOrNull()?.snapshotId,
            onDismiss = { showExportDialog = false },
            onExportRequested = { type, snapshotId ->
                viewModel.requestExport(type, snapshotId)
                showExportDialog = false
            }
        )
    }

    selectedRiskForGovernance?.let { risk ->
        CommunicationGovernanceActionDialog(
            targetType = "RISK_INDICATOR",
            targetId = risk.riskType.name,
            isLoading = uiState.isAcknowledgingGovernance,
            onDismiss = { selectedRiskForGovernance = null },
            onConfirm = { action, state, notes ->
                viewModel.acknowledgeGovernanceAction(
                    targetType = "RISK_INDICATOR",
                    targetId = risk.riskType.name,
                    actionType = action,
                    resultingState = state,
                    notes = notes
                )
                selectedRiskForGovernance = null
            }
        )
    }

    uiState.verificationResult?.let { result ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissVerificationResult() },
            title = { Text(if (result.status == SnapshotVerificationStatus.VERIFIED) "Integrity Verified" else "Integrity Failure") },
            text = { Text(result.explanation) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissVerificationResult() }) { Text("OK") }
            }
        )
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Communication Analytics", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                ),
                actions = {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Export", tint = Color.White)
                    }
                    IconButton(onClick = { showAuditLogs = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Audit Logs", tint = Color.White)
                    }
                }
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Operational Health
                    item {
                        uiState.operationalHealth?.let { health ->
                            AnalyticsCard(title = "Operational Health") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    StatItem("Status", health.communicationHealth.name)
                                    StatItem("High Risks", health.highRiskCount.toString())
                                    StatItem("Anomalies", health.criticalAnomalyCount.toString())
                                }
                            }
                        }
                    }

                    // KPI Summary
                    item {
                        uiState.kpiSummary?.let { kpi ->
                            AnalyticsCard(title = "KPI Summary") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    StatItem("Total", kpi.totalCommunications.toString())
                                    StatItem("Delivery", "${"%.1f".format(kpi.deliveryRate * 100)}%")
                                    StatItem("Read", "${"%.1f".format(kpi.readRate * 100)}%")
                                }
                            }
                        }
                    }

                    // Governance
                    item {
                        uiState.governanceResult?.let { gov ->
                            val govStatus = gov.communicationGovernanceStatus
                            val bgColor = when (govStatus) {
                                CommunicationGovernanceStatus.COMPLIANT -> Color(0xFF10B981).copy(alpha = 0.2f)
                                CommunicationGovernanceStatus.AT_RISK -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                CommunicationGovernanceStatus.NON_COMPLIANT -> Color(0xFFEF4444).copy(alpha = 0.2f)
                            }
                            val textColor = when (govStatus) {
                                CommunicationGovernanceStatus.COMPLIANT -> Color(0xFF34D399)
                                CommunicationGovernanceStatus.AT_RISK -> Color(0xFFFBBF24)
                                CommunicationGovernanceStatus.NON_COMPLIANT -> Color(0xFFF87171)
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = bgColor),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Governance Status: ${gov.governanceStatus.name}",
                                        color = textColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Score: ${gov.governanceScore}",
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Risks
                    if (uiState.riskIndicators.isNotEmpty()) {
                        item {
                            Text(
                                "Identified Risks",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(uiState.riskIndicators) { risk ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Risk",
                                        tint = if (risk.severity == com.sucharu.sucharupro.domain.model.communication.analytics.RiskSeverity.CRITICAL) Color.Red else Color.Yellow
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(risk.riskType.name, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(risk.explanation, color = Color.Gray, fontSize = 12.sp)
                                    }
                                    TextButton(onClick = { selectedRiskForGovernance = risk }) {
                                        Text("Action", color = Color(0xFF38BDF8))
                                    }
                                }
                            }
                        }
                    }

                    // Snapshots action
                    item {
                        Button(
                            onClick = { viewModel.generateSnapshot() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                            enabled = !uiState.isSnapshotGenerationLoading
                        ) {
                            Text(if (uiState.isSnapshotGenerationLoading) "Generating..." else "Generate Snapshot", color = Color(0xFF0F172A))
                        }
                    }

                    // Recent Snapshots
                    if (uiState.snapshots.isNotEmpty()) {
                        item {
                            Text(
                                "Recent Snapshots",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(uiState.snapshots.take(5)) { snapshot ->
                            SnapshotItem(
                                snapshot = snapshot,
                                isVerifying = uiState.isVerifyingSnapshot,
                                onVerify = { viewModel.verifySnapshot(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color(0xFF38BDF8), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun SnapshotItem(
    snapshot: CommunicationAnalyticsSnapshot,
    isVerifying: Boolean,
    onVerify: (String) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm").withZone(ZoneId.systemDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Snapshot ID: ${snapshot.snapshotId}", color = Color.White, fontSize = 12.sp)
                Text("Generated: ${formatter.format(snapshot.generatedAt)}", color = Color.Gray, fontSize = 12.sp)
                Text("Hash: ${snapshot.sha256Hash.take(8)}...", color = Color(0xFF38BDF8), fontSize = 12.sp)
            }
            TextButton(onClick = { onVerify(snapshot.snapshotId) }, enabled = !isVerifying) {
                Text("Verify", color = Color(0xFF38BDF8))
            }
        }
    }
}
