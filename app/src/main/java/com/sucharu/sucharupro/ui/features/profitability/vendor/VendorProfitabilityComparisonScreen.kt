package com.sucharu.sucharupro.ui.features.profitability.vendor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.profitability.VendorComparisonItemDto

/**
 * 11. Multi-Vendor Comparative Analysis Screen
 */
@Composable
fun VendorProfitabilityComparisonScreen(
    comparisons: List<VendorComparisonItemDto>,
    onBack: () -> Unit = {}
) {
    val bgNavy = Color(0xFF0F172A)
    val cardNavy = Color(0xFF1E293B)
    val accentCyan = Color(0xFF06B6D4)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgNavy)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Multi-Vendor Comparison", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Button(onClick = onBack) { Text("Back") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (comparisons.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No vendors selected for comparison", color = Color(0xFF94A3B8), fontSize = 14.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(comparisons) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = cardNavy)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(text = item.vendorName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Category: ${item.serviceCategory ?: "GENERAL"}", color = Color(0xFF94A3B8), fontSize = 12.sp)

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Total Spend: BDT ${item.totalVendorCost}", color = accentCyan, fontSize = 13.sp)
                                Text(text = "Efficiency: ${item.efficiencyScore}/100", color = Color.White, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Risk: ${item.riskClassification}", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                                Text(text = "Exposure: BDT ${item.outstandingExposure}", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
