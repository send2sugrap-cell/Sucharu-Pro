package com.sucharu.sucharupro.ui.features.profitability.vendor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.profitability.VendorProfitabilitySnapshotDto

/**
 * 2. Vendor Profitability Details Screen
 */
@Composable
fun VendorProfitabilityDetailsScreen(
    snapshot: VendorProfitabilitySnapshotDto?,
    onBack: () -> Unit = {}
) {
    val bgNavy = Color(0xFF0F172A)
    val cardNavy = Color(0xFF1E293B)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgNavy)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Vendor Economics Details", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Button(onClick = onBack) { Text("Back") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (snapshot != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardNavy)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Core Snapshot Metadata", color = Color(0xFF06B6D4), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    DetailRow("Vendor Name", snapshot.vendorName)
                    DetailRow("Vendor ID", snapshot.vendorId)
                    DetailRow("Service Category", snapshot.serviceCategory ?: "N/A")
                    DetailRow("Status", snapshot.vendorStatus)
                    DetailRow("Period", snapshot.periodId ?: "All Time")
                    DetailRow("Currency", snapshot.currency)
                    DetailRow("Total Vendor Cost", "${snapshot.currency} ${snapshot.totalVendorCost}")
                    DetailRow("Direct Cost", "${snapshot.currency} ${snapshot.directVendorCost}")
                    DetailRow("Paid Cost", "${snapshot.currency} ${snapshot.paidVendorCost}")
                    DetailRow("Outstanding Exposure", "${snapshot.currency} ${snapshot.outstandingExposure}")
                    DetailRow("Rework Cost", "${snapshot.currency} ${snapshot.reworkCost}")
                    DetailRow("Efficiency Score", "${snapshot.efficiencyScore} / 100")
                    DetailRow("Risk Tier", snapshot.riskClassification)
                    DetailRow("Dependency Tier", snapshot.dependencyClassification)
                    DetailRow("Trend", snapshot.trendDirection)
                    DetailRow("Data Readiness", snapshot.dataReadiness)
                    DetailRow("Integrity Hash", snapshot.integrityHash.take(16) + "...")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 13.sp)
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
