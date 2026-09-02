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
import com.sucharu.sucharupro.data.api.model.profitability.VendorProfitabilitySnapshotDto

/**
 * 8. Vendor Explainable Efficiency Scoring Screen
 */
@Composable
fun VendorProfitabilityEfficiencyScreen(
    snapshot: VendorProfitabilitySnapshotDto?,
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
            Column {
                Text(text = "Vendor Efficiency Score", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = "Explainable Multi-factor Operational Index", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
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
                    Text(text = "Overall Efficiency Index", color = accentCyan, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "${snapshot.efficiencyScore} / 100.00", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Contributing Factors & Deductions", color = Color(0xFF94A3B8), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (snapshot.efficiencyFactors.isEmpty()) {
                        Text(text = "Optimal operations across cost, quality, and disputes.", color = Color.White, fontSize = 13.sp)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(snapshot.efficiencyFactors) { factor ->
                                Text(text = "• $factor", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
