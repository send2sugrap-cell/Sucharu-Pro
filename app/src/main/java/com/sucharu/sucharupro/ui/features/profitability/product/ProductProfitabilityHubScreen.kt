package com.sucharu.sucharupro.ui.features.profitability.product

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.sucharu.sucharupro.data.api.model.profitability.ProductProfitabilitySnapshotDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductProfitabilityHubScreen(
    snapshot: ProductProfitabilitySnapshotDto?,
    onViewDetailsClick: () -> Unit = {},
    onViewUnitEconomicsClick: () -> Unit = {},
    onViewBreakdownClick: () -> Unit = {},
    onViewTrendClick: () -> Unit = {},
    onViewProvenanceClick: () -> Unit = {},
    onViewReconciliationClick: () -> Unit = {},
    onViewListClick: () -> Unit = {},
    onRecalculateClick: () -> Unit = {},
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
                            text = "Product Profitability Hub",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Product: ${snapshot?.productName ?: snapshot?.productId ?: "All Products"} • Module 16 Step 03",
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
                    IconButton(onClick = onViewListClick) {
                        Icon(Icons.Default.List, contentDescription = "Product List", tint = accentCyan)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. High Level Summary Card
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
                                text = snapshot?.productName ?: "Product Profitability Summary",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            val classificationColor = when (snapshot?.profitabilityClassification) {
                                "HIGHLY_PROFITABLE", "PROFITABLE" -> successGreen
                                "LOW_MARGIN", "BREAK_EVEN" -> warningOrange
                                else -> errorRed
                            }
                            Surface(
                                color = classificationColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = snapshot?.profitabilityClassification ?: "UNAVAILABLE",
                                    color = classificationColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Revenue", color = Color.Gray, fontSize = 12.sp)
                                Text("৳${snapshot?.recognizedRevenue ?: "0.0000"}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            }
                            Column {
                                Text("Actual Cost", color = Color.Gray, fontSize = 12.sp)
                                Text("৳${snapshot?.totalActualCost ?: "0.0000"}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            }
                            Column {
                                Text("Gross Profit", color = Color.Gray, fontSize = 12.sp)
                                val gpColor = if ((snapshot?.grossProfit?.toDouble() ?: 0.0) >= 0) successGreen else errorRed
                                Text("৳${snapshot?.grossProfit ?: "0.0000"}", color = gpColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column {
                                Text("Gross Margin", color = Color.Gray, fontSize = 12.sp)
                                Text("${snapshot?.grossMarginPercentage ?: "N/A"}%", color = accentCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

            // 2. Unit Economics Quick Preview Card
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
                            Text("Unit Economics Quick View", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            TextButton(onClick = onViewUnitEconomicsClick) {
                                Text("Full Breakdown →", color = accentCyan, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Total Units", color = Color.Gray, fontSize = 11.sp)
                                Text("${snapshot?.totalQuantity ?: 0}", color = Color.White, fontWeight = FontWeight.Medium)
                            }
                            Column {
                                Text("Unit Revenue", color = Color.Gray, fontSize = 11.sp)
                                Text("৳${snapshot?.unitEconomics?.unitRevenue ?: "N/A"}", color = Color.White, fontWeight = FontWeight.Medium)
                            }
                            Column {
                                Text("Unit Cost", color = Color.Gray, fontSize = 11.sp)
                                Text("৳${snapshot?.unitEconomics?.unitActualCost ?: "N/A"}", color = Color.White, fontWeight = FontWeight.Medium)
                            }
                            Column {
                                Text("Unit GP", color = Color.Gray, fontSize = 11.sp)
                                Text("৳${snapshot?.unitEconomics?.unitGrossProfit ?: "N/A"}", color = successGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. Navigation Hub Grid (Details, Breakdown, Trend, Provenance, Reconciliation, List)
            item {
                Text("Analytics & Workspace Views", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onViewDetailsClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = cardBg),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = accentCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Details", color = Color.White, fontSize = 13.sp)
                        }
                        Button(
                            onClick = onViewBreakdownClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = cardBg),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PieChart, contentDescription = null, tint = accentCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cost Breakdown", color = Color.White, fontSize = 13.sp)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onViewTrendClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = cardBg),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = accentCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Trends & Compare", color = Color.White, fontSize = 13.sp)
                        }
                        Button(
                            onClick = onViewProvenanceClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = cardBg),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = accentCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Provenance", color = Color.White, fontSize = 13.sp)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onViewReconciliationClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = cardBg),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.FactCheck, contentDescription = null, tint = accentCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reconciliation", color = Color.White, fontSize = 13.sp)
                        }
                        Button(
                            onClick = onViewListClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = cardBg),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Inventory, contentDescription = null, tint = accentCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("All Products", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
