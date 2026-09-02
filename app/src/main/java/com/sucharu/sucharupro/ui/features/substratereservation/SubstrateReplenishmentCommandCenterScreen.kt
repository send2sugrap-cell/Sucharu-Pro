package com.sucharu.sucharupro.ui.features.substratereservation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import com.sucharu.sucharupro.data.api.model.substratereservation.SubstrateReplenishmentResponseDto
import com.sucharu.sucharupro.data.api.model.substratereservation.SupplierReorderCandidateDto
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubstrateReplenishmentCommandCenterScreen(
    viewModel: SubstrateReplenishmentViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    val bgDarkNavy = Color(0xFF0A0E17)
    val cardNavy = Color(0xFF131B2E)
    val accentCyan = Color(0xFF00E5FF)
    val textLight = Color(0xFFE2E8F0)
    val textMuted = Color(0xFF94A3B8)
    val borderNeon = Color(0xFF1E293B)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Auto-Replenishment & Supplier Reorder Alerts",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Module 19 Step 04 • Real-Time Stock Risk & Automated Procurement Triggers",
                            color = accentCyan,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.setShowEvaluateDialog(true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Run Evaluation", tint = accentCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cardNavy)
            )
        },
        containerColor = bgDarkNavy
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Row
            val tabs = listOf("Overview", "Stock Risk", "Recommendations", "Supplier Alerts", "Audit & AI Handoff")
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = cardNavy,
                contentColor = accentCyan,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (uiState.selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (uiState.selectedTab == index) accentCyan else textMuted
                            )
                        }
                    )
                }
            }

            // Message Banner
            uiState.errorMessage?.let { msg ->
                Surface(
                    color = Color(0x33EF4444),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = msg, color = Color(0xFFEF4444), fontSize = 13.sp)
                    }
                }
            }

            uiState.successMessage?.let { msg ->
                Surface(
                    color = Color(0x3310B981),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = msg, color = Color(0xFF10B981), fontSize = 13.sp)
                    }
                }
            }

            // Content per tab
            when (uiState.selectedTab) {
                0 -> OverviewTab(uiState, viewModel)
                1 -> StockRiskTab(uiState)
                2 -> RecommendationsTab(uiState, viewModel)
                3 -> SupplierAlertsTab(uiState, viewModel)
                4 -> AuditAndAiHandoffTab(uiState)
            }
        }
    }
}

