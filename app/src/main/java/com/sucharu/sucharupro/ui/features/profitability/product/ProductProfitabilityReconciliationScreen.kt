package com.sucharu.sucharupro.ui.features.profitability.product

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.profitability.ProductProfitabilityReconciliationEventDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductProfitabilityReconciliationScreen(
    event: ProductProfitabilityReconciliationEventDto?,
    onBackClick: () -> Unit = {}
) {
    val darkNavyBg = Color(0xFF0B132B)
    val cardBg = Color(0xFF1C2541)
    val successGreen = Color(0xFF4EBA6F)
    val errorRed = Color(0xFFFF6B6B)
    val accentCyan = Color(0xFF9ECAFF)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Profitability Reconciliation", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Text("Non-Mutating Authority Verification", fontSize = 12.sp, color = Color.LightGray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = darkNavyBg)
            )
        },
        containerColor = darkNavyBg
    ) { paddingValues ->
        if (event == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No Reconciliation Event Loaded", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Status Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val icon = if (event.isReconciled) Icons.Default.CheckCircle else Icons.Default.Warning
                            val iconColor = if (event.isReconciled) successGreen else errorRed
                            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(32.dp))
                            Column {
                                Text(
                                    text = if (event.isReconciled) "100% Reconciled & Verified" else "Discrepancies Detected",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Checked by: ${event.checkedBy} • Diff: ৳${event.grossProfitDiscrepancy}",
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Verification Check Items
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Reconciliation Breakdown", color = accentCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            ReconciliationCheckRow("Revenue Attribution Check", event.revenueReconciled, "Expected: ৳${event.expectedRevenue}, Actual: ৳${event.actualRevenue}")
                            ReconciliationCheckRow("Cost Attribution Check", event.costReconciled, "Expected: ৳${event.expectedCost}, Actual: ৳${event.actualCost}")
                            ReconciliationCheckRow("Unit Economics Consistency", event.unitEconomicsReconciled, "Unit Cost * Qty ≈ Total Cost")
                        }
                    }
                }

                // Discrepancy List if any
                if (event.discrepancies.isNotEmpty()) {
                    item {
                        Text("Discrepancies (${event.discrepancies.size})", color = errorRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    items(event.discrepancies) { disc ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(disc, color = errorRed, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReconciliationCheckRow(title: String, isPassed: Boolean, detail: String) {
    val successGreen = Color(0xFF4EBA6F)
    val errorRed = Color(0xFFFF6B6B)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(detail, color = Color.Gray, fontSize = 11.sp)
        }
        Text(if (isPassed) "PASSED" else "FAILED", color = if (isPassed) successGreen else errorRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}
