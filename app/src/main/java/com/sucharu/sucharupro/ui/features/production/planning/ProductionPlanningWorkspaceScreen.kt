package com.sucharu.sucharupro.ui.features.production.planning

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.productionplanning.*
import java.text.SimpleDateFormat
import java.util.*

// Theme Colors
private val DarkBg = Color(0xFF0D1117)
private val SurfaceDark = Color(0xFF161B22)
private val SurfaceCard = Color(0xFF1E2530)
private val BorderDark = Color(0xFF30363D)
private val AccentCyan = Color(0xFF58A6FF)
private val AccentGreen = Color(0xFF3FB950)
private val AccentOrange = Color(0xFFD29922)
private val AccentRed = Color(0xFFF85149)
private val AccentPurple = Color(0xFFA371F7)
private val TextPrimary = Color(0xFFF0F6FC)
private val TextSecondary = Color(0xFF8B949E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionPlanningWorkspaceScreen(
    orderId: String,
    orderNumber: String = "ORD-2026-001",
    planningSnapshot: ProductionPlanningSnapshotDto? = null,
    evaluation: ManufacturingReadinessEvaluationDto? = null,
    reconciliation: ProductionPlanningReconciliationDto? = null,
    events: List<ProductionPlanningEventDto> = emptyList(),
    handoffContract: Module17Step04ProductionPlanningHandoffDto? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onEvaluateReadiness: () -> Unit = {},
    onCreatePlan: () -> Unit = {},
    onHandoffToProduction: () -> Unit = {},
    onSupersedePlan: (String) -> Unit = {},
    onReconcilePlan: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showHandoffDialog by remember { mutableStateOf(false) }

    val tabs = listOf(
        "Overview" to Icons.Default.PrecisionManufacturing,
        "Job Spec" to Icons.Default.Description,
        "Requirements" to Icons.Default.Inventory2,
        "Routing & Machines" to Icons.Default.AltRoute,
        "Diagnostics" to Icons.Default.WarningAmber,
        "Reconciliation" to Icons.Default.VerifiedUser,
        "Audit & AI Handoff" to Icons.Default.AutoAwesome
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Order-to-Production Planning",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Order: $orderNumber • Manufacturing Readiness Engine",
                            color = AccentCyan,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    if (planningSnapshot != null && planningSnapshot.status == "READY") {
                        Button(
                            onClick = { showHandoffDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Handoff to Production", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (planningSnapshot == null) {
                        Button(
                            onClick = onCreatePlan,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Generate Plan", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = DarkBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Error banner
            if (errorMessage != null) {
                Surface(
                    color = AccentRed.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = AccentRed)
                        Spacer(Modifier.width(8.dp))
                        Text(errorMessage, color = TextPrimary, fontSize = 13.sp)
                    }
                }
            }

            // Tabs Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceDark,
                contentColor = AccentCyan,
                edgePadding = 12.dp,
                divider = {}
            ) {
                tabs.forEachIndexed { idx, tab ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    tab.second,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedTab == idx) AccentCyan else TextSecondary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    tab.first,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == idx) AccentCyan else TextSecondary
                                )
                            }
                        }
                    )
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentCyan)
                }
            } else {
                when (selectedTab) {
                    0 -> OverviewTab(planningSnapshot, evaluation, onCreatePlan, onEvaluateReadiness)
                    1 -> JobSpecTab(planningSnapshot?.specification)
                    2 -> RequirementsTab(planningSnapshot?.requirements ?: emptyList())
                    3 -> RoutingAndMachinesTab(planningSnapshot?.operations ?: emptyList(), planningSnapshot?.machineCompatibility ?: emptyList())
                    4 -> DiagnosticsTab(planningSnapshot?.diagnostics ?: evaluation?.diagnostics ?: emptyList())
                    5 -> ReconciliationTab(reconciliation, onReconcilePlan)
                    6 -> AuditAndHandoffTab(events, handoffContract)
                }
            }
        }
    }

    // Confirmation Dialog for Handoff
    if (showHandoffDialog) {
        AlertDialog(
            onDismissRequest = { showHandoffDialog = false },
            title = { Text("Confirm Production Handoff", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This action will commit the production planning snapshot into the downstream Production Job Execution Engine. The planning snapshot will be marked as HANDED_OFF and become permanently immutable.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showHandoffDialog = false
                        onHandoffToProduction()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                ) {
                    Text("Confirm Handoff", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showHandoffDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

// ============================================================
// TAB 1: OVERVIEW
// ============================================================

@Composable
private fun OverviewTab(
    snapshot: ProductionPlanningSnapshotDto?,
    evaluation: ManufacturingReadinessEvaluationDto?,
    onCreatePlan: () -> Unit,
    onEvaluateReadiness: () -> Unit
) {
    val score = snapshot?.readinessScore ?: evaluation?.overallScore ?: "0.0000"
    val scoreDouble = score.toDoubleOrNull() ?: 0.0
    val status = snapshot?.status ?: if (evaluation?.isManufacturingReady == true) "READY" else "ANALYZING"
    val feasibility = snapshot?.feasibilityStatus ?: evaluation?.feasibilityStatus ?: "UNKNOWN"

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Gauge / Hero Readiness Card
            Surface(
                color = SurfaceCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Manufacturing Readiness Score", color = TextSecondary, fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "$score / 100",
                            color = if (scoreDouble >= 80.0) AccentGreen else if (scoreDouble >= 50.0) AccentOrange else AccentRed,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (status == "READY") AccentGreen.copy(alpha = 0.2f) else AccentOrange.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    status,
                                    color = if (status == "READY") AccentGreen else AccentOrange,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("Feasibility: $feasibility", color = TextSecondary, fontSize = 12.sp)
                        }
                    }

                    // Circular Score Ring Indicator
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { (scoreDouble / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxSize(),
                            color = if (scoreDouble >= 80.0) AccentGreen else if (scoreDouble >= 50.0) AccentOrange else AccentRed,
                            trackColor = BorderDark
                        )
                        Text(
                            "${scoreDouble.toInt()}%",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        item {
            // Action Buttons Card
            Surface(
                color = SurfaceDark,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onEvaluateReadiness,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Re-Evaluate", fontSize = 13.sp)
                    }
                    Button(
                        onClick = onCreatePlan,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Create/Update Plan", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            // Planning Dimensions Sub-Scores
            Text("Readiness Dimension Breakdown", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DimensionRow("Commercial Terms", evaluation?.commercialReadinessScore ?: "100.0000", Icons.Default.AttachMoney, AccentGreen)
                DimensionRow("Job Specification", evaluation?.specificationReadinessScore ?: "85.0000", Icons.Default.Description, AccentCyan)
                DimensionRow("Material Availability", evaluation?.materialReadinessScore ?: "100.0000", Icons.Default.Inventory2, AccentPurple)
                DimensionRow("Machine Compatibility", evaluation?.machineReadinessScore ?: "100.0000", Icons.Default.PrecisionManufacturing, AccentOrange)
                DimensionRow("Schedule Feasibility", evaluation?.scheduleReadinessScore ?: "80.0000", Icons.Default.Event, AccentCyan)
            }
        }
    }
}

@Composable
private fun DimensionRow(name: String, scoreStr: String, icon: ImageVector, accentColor: Color) {
    val score = scoreStr.toDoubleOrNull() ?: 0.0
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(name, color = TextPrimary, fontSize = 13.sp)
            }
            Text(
                "$scoreStr / 100",
                color = if (score >= 80) AccentGreen else AccentOrange,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ============================================================
// TAB 2: JOB SPECIFICATION
// ============================================================

@Composable
private fun JobSpecTab(spec: ProductionJobSpecificationDto?) {
    if (spec == null) {
        EmptyStateCard("No job specification resolved yet. Click 'Generate Plan' to normalize specifications.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Normalized Job Specification", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        item {
            SpecCard("Physical Dimensions & Layout", Icons.Default.AspectRatio, AccentCyan) {
                SpecField("Job Title", spec.jobTitle)
                SpecField("Finished Dimensions", "${spec.finishedWidthMm} x ${spec.finishedHeightMm} mm")
                SpecField("Parent Sheet", "${spec.parentSheetWidthMm} x ${spec.parentSheetHeightMm} mm")
                SpecField("Press Sheet", "${spec.pressSheetWidthMm} x ${spec.pressSheetHeightMm} mm")
                SpecField("Imposition Ups", "${spec.impositionUps} up")
            }
        }
        item {
            SpecCard("Substrate & Printing Method", Icons.Default.Layers, AccentPurple) {
                SpecField("Substrate", "${spec.substrateType} (${spec.substrateGsm} GSM)")
                SpecField("Printing Process", spec.printingMethod)
                SpecField("Colors (Front / Back)", "${spec.colorsFront} / ${spec.colorsBack}")
                SpecField("Ordered Quantity", "${spec.orderedQuantity} pcs")
                SpecField("Planned Production Quantity", "${spec.plannedQuantity} pcs (inc. make-ready & waste)")
            }
        }
        item {
            SpecCard("Finishing & Packaging", Icons.Default.Handyman, AccentOrange) {
                SpecField("Lamination", spec.lamination)
                SpecField("Binding Method", spec.bindingMethod)
                SpecField("Folding Type", spec.foldingType)
                SpecField("Packaging", spec.packagingMethod)
                SpecField("Artwork File", spec.artworkUrl ?: "Pending upload")
            }
        }
    }
}

@Composable
private fun SpecCard(title: String, icon: ImageVector, accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SpecField(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ============================================================
// TAB 3: REQUIREMENTS
// ============================================================

@Composable
private fun RequirementsTab(requirements: List<ProductionPlanningRequirementDto>) {
    if (requirements.isEmpty()) {
        EmptyStateCard("No production requirements estimated yet.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Material & Consumable Requirements", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Planning estimations only — does not deduct live inventory stock.", color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
        }

        items(requirements) { req ->
            Surface(
                color = SurfaceCard,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = AccentPurple.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                req.category,
                                color = AccentPurple,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            "${req.totalPlannedQuantity} ${req.unitOfMeasure}",
                            color = AccentCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(req.description, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text("Code: ${req.itemCode} • Make-ready: ${req.makeReadyQuantity} • Waste: ${req.wasteQuantity}", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

// ============================================================
// TAB 4: ROUTING & MACHINES
// ============================================================

@Composable
private fun RoutingAndMachinesTab(
    operations: List<ProductionPlanningOperationDto>,
    machines: List<MachineCompatibilityResultDto>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (machines.isNotEmpty()) {
            item {
                Text("Machine & Work Center Compatibility", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                machines.forEach { m ->
                    Surface(
                        color = SurfaceDark,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(m.machineName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(m.notes ?: "Format & substrate compatible", color = TextSecondary, fontSize = 11.sp)
                            }
                            Text(
                                m.status,
                                color = if (m.status == "COMPATIBLE") AccentGreen else AccentOrange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Text("Proposed Production Routing", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
        }

        items(operations) { op ->
            Surface(
                color = SurfaceCard,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(AccentCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${op.sequenceNumber}", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(op.operationName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            if (op.isQcCheckpoint) {
                                Spacer(Modifier.width(6.dp))
                                Surface(color = AccentOrange.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                    Text("QC CHECKPOINT", color = AccentOrange, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Text("Work Center: ${op.targetWorkCenter} • Est. Setup: ${op.estimatedSetupMinutes}m • Run: ${op.estimatedRunMinutes}m", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ============================================================
// TAB 5: DIAGNOSTICS
// ============================================================

@Composable
private fun DiagnosticsTab(diagnostics: List<PlanningDiagnosticDto>) {
    if (diagnostics.isEmpty()) {
        EmptyStateCard("Zero blocking issues or warnings detected. All specifications and terms are verified!")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Planning Diagnostics & Discrepancies", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
        }

        items(diagnostics) { diag ->
            val isBlocking = diag.isBlocking
            Surface(
                color = if (isBlocking) AccentRed.copy(alpha = 0.1f) else AccentOrange.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isBlocking) AccentRed.copy(alpha = 0.4f) else AccentOrange.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        if (isBlocking) Icons.Default.Cancel else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isBlocking) AccentRed else AccentOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(diag.code, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (isBlocking) "[BLOCKING]" else "[WARNING]",
                                color = if (isBlocking) AccentRed else AccentOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(diag.message, color = TextPrimary, fontSize = 12.sp)
                        if (diag.recommendedAction != null) {
                            Spacer(Modifier.height(4.dp))
                            Text("Recommended Action: ${diag.recommendedAction}", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// TAB 6: RECONCILIATION
// ============================================================

@Composable
private fun ReconciliationTab(
    reconciliation: ProductionPlanningReconciliationDto?,
    onReconcile: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Multi-Tier Reconciliation", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                OutlinedButton(
                    onClick = onReconcile,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Re-Check", fontSize = 12.sp)
                }
            }
        }

        if (reconciliation != null) {
            item {
                Surface(
                    color = SurfaceCard,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (reconciliation.isFullyReconciled) AccentGreen.copy(alpha = 0.5f) else AccentOrange.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (reconciliation.isFullyReconciled) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (reconciliation.isFullyReconciled) AccentGreen else AccentOrange
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (reconciliation.isFullyReconciled) "FULLY RECONCILED" else "PARTIAL RECONCILIATION",
                                color = if (reconciliation.isFullyReconciled) AccentGreen else AccentOrange,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        ReconCheckRow("Customer Identity Match", reconciliation.customerMatch)
                        ReconCheckRow("Quantity Match", reconciliation.quantityMatch)
                        ReconCheckRow("Spec Fingerprint Match", reconciliation.specFingerprintMatch)
                        ReconCheckRow("Pricing Boundary Preserved", reconciliation.pricingBoundaryPreserved)
                        ReconCheckRow("Tenant Isolation Verified", reconciliation.tenantIsolationVerified)
                    }
                }
            }
        } else {
            item {
                EmptyStateCard("Click 'Re-Check' to execute multi-tier reconciliation check.")
            }
        }
    }
}

@Composable
private fun ReconCheckRow(label: String, passed: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Icon(
            if (passed) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (passed) AccentGreen else AccentRed,
            modifier = Modifier.size(16.dp)
        )
    }
}

// ============================================================
// TAB 7: AUDIT & AI HANDOFF
// ============================================================

@Composable
private fun AuditAndHandoffTab(
    events: List<ProductionPlanningEventDto>,
    contract: Module17Step04ProductionPlanningHandoffDto?
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (contract != null) {
            item {
                Text("AI Agent Read-Only Handoff Contract", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = SurfaceCard,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Handoff Contract v${contract.contractVersion}", color = AccentCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Status: ${contract.planningStatus} • Readiness: ${contract.readinessScore}%", color = TextPrimary, fontSize = 12.sp)
                        Text("Primary Work Center: ${contract.primaryWorkCenter} • Total Run: ${contract.totalEstimatedRunMinutes}m", color = TextSecondary, fontSize = 12.sp)
                        Text("Integrity Hash: ${contract.integrityHash.take(16)}...", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        item {
            Text("Lifecycle Audit Timeline", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
        }

        if (events.isEmpty()) {
            item {
                EmptyStateCard("No lifecycle audit events recorded yet.")
            }
        } else {
            items(events) { ev ->
                Surface(
                    color = SurfaceDark,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ev.eventType, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            val payload = ev.eventPayload
                            if (payload != null) {
                                Text(payload, color = TextSecondary, fontSize = 12.sp)
                            }
                            Text(
                                "${dateFormat.format(Date(ev.performedAt))} by ${ev.performedBy}",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                message,
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