@Composable
private fun OverviewTab(
    uiState: SubstrateReplenishmentUiState,
    viewModel: SubstrateReplenishmentViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // KPI Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "Monitored SKUs",
                    value = "${uiState.evaluations.size.coerceAtLeast(1)}",
                    accentColor = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Reorder Active",
                    value = "${uiState.evaluations.count { it.isReorderRequired }}",
                    accentColor = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Alerts Sent",
                    value = "${uiState.alerts.size}",
                    accentColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Active Evaluation Banner
        item {
            uiState.currentEvaluation?.let { eval ->
                EnterpriseCard(title = "CURRENT EVALUATION SNAPSHOT", badge = eval.triggerState) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = eval.materialName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text(text = eval.sku, color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        }
                        Text(text = "Warehouse: ${eval.warehouseName} (${eval.warehouseId})", color = Color(0xFF94A3B8), fontSize = 12.sp)

                        HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MetricItem("On Hand", "${eval.onHandPhysicalSheets} sheets")
                            MetricItem("Active Reserved", "${eval.activeReservedSheets} sheets")
                            MetricItem("Net Projected", "${eval.netProjectedAvailabilitySheets} sheets")
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MetricItem("Safety Stock", "${eval.safetyStockSheets} sheets")
                            MetricItem("Reorder Point", "${eval.reorderPointSheets} sheets")
                            MetricItem("Shortfall", "${eval.projectedShortfallSheets} sheets", highlightColor = Color(0xFFEF4444))
                        }

                        if (eval.isReorderRequired) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.triggerSupplierAlert(eval.evaluationId) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Dispatch Supplier Reorder Alert (${eval.recommendedReorderSheets} sheets)", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } ?: run {
                Text(text = "No substrate replenishment evaluation loaded.", color = Color(0xFF94A3B8), fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun StockRiskTab(uiState: SubstrateReplenishmentUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(uiState.evaluations) { eval ->
            EnterpriseCard(title = eval.sku, badge = eval.priority) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = eval.materialName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "Trigger State: ${eval.triggerState} • Reason: ${eval.primaryReason}", color = Color(0xFF94A3B8), fontSize = 12.sp)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Available: ${eval.availableSheets}", color = Color(0xFFE2E8F0), fontSize = 13.sp)
                        Text(text = "Shortfall: ${eval.projectedShortfallSheets}", color = if (eval.projectedShortfallSheets > 0) Color(0xFFEF4444) else Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = "Target: ${eval.targetStockSheets}", color = Color(0xFFE2E8F0), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationsTab(
    uiState: SubstrateReplenishmentUiState,
    viewModel: SubstrateReplenishmentViewModel
) {
    val eval = uiState.currentEvaluation
    if (eval == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No evaluation selected.", color = Color(0xFF94A3B8))
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            EnterpriseCard(title = "RECOMMENDED REORDER SIZING", badge = "${eval.recommendedReorderReams} Reams") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Recommended Sheets:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        Text(text = "${eval.recommendedReorderSheets} sheets", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Standard Ream Multiplier:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        Text(text = "500 sheets / ream", color = Color(0xFFE2E8F0), fontSize = 13.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Urgency Priority:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        Text(text = eval.priority, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        item {
            Text(text = "DETERMINISTICALLY RANKED SUPPLIERS (MODULE 12)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        items(eval.recommendedSuppliers) { supplier ->
            SupplierCard(supplier, onSelect = { viewModel.triggerSupplierAlert(eval.evaluationId, supplier.vendorId) })
        }
    }
}

@Composable
private fun SupplierCard(supplier: SupplierReorderCandidateDto, onSelect: () -> Unit) {
    EnterpriseCard(title = "RANK #${supplier.rank} • ${supplier.vendorName}", badge = "Score: ${supplier.suitabilityScore}") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "Vendor Code: ${supplier.vendorCode}", color = Color(0xFF38BDF8), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Text(text = "Lead Time: ~${supplier.estimatedLeadTimeDays} days • MOQ: ${supplier.minimumOrderQuantitySheets} sheets", color = Color(0xFFE2E8F0), fontSize = 12.sp)
            Text(text = supplier.selectionRationale, color = Color(0xFF94A3B8), fontSize = 11.sp)

            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF))
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Dispatch Alert to this Vendor", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SupplierAlertsTab(
    uiState: SubstrateReplenishmentUiState,
    viewModel: SubstrateReplenishmentViewModel
) {
    if (uiState.alerts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No supplier alerts generated yet.", color = Color(0xFF94A3B8))
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(uiState.alerts) { alert ->
            EnterpriseCard(title = alert.alertId, badge = alert.status) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "To: ${alert.vendorName} (${alert.vendorCode})", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(text = "Substrate: ${alert.materialName} (${alert.sku})", color = Color(0xFF38BDF8), fontSize = 12.sp)
                    Text(text = "Requested: ${alert.requestedSheets} sheets (${alert.requestedReams} reams)", color = Color(0xFFE2E8F0), fontSize = 13.sp)
                    Text(text = "Requisition ID: ${alert.purchaseRequisitionId ?: "N/A"}", color = Color(0xFF94A3B8), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun AuditAndAiHandoffTab(uiState: SubstrateReplenishmentUiState) {
    val eval = uiState.currentEvaluation
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            EnterpriseCard(title = "CRYPTOGRAPHIC INTEGRITY & FINGERPRINT", badge = "SHA-256") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Deduplication Fingerprint:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text(
                        text = eval?.deduplicationFingerprint ?: "N/A",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(text = "Master Integrity Seal:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text(
                        text = eval?.masterIntegrityHash ?: "N/A",
                        color = Color(0xFF10B981),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        item {
            EnterpriseCard(title = "DOWNSTREAM AI HANDOFF CONTRACT (V4.0.0)", badge = "JSON") {
                Text(
                    text = uiState.jsonHandoffPreview ?: "{}",
                    color = Color(0xFFE2E8F0),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF050811), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp)),
        color = Color(0xFF131B2E)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, color = Color(0xFF94A3B8), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, color = accentColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EnterpriseCard(
    title: String,
    badge: String? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp)),
        color = Color(0xFF131B2E)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                badge?.let {
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = it,
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String, highlightColor: Color = Color.White) {
    Column {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 11.sp)
        Text(text = value, color = highlightColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}
