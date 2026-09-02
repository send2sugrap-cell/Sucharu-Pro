package com.sucharu.sucharupro.ui.features.imposition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.imposition.ImpositionSpecificationResponseDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImpositionCommandCenterScreen(
    viewModel: ImpositionViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    val darkNavyBg = Color(0xFF0F172A)
    val cardNavyBg = Color(0xFF1E293B)
    val accentCyan = Color(0xFF06B6D4)
    val accentEmerald = Color(0xFF10B981)
    val accentAmber = Color(0xFFF59E0B)
    val borderSlate = Color(0xFF334155)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Dynamic Imposition & Sheet Layout",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Module 18 Step 01 — Single-Job Orthogonal Optimizer",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = accentCyan
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadSpecifications() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = darkNavyBg
                )
            )
        },
        containerColor = darkNavyBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Status Alerts
            if (state.errorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF7F1D1D)
                ) {
                    Text(
                        text = state.errorMessage ?: "",
                        color = Color(0xFFFCA5A5),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (state.successMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF064E3B)
                ) {
                    Text(
                        text = state.successMessage ?: "",
                        color = Color(0xFF6EE7B7),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Tab Rows
            TabRow(
                selectedTabIndex = state.selectedTab,
                containerColor = cardNavyBg,
                contentColor = accentCyan,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.onTabSelected(0) },
                    text = { Text("Layout Visualizer", color = if (state.selectedTab == 0) accentCyan else Color.LightGray) }
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.onTabSelected(1) },
                    text = { Text("Job & Sheet Input", color = if (state.selectedTab == 1) accentCyan else Color.LightGray) }
                )
                Tab(
                    selected = state.selectedTab == 2,
                    onClick = { viewModel.onTabSelected(2) },
                    text = { Text("Specifications (${state.specificationsList.size})", color = if (state.selectedTab == 2) accentCyan else Color.LightGray) }
                )
            }

            when (state.selectedTab) {
                0 -> ImpositionVisualizerTab(
                    spec = state.currentSpecification,
                    onCalculate = { viewModel.calculateAndSaveImposition() },
                    isCalculating = state.isCalculating
                )
                1 -> ImpositionInputTab(
                    state = state,
                    viewModel = viewModel,
                    onCalculate = {
                        viewModel.calculateAndSaveImposition()
                        viewModel.onTabSelected(0)
                    }
                )
                2 -> ImpositionHistoryTab(
                    specs = state.specificationsList,
                    onSelect = { viewModel.selectSpecification(it) }
                )
            }
        }
    }
}

