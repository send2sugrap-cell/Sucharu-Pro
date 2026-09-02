package com.sucharu.sucharupro.ui.features.profitability.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.profitability.CustomerCostAttributionDto
import com.sucharu.sucharupro.data.api.model.profitability.CustomerRevenueAttributionDto

/**
 * 12. Customer Profitability Provenance Screen
 */
@Composable
fun CustomerProfitabilityProvenanceScreen(
    customerId: String,
    revenueSources: List<CustomerRevenueAttributionDto>,
    costSources: List<CustomerCostAttributionDto>,
    onBack: () -> Unit = {}
) {
    val bgNavy = Color(0xFF0F172A)
    val cardNavy = Color(0xFF1E293B)
    val accentCyan = Color(0xFF06B6D4)
    val accentAmber = Color(0xFFF59E0B)

    var showRevenue by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgNavy)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = cardNavy),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("← Back", color = Color.White, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Customer Provenance & Hashes",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Customer: $customerId",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Toggle Tabs
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { showRevenue = true },
                colors = ButtonDefaults.buttonColors(containerColor = if (showRevenue) accentCyan else cardNavy),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Revenue Sources (${revenueSources.size})", color = if (showRevenue) Color.Black else Color.White, fontSize = 12.sp)
            }
            Button(
                onClick = { showRevenue = false },
                colors = ButtonDefaults.buttonColors(containerColor = if (!showRevenue) accentAmber else cardNavy),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Cost Sources (${costSources.size})", color = if (!showRevenue) Color.Black else Color.White, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (showRevenue) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(revenueSources) { rev ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardNavy),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${rev.sourceModule} • ${rev.sourceEntityType}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "৳ ${rev.recognizedRevenue.toPlainString()}",
                                    color = accentCyan,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Source ID: ${rev.sourceEntityId} • Invoice: ${rev.invoiceId ?: "N/A"}",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                            if (rev.provenanceFingerprint.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "SHA-256: ${rev.provenanceFingerprint.take(20)}...",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(costSources) { cost ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardNavy),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${cost.sourceModule} • ${cost.componentType}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "৳ ${cost.attributedAmount.toPlainString()}",
                                    color = accentAmber,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Source ID: ${cost.sourceEntityId} • Basis: ${cost.allocationBasis}",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                            if (cost.provenanceFingerprint.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "SHA-256: ${cost.provenanceFingerprint.take(20)}...",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
