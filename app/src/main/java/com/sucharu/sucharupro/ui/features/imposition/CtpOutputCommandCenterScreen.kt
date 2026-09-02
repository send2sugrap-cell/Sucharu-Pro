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
import com.sucharu.sucharupro.data.api.model.imposition.*
import com.sucharu.sucharupro.domain.model.imposition.*
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

// Separation Colors
private val CyanColor = Color(0xFF00B0FF)
private val MagentaColor = Color(0xFFFF4081)
private val YellowColor = Color(0xFFFFEA00)
private val BlackColor = Color(0xFF424242)
private val SpotColor = Color(0xFFFF6D00)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CtpOutputCommandCenterScreen(
    viewModel: CtpViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PREPRESS CTP OUTPUT & PLATE PACKAGE COMMAND CENTER",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Plate Imposition, Marks Allocation & Production RIP Export (Module 18 Step 05)",
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
                    IconButton(onClick = { viewModel.generateDefaultCtpPackage() }) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "Generate Plates", tint = NeonCyan)
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
                    onClick = { viewModel.selectTab(0) },
                    text = { Text("Plate Visualizer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("Color Separations", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = state.selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    text = { Text("Prepress Marks", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = state.selectedTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    text = { Text("Production Package", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = state.selectedTab == 4,
                    onClick = { viewModel.selectTab(4) },
                    text = { Text("RIP Handoff & Audit", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
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
                0 -> PlateVisualizerTab(state, viewModel)
                1 -> ColorSeparationsTab(state, viewModel)
                2 -> PrepressMarksTab(state, viewModel)
                3 -> ProductionPackageTab(state, viewModel)
                4 -> HandoffAndHistoryTab(state, viewModel)
            }
        }
    }
}