@Composable
fun ImpositionVisualizerTab(
    spec: ImpositionSpecificationResponseDto?,
    onCalculate: () -> Unit,
    isCalculating: Boolean
) {
    val cardNavyBg = Color(0xFF1E293B)
    val accentCyan = Color(0xFF06B6D4)
    val accentEmerald = Color(0xFF10B981)
    val borderSlate = Color(0xFF334155)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        if (spec == null) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = cardNavyBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderSlate)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = null,
                            tint = accentCyan,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Imposition Layout Active",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Configure finished item and parent sheet dimensions to run the dynamic optimizer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onCalculate,
                            enabled = !isCalculating,
                            colors = ButtonDefaults.buttonColors(containerColor = accentCyan)
                        ) {
                            Text(if (isCalculating) "Optimizing..." else "Run Single-Job Optimizer", color = Color.Black)
                        }
                    }
                }
            }
        } else {
            // KPI Summary Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KpiCard(
                        title = "Copies Per Sheet",
                        value = "${spec.copiesPerSheet} UP",
                        subtext = "${spec.columns} cols × ${spec.rows} rows",
                        color = accentCyan,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Sheet Yield",
                        value = "${spec.yieldPercentage}%",
                        subtext = "Waste: ${spec.wasteAreaMm2.toPlainString()} mm²",
                        color = accentEmerald,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KpiCard(
                        title = "Required Sheets",
                        value = "${spec.requiredSheets}",
                        subtext = "For ${spec.requiredQuantity} items",
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Total Capacity",
                        value = "${spec.totalProducedCapacity}",
                        subtext = "Overage: +${spec.overageQuantity}",
                        color = Color(0xFFA855F7),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Visual Sheet Layout Box
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = cardNavyBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderSlate)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sheet Arrangement Blueprint",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF0F766E)
                            ) {
                                Text(
                                    text = spec.selectedOrientation,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Scaled Mock Visual Grid
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                .border(1.dp, borderSlate, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                for (r in 0 until minOf(spec.rows, 5)) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        for (c in 0 until minOf(spec.columns, 6)) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .background(Color(0xFF0369A1), RoundedCornerShape(2.dp))
                                                    .border(0.5.dp, accentCyan, RoundedCornerShape(2.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${r * spec.columns + c + 1}",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Dimension Details
                        Text(
                            text = "Parent Sheet: ${spec.parentSheetWidthMm}mm × ${spec.parentSheetHeightMm}mm (Usable: ${spec.usableWidthMm}mm × ${spec.usableHeightMm}mm)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                        Text(
                            text = "Item Cut: ${spec.finishedItemWidthMm}mm × ${spec.finishedItemHeightMm}mm (Bleed: ${spec.bleedMm}mm, Gutters: ${spec.horizontalGutterMm}mm / ${spec.verticalGutterMm}mm)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                        Text(
                            text = "Integrity Hash: ${spec.integrityHash.take(16)}...",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImpositionInputTab(
    state: ImpositionUiState,
    viewModel: ImpositionViewModel,
    onCalculate: () -> Unit
) {
    val cardNavyBg = Color(0xFF1E293B)
    val accentCyan = Color(0xFF06B6D4)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        item {
            Text(
                text = "Job & Finished Product",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = accentCyan
            )
        }

        item {
            OutlinedTextField(
                value = state.jobId,
                onValueChange = { viewModel.onJobIdChanged(it) },
                label = { Text("Job ID") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.orderId,
                    onValueChange = { viewModel.onOrderIdChanged(it) },
                    label = { Text("Order ID") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.orderItemId,
                    onValueChange = { viewModel.onOrderItemIdChanged(it) },
                    label = { Text("Order Item ID") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            OutlinedTextField(
                value = state.productName,
                onValueChange = { viewModel.onProductNameChanged(it) },
                label = { Text("Product Description") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Text(
                text = "Physical Dimensions (mm)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = accentCyan
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.itemWidthMm,
                    onValueChange = { viewModel.onItemWidthChanged(it) },
                    label = { Text("Item Width (mm)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.itemHeightMm,
                    onValueChange = { viewModel.onItemHeightChanged(it) },
                    label = { Text("Item Height (mm)") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.sheetWidthMm,
                    onValueChange = { viewModel.onSheetWidthChanged(it) },
                    label = { Text("Sheet Width (mm)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.sheetHeightMm,
                    onValueChange = { viewModel.onSheetHeightChanged(it) },
                    label = { Text("Sheet Height (mm)") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            OutlinedTextField(
                value = state.requiredQuantity,
                onValueChange = { viewModel.onQuantityChanged(it) },
                label = { Text("Required Quantity (pieces)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Button(
                onClick = onCalculate,
                enabled = !state.isCalculating,
                colors = ButtonDefaults.buttonColors(containerColor = accentCyan),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    text = if (state.isCalculating) "Calculating..." else "Optimize & Save Specification",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ImpositionHistoryTab(
    specs: List<ImpositionSpecificationResponseDto>,
    onSelect: (ImpositionSpecificationResponseDto) -> Unit
) {
    val cardNavyBg = Color(0xFF1E293B)
    val accentCyan = Color(0xFF06B6D4)
    val borderSlate = Color(0xFF334155)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        if (specs.isEmpty()) {
            item {
                Text(
                    text = "No saved imposition specifications found.",
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(specs) { s ->
                Surface(
                    onClick = { onSelect(s) },
                    shape = RoundedCornerShape(8.dp),
                    color = cardNavyBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderSlate),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = s.productName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "${s.impositionId} | ${s.selectedOrientation} | ${s.copiesPerSheet} UP (${s.columns}x${s.rows})",
                                style = MaterialTheme.typography.bodySmall,
                                color = accentCyan
                            )
                            Text(
                                text = "Req: ${s.requiredSheets} sheets | Yield: ${s.yieldPercentage}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    subtext: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val cardNavyBg = Color(0xFF1E293B)
    val borderSlate = Color(0xFF334155)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = cardNavyBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderSlate)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}
