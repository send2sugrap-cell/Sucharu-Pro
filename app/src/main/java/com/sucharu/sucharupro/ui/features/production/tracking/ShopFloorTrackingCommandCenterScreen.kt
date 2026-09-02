package com.sucharu.sucharupro.ui.features.production.tracking

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
import com.sucharu.sucharupro.data.api.model.shopfloortracking.*
import java.math.BigDecimal
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
fun ShopFloorTrackingCommandCenterScreen(
    jobId: String,
    timeRecords: List<OperatorTimeTrackingResponseDto> = emptyList(),
    materialConsumptions: List<ProductionMaterialConsumptionResponseDto> = emptyList(),
    telemetryLogs: List<MachineTelemetryResponseDto> = emptyList(),
    stageHandovers: List<StageOutputHandoverResponseDto> = emptyList(),
    varianceSummary: ProductionExecutionVarianceResponseDto? = null,
    reconciliation: ShopFloorTrackingReconciliationResponseDto? = null,
    handoffContract: Module17Step07ShopFloorTrackingHandoffContractDto? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    successMessage: String? = null,
    onNavigateBack: () -> Unit = {},
    onStartWorkOrder: (workOrderId: String) -> Unit = {},
    onPauseWorkOrder: (workOrderId: String, reason: String, category: String?) -> Unit = { _, _, _ -> },
    onResumeWorkOrder: (workOrderId: String) -> Unit = {},
    onRecordOutput: (workOrderId: String, goodQty: BigDecimal, scrapQty: BigDecimal, isCompleted: Boolean) -> Unit = { _, _, _, _ -> },
    onRecordMaterial: (workOrderId: String, materialCode: String, qty: BigDecimal, scrap: BigDecimal) -> Unit = { _, _, _, _ -> },
    onAcceptHandover: (handoverId: String) -> Unit = {},
    onExportHandoff: (jobId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    var isHandoffDialogOpen by remember { mutableStateOf(false) }

    val tabs = listOf(
        "Live Timers",
        "Material Depletion",
        "Machine Telemetry",
        "Stage Handovers",
        "Variance & AI Handoff"
    )

    Scaffold(
        modifier = modifier.background(DeepNavyBg),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Shop-Floor Live Tracking & Telemetry",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Execution Job: $jobId",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        onExportHandoff(jobId)
                        isHandoffDialogOpen = true
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "AI Handoff", tint = AccentPurple)
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

            // KPI Overview Banner
            if (varianceSummary != null) {
                TrackingHeaderKpiCard(variance = varianceSummary)
            }

            // Scrollable Tabs
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
            } else {
                when (selectedTab) {
                    0 -> LiveTimersTab(
                        records = timeRecords,
                        onResume = onResumeWorkOrder
                    )
                    1 -> MaterialDepletionTab(materials = materialConsumptions)
                    2 -> MachineTelemetryTab(logs = telemetryLogs)
                    3 -> StageHandoversTab(
                        handovers = stageHandovers,
                        onAccept = onAcceptHandover
                    )
                    4 -> VarianceAndAiTab(
                        variance = varianceSummary,
                        reconciliation = reconciliation,
                        handoffContract = handoffContract
                    )
                }
            }
        }
    }

    // AI Handoff Dialog
    if (isHandoffDialogOpen && handoffContract != null) {
        AlertDialog(
            onDismissRequest = { isHandoffDialogOpen = false },
            title = { Text("AI Handoff Contract (Step 07)", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    item {
                        Text(
                            text = "Contract Version: ${handoffContract.contractVersion}\n" +
                                    "Job: ${handoffContract.executionJobId}\n" +
                                    "Order #${handoffContract.orderNumber}\n" +
                                    "Overall Yield: ${handoffContract.overallYieldPercentage}%\n" +
                                    "Speed Efficiency: ${handoffContract.speedEfficiencyPercentage}%\n" +
                                    "Total Downtime: ${handoffContract.totalDowntimeMinutes} mins\n" +
                                    "Stages: ${handoffContract.completedStagesCount}/${handoffContract.totalStagesCount} Completed\n" +
                                    "Fully Reconciled: ${handoffContract.isFullyReconciled}\n" +
                                    "Integrity Hash: ${handoffContract.integrityHash}\n\n" +
                                    "Material Summary:\n" + handoffContract.materialConsumptionsSummary.joinToString("\n") { " • $it" } + "\n\n" +
                                    "Stage Handovers:\n" + handoffContract.stageHandoversSummary.joinToString("\n") { " • $it" },
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
private fun TrackingHeaderKpiCard(variance: ProductionExecutionVarianceResponseDto) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "Stage Yield", color = TextSecondary, fontSize = 11.sp)
                Text(
                    text = "${variance.overallYieldPercentage}%",
                    color = if (variance.overallYieldPercentage.toDouble() >= 90.0) AccentGreen else AccentRed,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column {
                Text(text = "Speed Efficiency", color = TextSecondary, fontSize = 11.sp)
                Text(
                    text = "${variance.averageMachineSpeedEfficiency}%",
                    color = AccentCyan,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column {
                Text(text = "Total Downtime", color = TextSecondary, fontSize = 11.sp)
                Text(
                    text = "${variance.totalDowntimeMinutes} mins",
                    color = if (variance.totalDowntimeMinutes > 30) AccentOrange else TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column {
                Text(text = "Tolerance Status", color = TextSecondary, fontSize = 11.sp)
                Text(
                    text = if (variance.isWithinTolerance) "OPTIMAL" else "VARIANCE ALERT",
                    color = if (variance.isWithinTolerance) AccentGreen else AccentRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LiveTimersTab(
    records: List<OperatorTimeTrackingResponseDto>,
    onResume: (String) -> Unit
) {
    if (records.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active operator time tracking records.", color = TextSecondary, fontSize = 13.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(records) { record ->
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
                                        Text(text = "${record.sequenceNumber}", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = record.stageType, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            TrackingStateBadge(state = record.currentState)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Machine: ${record.machineName} • Operator: ${record.operatorName}", color = TextSecondary, fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Setup: ${record.setupMinutes}m", color = TextSecondary, fontSize = 11.sp)
                            Text(text = "Run: ${record.runMinutes}m", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = "Downtime: ${record.downtimeMinutes}m", color = AccentOrange, fontSize = 11.sp)
                            Text(text = "Good: ${record.goodQuantityProduced}", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        if (record.currentState == "PAUSED" || record.currentState == "DOWNTIME") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Button(
                                    onClick = { onResume(record.workOrderId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Resume Execution", fontSize = 11.sp)
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
private fun MaterialDepletionTab(materials: List<ProductionMaterialConsumptionResponseDto>) {
    if (materials.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No material consumption records registered.", color = TextSecondary, fontSize = 13.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(materials) { mat ->
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
                            Text(text = mat.materialName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            MaterialStatusBadge(mat.status)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Code: ${mat.materialCode} • Lot: ${mat.batchLotNumber ?: "N/A"}", color = TextSecondary, fontSize = 11.sp)

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Planned: ${mat.plannedQuantity} ${mat.unitOfMeasure}", color = TextSecondary, fontSize = 11.sp)
                            Text(text = "Actual: ${mat.actualQuantityConsumed} ${mat.unitOfMeasure}", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Scrap: ${mat.scrapQuantity}", color = AccentRed, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MachineTelemetryTab(logs: List<MachineTelemetryResponseDto>) {
    if (logs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No machine telemetry logs registered.", color = TextSecondary, fontSize = 13.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(logs) { log ->
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
                            Text(text = log.machineName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = "${log.speedEfficiencyPercentage}% Efficiency",
                                color = if (log.speedEfficiencyPercentage.toDouble() >= 80.0) AccentGreen else AccentOrange,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Speed: ${log.recordedSpeedUnitsPerHour} units/hr", color = AccentCyan, fontSize = 11.sp)
                            Text(text = "Impressions: ${log.totalImpressions}", color = TextSecondary, fontSize = 11.sp)
                        }

                        if (log.currentDowntimeCategory != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Downtime Interruption: ${log.currentDowntimeCategory} (${log.downtimeMinutes}m)", color = AccentRed, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StageHandoversTab(
    handovers: List<StageOutputHandoverResponseDto>,
    onAccept: (String) -> Unit
) {
    if (handovers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No stage handovers recorded yet.", color = TextSecondary, fontSize = 13.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(handovers) { handover ->
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
                            Text(
                                text = "${handover.fromStage} → ${handover.toStage ?: "COMPLETED"}",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            HandoverStatusBadge(handover.status)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Good Output: ${handover.actualGoodQuantity}", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Scrap: ${handover.scrapQuantity}", color = AccentRed, fontSize = 11.sp)
                            Text(text = "Yield: ${handover.yieldPercentage}%", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        if (handover.status == "PENDING_VERIFICATION") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Button(
                                    onClick = { onAccept(handover.handoverId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Accept & Sign-Off", fontSize = 11.sp)
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
private fun VarianceAndAiTab(
    variance: ProductionExecutionVarianceResponseDto?,
    reconciliation: ShopFloorTrackingReconciliationResponseDto?,
    handoffContract: Module17Step07ShopFloorTrackingHandoffContractDto?
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                        Text(text = "8-Way Multi-Tier Shop-Floor Reconciliation", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        ReconRow("Work Orders Matched", reconciliation.workOrdersMatched)
                        ReconRow("Timers Consistent", reconciliation.timersConsistent)
                        ReconRow("Material Depletion Reconciled", reconciliation.materialDepletionReconciled)
                        ReconRow("Machine Telemetry Logged", reconciliation.telemetryLogged)
                        ReconRow("Stage Handovers Continuous", reconciliation.handoversContinuous)
                        ReconRow("Zero Unresolved Scrap Discrepancies", reconciliation.zeroUnresolvedScrapDiscrepancies)
                        ReconRow("Cryptographic Hash Integrity", reconciliation.cryptographicIntegrityPassed)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReconRow(label: String, passed: Boolean) {
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
private fun TrackingStateBadge(state: String) {
    val (bgColor, textColor) = when (state) {
        "RUNNING" -> Pair(AccentGreen.copy(alpha = 0.2f), AccentGreen)
        "SETUP" -> Pair(AccentCyan.copy(alpha = 0.2f), AccentCyan)
        "PAUSED", "DOWNTIME" -> Pair(AccentOrange.copy(alpha = 0.2f), AccentOrange)
        "COMPLETED" -> Pair(AccentPurple.copy(alpha = 0.2f), AccentPurple)
        else -> Pair(TextSecondary.copy(alpha = 0.2f), TextSecondary)
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.border(1.dp, textColor, RoundedCornerShape(4.dp))
    ) {
        Text(text = state, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
private fun MaterialStatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        "OVER_CONSUMED" -> Pair(AccentRed.copy(alpha = 0.2f), AccentRed)
        "RECORDED" -> Pair(AccentGreen.copy(alpha = 0.2f), AccentGreen)
        else -> Pair(TextSecondary.copy(alpha = 0.2f), TextSecondary)
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.border(1.dp, textColor, RoundedCornerShape(4.dp))
    ) {
        Text(text = status, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
private fun HandoverStatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        "ACCEPTED" -> Pair(AccentGreen.copy(alpha = 0.2f), AccentGreen)
        "PENDING_VERIFICATION" -> Pair(AccentOrange.copy(alpha = 0.2f), AccentOrange)
        else -> Pair(AccentRed.copy(alpha = 0.2f), AccentRed)
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.border(1.dp, textColor, RoundedCornerShape(4.dp))
    ) {
        Text(text = status, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}
