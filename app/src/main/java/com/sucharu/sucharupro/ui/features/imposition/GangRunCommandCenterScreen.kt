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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.imposition.GangRunItemAllocationDto
import com.sucharu.sucharupro.data.api.model.imposition.GangRunSpecificationResponseDto

// Palette
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
fun GangRunCommandCenterScreen(
    viewModel: GangRunViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "GANG-RUN OPTIMIZER ENGINE",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Multi-Job Batching & Compatibility Clustering (Module 18 Step 02)",
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
                    IconButton(onClick = { viewModel.optimizeGangRun() }) {
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
            // Tab Row
            TabRow(
                selectedTabIndex = state.selectedTab,
                containerColor = CardBg,
                contentColor = NeonCyan,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.onTabSelected(0) },
                    text = { Text("Visual Gang Form", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.onTabSelected(1) },
                    text = { Text("Candidate Pool (${state.candidatePool.size})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = state.selectedTab == 2,
                    onClick = { viewModel.onTabSelected(2) },
                    text = { Text("Batch History", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notifications
            state.errorMessage?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3B151E)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text(text = "❌ $it", color = Color(0xFFFF8A80), modifier = Modifier.padding(10.dp), fontSize = 12.sp)
                }
            }
            state.successMessage?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F3822)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text(text = "✓ $it", color = NeonGreen, modifier = Modifier.padding(10.dp), fontSize = 12.sp)
                }
            }

            // Tab Content
            when (state.selectedTab) {
                0 -> VisualGangFormTab(state)
                1 -> CandidatePoolTab(state, viewModel)
                2 -> BatchHistoryTab(state, viewModel)
            }
        }
    }
}

@Composable
private fun VisualGangFormTab(state: GangRunUiState) {
    val spec = state.currentSpecification
    if (spec == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active gang-run layout. Optimize candidate pool to generate.", color = TextSecondary)
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            // KPI Summary Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiCard(
                    title = "OVERALL YIELD",
                    value = "${spec.sheetYieldPercentage}%",
                    accent = NeonGreen,
                    icon = Icons.Filled.Percent,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "PRESS SHEETS",
                    value = "${spec.commonRequiredSheets}",
                    accent = NeonCyan,
                    icon = Icons.Filled.Layers,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "CO-LOCATED JOBS",
                    value = "${spec.allocations.size}",
                    accent = NeonAmber,
                    icon = Icons.Filled.ViewQuilt,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            // Sheet Canvas Representation
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PARENT SHEET FORM: ${spec.sheetWidthMm}mm x ${spec.sheetHeightMm}mm",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Substrate: ${spec.paperStockType} ${spec.gsm} GSM • ${spec.colorMode}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Gang-Run Slot Visualizer
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardBgElevated)
                            .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        spec.allocations.forEachIndexed { idx, alloc ->
                            val color = JobColorPalette[idx % JobColorPalette.size]
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(alloc.assignedSlots.toFloat().coerceAtLeast(1f))
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(color.copy(alpha = 0.2f))
                                    .border(1.dp, color, RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${alloc.jobId} — ${alloc.productName}",
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${alloc.assignedSlots} UP slots • ${alloc.slotItemWidthMm}x${alloc.slotItemHeightMm}mm (${alloc.orientation})",
                                            color = color,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Prod: ${alloc.producedQuantity} / Req: ${alloc.requiredQuantity}",
                                            color = TextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "+${alloc.overageQuantity} overage (${alloc.relativeYieldPercentage}%)",
                                            color = NeonAmber,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Integrity verification
                    Text(
                        text = "INTEGRITY HASH: ${spec.integrityHash}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        item {
            Text(
                text = "CO-LOCATED JOB BREAKDOWN",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(spec.allocations) { alloc ->
            JobAllocationCard(alloc)
        }
    }
}

@Composable
private fun JobAllocationCard(alloc: GangRunItemAllocationDto) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = alloc.productName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = "Job: ${alloc.jobId} • Order: ${alloc.orderId}", color = TextSecondary, fontSize = 11.sp)
                Text(text = "Assigned Slots: ${alloc.assignedSlots} UP • ${alloc.orientation}", color = NeonCyan, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${alloc.producedQuantity} Pcs", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = "Req: ${alloc.requiredQuantity}", color = TextSecondary, fontSize = 11.sp)
                Text(text = "+${alloc.overageQuantity} Overage", color = NeonAmber, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun CandidatePoolTab(state: GangRunUiState, viewModel: GangRunViewModel) {
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                text = "ACTIVE CANDIDATE JOBS FOR GANGING",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        items(state.candidatePool) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = item.productName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = "${item.jobId} • ${item.finishedWidthMm}x${item.finishedHeightMm}mm", color = TextSecondary, fontSize = 11.sp)
                        Text(text = "Substrate: ${item.paperStockType} ${item.gsm} GSM", color = NeonAmber, fontSize = 11.sp)
                        Text(text = "Req Qty: ${item.requiredQuantity} Pcs", color = NeonCyan, fontSize = 11.sp)
                    }
                    IconButton(onClick = { viewModel.removeCandidate(item.jobId) }) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove", tint = Color(0xFFFF5252))
                    }
                }
            }
        }
    }
}

@Composable
private fun BatchHistoryTab(state: GangRunUiState, viewModel: GangRunViewModel) {
    if (state.specificationsList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No historical gang-run specifications found.", color = TextSecondary)
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(state.specificationsList) { spec ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectSpecification(spec) }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = spec.batchName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = "${spec.sheetYieldPercentage}% Yield", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "${spec.gangRunId} • ${spec.allocations.size} Jobs co-located • ${spec.commonRequiredSheets} Sheets", color = TextSecondary, fontSize = 11.sp)
                    Text(text = "Substrate: ${spec.paperStockType} ${spec.gsm} GSM", color = NeonCyan, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = title, color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}
