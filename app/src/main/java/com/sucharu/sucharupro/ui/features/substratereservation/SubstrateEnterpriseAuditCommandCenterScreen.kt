package com.sucharu.sucharupro.ui.features.substratereservation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.sucharu.sucharupro.data.api.model.substratereservation.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubstrateEnterpriseAuditCommandCenterScreen(
    viewModel: SubstrateEnterpriseAuditViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    val bgDarkNavy = Color(0xFF0A0E17)
    val cardNavy = Color(0xFF131B2E)
    val accentCyan = Color(0xFF00E5FF)
    val textLight = Color(0xFFE2E8F0)
    val textMuted = Color(0xFF94A3B8)
    val borderNeon = Color(0xFF1E293B)
    val successGreen = Color(0xFF10B981)
    val warningAmber = Color(0xFFF59E0B)
    val criticalRed = Color(0xFFEF4444)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Enterprise Reservation Audit & AI Governance",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Module 19 Step 06 • Final Lifecycle Audit, Reconciliation, RLS & AI Handoff",
                            color = accentCyan,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadInitialData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = accentCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cardNavy)
            )
        },
        containerColor = bgDarkNavy
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = cardNavy,
                contentColor = accentCyan,
                edgePadding = 16.dp,
                divider = { HorizontalDivider(color = borderNeon) }
            ) {
                EnterpriseAuditTab.values().forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Text(
                                text = tab.title,
                                fontWeight = if (uiState.selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                color = if (uiState.selectedTab == tab) accentCyan else textMuted,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            // Notification Banner
            uiState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = criticalRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = criticalRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = error, color = criticalRed, fontSize = 13.sp)
                    }
                }
            }

            uiState.successMessage?.let { success ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = successGreen.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = successGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = success, color = successGreen, fontSize = 13.sp)
                    }
                }
            }

            // Content Body
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (uiState.selectedTab) {
                    EnterpriseAuditTab.OVERVIEW -> EnterpriseOverviewTab(uiState, cardNavy, borderNeon, accentCyan, textLight, textMuted, successGreen, warningAmber, criticalRed)
                    EnterpriseAuditTab.AUDIT_TRAIL -> AuditTrailTab(uiState, cardNavy, borderNeon, accentCyan, textLight, textMuted)
                    EnterpriseAuditTab.RECONCILIATION -> ReconciliationTab(uiState, cardNavy, borderNeon, accentCyan, textLight, textMuted, successGreen, warningAmber, criticalRed, onReconcile = { viewModel.runReconciliation(uiState.selectedReservationId) })
                    EnterpriseAuditTab.INTEGRITY_SECURITY -> IntegritySecurityTab(uiState, cardNavy, borderNeon, accentCyan, textLight, textMuted, successGreen, criticalRed, onVerify = { viewModel.verifyAuditIntegrity(uiState.selectedReservationId) })
                    EnterpriseAuditTab.AI_HANDOFF -> AiHandoffTab(uiState, cardNavy, borderNeon, accentCyan, textLight, textMuted, onGenerate = { viewModel.generateAiHandoff(uiState.selectedReservationId) })
                }
            }
        }
    }
}

@Composable
private fun EnterpriseOverviewTab(
    uiState: SubstrateEnterpriseAuditUiState,
    cardNavy: Color,
    borderNeon: Color,
    accentCyan: Color,
    textLight: Color,
    textMuted: Color,
    successGreen: Color,
    warningAmber: Color,
    criticalRed: Color
) {
    val summary = uiState.governanceSummary

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Enterprise Status Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderNeon, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = cardNavy),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Module 19 Substrate Governance Apex",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(successGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "RLS FORCED • ACTIVE", color = successGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Real-time enterprise surveillance across Requirement Resolution (Step 01), Soft/Hard Allocation (Step 02), Batch/Lot Selection (Step 03), Auto-Replenishment (Step 04), and Release Governance (Step 05).",
                        color = textMuted,
                        fontSize = 13.sp
                    )
                }
            }
        }

        item {
            // KPI Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Audited",
                    value = "${summary?.totalReservationsAudited ?: 0}",
                    accentColor = accentCyan,
                    cardNavy = cardNavy,
                    borderNeon = borderNeon,
                    textLight = textLight,
                    textMuted = textMuted
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    title = "Hard Allocated",
                    value = "${summary?.activeHardAllocations ?: 0}",
                    accentColor = successGreen,
                    cardNavy = cardNavy,
                    borderNeon = borderNeon,
                    textLight = textLight,
                    textMuted = textMuted
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    title = "Soft Reserved",
                    value = "${summary?.activeSoftReservations ?: 0}",
                    accentColor = warningAmber,
                    cardNavy = cardNavy,
                    borderNeon = borderNeon,
                    textLight = textLight,
                    textMuted = textMuted
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    modifier = Modifier.weight(1f),
                    title = "Reconciled Healthy",
                    value = "${summary?.reconciledHealthyCount ?: 0}",
                    accentColor = successGreen,
                    cardNavy = cardNavy,
                    borderNeon = borderNeon,
                    textLight = textLight,
                    textMuted = textMuted
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    title = "Discrepancies",
                    value = "${summary?.discrepanciesDetectedCount ?: 0}",
                    accentColor = if ((summary?.discrepanciesDetectedCount ?: 0) > 0) criticalRed else textMuted,
                    cardNavy = cardNavy,
                    borderNeon = borderNeon,
                    textLight = textLight,
                    textMuted = textMuted
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    title = "Integrity Intact",
                    value = "${summary?.integrityVerifiedIntactCount ?: 0}",
                    accentColor = accentCyan,
                    cardNavy = cardNavy,
                    borderNeon = borderNeon,
                    textLight = textLight,
                    textMuted = textMuted
                )
            }
        }
    }
}

