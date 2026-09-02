package com.sucharu.sucharupro.ui.features.production.execution

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.sucharu.sucharupro.data.api.model.productionexecution.*
import java.text.SimpleDateFormat
import java.util.*

// Premium SaaS Deep Navy Palette
private val DeepNavyBg = Color(0xFF0D1117)
private val CardBg = Color(0xFF161B22)
private val CardBgElevated = Color(0xFF21262D)
private val BorderColor = Color(0xFF30363D)
private val AccentCyan = Color(0xFF58A6FF)
private val AccentGreen = Color(0xFF3FB950)
private val AccentOrange = Color(0xFFD29922)
private val AccentRed = Color(0xFFF85149)
private val AccentPurple = Color(0xFFBC8CFF)
private val TextPrimary = Color(0xFFF0F6FC)
private val TextSecondary = Color(0xFF8B949E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionJobCommandCenterScreen(
    job: ProductionJobExecutionDto?,
    events: List<ProductionExecutionEventDto> = emptyList(),
    reconciliation: ProductionExecutionReconciliationDto? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onNavigateBack: () -> Unit = {},
    onStartStage: (jobId: String, woId: String) -> Unit = { _, _ -> },
    onPauseStage: (jobId: String, woId: String, reason: String?) -> Unit = { _, _, _ -> },
    onResumeStage: (jobId: String, woId: String) -> Unit = { _, _ -> },
    onCompleteStage: (jobId: String, woId: String, goodQty: String, scrapQty: String) -> Unit = { _, _, _, _ -> },
    onHoldJob: (jobId: String, category: String, reason: String) -> Unit = { _, _, _ -> },
    onReleaseHold: (jobId: String, notes: String?) -> Unit = { _, _ -> },
    onCompleteJob: (jobId: String, summary: String?) -> Unit = { _, _ -> },
    onRecordWastage: (jobId: String, woId: String, code: String, qty: String, reason: String) -> Unit = { _, _, _, _, _ -> },
    onRequestQc: (jobId: String, woId: String) -> Unit = { _, _ -> },
    onReconcile: (jobId: String) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        "Overview",
        "Work Orders",
        "Live Execution",
        "Quantities",
        "Machines & Operators",
        "QC & Rework",
        "Reconciliation",
        "Audit & AI"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = job?.title ?: "Production Job Command Center",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (job != null) {
                            Text(
                                text = "Job ID: ${job.executionJobId} • Order: ${job.orderNumber}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    if (job != null) {
                        StatusPill(status = job.status)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepNavyBg)
            )
        },
        containerColor = DeepNavyBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = DeepNavyBg,
                contentColor = AccentCyan,
                edgePadding = 16.dp,
                divider = { HorizontalDivider(color = BorderColor) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                color = if (selectedTab == index) AccentCyan else TextSecondary,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentCyan)
                }
            } else if (errorMessage != null) {
                Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("Error: $errorMessage", color = AccentRed)
                }
            } else if (job == null) {
                Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("No production job loaded.", color = TextSecondary)
                }
            } else {
                when (selectedTab) {
                    0 -> OverviewTab(job, onStartStage, onPauseStage, onResumeStage, onHoldJob, onReleaseHold, onCompleteJob)
                    1 -> WorkOrdersTab(job, onStartStage, onCompleteStage, onRequestQc)
                    2 -> LiveExecutionTab(job, onCompleteStage, onPauseStage, onResumeStage)
                    3 -> QuantitiesTab(job, onRecordWastage)
                    4 -> MachineOperatorTab(job)
                    5 -> QcReworkTab(job, onRequestQc)
                    6 -> ReconciliationTab(job, reconciliation, onReconcile)
                    7 -> AuditAndAiTab(job, events)
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(
    job: ProductionJobExecutionDto,
    onStartStage: (String, String) -> Unit,
    onPauseStage: (String, String, String?) -> Unit,
    onResumeStage: (String, String) -> Unit,
    onHoldJob: (String, String, String) -> Unit,
    onReleaseHold: (String, String?) -> Unit,
    onCompleteJob: (String, String?) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SaaSContainer(icon = Icons.Default.Factory, title = "Job Execution Summary") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Planned Quantity", color = TextSecondary, fontSize = 12.sp)
                        Text("${job.plannedQuantity} Pcs", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Completed", color = TextSecondary, fontSize = 12.sp)
                        Text("${job.completedQuantity} Pcs", color = AccentGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Progress", color = TextSecondary, fontSize = 12.sp)
                        Text("${(job.progressFraction * 100).toInt()}%", color = AccentCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { job.progressFraction },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = AccentCyan,
                    trackColor = CardBgElevated
                )
            }
        }

        item {
            SaaSContainer(icon = Icons.Default.AltRoute, title = "Active Production Status") {
                Text("Current Stage: ${job.currentStageType ?: "N/A"}", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Total Work Orders: ${job.workOrders.size} (${job.workOrders.count { it.status == "COMPLETED" }} completed)", color = TextSecondary, fontSize = 13.sp)

                val hold = job.currentHold
                if (hold != null) {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier.fillMaxWidth().background(AccentRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp)).border(1.dp, AccentRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(12.dp)
                    ) {
                        Column {
                            Text("JOB ON HOLD: ${hold.category}", color = AccentRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(hold.reason, color = TextPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            SaaSContainer(icon = Icons.Default.PlayArrow, title = "Shop-Floor Controls") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (job.status == "ON_HOLD") {
                        Button(
                            onClick = { onReleaseHold(job.executionJobId, null) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Release Hold")
                        }
                    } else {
                        Button(
                            onClick = { onHoldJob(job.executionJobId, "MATERIAL_SHORTAGE", "Hold from Command Center") },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Hold Job")
                        }
                    }
                    Button(
                        onClick = { onCompleteJob(job.executionJobId, "Completed from command center") },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                        modifier = Modifier.weight(1f),
                        enabled = job.workOrders.filter { it.isMandatory }.all { it.status == "COMPLETED" || it.status == "SKIPPED" } && job.status != "COMPLETED"
                    ) {
                        Text("Complete Job")
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkOrdersTab(
    job: ProductionJobExecutionDto,
    onStartStage: (String, String) -> Unit,
    onCompleteStage: (String, String, String, String) -> Unit,
    onRequestQc: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(job.workOrders) { wo ->
            SaaSContainer(
                icon = if (wo.isQcCheckpoint) Icons.Default.Verified else Icons.Default.Assignment,
                title = "${wo.sequenceNumber}. ${wo.operationName}"
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Work Center: ${wo.targetWorkCenter}", color = TextSecondary, fontSize = 12.sp)
                        Text("Stage: ${wo.stageType}", color = AccentCyan, fontSize = 12.sp)
                        if (wo.assignedMachineName != null) {
                            Text("Machine: ${wo.assignedMachineName}", color = TextPrimary, fontSize = 12.sp)
                        }
                        if (wo.assignedOperatorName != null) {
                            Text("Operator: ${wo.assignedOperatorName}", color = TextPrimary, fontSize = 12.sp)
                        }
                    }
                    StatusPill(status = wo.status)
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (wo.status == "READY" || wo.status == "PENDING") {
                        Button(
                            onClick = { onStartStage(job.executionJobId, wo.workOrderId) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Start Stage", fontSize = 12.sp)
                        }
                    } else if (wo.status == "IN_PROGRESS") {
                        Button(
                            onClick = { onCompleteStage(job.executionJobId, wo.workOrderId, wo.plannedQuantity, "0.0000") },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Complete", fontSize = 12.sp)
                        }
                        if (wo.isQcCheckpoint) {
                            Button(
                                onClick = { onRequestQc(job.executionJobId, wo.workOrderId) },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Request QC", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveExecutionTab(
    job: ProductionJobExecutionDto,
    onCompleteStage: (String, String, String, String) -> Unit,
    onPauseStage: (String, String, String?) -> Unit,
    onResumeStage: (String, String) -> Unit
) {
    val activeWo = job.workOrders.find { it.status == "IN_PROGRESS" }
        ?: job.workOrders.find { it.status == "PAUSED" }
        ?: job.workOrders.find { it.status == "READY" }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (activeWo == null) {
            item {
                SaaSContainer(icon = Icons.Default.CheckCircle, title = "No Active Execution") {
                    Text("All work orders are completed or pending release.", color = TextSecondary)
                }
            }
        } else {
            item {
                SaaSContainer(icon = Icons.Default.PlayArrow, title = "Live Stage: ${activeWo.operationName}") {
                    Text("Sequence: #${activeWo.sequenceNumber} • Type: ${activeWo.stageType}", color = TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Planned Target", color = TextSecondary, fontSize = 12.sp)
                            Text("${activeWo.plannedQuantity} Pcs", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Status", color = TextSecondary, fontSize = 12.sp)
                            StatusPill(status = activeWo.status)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (activeWo.status == "IN_PROGRESS") {
                            Button(
                                onClick = { onPauseStage(job.executionJobId, activeWo.workOrderId, "Break") },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Pause")
                            }
                            Button(
                                onClick = { onCompleteStage(job.executionJobId, activeWo.workOrderId, activeWo.plannedQuantity, "0.0000") },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Complete Stage")
                            }
                        } else if (activeWo.status == "PAUSED") {
                            Button(
                                onClick = { onResumeStage(job.executionJobId, activeWo.workOrderId) },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Resume")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuantitiesTab(
    job: ProductionJobExecutionDto,
    onRecordWastage: (String, String, String, String, String) -> Unit
) {
    var showWastageDialog by remember { mutableStateOf(false) }
    var wastageQty by remember { mutableStateOf("10.0000") }
    var wastageReason by remember { mutableStateOf("Setup Trim") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SaaSContainer(icon = Icons.Default.Layers, title = "Quantity Breakdown") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuantityRow("Planned Quantity", "${job.plannedQuantity} Pcs", TextPrimary)
                    QuantityRow("Completed Quantity", "${job.completedQuantity} Pcs", AccentGreen)
                    QuantityRow("Rejected / Scrap", "${job.rejectedQuantity} Pcs", AccentRed)
                    QuantityRow("Wastage", "${job.wastageQuantity} Pcs", AccentOrange)
                    QuantityRow("Rework Quantity", "${job.reworkQuantity} Pcs", AccentPurple)
                    QuantityRow("Remaining", "${job.remainingQuantity} Pcs", AccentCyan)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { showWastageDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Record Material Wastage")
                }
            }
        }
    }

    if (showWastageDialog) {
        AlertDialog(
            onDismissRequest = { showWastageDialog = false },
            title = { Text("Record Wastage", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = wastageQty,
                        onValueChange = { wastageQty = it },
                        label = { Text("Wastage Quantity") }
                    )
                    OutlinedTextField(
                        value = wastageReason,
                        onValueChange = { wastageReason = it },
                        label = { Text("Reason") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val firstWo = job.workOrders.firstOrNull()?.workOrderId ?: "WO-DEFAULT"
                    onRecordWastage(job.executionJobId, firstWo, "MAT-ART-150", wastageQty, wastageReason)
                    showWastageDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWastageDialog = false }) { Text("Cancel") }
            },
            containerColor = CardBg
        )
    }
}

@Composable
private fun MachineOperatorTab(job: ProductionJobExecutionDto) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(job.workOrders) { wo ->
            SaaSContainer(icon = Icons.Default.Build, title = "${wo.sequenceNumber}. ${wo.operationName}") {
                Text("Assigned Machine: ${wo.assignedMachineName ?: "Unassigned"}", color = TextPrimary, fontSize = 13.sp)
                Text("Assigned Operator: ${wo.assignedOperatorName ?: "Unassigned"}", color = TextPrimary, fontSize = 13.sp)
                Text("Estimated Run: ${wo.estimatedRunMinutes} min", color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun QcReworkTab(
    job: ProductionJobExecutionDto,
    onRequestQc: (String, String) -> Unit
) {
    val qcOrders = job.workOrders.filter { it.isQcCheckpoint }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (qcOrders.isEmpty()) {
            item {
                SaaSContainer(icon = Icons.Default.Verified, title = "Quality Control") {
                    Text("No dedicated QC checkpoints configured for this job.", color = TextSecondary)
                }
            }
        } else {
            items(qcOrders) { wo ->
                SaaSContainer(icon = Icons.Default.Verified, title = "QC Checkpoint: ${wo.operationName}") {
                    Text("Stage: ${wo.stageType}", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    StatusPill(status = wo.status)
                    Spacer(Modifier.height(10.dp))
                    if (wo.status != "COMPLETED") {
                        Button(
                            onClick = { onRequestQc(job.executionJobId, wo.workOrderId) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Request QC Inspection")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReconciliationTab(
    job: ProductionJobExecutionDto,
    reconciliation: ProductionExecutionReconciliationDto?,
    onReconcile: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SaaSContainer(icon = Icons.Default.Balance, title = "7-Way Multi-Tier Reconciliation") {
                Button(
                    onClick = { onReconcile(job.executionJobId) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Execute 7-Way Reconciliation")
                }
                if (reconciliation != null) {
                    Spacer(Modifier.height(12.dp))
                    ReconItem("Quotation Match", reconciliation.quotationMatch)
                    ReconItem("Commitment Match", reconciliation.commitmentMatch)
                    ReconItem("Order Match", reconciliation.orderMatch)
                    ReconItem("Planning Snapshot Match", reconciliation.planningMatch)
                    ReconItem("Work Orders Complete", reconciliation.workOrdersComplete)
                    ReconItem("Quantity Balanced", reconciliation.quantityBalanced)
                    ReconItem("QC Checkpoints Passed", reconciliation.qcCheckpointsPassed)

                    if (reconciliation.discrepancies.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text("Discrepancies:", color = AccentOrange, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        reconciliation.discrepancies.forEach {
                            Text("• $it", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditAndAiTab(
    job: ProductionJobExecutionDto,
    events: List<ProductionExecutionEventDto>
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SaaSContainer(icon = Icons.Default.AutoAwesome, title = "AI Agent Handoff Contract (Read-Only)") {
                Text("Contract Version: 1.0.0", color = TextSecondary, fontSize = 12.sp)
                Text("Integrity Hash: ${job.integrityHash}", color = AccentCyan, fontSize = 11.sp)
                Text("Job Fingerprint: ${job.jobFingerprint}", color = TextSecondary, fontSize = 11.sp)
            }
        }

        item {
            Text("Execution Event Audit Timeline", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        items(events) { ev ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBgElevated, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ev.eventType, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        val payload = ev.payload
                        if (payload != null) {
                            Text(payload, color = TextSecondary, fontSize = 12.sp)
                        }
                        Text("${dateFormat.format(Date(ev.performedAt))} by ${ev.performedBy}", color = TextSecondary, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SaaSContainer(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(16.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(CardBgElevated, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    val (bg, fg) = when (status) {
        "READY", "READY_FOR_PRODUCTION" -> AccentCyan.copy(alpha = 0.2f) to AccentCyan
        "IN_PROGRESS" -> AccentGreen.copy(alpha = 0.2f) to AccentGreen
        "COMPLETED" -> AccentGreen.copy(alpha = 0.2f) to AccentGreen
        "ON_HOLD", "PAUSED" -> AccentOrange.copy(alpha = 0.2f) to AccentOrange
        "CANCELLED", "BLOCKED" -> AccentRed.copy(alpha = 0.2f) to AccentRed
        "QC_PENDING", "REWORK_REQUIRED" -> AccentPurple.copy(alpha = 0.2f) to AccentPurple
        else -> TextSecondary.copy(alpha = 0.2f) to TextSecondary
    }

    Box(
        modifier = Modifier
            .background(bg, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(status, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun QuantityRow(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ReconItem(title: String, passed: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = TextSecondary, fontSize = 12.sp)
        Icon(
            imageVector = if (passed) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (passed) AccentGreen else AccentRed,
            modifier = Modifier.size(16.dp)
        )
    }
}
