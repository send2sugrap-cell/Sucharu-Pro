package com.sucharu.sucharupro.ui.features.substratereservation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.substratereservation.SubstrateReleaseGovernanceResponseDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubstrateReleaseGovernanceCommandCenterScreen(
    viewModel: SubstrateReleaseGovernanceViewModel,
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
                            text = "Substrate Release & Revision Governance",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Module 19 Step 05 • Job Cancellation, Revision Delta & Release Integrity",
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
                    IconButton(onClick = { viewModel.loadDefaultSampleCase() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload Sample", tint = accentCyan)
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
            val tabs = listOf("Overview", "Cancellation", "Revision Delta", "Release Execution", "Audit & AI Handoff")
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
                                fontSize = 12.sp,
                                fontWeight = if (uiState.selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (uiState.selectedTab == index) accentCyan else textMuted
                            )
                        }
                    )
                }
            }

            // Messages
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

            // Tab Content
            when (uiState.selectedTab) {
                0 -> OverviewTab(uiState, viewModel)
                1 -> CancellationTab(uiState)
                2 -> RevisionDeltaTab(uiState)
                3 -> ReleaseExecutionTab(uiState, viewModel)
                4 -> AuditAndHandoffTab(uiState)
            }
        }
    }
}

@Composable
private fun OverviewTab(
    uiState: SubstrateReleaseGovernanceUiState,
    viewModel: SubstrateReleaseGovernanceViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    title = "Total Cases",
                    value = "${uiState.records.size.coerceAtLeast(1)}",
                    accentColor = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Eligible",
                    value = "${uiState.records.count { it.decision.contains("ELIGIBLE") }}",
                    accentColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Blocked",
                    value = "${uiState.records.count { it.decision == "RELEASE_BLOCKED" }}",
                    accentColor = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Executed",
                    value = "${uiState.records.count { it.executionStatus == "RELEASE_EXECUTED" }}",
                    accentColor = Color(0xFF818CF8),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            uiState.currentRecord?.let { rec ->
                EnterpriseCard(title = "ACTIVE GOVERNANCE CASE SNAPSHOT", badge = rec.decision) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Case: ${rec.governanceId}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = rec.triggerType, color = Color(0xFF00E5FF), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                        Text(text = "Substrate: ${rec.materialName} (${rec.sku})", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text(text = "Order: ${rec.orderId} • Job: ${rec.executionJobId ?: "N/A"} • Reservation: ${rec.reservationId}", color = Color(0xFF94A3B8), fontSize = 12.sp)

                        HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MetricItem("Allocated", "${rec.allocatedSheets} sheets")
                            MetricItem("Consumed", "${rec.consumedSheets} sheets", highlightColor = Color(0xFFF59E0B))
                            MetricItem("Committed", "${rec.committedSheets} sheets", highlightColor = Color(0xFFF59E0B))
                            MetricItem("Releasable", "${rec.releasableSheets} sheets", highlightColor = Color(0xFF10B981))
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Explanation: ${rec.explanation}", color = Color(0xFFE2E8F0), fontSize = 12.sp)

                        if (rec.blockingReason != "NONE") {
                            Text(text = "Operational Blocker: ${rec.blockingReason}", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } ?: run {
                Text(text = "No governance case loaded.", color = Color(0xFF94A3B8), fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun CancellationTab(uiState: SubstrateReleaseGovernanceUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(uiState.records) { rec ->
            EnterpriseCard(title = "${rec.orderId} • ${rec.sku}", badge = rec.decision) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Trigger: ${rec.triggerType} • Status: ${rec.executionStatus}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Allocated: ${rec.allocatedSheets}", color = Color(0xFFE2E8F0), fontSize = 13.sp)
                        Text(text = "Consumed: ${rec.consumedSheets}", color = Color(0xFFF59E0B), fontSize = 13.sp)
                        Text(text = "Releasable: ${rec.releasableSheets}", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text(text = rec.explanation, color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun RevisionDeltaTab(uiState: SubstrateReleaseGovernanceUiState) {
    val rec = uiState.currentRecord
    if (rec == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No active case selected.", color = Color(0xFF94A3B8))
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
            EnterpriseCard(title = "REVISION DELTA ANALYSIS", badge = rec.triggerType) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Previous Requirement:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        Text(text = "${rec.previousRequiredSheets} sheets", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "New Requirement:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        Text(text = "${rec.newRequiredSheets} sheets", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Retained in Allocation:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        Text(text = "${rec.retainedSheets} sheets", color = Color(0xFFE2E8F0), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Eligible for Release:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        Text(text = "${rec.releasableSheets} sheets", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Additional Demand Required:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        Text(text = "${rec.additionalRequiredSheets} sheets", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseExecutionTab(
    uiState: SubstrateReleaseGovernanceUiState,
    viewModel: SubstrateReleaseGovernanceViewModel
) {
    val rec = uiState.currentRecord
    if (rec == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No case selected.", color = Color(0xFF94A3B8))
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
            EnterpriseCard(title = "SEGREGATION OF DUTIES & RELEASE EXECUTION", badge = rec.executionStatus) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Substrate: ${rec.materialName} (${rec.sku})", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(text = "Releasable Sheets: ${rec.releasableSheets} • Warehouse: ${rec.warehouseId}", color = Color(0xFF00E5FF), fontSize = 13.sp)

                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

                    Text(
                        text = "Step 1: Supervisor Approval (${rec.approvedBy?.let { "Approved by $it" } ?: "Pending Approval"})",
                        color = if (rec.approvedBy != null) Color(0xFF10B981) else Color(0xFFF59E0B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (rec.executionStatus == "EVALUATED" && (rec.decision == "RELEASE_ELIGIBLE" || rec.decision == "PARTIAL_RELEASE_ELIGIBLE")) {
                        Button(
                            onClick = { viewModel.approveRelease(rec.governanceId) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Approve Release (${rec.releasableSheets} sheets)", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = "Step 2: Inventory Release Execution (${rec.executedBy?.let { "Executed by $it" } ?: "Pending Execution"})",
                        color = if (rec.executedBy != null) Color(0xFF10B981) else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (rec.executionStatus == "APPROVED" && rec.releasableSheets > 0L) {
                        Button(
                            onClick = { viewModel.executeRelease(rec.governanceId) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Execute Substrate Release (${rec.releasableSheets} sheets)", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (rec.executionStatus == "RELEASE_EXECUTED") {
                        Surface(
                            color = Color(0x3310B981),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Material release completed. Inventory balance successfully restored via Step 02 interlock.",
                                    color = Color(0xFF10B981),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditAndHandoffTab(uiState: SubstrateReleaseGovernanceUiState) {
    val rec = uiState.currentRecord
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            EnterpriseCard(title = "CRYPTOGRAPHIC INTEGRITY & AUDIT TRAIL", badge = "SHA-256") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Deduplication Fingerprint:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text(
                        text = rec?.deduplicationFingerprint ?: "N/A",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(text = "Master Integrity Seal:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text(
                        text = rec?.masterIntegrityHash ?: "N/A",
                        color = Color(0xFF10B981),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        item {
            EnterpriseCard(title = "DOWNSTREAM AI GOVERNANCE HANDOFF CONTRACT (V5.0.0)", badge = "JSON") {
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
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, color = Color(0xFF94A3B8), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, color = accentColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
