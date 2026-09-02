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
import com.sucharu.sucharupro.data.api.model.profitability.VendorRankingItemDto

/**
 * 12. Vendor Performance & Spend Ranking Screen
 */
@Composable
fun VendorProfitabilityRankingScreen(
    rankedVendors: List<VendorRankingItemDto>,
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
            Text(text = "Vendor Rankings", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Button(onClick = onBack) { Text("Back") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (rankedVendors.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No vendor ranking records found", color = Color(0xFF94A3B8), fontSize = 14.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(rankedVendors) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = cardNavy)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF334155),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = "#${item.rank}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(text = item.vendorName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text(text = "Efficiency: ${item.efficiencyScore}/100 | Risk: ${item.riskClassification}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "${item.metricValue}", color = accentCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(text = item.metricLabel, color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
