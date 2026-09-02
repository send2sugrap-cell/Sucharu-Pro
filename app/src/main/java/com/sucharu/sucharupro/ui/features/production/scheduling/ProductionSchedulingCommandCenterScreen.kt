package com.sucharu.sucharupro.ui.features.production.scheduling

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.productionscheduling.*
import java.text.SimpleDateFormat
import java.util.*

// Theme Palette (Deep Navy SaaS Analytics)
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
fun ProductionSchedulingCommandCenterScreen(
    schedule: ProductionScheduleResponseDto?,
    capacityWindows: List<ProductionCapacityWindowDto> = emptyList(),
    dispatchQueue: List<ProductionDispatchQueueItemDto> = emptyList(),
    conflicts: List<ProductionScheduleConflictDto> = emptyList(),
    reconciliation: ProductionScheduleReconciliationResponseDto? = null,
    handoffContract: Module17Step06ProductionSchedulingHandoffContractDto? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    successMessage: String? = null,
    onNavigateBack: () -> Unit = {},
    onApproveSchedule: (scheduleId: String) -> Unit = {},
    onSupersedeSchedule: (scheduleId: String, reason: String) -> Unit = { _, _ -> },
    onDispatchQueueItem: (queueItemId: String) -> Unit = {},
    onAcknowledgeQueueItem: (queueItemId: String) -> Unit = {},
    onReconcile: (scheduleId: String) -> Unit = {},
    onExportHandoff: (scheduleId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    var isSupersedeDialogOpen by remember { mutableStateOf(false) }
    var supersedeReason by remember { mutableStateOf("") }
    var isHandoffDialogOpen by remember { mutableStateOf(false) }

    val tabs = listOf(
        "Timeline & Slots",
        "Capacity Matrix",
        "Dispatch Queue",
        "Conflicts & Integrity",
        "Reconciliation & AI"
    )

    Scaffold(
        modifier = modifier.background(DeepNavyBg),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Production Scheduling & Dispatch",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (schedule != null) {
                            Text(
                                text = "Schedule: ${schedule.scheduleId} (V${schedule.version}) • Order #${schedule.orderNumber}",
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
                    if (schedule != null) {
                        if (schedule.status == "PROPOSED" || schedule.status == "RECALCULATED") {
                            Button(
                                onClick = { onApproveSchedule(schedule.scheduleId) },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Approve & Queue", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        IconButton(onClick = { isSupersedeDialogOpen = true }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reschedule", tint = AccentOrange)
                        }

                        IconButton(onClick = {
                            onExportHandoff(schedule.scheduleId)
                            isHandoffDialogOpen = true
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "AI Handoff", tint = AccentPurple)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBg)
            )
        },
        containerColor = DeepNavyBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Status and Alert Banners
            if (errorMessage != null) {
                Surface(
                    color = AccentRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .border(1.dp, AccentRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AccentRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = errorMessage, color = TextPrimary, fontSize = 13.sp)
                    }
                }
            }

            if (successMessage != null) {
                Surface(
                    color = AccentGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .border(1.dp, AccentGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = successMessage, color = TextPrimary, fontSize = 13.sp)
                    }
                }
            }

            // Summary Header KPI Row
            if (schedule != null) {
                SchedulingHeaderKpiCard(schedule = schedule, conflicts = conflicts)
            }

            // Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = CardBg,
                contentColor = AccentCyan,
                edgePadding = 12.dp,
                divider = { HorizontalDivider(color = BorderColor) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                color = if (selectedTab == index) AccentCyan else TextSecondary,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentCyan)
                }
            } else if (schedule == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No Production Schedule Active", color = TextSecondary, fontSize = 16.sp)
                    }
                }
            } else {
                when (selectedTab) {
                    0 -> TimelineAndSlotsTab(schedule)
                    1 -> CapacityMatrixTab(capacityWindows.ifEmpty { schedule.capacityWindows })
                    2 -> DispatchQueueTab(
                        queueItems = dispatchQueue,
                        onDispatch = onDispatchQueueItem,
                        onAcknowledge = onAcknowledgeQueueItem
                    )
                    3 -> ConflictsAndIntegrityTab(schedule, conflicts)
                    4 -> ReconciliationAndAiTab(
                        schedule = schedule,
                        reconciliation = reconciliation,
                        handoffContract = handoffContract,
                        onReconcile = { onReconcile(schedule.scheduleId) }
                    )
                }
            }
        }
    }

    // Supersede / Reschedule Dialog
    if (isSupersedeDialogOpen && schedule != null) {
        AlertDialog(
            onDismissRequest = { isSupersedeDialogOpen = false },
            title = { Text("Reschedule & Supersede Schedule", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Creating a new schedule version will mark V${schedule.version} as SUPERSEDED and compute an immutable V${schedule.version + 1}.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = supersedeReason,
                        onValueChange = { supersedeReason = it },
                        label = { Text("Reschedule Reason") },
                        placeholder = { Text("e.g. Machine calibration delay, rush order priority shift") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (supersedeReason.isNotBlank()) {
                            onSupersedeSchedule(schedule.scheduleId, supersedeReason)
                            isSupersedeDialogOpen = false
                            supersedeReason = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                ) {
                    Text("Generate Superseding Version")
                }
            },
            dismissButton = {
                TextButton(onClick = { isSupersedeDialogOpen = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardBgElevated
        )
    }

    // AI Handoff Dialog
    if (isHandoffDialogOpen && handoffContract != null) {
        AlertDialog(
            onDismissRequest = { isHandoffDialogOpen = false },
            title = { Text("AI Handoff Contract (Step 06)", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    item {
                        Text(
                            text = "Contract Version: ${handoffContract.contractVersion}\n" +
                                    "Schedule: ${handoffContract.scheduleId} (V${handoffContract.scheduleVersion})\n" +
                                    "Status: ${handoffContract.status}\n" +
                                    "Total Slots: ${handoffContract.slotsCount}\n" +
                                    "Est. Duration: ${handoffContract.totalEstimatedDurationMinutes} mins\n" +
                                    "Avg Capacity Utilization: ${handoffContract.capacityUtilizationAvg}\n" +
                                    "Active Conflicts: ${handoffContract.activeConflictsCount}\n" +
                                    "Fully Reconciled: ${handoffContract.isFullyReconciled}\n" +
                                    "Integrity Hash: ${handoffContract.integrityHash}\n\n" +
                                    "Machines:\n" + handoffContract.machineAssignmentsSummary.joinToString("\n") { " • $it" } + "\n\n" +
                                    "Dispatch Status:\n" + handoffContract.dispatchStatusSummary.entries.joinToString("\n") { " • ${it.key}: ${it.value}" },
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { isHandoffDialogOpen = false }) {
                    Text("Close")
                }
            },
            containerColor = CardBgElevated
        )
    }
}

@Composable
private fun SchedulingHeaderKpiCard(
    schedule: ProductionScheduleResponseDto,
    conflicts: List<ProductionScheduleConflictDto>
) {
    val sdf = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.US) }
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(status = schedule.status)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "V${schedule.version} ${if (schedule.isCurrent) "• Current Active" else "• Superseded"}",
                        color = if (schedule.isCurrent) AccentCyan else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (conflicts.any { it.isBlocking }) {
                    Surface(
                        color = AccentRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.border(1.dp, AccentRed, RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            text = "BLOCKING CONFLICTS",
                            color = AccentRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                KpiMetricColumn(label = "Planned Start", value = sdf.format(Date(schedule.plannedStartAt)))
                KpiMetricColumn(label = "Planned Finish", value = sdf.format(Date(schedule.plannedEndAt)))
                KpiMetricColumn(label = "Total Est. Duration", value = "${schedule.totalEstimatedMinutes} mins")
                KpiMetricColumn(label = "Operations", value = "${schedule.slots.size} Slots")
            }
        }
    }
}

@Composable
private fun KpiMetricColumn(label: String, value: String) {
    Column {
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TimelineAndSlotsTab(schedule: ProductionScheduleResponseDto) {
    val timeSdf = remember { SimpleDateFormat("HH:mm", Locale.US) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(schedule.slots) { slot ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBgElevated),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = AccentCyan.copy(alpha = 0.2f),
                                shape = CircleShape,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = "${slot.sequenceNumber}", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = slot.stageType, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Surface(
                            color = CardBg,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.border(1.dp, BorderColor, RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                text = "Score: ${slot.priorityScore}",
                                color = AccentOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${timeSdf.format(Date(slot.scheduledStartTimestamp))} → ${timeSdf.format(Date(slot.scheduledEndTimestamp))} • Duration: ${slot.totalEstimatedMinutes} mins",
                        color = AccentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Machine: ${slot.machineName} (${slot.machineId})",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    if (slot.operatorName != null) {
                        Text(
                            text = "Operator: ${slot.operatorName}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CapacityMatrixTab(capacityWindows: List<ProductionCapacityWindowDto>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(capacityWindows) { cap ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBgElevated),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = cap.machineName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "ID: ${cap.machineId} • Date: ${cap.shiftDate}", color = TextSecondary, fontSize = 11.sp)
                        }
                        Text(
                            text = "${cap.utilizationRate.multiply(java.math.BigDecimal(100)).toInt()}% Utilized",
                            color = if (cap.utilizationRate.toDouble() > 0.85) AccentRed else AccentGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val util = cap.utilizationRate.toFloat().coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { util },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (util > 0.85f) AccentRed else AccentGreen,
                        trackColor = BorderColor
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Allocated: ${cap.allocatedMinutes}m", color = TextSecondary, fontSize = 11.sp)
                        Text(text = "Available: ${cap.availableMinutes}m", color = TextSecondary, fontSize = 11.sp)
                        Text(text = "Total Shift: ${cap.totalCapacityMinutes}m", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DispatchQueueTab(
    queueItems: List<ProductionDispatchQueueItemDto>,
    onDispatch: (String) -> Unit,
    onAcknowledge: (String) -> Unit
) {
    if (queueItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No queue items available. Approve schedule to populate queue.", color = TextSecondary, fontSize = 13.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(queueItems) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBgElevated),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = AccentOrange.copy(alpha = 0.2f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = "${item.sequenceNumber}", color = AccentOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = item.operationCode, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = "WO: ${item.workOrderId} • ${item.stageType}", color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                            DispatchStatusBadge(item.dispatchStatus)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Machine: ${item.machineName}", color = TextSecondary, fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (item.dispatchStatus == "READY") {
                                Button(
                                    onClick = { onDispatch(item.queueItemId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Dispatch to Floor", fontSize = 12.sp)
                                }
                            } else if (item.dispatchStatus == "DISPATCHED") {
                                Button(
                                    onClick = { onAcknowledge(item.queueItemId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Acknowledge", fontSize = 12.sp)
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
private fun ConflictsAndIntegrityTab(
    schedule: ProductionScheduleResponseDto,
    conflicts: List<ProductionScheduleConflictDto>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBgElevated),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Cryptographic Audit & Fingerprint", color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "SHA-256 Fingerprint: ${schedule.scheduleFingerprint}", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text(text = "Integrity Hash: ${schedule.integrityHash}", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        if (conflicts.isEmpty()) {
            item {
                Surface(
                    color = AccentGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AccentGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Zero Active Conflicts Detected. Schedule is clean for execution.", color = TextPrimary, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(conflicts) { conflict ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBgElevated),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (conflict.isBlocking) AccentRed else AccentOrange, RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = conflict.conflictType, color = if (conflict.isBlocking) AccentRed else AccentOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = conflict.severity, color = TextSecondary, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = conflict.message, color = TextPrimary, fontSize = 12.sp)
                        if (conflict.recommendedAction.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Remediation: ${conflict.recommendedAction}", color = AccentCyan, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReconciliationAndAiTab(
    schedule: ProductionScheduleResponseDto,
    reconciliation: ProductionScheduleReconciliationResponseDto?,
    handoffContract: Module17Step06ProductionSchedulingHandoffContractDto?,
    onReconcile: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Multi-Tier Scheduling Reconciliation", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Button(
                    onClick = onReconcile,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Run 8-Way Audit", fontSize = 12.sp)
                }
            }
        }

        if (reconciliation != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBgElevated),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (reconciliation.isFullyReconciled) AccentGreen else AccentRed, RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Reconciliation Status", color = TextSecondary, fontSize = 12.sp)
                            Text(
                                text = if (reconciliation.isFullyReconciled) "100% RECONCILED" else "DISCREPANCIES FOUND",
                                color = if (reconciliation.isFullyReconciled) AccentGreen else AccentRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        ReconciliationCheckRow("Job Execution Reference", reconciliation.executionJobMatch)
                        ReconciliationCheckRow("Slot Quantity Completeness", reconciliation.slotsComplete)
                        ReconciliationCheckRow("Planning Compatibility", reconciliation.planningMatch)
                        ReconciliationCheckRow("Work Orders Matched", reconciliation.workOrdersMatched)
                        ReconciliationCheckRow("Machine Capacity Feasible", reconciliation.capacityFeasible)
                        ReconciliationCheckRow("Zero Blocking Conflicts", reconciliation.zeroBlockingConflicts)
                        ReconciliationCheckRow("Dispatch Queue Alignment", reconciliation.dispatchAligned)
                        ReconciliationCheckRow("Tenant Isolation Verified", reconciliation.tenantIsolationVerified)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReconciliationCheckRow(label: String, passed: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextPrimary, fontSize = 12.sp)
        Icon(
            imageVector = if (passed) Icons.Default.CheckCircle else Icons.Default.Close,
            contentDescription = null,
            tint = if (passed) AccentGreen else AccentRed,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        "APPROVED" -> Pair(AccentGreen.copy(alpha = 0.2f), AccentGreen)
        "SUPERSEDED" -> Pair(TextSecondary.copy(alpha = 0.2f), TextSecondary)
        "RECALCULATED" -> Pair(AccentOrange.copy(alpha = 0.2f), AccentOrange)
        else -> Pair(AccentCyan.copy(alpha = 0.2f), AccentCyan)
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.border(1.dp, textColor, RoundedCornerShape(4.dp))
    ) {
        Text(
            text = status,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun DispatchStatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        "ACKNOWLEDGED" -> Pair(AccentGreen.copy(alpha = 0.2f), AccentGreen)
        "DISPATCHED" -> Pair(AccentCyan.copy(alpha = 0.2f), AccentCyan)
        "READY" -> Pair(AccentOrange.copy(alpha = 0.2f), AccentOrange)
        else -> Pair(TextSecondary.copy(alpha = 0.2f), TextSecondary)
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.border(1.dp, textColor, RoundedCornerShape(4.dp))
    ) {
        Text(
            text = status,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
