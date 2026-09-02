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
import com.sucharu.sucharupro.data.api.model.profitability.VendorCostAttributionDto

/**
 * 6. Vendor Customer-Wise Cost Attribution Screen
 */
@Composable
fun VendorProfitabilityCustomerImpactScreen(
    vendorId: String,
    attributions: List<VendorCostAttributionDto>,
    onBack: () -> Unit = {}
) {
    val bgNavy = Color(0xFF0F172A)
    val cardNavy = Color(0xFF1E293B)
    val accentRose = Color(0xFFF43F5E)

    val customerAttributions = attributions.filter { it.customerId != null }

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
            Column {
                Text(text = "Customer-Wise Cost Attribution", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = "Vendor ID: $vendorId", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
            Button(onClick = onBack) { Text("Back") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (customerAttributions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No customer-specific vendor cost attribution recorded", color = Color(0xFF94A3B8), fontSize = 14.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(customerAttributions) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = cardNavy)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Customer: ${item.customerId ?: "N/A"}",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "BDT ${item.attributedAmount}",
                                    color = accentRose,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Job ID: ${item.jobId ?: "N/A"} | Method: ${item.attributionMethod}",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
