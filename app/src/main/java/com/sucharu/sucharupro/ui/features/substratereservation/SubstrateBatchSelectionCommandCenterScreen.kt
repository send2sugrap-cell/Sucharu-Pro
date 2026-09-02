package com.sucharu.sucharupro.ui.features.substratereservation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.substratereservation.BatchLotSelectionResponseDto
import com.sucharu.sucharupro.data.api.model.substratereservation.EvaluatedBatchCandidateDto
import com.sucharu.sucharupro.data.api.model.substratereservation.SelectedBatchAllocationDto
import java.math.BigDecimal

/**
 * Enterprise Dark Navy Command Center for Substrate Batch/Lot Selection & Grain Matching.
 * Module 19 Step 03.
 */
@Composable
fun SubstrateBatchSelectionCommandCenterScreen(
    viewModel: SubstrateBatchSelectionViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    val darkNavyBg = Color(0xFF0D1527)
    val cardBg = Color(0xFF14223D)
    val cardBorder = Color(0xFF223A63)
    val accentCyan = Color(0xFF00E5FF)
    val accentGreen = Color(0xFF00E676)
    val accentAmber = Color(0xFFFFB300)
    val accentRed = Color(0xFFFF5252)
    val textPrimary = Color(0xFFECEFF1)
    val textSecondary = Color(0xFF90A4AE)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(darkNavyBg)
            .padding(16.dp)
    ) {
        // Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Substrate Batch & Grain Optimizer",
                    color = textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Module 19 Step 03: Batch/Lot Selection, Grain Direction & Sheet Dimension Matching",
                    color = textSecondary,
                    fontSize = 12.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.evaluateSampleSelection() },
                    colors = ButtonDefaults.buttonColors(containerColor = cardBorder)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Re-evaluate", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Evaluate", fontSize = 12.sp)
                }

                Button(
                    onClick = { viewModel.confirmSelection() },
                    colors = ButtonDefaults.buttonColors(containerColor = accentGreen),
                    enabled = uiState.currentSelection?.isConfirmedAndAllocated == false
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Confirm", modifier = Modifier.size(16.dp), tint = Color.Black)
                    Spacer(Modifier.width(4.dp))
                    Text("Confirm & Allocate", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Success / Error Banner
        uiState.successMessage?.let { msg ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF00331A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, accentGreen)
            ) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = accentGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(msg, color = accentGreen, fontSize = 12.sp)
                }
            }
        }

        // 5 Enterprise Tabs
        val tabs = listOf(
            "1. Overview",
            "2. Candidate Lots",
            "3. 2D Visualizer",
            "4. Decision Breakdown",
            "5. AI & Audit"
        )

        ScrollableTabRow(
            selectedTabIndex = uiState.selectedTab,
            containerColor = cardBg,
            contentColor = accentCyan,
            edgePadding = 0.dp,
            modifier = Modifier.clip(RoundedCornerShape(8.dp))
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = uiState.selectedTab == index,
                    onClick = { viewModel.selectTab(index) },
                    text = {
                        Text(
                            text = title,
                            color = if (uiState.selectedTab == index) accentCyan else textSecondary,
                            fontWeight = if (uiState.selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Tab Content Router
        val selection = uiState.currentSelection
        if (selection != null) {
            when (uiState.selectedTab) {
                0 -> SelectionOverviewTab(selection, cardBg, cardBorder, accentCyan, accentGreen, textPrimary, textSecondary)
                1 -> CandidateLotsTab(selection.evaluatedCandidates, cardBg, cardBorder, accentCyan, accentGreen, accentAmber, accentRed, textPrimary, textSecondary)
                2 -> GrainDimensionVisualizerTab(selection, cardBg, cardBorder, accentCyan, accentGreen, accentAmber, textPrimary, textSecondary)
                3 -> SelectionDecisionTab(selection, cardBg, cardBorder, accentCyan, accentGreen, textPrimary, textSecondary)
                4 -> AuditAndHandoffTab(selection, viewModel, uiState.jsonHandoffPreview, cardBg, cardBorder, accentCyan, textPrimary, textSecondary)
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accentCyan)
            }
        }
    }
}

@Composable
private fun SelectionOverviewTab(
    selection: BatchLotSelectionResponseDto,
    cardBg: Color,
    cardBorder: Color,
    accentCyan: Color,
    accentGreen: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            // Status & KPI Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Selection Status: ${selection.status}", color = accentGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Score: ${selection.overallCompatibilityScore}/100", color = accentCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(selection.selectionExplanation, color = textPrimary, fontSize = 13.sp)
                }
            }
        }

        item {
            // Target Specification Details
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Target Production Requirement", color = accentCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    HorizontalDivider(color = cardBorder)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Order / Job Anchor:", color = textSecondary, fontSize = 12.sp)
                        Text("${selection.orderId} (${selection.executionJobId ?: "N/A"})", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Requested Material:", color = textSecondary, fontSize = 12.sp)
                        Text("${selection.requestedMaterialName} (${selection.targetGsm} GSM)", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Target Sheet Dimensions:", color = textSecondary, fontSize = 12.sp)
                        Text("${selection.requiredSheetWidthMm} x ${selection.requiredSheetHeightMm} mm", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Target Grain Direction:", color = textSecondary, fontSize = 12.sp)
                        Text(selection.requiredGrainDirection, color = accentCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Required Sheets:", color = textSecondary, fontSize = 12.sp)
                        Text("${selection.requiredSheets} sheets (${selection.allocatedReams} reams)", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item {
            // Selected Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Physical Batch/Lot Allocation Result", color = accentGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    HorizontalDivider(color = cardBorder)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Allocated Quantity:", color = textSecondary, fontSize = 12.sp)
                        Text("${selection.allocatedSheets} / ${selection.requiredSheets} sheets", color = accentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Primary Batch / Lot:", color = textSecondary, fontSize = 12.sp)
                        Text("${selection.primarySelectedBatchNumber ?: "N/A"} / ${selection.primarySelectedLotNumber ?: "N/A"}", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Fulfillment Mode:", color = textSecondary, fontSize = 12.sp)
                        Text(if (selection.isMultiBatchFulfillment) "Multi-Batch Split" else "Single Lot Fulfillment", color = textPrimary, fontSize = 12.sp)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Allocation Status:", color = textSecondary, fontSize = 12.sp)
                        Text(if (selection.isConfirmedAndAllocated) "CONFIRMED & ALLOCATED" else "EVALUATED (PENDING CONFIRM)", color = if (selection.isConfirmedAndAllocated) accentGreen else Color(0xFFFFB300), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CandidateLotsTab(
    candidates: List<EvaluatedBatchCandidateDto>,
    cardBg: Color,
    cardBorder: Color,
    accentCyan: Color,
    accentGreen: Color,
    accentAmber: Color,
    accentRed: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(candidates) { eval ->
            val isEligible = eval.isEligible
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isEligible) accentGreen else cardBorder)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${eval.candidate.lotNumber} (Batch: ${eval.candidate.batchNumber})",
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        val badgeColor = when {
                            eval.allocatedSheetsFromThisBatch > 0 -> accentGreen
                            isEligible -> accentCyan
                            else -> accentRed
                        }
                        Text(
                            text = if (eval.allocatedSheetsFromThisBatch > 0) "SELECTED (${eval.allocatedSheetsFromThisBatch} sh)" else if (isEligible) "ELIGIBLE" else "REJECTED",
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Text("Warehouse: ${eval.candidate.warehouseName} (${eval.candidate.locationId ?: "Default"})", color = textSecondary, fontSize = 11.sp)
                    Text("Dimensions: ${eval.candidate.sheetWidthMm} x ${eval.candidate.sheetHeightMm} mm • Grain: ${eval.candidate.grainDirection}", color = textSecondary, fontSize = 11.sp)
                    Text("Stock: ${eval.candidate.usableSheets} usable / ${eval.candidate.onHandPhysicalSheets} on-hand • Match Score: ${eval.overallScore}/100", color = accentCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

                    if (eval.evaluationReasons.isNotEmpty()) {
                        Text("Match Rationale: " + eval.evaluationReasons.joinToString(" • "), color = textSecondary, fontSize = 10.sp)
                    }

                    if (eval.rejectionReasons.isNotEmpty()) {
                        Text("Rejection: " + eval.rejectionReasons.joinToString(" • "), color = accentRed, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun GrainDimensionVisualizerTab(
    selection: BatchLotSelectionResponseDto,
    cardBg: Color,
    cardBorder: Color,
    accentCyan: Color,
    accentGreen: Color,
    accentAmber: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
            ) {
                Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("2D Sheet Dimension & Grain Alignment Visualizer", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Target: ${selection.requiredSheetWidthMm} x ${selection.requiredSheetHeightMm} mm (${selection.requiredGrainDirection})", color = accentCyan, fontSize = 11.sp)

                    Spacer(Modifier.height(16.dp))

                    // 2D Canvas Visualizer
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color(0xFF09101E), RoundedCornerShape(8.dp))
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        // Required Sheet (Cyan Wireframe)
                        val reqRectWidth = canvasWidth * 0.40f
                        val reqRectHeight = canvasHeight * 0.65f
                        val reqLeft = canvasWidth * 0.10f
                        val reqTop = (canvasHeight - reqRectHeight) / 2f

                        drawRoundRect(
                            color = Color(0xFF00E5FF),
                            topLeft = Offset(reqLeft, reqTop),
                            size = Size(reqRectWidth, reqRectHeight),
                            cornerRadius = CornerRadius(6f, 6f),
                            style = Stroke(width = 3f)
                        )

                        // Candidate Sheet (Green Filled/Border)
                        val candRectWidth = canvasWidth * 0.40f
                        val candRectHeight = canvasHeight * 0.65f
                        val candLeft = canvasWidth * 0.55f
                        val candTop = (canvasHeight - candRectHeight) / 2f

                        drawRoundRect(
                            color = Color(0xFF00E676),
                            topLeft = Offset(candLeft, candTop),
                            size = Size(candRectWidth, candRectHeight),
                            cornerRadius = CornerRadius(6f, 6f),
                            style = Stroke(width = 3f)
                        )

                        // Grain Direction Lines
                        // Required Grain (Vertical Lines for LONG_GRAIN)
                        for (i in 1..4) {
                            val x = reqLeft + (reqRectWidth / 5f) * i
                            drawLine(
                                color = Color(0x6600E5FF),
                                start = Offset(x, reqTop + 10f),
                                end = Offset(x, reqTop + reqRectHeight - 10f),
                                strokeWidth = 2f
                            )
                        }

                        // Candidate Grain Lines
                        for (i in 1..4) {
                            val x = candLeft + (candRectWidth / 5f) * i
                            drawLine(
                                color = Color(0x6600E676),
                                start = Offset(x, candTop + 10f),
                                end = Offset(x, candTop + candRectHeight - 10f),
                                strokeWidth = 2f
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(accentCyan, RoundedCornerShape(2.dp)))
                            Spacer(Modifier.width(4.dp))
                            Text("Target Sheet Layout", color = textSecondary, fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(accentGreen, RoundedCornerShape(2.dp)))
                            Spacer(Modifier.width(4.dp))
                            Text("Selected Lot Stock", color = textSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionDecisionTab(
    selection: BatchLotSelectionResponseDto,
    cardBg: Color,
    cardBorder: Color,
    accentCyan: Color,
    accentGreen: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(selection.selectedBatches) { alloc ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, accentGreen)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Lot: ${alloc.lotNumber} (Batch: ${alloc.batchNumber})", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${alloc.allocatedSheets} Sheets", color = accentGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    HorizontalDivider(color = cardBorder)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Warehouse & Location:", color = textSecondary, fontSize = 12.sp)
                        Text("${alloc.warehouseName} (${alloc.locationId ?: "General"})", color = textPrimary, fontSize = 12.sp)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Dimensions & Grain:", color = textSecondary, fontSize = 12.sp)
                        Text("${alloc.sheetWidthMm} x ${alloc.sheetHeightMm} mm • ${alloc.grainDirection} (Rotated: ${alloc.isRotated})", color = textPrimary, fontSize = 12.sp)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Weight & Volume:", color = textSecondary, fontSize = 12.sp)
                        Text("${alloc.allocatedWeightKg} kg • ${alloc.allocatedReams} reams", color = accentCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditAndHandoffTab(
    selection: BatchLotSelectionResponseDto,
    viewModel: SubstrateBatchSelectionViewModel,
    jsonPreview: String?,
    cardBg: Color,
    cardBorder: Color,
    accentCyan: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Cryptographic Audit & Master Seal", color = accentCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    HorizontalDivider(color = cardBorder)
                    Text("Selection ID: ${selection.selectionId}", color = textPrimary, fontSize = 12.sp)
                    Text("SHA-256 Seal: ${selection.masterIntegrityHash}", color = textSecondary, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    Text("Selected By: ${selection.selectedBy}", color = textSecondary, fontSize = 12.sp)
                    Text("Confirmed: ${selection.isConfirmedAndAllocated} (${selection.confirmedBy ?: "Pending"})", color = textSecondary, fontSize = 12.sp)

                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.exportHandoffJson() },
                        colors = ButtonDefaults.buttonColors(containerColor = cardBorder)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Generate AI Handoff Contract (v3.0.0)", fontSize = 12.sp)
                    }
                }
            }
        }

        jsonPreview?.let { json ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF09101E)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentCyan)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Module 19 Step 03 Handoff JSON Contract", color = accentCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(json, color = textPrimary, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
