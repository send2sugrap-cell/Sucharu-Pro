package com.sucharu.sucharupro.ui.features.profitability.product

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.profitability.ProductProfitabilitySnapshotDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductUnitEconomicsScreen(
    snapshot: ProductProfitabilitySnapshotDto?,
    onBackClick: () -> Unit = {}
) {
    val darkNavyBg = Color(0xFF0B132B)
    val cardBg = Color(0xFF1C2541)
    val accentCyan = Color(0xFF9ECAFF)
    val successGreen = Color(0xFF4EBA6F)
    val errorRed = Color(0xFFFF6B6B)

    val ue = snapshot?.unitEconomics

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Product Unit Economics", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Text("${snapshot?.productName ?: "Product"} • Qty: ${ue?.quantity ?: 0} Units", fontSize = 12.sp, color = Color.LightGray)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Core Unit Metrics Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Unit Level Profitability", color = accentCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Unit Revenue (ASP)", color = Color.Gray, fontSize = 12.sp)
                                Text("৳${ue?.unitRevenue ?: "N/A"}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column {
                                Text("Unit Actual Cost", color = Color.Gray, fontSize = 12.sp)
                                Text("৳${ue?.unitActualCost ?: "N/A"}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column {
                                Text("Unit Gross Profit", color = Color.Gray, fontSize = 12.sp)
                                val unitGpColor = if ((ue?.unitGrossProfit?.toDouble() ?: 0.0) >= 0) successGreen else errorRed
                                Text("৳${ue?.unitGrossProfit ?: "N/A"}", color = unitGpColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

            // Component-Wise Unit Cost Breakdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Unit Cost Breakdown by Component", color = accentCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        UnitCostRow("Material Cost / Unit", ue?.unitMaterialCost)
                        UnitCostRow("Labour Cost / Unit", ue?.unitLabourCost)
                        UnitCostRow("Machine Cost / Unit", ue?.unitMachineCost)
                        UnitCostRow("Vendor Outsource / Unit", ue?.unitVendorCost)
                        UnitCostRow("Rework Cost / Unit", ue?.unitReworkCost)
                        UnitCostRow("Wastage Cost / Unit", ue?.unitWastageCost)
                        UnitCostRow("Finishing Cost / Unit", ue?.unitFinishingCost)
                        UnitCostRow("Packaging Cost / Unit", ue?.unitPackagingCost)
                        UnitCostRow("Transport Cost / Unit", ue?.unitTransportCost)
                        UnitCostRow("Other Direct / Unit", ue?.unitOtherDirectCost)
                        UnitCostRow("Allocated Indirect / Unit", ue?.unitAllocatedIndirectCost)
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitCostRow(label: String, value: java.math.BigDecimal?) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 13.sp)
        Text(if (value != null) "৳$value" else "৳0.0000", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}
