package com.sucharu.sucharupro.ui.features.profitability

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.profitability.*
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobActualCostAnalysisScreen(
    snapshot: JobCostSnapshotDto?,
    onRecalculateClick: () -> Unit = {},
    onReconcileClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val darkNavyBg = Color(0xFF0B132B)
    val cardBg = Color(0xFF1C2541)
    val accentCyan = Color(0xFF9ECAFF)
    val successGreen = Color(0xFF4EBA6F)
    val warningOrange = Color(0xFFFFB74D)
    val errorRed = Color(0xFFFF6B6B)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Job Actual Cost Analysis",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Job: ${snapshot?.jobNumber ?: snapshot?.jobId ?: "N/A"} • Module 16 Step 02",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onReconcileClick) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Reconcile", tint = accentCyan)
                    }
                    IconButton(onClick = onRecalculateClick) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recalculate", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = darkNavyBg)
            )
        },
        containerColor = darkNavyBg
    ) { paddingValues ->
        if (snapshot == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No Job Cost Snapshot Loaded", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Status Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Job #${snapshot.jobNumber ?: snapshot.jobId}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                                val badgeColor = when (snapshot.readinessStatus) {
                                    "COMPLETE" -> successGreen
                                    "PARTIAL", "UNALLOCATED" -> warningOrange
                                    else -> errorRed
                                }
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(snapshot.readinessStatus, color = badgeColor, fontSize = 11.sp) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = badgeColor.copy(alpha = 0.15f))
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Customer: ${snapshot.customerId ?: "N/A"} | Product: ${snapshot.productId ?: "N/A"} | Qty: ${snapshot.jobQuantity}",
                                color = Color.LightGray,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Engine: ${snapshot.calculationVersion} | Hash: ${snapshot.integrityHash.take(12)}...",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Financial KPI Grid
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiCard(
                            title = "Total Actual Cost",
                            value = "৳ ${snapshot.totalActualCost}",
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                            containerColor = cardBg
                        )
                        KpiCard(
                            title = "Estimated Baseline",
                            value = snapshot.estimatedCost?.let { "৳ $it" } ?: "N/A",
                            color = accentCyan,
                            modifier = Modifier.weight(1f),
                            containerColor = cardBg
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiCard(
                            title = "Direct Cost",
                            value = "৳ ${snapshot.totalDirectCost}",
                            color = Color.LightGray,
                            modifier = Modifier.weight(1f),
                            containerColor = cardBg
                        )
                        KpiCard(
                            title = "Indirect Overhead",
                            value = "৳ ${snapshot.totalIndirectCost}",
                            color = Color.LightGray,
                            modifier = Modifier.weight(1f),
                            containerColor = cardBg
                        )
                    }
                }

                // Variance Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Cost Variance vs. Estimation",
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val varianceColor = when (snapshot.varianceClassification) {
                                    "UNDER_BUDGET" -> successGreen
                                    "ON_TARGET" -> accentCyan
                                    "OVER_BUDGET" -> errorRed
                                    else -> Color.Gray
                                }
                                Text(
                                    text = snapshot.costVariance?.let { "৳ $it (${snapshot.costVariancePercentage}%)" } ?: "Baseline Unavailable",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = varianceColor
                                )
                                Text(
                                    text = snapshot.varianceClassification,
                                    fontSize = 12.sp,
                                    color = varianceColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // 12-Component Cost Breakdown Title
                item {
                    Text(
                        text = "Cost Component Breakdown (${snapshot.costComponents.size})",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }

                // Cost Components List
                items(snapshot.costComponents) { comp ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = comp.componentType.replace("_", " "),
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${comp.directness} • ${comp.attributionBasis} • ${comp.percentageOfTotalCost}%",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = "৳ ${comp.attributedAmount}",
                                fontWeight = FontWeight.Bold,
                                color = if (comp.directness == "DIRECT") Color.White else accentCyan,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                // Provenance Count & Warnings
                if (snapshot.warnings.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = errorRed.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Source Warnings & Deduplication Alerts",
                                    fontWeight = FontWeight.Bold,
                                    color = errorRed,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                snapshot.warnings.forEach { warning ->
                                    Text(
                                        text = "• $warning",
                                        color = Color.White,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    color: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, color = Color.Gray, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontWeight = FontWeight.Bold, color = color, fontSize = 15.sp)
        }
    }
}
