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
import com.sucharu.sucharupro.data.api.model.profitability.ProductCostAttributionDto
import com.sucharu.sucharupro.data.api.model.profitability.ProductRevenueAttributionDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductProfitabilityProvenanceScreen(
    revenueAttributions: List<ProductRevenueAttributionDto>,
    costAttributions: List<ProductCostAttributionDto>,
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
                        Text("Provenance & Source Trace", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Text("Canonical SHA-256 Fingerprint Traceability", fontSize = 12.sp, color = Color.LightGray)
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
            // Revenue Sources
            item {
                Text("Revenue Sources (${revenueAttributions.size})", color = accentCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            if (revenueAttributions.isEmpty()) {
                item { Text("No Revenue Attributions", color = Color.Gray, fontSize = 12.sp) }
            } else {
                items(revenueAttributions) { rev ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${rev.sourceModule} • ${rev.sourceEntityType}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text("৳${rev.recognizedRevenue}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Text("Source Entity: ${rev.sourceEntityId} (Qty: ${rev.quantity})", color = Color.LightGray, fontSize = 11.sp)
                            Text("Fingerprint: ${rev.provenanceFingerprint.take(24)}...", color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                }
            }

            // Cost Sources
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Cost Sources (${costAttributions.size})", color = accentCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            if (costAttributions.isEmpty()) {
                item { Text("No Cost Attributions", color = Color.Gray, fontSize = 12.sp) }
            } else {
                items(costAttributions) { cost ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${cost.componentType} (${cost.directness})", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text("৳${cost.attributedAmount}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Text("Source: ${cost.sourceModule} / ${cost.sourceEntityId} • Basis: ${cost.allocationBasis}", color = Color.LightGray, fontSize = 11.sp)
                            Text("Fingerprint: ${cost.provenanceFingerprint.take(24)}...", color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