@Composable
private fun KpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    accentColor: Color,
    cardNavy: Color,
    borderNeon: Color,
    textLight: Color,
    textMuted: Color
) {
    Card(
        modifier = modifier.border(1.dp, borderNeon, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = cardNavy),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, color = textMuted, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
    }
}

@Composable
private fun AuditTrailTab(
    uiState: SubstrateEnterpriseAuditUiState,
    cardNavy: Color,
    borderNeon: Color,
    accentCyan: Color,
    textLight: Color,
    textMuted: Color
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(uiState.auditEvents) { record ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderNeon, RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = cardNavy),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = record.eventType,
                            color = accentCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Actor: ${record.actorId} (${record.role})",
                            color = textMuted,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = record.reason, color = textLight, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Chain: ${record.chainHash.take(16)}...",
                            color = Color(0xFF64748B),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "Op: ${record.sourceOperation}",
                            color = textMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReconciliationTab(
    uiState: SubstrateEnterpriseAuditUiState,
    cardNavy: Color,
    borderNeon: Color,
    accentCyan: Color,
    textLight: Color,
    textMuted: Color,
    successGreen: Color,
    warningAmber: Color,
    criticalRed: Color,
    onReconcile: () -> Unit
) {
    val recon = uiState.activeReconciliation

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderNeon, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = cardNavy),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reconciliation Status",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        val statusColor = when (recon?.status) {
                            "HEALTHY" -> successGreen
                            "WARNING_DETECTED" -> warningAmber
                            "DISCREPANCIES_DETECTED" -> criticalRed
                            else -> textMuted
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusColor.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = recon?.status ?: "PENDING", color = statusColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onReconcile,
                        colors = ButtonDefaults.buttonColors(containerColor = accentCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Run Cross-Module Reconciliation", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (recon != null && recon.discrepancies.isNotEmpty()) {
            items(recon.discrepancies) { disc ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderNeon, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardNavy),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = disc.discrepancyType, color = criticalRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = disc.severity, color = warningAmber, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = disc.explanation, color = textLight, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Resolution: ${disc.resolutionRecommendation}", color = accentCyan, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun IntegritySecurityTab(
    uiState: SubstrateEnterpriseAuditUiState,
    cardNavy: Color,
    borderNeon: Color,
    accentCyan: Color,
    textLight: Color,
    textMuted: Color,
    successGreen: Color,
    criticalRed: Color,
    onVerify: () -> Unit
) {
    val result = uiState.integrityResult

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderNeon, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = cardNavy),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Cryptographic Audit Chain Integrity",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = result?.diagnosticMessage ?: "Audit history verification not executed.",
                        color = if (result?.isValidChain == true) successGreen else textLight,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onVerify,
                        colors = ButtonDefaults.buttonColors(containerColor = accentCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Verify Cryptographic Seal & Chain", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AiHandoffTab(
    uiState: SubstrateEnterpriseAuditUiState,
    cardNavy: Color,
    borderNeon: Color,
    accentCyan: Color,
    textLight: Color,
    textMuted: Color,
    onGenerate: () -> Unit
) {
    val contract = uiState.aiHandoffContract

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderNeon, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = cardNavy),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI Handoff Contract (v${contract?.contractVersion ?: "6.0.0"})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accentCyan.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "READ ONLY", color = accentCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Master Hash: ${contract?.masterIntegrityHash ?: "N/A"}",
                        color = textMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onGenerate,
                        colors = ButtonDefaults.buttonColors(containerColor = accentCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Synthesize AI Handoff Contract", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (contract != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderNeon, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardNavy),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "Allowed AI Operations", color = accentCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        contract.allowedActions.forEach { action ->
                            Text(text = "• $action", color = textLight, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Strictly Forbidden Actions (Enforced at Core)", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        contract.forbiddenActions.forEach { action ->
                            Text(text = "✕ $action", color = textMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
