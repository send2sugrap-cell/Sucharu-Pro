package com.sucharu.sucharupro.ui.features.inventory.substratereservation

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.substratereservation.SubstrateReservationResponseDto
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubstrateReservationCommandCenterScreen(
    viewModel: SubstrateReservationViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Substrate Stock Auto-Reservation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Module 19 Step 02 • Real-Time Soft/Hard Stock Reservation & Allocation Engine",
                            fontSize = 12.sp,
                            color = Color(0xFF64B5F6)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.openCreateDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = "New Reservation", tint = Color(0xFF64B5F6))
                    }
                    IconButton(onClick = { viewModel.loadReservations() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D1B2A))
            )
        },
        containerColor = Color(0xFF0A1118)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // KPI Summary Row
            SubstrateKpiSummaryRow(
                totalSheets = uiState.totalReservedSheets,
                totalReams = uiState.totalReservedReams,
                totalWeightKg = uiState.totalReservedWeightKg
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = Color(0xFF132238),
                contentColor = Color.White,
                edgePadding = 8.dp
            ) {
                SubstrateReservationTab.values().forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Text(
                                text = tab.title,
                                color = if (uiState.selectedTab == tab) Color(0xFF64B5F6) else Color(0xFFB0BEC5),
                                fontWeight = if (uiState.selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Feedback banners
            if (uiState.errorMessage != null) {
                Surface(
                    color = Color(0xFF441C1C),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFFF8A80))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = uiState.errorMessage ?: "", color = Color(0xFFFF8A80), fontSize = 13.sp)
                    }
                }
            }

            if (uiState.successMessage != null) {
                Surface(
                    color = Color(0xFF1B382B),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF81C784))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = uiState.successMessage ?: "", color = Color(0xFF81C784), fontSize = 13.sp)
                    }
                }
            }

            // Tab Content
            when (uiState.selectedTab) {
                SubstrateReservationTab.ACTIVE_RESERVATIONS -> ActiveHoldsTabContent(
                    reservations = uiState.activeReservations,
                    onPromoteClick = { res -> viewModel.openPromoteDialog(res) },
                    onReleaseClick = { res -> viewModel.openReleaseDialog(res) }
                )
                SubstrateReservationTab.REQUIREMENT_RESOLVER -> RequirementResolverTabContent(
                    isResolving = uiState.isResolving,
                    resolutionResult = uiState.resolutionResult,
                    onResolve = { orderId, orderItemId, code, name, gsm, w, h, prod, waste ->
                        viewModel.resolveRequirement(orderId, orderItemId, code, name, gsm, w, h, prod, waste)
                    }
                )
                SubstrateReservationTab.INVENTORY_INTERLOCK -> InventoryInterlockTabContent(uiState.activeReservations)
                SubstrateReservationTab.AUDIT_TIMELINE -> AuditTimelineTabContent(uiState.activeReservations)
                SubstrateReservationTab.AI_HANDOFF -> AiHandoffTabContent(uiState.activeReservations)
            }
        }
    }

    // Dialogs
    if (uiState.showCreateDialog) {
        CreateReservationDialog(
            isSubmitting = uiState.isSubmittingReservation,
            onDismiss = { viewModel.closeCreateDialog() },
            onSubmit = { orderId, orderItemId, jobId, prodId, sku, name, sheets, isHard, notes ->
                viewModel.createReservation(orderId, orderItemId, jobId, prodId, sku, name, sheets, isHard, notes)
            }
        )
    }

    if (uiState.showPromoteDialog && uiState.selectedReservation != null) {
        PromoteToHardDialog(
            reservation = uiState.selectedReservation!!,
            isPromoting = uiState.isPromoting,
            onDismiss = { viewModel.closePromoteDialog() },
            onConfirm = { jobId, workOrderId, whId, locId, batch ->
                viewModel.promoteSoftToHard(
                    reservationId = uiState.selectedReservation!!.reservationId,
                    executionJobId = jobId,
                    workOrderId = workOrderId,
                    warehouseId = whId,
                    locationId = locId,
                    batchNumber = batch
                )
            }
        )
    }

    if (uiState.showReleaseDialog && uiState.selectedReservation != null) {
        ReleaseReservationDialog(
            reservation = uiState.selectedReservation!!,
            onDismiss = { viewModel.closeReleaseDialog() },
            onConfirm = { reason ->
                viewModel.releaseReservation(uiState.selectedReservation!!.reservationId, reason)
            }
        )
    }
}

