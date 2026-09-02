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
import com.sucharu.sucharupro.data.api.model.imposition.DynamicNestingSpecificationResponseDto
import com.sucharu.sucharupro.data.api.model.imposition.NestingCandidateItemDto
import java.math.BigDecimal

// Sucharu Pro Enterprise Palette
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

private val JobColorPalette = listOf(
    NeonCyan,
    NeonGreen,
    NeonAmber,
    NeonPurple,
    NeonPink,
    Color(0xFF64B5F6),
    Color(0xFFFFB74D),
    Color(0xFF81C784)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicNestingCommandCenterScreen(
    viewModel: NestingViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "DYNAMIC NESTING & WASTAGE OPTIMIZER",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "2D Rectangular Bin-Packing & Substrate Utilization (Module 18 Step 03)",
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
                    IconButton(onClick = { viewModel.optimizeNesting() }) {
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
                .padding(horizontal = 16.dp)
        ) {
            // Tab Navigation
            TabRow(
                selectedTabIndex = state.selectedTab,
                containerColor = CardBg,
                contentColor = NeonCyan,
                divider = {}
            ) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.onTabSelected(0) },
                    text = { Text("2D Sheet Canvas", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.onTabSelected(1) },
                    text = { Text("Candidate Pool (${state.candidatePool.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = state.selectedTab == 2,
                    onClick = { viewModel.onTabSelected(2) },
                    text = { Text("Offcuts & Waste", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = state.selectedTab == 3,
                    onClick = { viewModel.onTabSelected(3) },
                    text = { Text("History (${state.specificationsList.size})", fontWeight = FontWeight.SemiBold) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notifications
            state.errorMessage?.let { msg ->
                Surface(
                    color = Color(0x33FF5252),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ErrorOutline, contentDescription = "Error", tint = Color(0xFFFF5252))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(msg, color = Color(0xFFFF5252), fontSize = 13.sp)
                    }
                }
            }

            state.successMessage?.let { msg ->
                Surface(
                    color = Color(0x2200E676),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircleOutline, contentDescription = "Success", tint = NeonGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(msg, color = NeonGreen, fontSize = 13.sp)
                    }
                }
            }

            // Body content
            when (state.selectedTab) {
                0 -> VisualNestingCanvasTab(state)
                1 -> CandidatePoolTab(state, viewModel)
                2 -> OffcutsAndWasteTab(state)
                3 -> NestingHistoryTab(state, viewModel)
            }
        }
    }
}

@Composable
private fun VisualNestingCanvasTab(state: NestingUiState) {
    val spec = state.currentSpecification

    if (spec == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No nesting specification calculated. Add candidate jobs and tap Optimize.", color = TextSecondary)
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // KPI Overview
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiCard(
                    title = "USABLE YIELD",
                    value = "${spec.usableYieldPercentage}%",
                    accentColor = NeonCyan,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "TOTAL UTILIZATION",
                    value = "${spec.sheetUtilizationPercentage}%",
                    accentColor = NeonGreen,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "PRESS RUN SHEETS",
                    value = "${spec.commonRequiredSheets}",
                    accentColor = NeonAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2D Interactive Canvas
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2D NESTING CANVAS (${spec.sheetWidthMm} x ${spec.sheetHeightMm} mm)",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${spec.totalItemsPlaced} items placed",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Draw 2D Sheet Canvas
                    val parentW = spec.sheetWidthMm.toFloat().coerceAtLeast(100f)
                    val parentH = spec.sheetHeightMm.toFloat().coerceAtLeast(100f)
                    val distinctJobs = spec.jobSummaries.map { it.jobId }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .background(Color(0xFF0D131F), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val canvasW = size.width
                            val canvasH = size.height

                            val scale = minOf(canvasW / parentW, canvasH / parentH)
                            val drawW = parentW * scale
                            val drawH = parentH * scale
                            val offsetX = (canvasW - drawW) / 2f
                            val offsetY = (canvasH - drawH) / 2f

                            // Outer sheet border
                            drawRect(
                                color = Color(0xFF334155),
                                topLeft = Offset(offsetX, offsetY),
                                size = Size(drawW, drawH),
                                style = Stroke(width = 2f)
                            )

                            // Usable area border (dashed margin)
                            val marginL = spec.marginLeftMm.toFloat() * scale
                            val marginT = spec.marginTopMm.toFloat() * scale
                            val usableW = spec.usableWidthMm.toFloat() * scale
                            val usableH = spec.usableHeightMm.toFloat() * scale

                            drawRect(
                                color = Color(0xFF475569),
                                topLeft = Offset(offsetX + marginL, offsetY + marginT),
                                size = Size(usableW, usableH),
                                style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                            )

                            // Draw recoverable offcuts (Amber dotted)
                            spec.offcutRemnants.forEach { offcut ->
                                val ox = offsetX + offcut.xMm.toFloat() * scale
                                val oy = offsetY + offcut.yMm.toFloat() * scale
                                val ow = offcut.widthMm.toFloat() * scale
                                val oh = offcut.heightMm.toFloat() * scale

                                val offcutColor = if (offcut.isRecoverable) Color(0x33FFB300) else Color(0x11FF5252)
                                drawRect(
                                    color = offcutColor,
                                    topLeft = Offset(ox, oy),
                                    size = Size(ow, oh)
                                )
                            }

                            // Draw placed items
                            spec.placements.forEach { placement ->
                                val px = offsetX + placement.xMm.toFloat() * scale
                                val py = offsetY + placement.yMm.toFloat() * scale
                                val pw = placement.placedWidthMm.toFloat() * scale
                                val ph = placement.placedHeightMm.toFloat() * scale

                                val jobIdx = distinctJobs.indexOf(placement.jobId).coerceAtLeast(0)
                                val baseColor = JobColorPalette[jobIdx % JobColorPalette.size]

                                // Fill
                                drawRect(
                                    color = baseColor.copy(alpha = 0.35f),
                                    topLeft = Offset(px, py),
                                    size = Size(pw, ph)
                                )
                                // Stroke
                                drawRect(
                                    color = baseColor,
                                    topLeft = Offset(px, py),
                                    size = Size(pw, ph),
                                    style = Stroke(width = 2f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Job Allocations on Form
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "ALLOCATED JOBS BREAKDOWN",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    spec.jobSummaries.forEachIndexed { idx, job ->
                        val color = JobColorPalette[idx % JobColorPalette.size]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .background(CardBgElevated, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(color, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(job.productName, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("${job.jobId} • ${job.assignedCopiesOnSheet} UP on form", color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${job.producedQuantity} pcs (req ${job.requiredQuantity})", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("+${job.overageQuantity} overage (${job.relativeYieldPercentage}% yield)", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CandidatePoolTab(state: NestingUiState, viewModel: NestingViewModel) {
    var newJobId by remember { mutableStateOf("JOB-NEW-01") }
    var newProductName by remember { mutableStateOf("Custom Label Pack") }
    var newWidthMm by remember { mutableStateOf("105.0000") }
    var newHeightMm by remember { mutableStateOf("148.0000") }
    var newQuantity by remember { mutableStateOf("2500") }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Form to add candidate
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("ADD CANDIDATE TO NESTING POOL", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newJobId,
                            onValueChange = { newJobId = it },
                            label = { Text("Job ID", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newProductName,
                            onValueChange = { newProductName = it },
                            label = { Text("Product Name", fontSize = 11.sp) },
                            modifier = Modifier.weight(2f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newWidthMm,
                            onValueChange = { newWidthMm = it },
                            label = { Text("Width (mm)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newHeightMm,
                            onValueChange = { newHeightMm = it },
                            label = { Text("Height (mm)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newQuantity,
                            onValueChange = { newQuantity = it },
                            label = { Text("Qty", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val item = NestingCandidateItemDto(
                                jobId = newJobId,
                                orderId = "ORD-${newJobId.takeLast(4)}",
                                orderItemId = "ITEM-01",
                                productName = newProductName,
                                finishedWidthMm = newWidthMm.toBigDecimalOrNull() ?: BigDecimal("105.0000"),
                                finishedHeightMm = newHeightMm.toBigDecimalOrNull() ?: BigDecimal("148.0000"),
                                requiredQuantity = newQuantity.toLongOrNull() ?: 1000L,
                                paperStockType = "ART_CARD",
                                gsm = BigDecimal("300.0000")
                            )
                            viewModel.addCandidate(item)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Candidate & Recalculate Nesting", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // List of candidate items
        items(state.candidatePool) { candidate ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(10.dp))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(candidate.productName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            "${candidate.jobId} • ${candidate.finishedWidthMm} x ${candidate.finishedHeightMm} mm • Qty: ${candidate.requiredQuantity}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = { viewModel.removeCandidate(candidate.jobId) }) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove", tint = Color(0xFFFF5252))
                    }
                }
            }
        }
    }
}

@Composable
private fun OffcutsAndWasteTab(state: NestingUiState) {
    val spec = state.currentSpecification

    if (spec == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No nesting specification calculated.", color = TextSecondary)
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Metrics Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("SUBSTRATE & OFFCUT SUMMARY", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    MetricRow("Total Sheet Area", "${spec.totalSheetAreaMm2} mm²")
                    MetricRow("Usable Canvas Area", "${spec.usableAreaMm2} mm²")
                    MetricRow("Occupied Items Area", "${spec.occupiedAreaMm2} mm²")
                    MetricRow("Recoverable Offcut Area", "${spec.recoverableOffcutAreaMm2} mm² (${spec.offcutRecoveryPercentage}%)", NeonAmber)
                    MetricRow("Total Waste Area", "${spec.wasteAreaMm2} mm²", Color(0xFFFF5252))
                    MetricRow("Cryptographic Hash", spec.integrityHash.take(16) + "...", NeonCyan)
                }
            }
        }

        // List of offcut remnants
        item {
            Text("IDENTIFIED REMNANT PIECES (${spec.offcutRemnants.size})", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        items(spec.offcutRemnants) { offcut ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBgElevated),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, if (offcut.isRecoverable) NeonAmber else BorderColor, RoundedCornerShape(8.dp))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(offcut.offcutId, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${offcut.widthMm} x ${offcut.heightMm} mm at (${offcut.xMm}, ${offcut.yMm})", color = TextSecondary, fontSize = 11.sp)
                    }
                    Text(
                        text = if (offcut.isRecoverable) "RECOVERABLE (${offcut.areaMm2} mm²)" else "TRIM WASTE",
                        color = if (offcut.isRecoverable) NeonAmber else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun NestingHistoryTab(state: NestingUiState, viewModel: NestingViewModel) {
    if (state.specificationsList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No nesting history recorded.", color = TextSecondary)
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(state.specificationsList) { spec ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (spec.nestingId == state.currentSpecification?.nestingId) NeonCyan else BorderColor, RoundedCornerShape(10.dp))
                    .clickable { viewModel.selectSpecification(spec) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(spec.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${spec.nestingId} • ${spec.totalItemsPlaced} items • ${spec.commonRequiredSheets} sheets", color = TextSecondary, fontSize = 11.sp)
                    }
                    Text(
                        text = "${spec.usableYieldPercentage}% Yield",
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
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
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.border(1.dp, BorderColor, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = accentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Text(value, color = valueColor, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}
