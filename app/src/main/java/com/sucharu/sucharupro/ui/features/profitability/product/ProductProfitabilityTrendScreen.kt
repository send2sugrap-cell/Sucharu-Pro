package com.sucharu.sucharupro.ui.features.profitability.product

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.sucharu.sucharupro.data.api.model.profitability.ProductProfitabilityComparisonItemDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductProfitabilityTrendScreen(
    comparisonItems: List<ProductProfitabilityComparisonItemDto>,
    onBackClick: () -> Unit = {}
) {
    val darkNavyBg = Color(0xFF0B132B)
    val cardBg = Color(0xFF1C2541)
    val accentCyan = Color(0xFF9ECAFF)
    val successGreen = Color(0xFF4EBA6F)
    val errorRed = Color(0xFFFF6B6B)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Product Trends & Comparison", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Text("Cross-Product & Period Profitability Analysis", fontSize = 12.sp, color = Color.LightGray)
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
        if (comparisonItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No Comparison Data Available", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(comparisonItems) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(item.productName ?: item.productId, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                val gpColor = if (item.grossProfit.toDouble() >= 0) successGreen else errorRed
                                Text("GP: ৳${item.grossProfit} (${item.grossMarginPercentage ?: 0}%)", color = gpColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Units: ${item.quantity} • Unit GP: ৳${item.unitGrossProfit ?: 0}", color = Color.LightGray, fontSize = 12.sp)
                                Text("Rev: ৳${item.recognizedRevenue}", color = Color.White, fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Vendor Outsource: ৳${item.vendorOutsourceCost}", color = Color.Gray, fontSize = 11.sp)
                                Text("Rework + Waste: ৳${item.reworkCost.add(item.wastageCost)}", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