@Composable
fun SubstrateKpiSummaryRow(
    totalSheets: Long,
    totalReams: BigDecimal,
    totalWeightKg: BigDecimal
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        KpiCard(
            title = "Active Reserved Sheets",
            value = String.format("%,d", totalSheets),
            subtitle = "Physical Stock Reserved",
            modifier = Modifier.weight(1f),
            accentColor = Color(0xFF64B5F6)
        )
        KpiCard(
            title = "Total Reams Held",
            value = String.format("%.2f", totalReams),
            subtitle = "500 Sheets / Ream",
            modifier = Modifier.weight(1f),
            accentColor = Color(0xFF81C784)
        )
        KpiCard(
            title = "Committed Tonnage",
            value = String.format("%.1f kg", totalWeightKg),
            subtitle = "Calculated Weight",
            modifier = Modifier.weight(1f),
            accentColor = Color(0xFFFFB74D)
        )
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF132238)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, fontSize = 11.sp, color = Color(0xFFB0BEC5), fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 18.sp, color = accentColor, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 10.sp, color = Color(0xFF78909C))
        }
    }
}

@Composable
fun ActiveHoldsTabContent(
    reservations: List<SubstrateReservationResponseDto>,
    onPromoteClick: (SubstrateReservationResponseDto) -> Unit,
    onReleaseClick: (SubstrateReservationResponseDto) -> Unit
) {
    if (reservations.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No active substrate holds currently reserved.\nUse '+ New Reservation' or 'Requirement Resolver' to allocate substrate stock.",
                color = Color(0xFF78909C),
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(reservations) { res ->
                SubstrateReservationCard(
                    reservation = res,
                    onPromoteClick = { onPromoteClick(res) },
                    onReleaseClick = { onReleaseClick(res) }
                )
            }
        }
    }
}

