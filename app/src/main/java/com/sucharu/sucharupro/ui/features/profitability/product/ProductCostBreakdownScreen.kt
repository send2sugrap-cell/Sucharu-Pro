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
import com.sucharu.sucharupro.data.api.model.profitability.ProductCostBreakdownItemDto
import com.sucharu.sucharupro.data.api.model.profitability.ProductProfitabilitySnapshotDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCostBreakdownScreen(
    snapshot: ProductProfitabilitySnapshotDto?,
    onBackClick: () -> Unit = {}
) {
    val darkNavyBg = Color(0xFF0B132B)
    val cardBg = Color(0xFF1C2541)
    val accentCyan = Color(0xFF9ECAFF)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Product Cost Breakdown", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Text("${snapshot?.productName ?: "Product"} • 12 Canonical Cost Components", fontSize = 12.sp, color = Color.LightGray)
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
        val components = snapshot?.costBreakdown ?: emptyList()
        if (components.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No Cost Breakdown Components", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("Total Actual Cost: ৳${snapshot?.totalActualCost ?: "0.0000"}", color = accentCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                items(components) { comp ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(comp.displayName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("৳${comp.amount}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Allocation: ${comp.allocationBasis} • Sources: ${comp.sourceCount}", color = Color.Gray, fontSize = 11.sp)
                                Text("${comp.percentageOfTotalCost}% of Total", color = accentCyan, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (comp.percentageOfTotalCost.toFloat() / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = accentCyan,
                                trackColor = Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}
