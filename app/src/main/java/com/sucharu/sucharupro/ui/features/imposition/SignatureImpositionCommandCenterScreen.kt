package com.sucharu.sucharupro.ui.features.imposition

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.imposition.SignatureFormDto
import com.sucharu.sucharupro.data.api.model.imposition.SignatureImpositionSpecificationResponseDto
import com.sucharu.sucharupro.data.api.model.imposition.SignaturePagePlacementDto
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
fun SignatureImpositionCommandCenterScreen(
    viewModel: SignatureViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SIGNATURE IMPOSITION COMMAND CENTER",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Multi-Page Layouts, Folding Schemes & Work-and-Turn (Module 18 Step 04)",
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
                    IconButton(onClick = { viewModel.optimizeSignature() }) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "Optimize", tint = NeonCyan)
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
            // Tab Selector
            TabRow(
                selectedTabIndex = state.selectedTab,
                containerColor = CardBg,
                contentColor = NeonCyan
            ) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.onTabSelected(0) },
                    text = { Text("Signature Visualizer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Filled.Layers, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.onTabSelected(1) },
                    text = { Text("Folding & Creep", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = state.selectedTab == 2,
                    onClick = { viewModel.onTabSelected(2) },
                    text = { Text("Run Length & Yield", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Filled.Analytics, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = state.selectedTab == 3,
                    onClick = { viewModel.onTabSelected(3) },
                    text = { Text("Specs & Handoff", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            // Messages
            state.errorMessage?.let { msg ->
                Surface(
                    color = Color(0xFF3B1D24),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = msg, color = NeonPink, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                }
            }
            state.successMessage?.let { msg ->
                Surface(
                    color = Color(0xFF132F24),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = msg, color = NeonGreen, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                }
            }

            // Tab Content
            when (state.selectedTab) {
                0 -> SignatureVisualizerTab(state, viewModel)
                1 -> FoldingAndCreepTab(state, viewModel)
                2 -> RunLengthAndYieldTab(state)
                3 -> HistoryAndHandoffTab(state, viewModel)
            }
        }
    }
}

@Composable
private fun SignatureVisualizerTab(
    state: SignatureUiState,
    viewModel: SignatureViewModel
) {
    val spec = state.currentSpecification

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PRESS SHEET SIGNATURE FORM VIEW",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        spec?.let {
                            Text(
                                text = "${it.sheetUtilizationPercentage}% Sheet Utilized",
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (spec != null && spec.signatureForms.isNotEmpty()) {
                        // Signature selector chips
                        Text("Select Signature:", color = TextSecondary, fontSize = 11.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val sigCount = spec.totalSignaturesCount
                            for (i in 0 until sigCount) {
                                val isSelected = state.selectedSignatureIndex == i
                                Surface(
                                    color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else CardBgElevated,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NeonCyan else BorderColor),
                                    modifier = Modifier.clickable { viewModel.onSignatureIndexSelected(i) }
                                ) {
                                    Text(
                                        text = "Sig #${i + 1}",
                                        color = if (isSelected) NeonCyan else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Form side selector (Front vs Back)
                        val currentSigForms = spec.signatureForms.filter { it.signatureNumber == state.selectedSignatureIndex + 1 }
                        if (currentSigForms.size > 1) {
                            Text("Form Side:", color = TextSecondary, fontSize = 11.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                currentSigForms.forEachIndexed { idx, f ->
                                    val isSideSelected = state.selectedFormSideIndex == idx
                                    Surface(
                                        color = if (isSideSelected) NeonAmber.copy(alpha = 0.2f) else CardBgElevated,
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSideSelected) NeonAmber else BorderColor),
                                        modifier = Modifier.clickable { viewModel.onFormSideIndexSelected(idx) }
                                    ) {
                                        Text(
                                            text = if (f.formSide.contains("FRONT")) "Front (Outer Form)" else "Back (Inner Form)",
                                            color = if (isSideSelected) NeonAmber else TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        val activeForm = currentSigForms.getOrNull(state.selectedFormSideIndex) ?: currentSigForms.firstOrNull()

                        activeForm?.let { form ->
                            Spacer(modifier = Modifier.height(12.dp))
                            // Interactive Press Sheet Canvas
                            SignaturePressSheetCanvas(
                                form = form,
                                parentWidthMm = spec.parentSheetWidthMm,
                                parentHeightMm = spec.parentSheetHeightMm
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Grid Placements Summary Table
                            Text("Form Page Folio Allocation Matrix:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            form.pagePlacements.forEach { p ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CardBgElevated, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Slot #${p.slotIndex + 1} (R${p.row + 1}:C${p.column + 1}): Page ${if (p.isBlankPage) "BLANK" else p.pageNumber}",
                                        color = if (p.isBlankPage) TextSecondary else NeonCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Head: ${p.headOrientation} | Creep: ${p.creepShiftXMm}mm",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    } else {
                        Text("No signature layout generated yet.", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SignaturePressSheetCanvas(
    form: SignatureFormDto,
    parentWidthMm: BigDecimal,
    parentHeightMm: BigDecimal
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color(0xFF070B12), RoundedCornerShape(8.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            val sheetW = parentWidthMm.toFloat()
            val sheetH = parentHeightMm.toFloat()

            val scaleX = canvasW / sheetW
            val scaleY = canvasH / sheetH
            val scale = minOf(scaleX, scaleY) * 0.95f

            val drawnSheetW = sheetW * scale
            val drawnSheetH = sheetH * scale
            val offsetX = (canvasW - drawnSheetW) / 2f
            val offsetY = (canvasH - drawnSheetH) / 2f

            // Parent sheet outline
            drawRect(
                color = Color(0xFF1B2838),
                topLeft = Offset(offsetX, offsetY),
                size = Size(drawnSheetW, drawnSheetH)
            )
            drawRect(
                color = Color(0xFF4A6572),
                topLeft = Offset(offsetX, offsetY),
                size = Size(drawnSheetW, drawnSheetH),
                style = Stroke(width = 1.5f)
            )

            // Draw center folding lines (cross fold lines)
            drawLine(
                color = NeonPink.copy(alpha = 0.5f),
                start = Offset(offsetX + drawnSheetW / 2f, offsetY),
                end = Offset(offsetX + drawnSheetW / 2f, offsetY + drawnSheetH),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
            )
            drawLine(
                color = NeonPink.copy(alpha = 0.5f),
                start = Offset(offsetX, offsetY + drawnSheetH / 2f),
                end = Offset(offsetX + drawnSheetW, offsetY + drawnSheetH / 2f),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
            )

            // Draw each page cell
            form.pagePlacements.forEach { p ->
                val px = offsetX + (p.xMm.toFloat() * scale)
                val py = offsetY + (p.yMm.toFloat() * scale)
                val pw = p.widthMm.toFloat() * scale
                val ph = p.heightMm.toFloat() * scale

                // Page fill
                drawRect(
                    color = if (p.isBlankPage) Color(0xFF2A2A2A) else Color(0xFF0E3A40),
                    topLeft = Offset(px, py),
                    size = Size(pw, ph)
                )
                // Page border
                drawRect(
                    color = if (p.isBlankPage) Color(0xFF555555) else NeonCyan,
                    topLeft = Offset(px, py),
                    size = Size(pw, ph),
                    style = Stroke(width = 1f)
                )

                // Head indicator bar
                val barThickness = 3f * scale
                if (p.headOrientation.contains("UP")) {
                    drawRect(
                        color = NeonAmber,
                        topLeft = Offset(px, py),
                        size = Size(pw, barThickness)
                    )
                } else if (p.headOrientation.contains("DOWN")) {
                    drawRect(
                        color = NeonAmber,
                        topLeft = Offset(px, py + ph - barThickness),
                        size = Size(pw, barThickness)
                    )
                }
            }
        }
    }
}

@Composable
private fun FoldingAndCreepTab(
    state: SignatureUiState,
    viewModel: SignatureViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PUBLICATION BINDING & TURNING PARAMETERS",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = state.totalPages,
                        onValueChange = { viewModel.onTotalPagesChanged(it) },
                        label = { Text("Total Page Count (e.g. 64, 32, 16)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = state.signaturePageCount,
                        onValueChange = { viewModel.onSignaturePageCountChanged(it) },
                        label = { Text("Signature Page Count (4, 8, 16, 32)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = state.sheetTurningMethod,
                        onValueChange = { viewModel.onSheetTurningMethodChanged(it) },
                        label = { Text("Turning Method (SHEETWISE, WORK_AND_TURN, WORK_AND_TUMBLE, PERFECTING)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = state.bindingMethod,
                        onValueChange = { viewModel.onBindingMethodChanged(it) },
                        label = { Text("Binding Method (SADDLE_STITCH, PERFECT_BOUND, SECTION_SEWN)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enable Saddle Stitch Creep Compensation",
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                        Switch(
                            checked = state.enableCreepCompensation,
                            onCheckedChange = { viewModel.onCreepToggleChanged(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = CardBgElevated
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.optimizeSignature() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Re-Calculate Signature Layout", color = DeepNavyBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RunLengthAndYieldTab(state: SignatureUiState) {
    val spec = state.currentSpecification

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PRESS RUN LENGTH & SUBSTRATE CONSUMPTION",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (spec != null) {
                        MetricRow("Total Signatures:", "${spec.totalSignaturesCount} signature section(s)")
                        MetricRow("Signature Folio Count:", "${spec.signaturePageCount} pages per signature")
                        MetricRow("Press Run per Signature:", "${spec.commonRequiredSheets} parent sheets")
                        MetricRow("Total Parent Sheets Required:", "${spec.totalParentSheetsRequired} sheets", isHighlight = true)
                        MetricRow("Sheet Turning Mode:", spec.sheetTurningMethod)
                        MetricRow("Binding Method:", spec.bindingMethod)
                        MetricRow("Paper Stock & Weight:", "${spec.paperStockType} ${spec.gsm} GSM")
                        MetricRow("Sheet Utilization Rate:", "${spec.sheetUtilizationPercentage}%", isSuccess = true)
                        MetricRow("Usable Yield Rate:", "${spec.usableYieldPercentage}%")
                        MetricRow("Cryptographic Integrity Hash:", spec.integrityHash.take(16) + "...", isMono = true)
                    } else {
                        Text("No active calculation.", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryAndHandoffTab(
    state: SignatureUiState,
    viewModel: SignatureViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MODULE 19 SUBSTRATE RESERVATION HANDOFF",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Exports cryptographic specification to Module 19 Substrate Reservation contract without shadow ledger.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.exportHandoffContract() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Export Module 19 Handoff Contract", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }

                    state.handoffExportedJson?.let { json ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Exported Handoff Payload:", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = Color(0xFF070B12),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = json,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    isSuccess: Boolean = false,
    isMono: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
        Text(
            text = value,
            color = when {
                isHighlight -> NeonAmber
                isSuccess -> NeonGreen
                else -> TextPrimary
            },
            fontWeight = if (isHighlight || isSuccess) FontWeight.Bold else FontWeight.Normal,
            fontFamily = if (isMono) FontFamily.Monospace else FontFamily.Default,
            fontSize = 12.sp
        )
    }
}