@Composable
fun SubstrateReservationCard(
    reservation: SubstrateReservationResponseDto,
    onPromoteClick: () -> Unit,
    onReleaseClick: () -> Unit
) {
    val isSoft = reservation.status == "RESERVED_SOFT"
    val isHard = reservation.status == "ALLOCATED_HARD"

    val statusColor = when (reservation.status) {
        "ALLOCATED_HARD" -> Color(0xFF81C784)
        "RESERVED_SOFT" -> Color(0xFFFFB74D)
        "ISSUED_TO_FLOOR" -> Color(0xFF64B5F6)
        "CANCELLED" -> Color(0xFFE57373)
        else -> Color(0xFFB0BEC5)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF132238)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = reservation.sku,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = if (isHard) Color(0xFF1B382B) else Color(0xFF3E2723),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (isHard) "HARD COMMIT" else "SOFT HOLD",
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = reservation.status,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = reservation.productName, color = Color(0xFFB0BEC5), fontSize = 12.sp)

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFF1E334D))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Order: ${reservation.orderId}", color = Color(0xFF90A4AE), fontSize = 11.sp)
                    Text(text = "Job: ${reservation.executionJobId ?: "Unscheduled"}", color = Color(0xFF90A4AE), fontSize = 11.sp)
                    Text(text = "Warehouse: ${reservation.warehouseId}", color = Color(0xFF90A4AE), fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${String.format("%,d", reservation.reservedSheets)} Sheets",
                        color = Color(0xFF64B5F6),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(text = "${String.format("%.2f", reservation.reservedReams)} Reams", color = Color(0xFFB0BEC5), fontSize = 11.sp)
                    Text(text = "${String.format("%.1f", reservation.reservedWeightKg)} kg", color = Color(0xFFB0BEC5), fontSize = 11.sp)
                }
            }

            // Allocation sources breakdown if available
            if (reservation.allocationSources.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFF0F1B2B),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Allocated Warehouse Sources (${reservation.allocationSources.size}):",
                            color = Color(0xFF64B5F6),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        reservation.allocationSources.forEach { alloc ->
                            Text(
                                text = "• ${alloc.warehouseId} [${alloc.batchNumber ?: "NO-BATCH"}]: ${alloc.allocatedSheets} sheets",
                                color = Color(0xFFB0BEC5),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSoft) {
                    Button(
                        onClick = onPromoteClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("Promote to Hard", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (reservation.status != "CANCELLED") {
                    OutlinedButton(
                        onClick = onReleaseClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373)),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("Release", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun RequirementResolverTabContent(
    isResolving: Boolean,
    resolutionResult: com.sucharu.sucharupro.data.api.model.substratereservation.SubstrateSkuResolutionResponseDto?,
    onResolve: (String, String, String?, String, BigDecimal, BigDecimal, BigDecimal, Long, Long) -> Unit
) {
    var orderId by remember { mutableStateOf("ORD-2026-001") }
    var orderItemId by remember { mutableStateOf("ITEM-01") }
    var materialName by remember { mutableStateOf("Art Card 300 GSM") }
    var materialCode by remember { mutableStateOf("ART-300-25X36") }
    var productiveSheets by remember { mutableStateOf("4500") }
    var wasteSheets by remember { mutableStateOf("500") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF132238)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Upstream Imposition Demand Resolver",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = orderId,
                onValueChange = { orderId = it },
                label = { Text("Order ID") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = materialName,
                onValueChange = { materialName = it },
                label = { Text("Material Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = materialCode,
                onValueChange = { materialCode = it },
                label = { Text("Material SKU") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = productiveSheets,
                    onValueChange = { productiveSheets = it },
                    label = { Text("Productive Sheets") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = wasteSheets,
                    onValueChange = { wasteSheets = it },
                    label = { Text("Waste Sheets") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = {
                    onResolve(
                        orderId,
                        orderItemId,
                        materialCode,
                        materialName,
                        BigDecimal("300.0000"),
                        BigDecimal("635.0000"),
                        BigDecimal("914.4000"),
                        productiveSheets.toLongOrNull() ?: 4500L,
                        wasteSheets.toLongOrNull() ?: 500L
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isResolving
            ) {
                Text(if (isResolving) "Resolving..." else "Resolve & Check Inventory Interlock")
            }

            if (resolutionResult != null) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF1E334D))
                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Resolution Outcome:", color = Color(0xFF64B5F6), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Confidence: ${resolutionResult.confidence}", color = Color.White, fontSize = 12.sp)
                Text(text = "Total Demand: ${resolutionResult.requirement.totalSheetsRequired} Sheets (${resolutionResult.requirement.totalReamsRequired} Reams)", color = Color.White, fontSize = 12.sp)
                Text(text = "On-Hand Physical: ${resolutionResult.onHandPhysicalSheets} Sheets", color = Color.White, fontSize = 12.sp)
                Text(text = "Active Holds: ${resolutionResult.currentlyReservedSheets} Sheets", color = Color(0xFFFFB74D), fontSize = 12.sp)
                Text(text = "Reservable Available: ${resolutionResult.availableReservableSheets} Sheets", color = Color(0xFF81C784), fontSize = 12.sp)
                Text(
                    text = if (resolutionResult.isSufficientStockAvailable) "Status: SUFFICIENT STOCK AVAILABLE" else "Status: DEFICIT DETECTED (${resolutionResult.missingDeficitSheets} Missing)",
                    color = if (resolutionResult.isSufficientStockAvailable) Color(0xFF81C784) else Color(0xFFFF8A80),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun InventoryInterlockTabContent(reservations: List<SubstrateReservationResponseDto>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF132238)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = "Module 06 Canonical Stock Interlock", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Substrate Stock Auto-Reservation does not maintain shadow balances. Every hold is verified and synchronized against Module 06 Inventory Product master and warehouse storage locations.",
                color = Color(0xFFB0BEC5),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Connected Warehouses: WH-MAIN-01 (Central Paper Warehouse)", color = Color(0xFF64B5F6), fontSize = 12.sp)
            Text(text = "Active Synchronized SKU Records: ${reservations.map { it.sku }.distinct().size}", color = Color(0xFF81C784), fontSize = 12.sp)
        }
    }
}

@Composable
fun AuditTimelineTabContent(reservations: List<SubstrateReservationResponseDto>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF132238)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = "Immutable Reservation Audit Trail", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "All reservation actions (soft holds, hard allocations, promotions, releases) generate tamper-evident audit logs with actor identity, timestamp, and tenant scope.", color = Color(0xFFB0BEC5), fontSize = 12.sp)
        }
    }
}

@Composable
fun AiHandoffTabContent(reservations: List<SubstrateReservationResponseDto>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF132238)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = "Module 19 Step 02 AI Governance Contract", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Structured JSON handoff contracts (v2.0.0) are emitted to AI agents and n8n workflow engines for auto-replenishment triggers, stock alerts, and production scheduling.",
                color = Color(0xFFB0BEC5),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun CreateReservationDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String?, String, String, String, Long, Boolean, String?) -> Unit
) {
    var orderId by remember { mutableStateOf("ORD-201") }
    var orderItemId by remember { mutableStateOf("ITEM-01") }
    var sku by remember { mutableStateOf("ART-300-25X36") }
    var productName by remember { mutableStateOf("Art Card 300 GSM") }
    var sheets by remember { mutableStateOf("5000") }
    var isHard by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Substrate Reservation", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = orderId, onValueChange = { orderId = it }, label = { Text("Order ID") })
                OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("Substrate SKU") })
                OutlinedTextField(value = sheets, onValueChange = { sheets = it }, label = { Text("Required Sheets") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isHard, onCheckedChange = { isHard = it })
                    Text("Hard Allocate for Scheduled Job", color = Color.White, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(orderId, orderItemId, null, "PROD-01", sku, productName, sheets.toLongOrNull() ?: 1000L, isHard, null)
                },
                enabled = !isSubmitting
            ) {
                Text("Reserve Stock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = Color(0xFF132238)
    )
}

@Composable
fun PromoteToHardDialog(
    reservation: SubstrateReservationResponseDto,
    isPromoting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String?, String?, String?) -> Unit
) {
    var executionJobId by remember { mutableStateOf("JOB-EXEC-001") }
    var workOrderId by remember { mutableStateOf("WO-001") }
    var warehouseId by remember { mutableStateOf("WH-MAIN-01") }
    var batchNumber by remember { mutableStateOf("BATCH-2026-09") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Promote Soft Hold to HARD", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Promoting ${reservation.reservedSheets} sheets for ${reservation.sku} to hard commitment.",
                    color = Color(0xFFB0BEC5),
                    fontSize = 12.sp
                )
                OutlinedTextField(value = executionJobId, onValueChange = { executionJobId = it }, label = { Text("Execution Job ID") })
                OutlinedTextField(value = workOrderId, onValueChange = { workOrderId = it }, label = { Text("Work Order ID") })
                OutlinedTextField(value = warehouseId, onValueChange = { warehouseId = it }, label = { Text("Allocated Warehouse") })
                OutlinedTextField(value = batchNumber, onValueChange = { batchNumber = it }, label = { Text("Batch / Lot Number") })
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(executionJobId, workOrderId, warehouseId, null, batchNumber) },
                enabled = !isPromoting && executionJobId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text(if (isPromoting) "Promoting..." else "Confirm Hard Allocation")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = Color(0xFF132238)
    )
}

@Composable
fun ReleaseReservationDialog(
    reservation: SubstrateReservationResponseDto,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("Production job revised / cancelled") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Release Substrate Reservation", color = Color.White) },
        text = {
            Column {
                Text("Are you sure you want to release ${reservation.reservedSheets} sheets for ${reservation.sku} back to available inventory?", color = Color(0xFFB0BEC5), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Release Reason") })
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(reason) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))) {
                Text("Confirm Release")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = Color(0xFF132238)
    )
}