@Composable
private fun PlateVisualizerTab(state: CtpUiState, viewModel: CtpViewModel) {
    val spec = state.currentSpecification
    val plates = spec?.outputPackage?.plates ?: emptyList()
    val activePlate = plates.getOrNull(state.activePlateIndex)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Left Column: Interactive Plate Canvas (65% width)
        Card(
            modifier = Modifier
                .weight(0.65f)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activePlate?.plateName ?: "Plate Canvas Visualizer",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.showGripperZone,
                            onClick = { viewModel.toggleGripperZone(!state.showGripperZone) },
                            label = { Text("Gripper", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = state.showMarks,
                            onClick = { viewModel.toggleMarks(!state.showMarks) },
                            label = { Text("Marks", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = state.showColorBars,
                            onClick = { viewModel.toggleColorBars(!state.showColorBars) },
                            label = { Text("Color Bar", fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Plate & Sheet Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(DeepNavyBg, RoundedCornerShape(6.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasW = size.width
                        val canvasH = size.height

                        val plateW = (spec?.outputPackage?.plateWidthMm ?: BigDecimal("695")).toFloat()
                        val plateH = (spec?.outputPackage?.plateHeightMm ?: BigDecimal("994.4")).toFloat()
                        val sheetW = (spec?.outputPackage?.pressSheetWidthMm ?: BigDecimal("635")).toFloat()
                        val sheetH = (spec?.outputPackage?.pressSheetHeightMm ?: BigDecimal("914.4")).toFloat()

                        val scale = minOf(canvasW / (plateW * 1.1f), canvasH / (plateH * 1.1f))

                        val drawPlateW = plateW * scale
                        val drawPlateH = plateH * scale
                        val plateLeft = (canvasW - drawPlateW) / 2
                        val plateTop = (canvasH - drawPlateH) / 2

                        // 1. Draw Physical Plate (Aluminum Plate Bounding Box)
                        drawRect(
                            color = Color(0xFF263238),
                            topLeft = Offset(plateLeft, plateTop),
                            size = Size(drawPlateW, drawPlateH)
                        )
                        drawRect(
                            color = NeonCyan,
                            topLeft = Offset(plateLeft, plateTop),
                            size = Size(drawPlateW, drawPlateH),
                            style = Stroke(width = 2f)
                        )

                        // 2. Draw Gripper Clearance Zone at Bottom
                        val gripperH = (spec?.outputPackage?.gripperMarginMm ?: BigDecimal("45")).toFloat() * scale
                        if (state.showGripperZone) {
                            drawRect(
                                color = Color(0x33FFB300),
                                topLeft = Offset(plateLeft, plateTop + drawPlateH - gripperH),
                                size = Size(drawPlateW, gripperH)
                            )
                        }

                        // 3. Draw Press Sheet inside Plate
                        val sheetOffsetLeft = (spec?.outputPackage?.sideGuideMarginLeftMm ?: BigDecimal("30")).toFloat() * scale
                        val sheetOffsetBottom = gripperH
                        val drawSheetW = sheetW * scale
                        val drawSheetH = sheetH * scale
                        val sheetX = plateLeft + sheetOffsetLeft
                        val sheetY = plateTop + drawPlateH - sheetOffsetBottom - drawSheetH

                        drawRect(
                            color = Color(0xFF1E2A38),
                            topLeft = Offset(sheetX, sheetY),
                            size = Size(drawSheetW, drawSheetH)
                        )
                        drawRect(
                            color = Color(0xFFE0E0E0),
                            topLeft = Offset(sheetX, sheetY),
                            size = Size(drawSheetW, drawSheetH),
                            style = Stroke(width = 1.5f)
                        )

                        // 4. Draw Registration Marks (Crosshairs)
                        if (state.showMarks) {
                            val markLen = 12f
                            val regCorners = listOf(
                                Offset(sheetX - 10f, sheetY - 10f),
                                Offset(sheetX + drawSheetW + 10f, sheetY - 10f),
                                Offset(sheetX - 10f, sheetY + drawSheetH + 10f),
                                Offset(sheetX + drawSheetW + 10f, sheetY + drawSheetH + 10f),
                                Offset(sheetX + drawSheetW / 2, sheetY - 10f)
                            )
                            for (c in regCorners) {
                                drawCircle(color = NeonCyan, radius = 6f, center = c, style = Stroke(width = 1.5f))
                                drawLine(color = NeonCyan, start = Offset(c.x - markLen, c.y), end = Offset(c.x + markLen, c.y), strokeWidth = 1.5f)
                                drawLine(color = NeonCyan, start = Offset(c.x, c.y - markLen), end = Offset(c.x, c.y + markLen), strokeWidth = 1.5f)
                            }
                        }

                        // 5. Draw Color Calibration Bar at Top
                        if (state.showColorBars) {
                            val barY = sheetY - 18f
                            val barW = drawSheetW * 0.8f
                            val barX = sheetX + drawSheetW * 0.1f
                            val barH = 10f

                            val colors = listOf(CyanColor, MagentaColor, YellowColor, BlackColor, SpotColor)
                            val patchW = barW / (colors.size * 5)
                            var curX = barX
                            for (col in colors) {
                                for (step in 1..5) {
                                    val alpha = step * 0.2f
                                    drawRect(color = col.copy(alpha = alpha), topLeft = Offset(curX, barY), size = Size(patchW, barH))
                                    curX += patchW
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right Column: Plate List & Separation Selector (35% width)
        Card(
            modifier = Modifier
                .weight(0.35f)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Text(
                    text = "PLATE SEPARATIONS (${plates.size})",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(plates.indices.toList()) { idx ->
                        val p = plates[idx]
                        val isSelected = idx == state.activePlateIndex
                        val sepColor = when (p.colorSeparation) {
                            "CYAN" -> CyanColor
                            "MAGENTA" -> MagentaColor
                            "YELLOW" -> YellowColor
                            "BLACK" -> Color(0xFFB0BEC5)
                            else -> SpotColor
                        }

                        Surface(
                            color = if (isSelected) CardBgElevated else Color(0xFF0E1420),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, if (isSelected) NeonCyan else BorderColor, RoundedCornerShape(6.dp))
                                .clickable { viewModel.selectPlateIndex(idx) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(modifier = Modifier.size(14.dp).background(sepColor, RoundedCornerShape(2.dp)))
                                    Column {
                                        Text(p.plateName, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("${p.plateSide} | ${p.resolutionDpi} DPI | ${p.screenRulingLpi} LPI", color = TextSecondary, fontSize = 10.sp)
                                    }
                                }
                                Text("V${p.signatureNumber}", color = NeonCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.approveCtpPackage() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("APPROVE FOR CTP RIP", color = Color(0xFF003314), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ColorSeparationsTab(state: CtpUiState, viewModel: CtpViewModel) {
    val spec = state.currentSpecification
    val plates = spec?.outputPackage?.plates ?: emptyList()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("OFFSET COLOR SEPARATION CHANNELS", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("4-Color Process (CMYK) and Spot Pantone Separation Matrix", color = TextSecondary, fontSize = 12.sp)
        }

        items(plates) { plate ->
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
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(plate.plateName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Plate Side: ${plate.plateSide} | Color: ${plate.colorSeparation} ${plate.spotColorName?.let { "($it)" } ?: ""}", color = NeonCyan, fontSize = 12.sp)
                        Text("Screen Angle: ${plate.screenAngleDegrees}° | Ruling: ${plate.screenRulingLpi} LPI | Dot: ${plate.dotShape}", color = TextSecondary, fontSize = 11.sp)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Area: ${plate.plateAreaMm2} mm²", color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text("SHA-256: ${plate.plateIntegrityHash.take(12)}...", color = NeonGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun PrepressMarksTab(state: CtpUiState, viewModel: CtpViewModel) {
    val marks = state.currentSpecification?.outputPackage?.marks ?: emptyList()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("PREPRESS MARKS PLACEMENT TABLE (${marks.size} MARKS)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Registration crosshairs, crop corners, bleed limits, color bars, slugs & gripper zones", color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(marks) { mark ->
            Surface(
                color = CardBg,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(6.dp))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(mark.markType, color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(mark.labelText ?: "No label", color = TextPrimary, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("X: ${mark.xPositionMm}mm | Y: ${mark.yPositionMm}mm", color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text("Size: ${mark.widthMm} x ${mark.heightMm}mm", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductionPackageTab(state: CtpUiState, viewModel: CtpViewModel) {
    val spec = state.currentSpecification
    val pkg = spec?.outputPackage

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("CTP PRODUCTION PACKAGE SPECIFICATION", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Complete auditable container ready for Prepress CTP RIP imaging dispatch", color = TextSecondary, fontSize = 12.sp)
        }

        if (pkg != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("PACKAGE HEADER", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Package ID: ${pkg.packageId} (Version ${pkg.packageVersion})", color = TextPrimary, fontSize = 13.sp)
                        Text("Source Imposition: ${pkg.sourceImpositionType} [${pkg.sourceImpositionId}]", color = TextSecondary, fontSize = 12.sp)
                        Text("Source Integrity Hash: ${pkg.sourceIntegrityHash}", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Divider(color = BorderColor)
                        Text("PLATE COUNTS", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Total Plates: ${pkg.totalPlatesCount} | Front: ${pkg.frontPlatesCount} | Back: ${pkg.backPlatesCount} | Spot: ${pkg.spotColorsCount}", color = TextPrimary, fontSize = 13.sp)
                        Divider(color = BorderColor)
                        Text("RIP INSTRUCTIONS", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(pkg.ripInstructions, color = TextPrimary, fontSize = 12.sp)
                        Divider(color = BorderColor)
                        Text("VALIDATION STATUS", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(pkg.validationSummary, color = NeonGreen, fontSize = 12.sp)
                        Divider(color = BorderColor)
                        Text("CRYPTOGRAPHIC INTEGRITY SEAL (SHA-256)", color = NeonAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(pkg.integrityHash, color = NeonAmber, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun HandoffAndHistoryTab(state: CtpUiState, viewModel: CtpViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("DOWNSTREAM CTP RIP HANDOFF CONTRACT & AUDIT", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Module 18 Step 05 Prepress CTP Production Handoff Contract", color = TextSecondary, fontSize = 12.sp)
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
