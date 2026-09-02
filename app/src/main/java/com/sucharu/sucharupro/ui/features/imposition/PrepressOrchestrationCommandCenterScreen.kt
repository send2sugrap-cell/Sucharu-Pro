package com.sucharu.sucharupro.ui.features.imposition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.imposition.*
import java.math.BigDecimal

// Enterprise High-Contrast Color Palette
private val DeepNavyBg = Color(0xFF0A0E17)
private val CardBg = Color(0xFF131B2A)
private val CardBgElevated = Color(0xFF1C2638)
private val BorderColor = Color(0xFF2B3A52)
private val NeonCyan = Color(0xFF00E5FF)
private val NeonGreen = Color(0xFF00E676)
private val NeonAmber = Color(0xFFFFB300)
private val NeonPurple = Color(0xFFD500F9)
private val NeonPink = Color(0xFFFF4081)
private val TextPrimary = Color(0xFFF0F6FC)
private val TextSecondary = Color(0xFF8B949E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrepressOrchestrationCommandCenterScreen(
    viewModel: PrepressOrchestrationViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PREPRESS MASTER ORCHESTRATION & AI GOVERNANCE",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "End-to-End Imposition Interlock, Cross-Step Reconciliation & RIP Dispatch (Module 18 Step 06)",
                            color = NeonCyan,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.generateDefaultOrchestrationPlan() }) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "Reconcile Plan", tint = NeonCyan)
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
            // Scrollable Tab Selector (6 Tabs)
            ScrollableTabRow(
                selectedTabIndex = state.selectedTab,
                containerColor = CardBg,
                contentColor = NeonCyan,
                edgePadding = 8.dp
            ) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text("Executive Overview", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("Pipeline Stages (01-05)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = state.selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    text = { Text("Reconciliation Matrix", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = state.selectedTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    text = { Text("Optimization Intelligence", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = state.selectedTab == 4,
                    onClick = { viewModel.selectTab(4) },
                    text = { Text("Final Prepress Package", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = state.selectedTab == 5,
                    onClick = { viewModel.selectTab(5) },
                    text = { Text("Audit & AI Handoff", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
            }

            // Message banners
            state.errorMessage?.let { err ->
                Surface(
                    color = Color(0xFF3B1219),
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(err, color = Color(0xFFFF8080), modifier = Modifier.padding(12.dp), fontSize = 12.sp)
                }
            }

            state.successMessage?.let { msg ->
                Surface(
                    color = Color(0xFF0F3822),
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(msg, color = NeonGreen, modifier = Modifier.padding(12.dp), fontSize = 12.sp)
                }
            }

            // Body content per tab
            when (state.selectedTab) {
                0 -> ExecutiveOverviewTab(state, viewModel)
                1 -> PipelineStagesTab(state, viewModel)
                2 -> ReconciliationMatrixTab(state, viewModel)
                3 -> OptimizationIntelligenceTab(state, viewModel)
                4 -> FinalPrepressPackageTab(state, viewModel)
                5 -> AuditAndAiHandoffTab(state, viewModel)
            }
        }
    }
}

@Composable
private fun ExecutiveOverviewTab(state: PrepressOrchestrationUiState, viewModel: PrepressOrchestrationViewModel) {
    val plan = state.currentPlan

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (plan != null) {
            item {
                // Readiness Score Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("PREPRESS PRODUCTION READINESS SCORE", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(plan.readinessScore.summary, color = TextPrimary, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Status: ${plan.status} | Approval: ${plan.approvalStatus}", color = TextSecondary, fontSize = 11.sp)
                        }
                        Surface(
                            color = if (plan.readinessScore.overallScore >= BigDecimal("90.0000")) Color(0xFF0F3822) else Color(0xFF38290F),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.border(1.dp, if (plan.readinessScore.overallScore >= BigDecimal("90.0000")) NeonGreen else NeonAmber, RoundedCornerShape(8.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${plan.readinessScore.overallScore}", color = if (plan.readinessScore.overallScore >= BigDecimal("90.0000")) NeonGreen else NeonAmber, fontWeight = FontWeight.Bold, fontSize = 22.sp, fontFamily = FontFamily.Monospace)
                                Text("POINTS / 100", color = TextSecondary, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }

            item {
                // Production Key Metrics Grid
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("RECONCILED PRODUCTION PARAMETERS", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Order Quantity", color = TextSecondary, fontSize = 11.sp)
                                Text("${plan.requiredQuantity} pcs", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column {
                                Text("Produced Capacity", color = TextSecondary, fontSize = 11.sp)
                                Text("${plan.totalProducedQuantity} pcs", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column {
                                Text("Required Sheets", color = TextSecondary, fontSize = 11.sp)
                                Text("${plan.requiredSheets} sheets", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column {
                                Text("Sheet Utilization", color = TextSecondary, fontSize = 11.sp)
                                Text("${plan.sheetUtilizationPercentage}%", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        HorizontalDivider(color = BorderColor)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Total Signatures", color = TextSecondary, fontSize = 11.sp)
                                Text("${plan.totalSignaturesCount} signatures", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column {
                                Text("Total CTP Plates", color = TextSecondary, fontSize = 11.sp)
                                Text("${plan.totalPlatesCount} plates", color = NeonAmber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column {
                                Text("Press Sheet Dimensions", color = TextSecondary, fontSize = 11.sp)
                                Text("${plan.pressSheetWidthMm} x ${plan.pressSheetHeightMm}mm", color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                            Column {
                                Text("Plate Dimensions", color = TextSecondary, fontSize = 11.sp)
                                Text("${plan.plateWidthMm} x ${plan.plateHeightMm}mm", color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            item {
                // Cryptographic Master Seal Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBgElevated),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, NeonAmber, RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.VerifiedUser, contentDescription = "Seal", tint = NeonAmber, modifier = Modifier.size(16.dp))
                            Text("CRYPTOGRAPHIC MASTER INTEGRITY SEAL (SHA-256)", color = NeonAmber, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Text(plan.masterIntegrityHash, color = TextPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            item {
                // Action Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { viewModel.approvePlan() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("APPROVE PREPRESS PLAN", color = Color(0xFF003314), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.finalizePlan() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("FINALIZE & DISPATCH", color = Color(0xFF002933), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PipelineStagesTab(state: PrepressOrchestrationUiState, viewModel: PrepressOrchestrationViewModel) {
    val stages = state.currentPlan?.pipelineStages ?: emptyList()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("MODULE 18 PIPELINE STAGES AUDIT TRAIL (STEPS 01 TO 05)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Live status, upstream references, and cryptographic verification of all pipeline steps", color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
        }

        items(stages) { stage ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stage.stageStep, color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(stage.stageName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text(stage.summary, color = TextSecondary, fontSize = 11.sp)
                        stage.referenceId?.let {
                            Text("Ref ID: $it | Hash: ${stage.integrityHash?.take(16) ?: "N/A"}...", color = NeonGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Surface(
                        color = if (stage.isApplicable) Color(0xFF0F3822) else Color(0xFF1E2633),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(stage.status, color = if (stage.isApplicable) NeonGreen else TextSecondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReconciliationMatrixTab(state: PrepressOrchestrationUiState, viewModel: PrepressOrchestrationViewModel) {
    val recon = state.currentPlan?.reconciliationResult

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("CROSS-STEP RECONCILIATION MATRIX", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Mathematical verification of quantities, sheets, pages, plates, and dimensions", color = TextSecondary, fontSize = 12.sp)
        }

        if (recon != null) {
            item {
                Surface(
                    color = if (recon.isReconciled) Color(0xFF0F3822) else Color(0xFF3B1219),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, if (recon.isReconciled) NeonGreen else Color(0xFFFF8080), RoundedCornerShape(6.dp))
                ) {
                    Text(recon.summary, color = if (recon.isReconciled) NeonGreen else Color(0xFFFF8080), modifier = Modifier.padding(12.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (recon.discrepancies.isEmpty()) {
                item {
                    Text("No discrepancies detected. All parameters are 100% harmonized.", color = NeonGreen, fontSize = 12.sp)
                }
            } else {
                items(recon.discrepancies) { disc ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${disc.sourceStep} → ${disc.targetStep} [${disc.field}]", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(disc.severity, color = if (disc.severity == "BLOCKING_ERROR") Color(0xFFFF8080) else NeonAmber, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Text(disc.message, color = TextPrimary, fontSize = 12.sp)
                            Text("Expected: ${disc.expectedValue} | Actual: ${disc.actualValue}", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptimizationIntelligenceTab(state: PrepressOrchestrationUiState, viewModel: PrepressOrchestrationViewModel) {
    val recs = state.currentPlan?.recommendations ?: emptyList()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("PREPRESS OPTIMIZATION & AI RECOMMENDATIONS", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Deterministic suggestions for waste reduction, plate consolidation, and layout efficiency", color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (recs.isEmpty()) {
            item {
                Text("Plan is already operating at peak mathematical efficiency. No recommendations needed.", color = NeonGreen, fontSize = 12.sp)
            }
        } else {
            items(recs) { rec ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(rec.title, color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Confidence: ${(rec.confidenceScore.toFloat() * 100).toInt()}%", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(rec.description, color = TextPrimary, fontSize = 12.sp)
                        Text("Rationale: ${rec.rationale}", color = TextSecondary, fontSize = 11.sp)
                        HorizontalDivider(color = BorderColor)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Est. Waste Reduction: ${rec.estimatedWasteReductionPercentage}%", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Plate Savings: ${rec.estimatedPlateSavingsCount} plates", color = NeonAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinalPrepressPackageTab(state: PrepressOrchestrationUiState, viewModel: PrepressOrchestrationViewModel) {
    val plan = state.currentPlan

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("FINAL PREPRESS PRODUCTION PACKAGE SPECIFICATION", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Immutable master plan container ready for shop floor dispatch & substrate auto-reservation", color = TextSecondary, fontSize = 12.sp)
        }

        if (plan != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("MASTER CONTAINER HEADER", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Plan ID: ${plan.planId} (Version ${plan.version})", color = TextPrimary, fontSize = 13.sp)
                        Text("Job: ${plan.jobId ?: "N/A"} | Order: ${plan.orderId} | Item: ${plan.orderItemId}", color = TextSecondary, fontSize = 12.sp)
                        Text("Product: ${plan.productName}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        HorizontalDivider(color = BorderColor)
                        Text("UPSTREAM REFS & HASHE", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        plan.step05CtpOutputId?.let { Text("CTP Output ID: $it", color = TextSecondary, fontSize = 11.sp) }
                        plan.step04SignatureId?.let { Text("Signature Layout ID: $it", color = TextSecondary, fontSize = 11.sp) }
                        plan.step03NestingId?.let { Text("Nesting Spec ID: $it", color = TextSecondary, fontSize = 11.sp) }
                        plan.step01ImpositionId?.let { Text("Single Job Imposition ID: $it", color = TextSecondary, fontSize = 11.sp) }
                        HorizontalDivider(color = BorderColor)
                        Text("GOVERNANCE & AUDIT TRAIL", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Created By: ${plan.createdBy} | Status: ${plan.status}", color = TextSecondary, fontSize = 12.sp)
                        plan.approvedBy?.let { Text("Approved By: $it at ${plan.approvedAt}", color = NeonGreen, fontSize = 12.sp) }
                        plan.notes?.let { Text("Notes: $it", color = TextSecondary, fontSize = 11.sp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditAndAiHandoffTab(state: PrepressOrchestrationUiState, viewModel: PrepressOrchestrationViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("AI AGENT HANDOFF & MODULE 19/17 DOWNSTREAM CONTRACT", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Structured, explainable, read-only intelligence contract emitted to AI Agents, Production Dispatch & Substrate Auto-Reservation", color = TextSecondary, fontSize = 12.sp)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = state.handoffContractJson ?: "No handoff contract generated",
                        color = NeonGreen,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
