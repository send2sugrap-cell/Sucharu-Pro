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
 * 9. Vendor Risk & Financial Exposure Assessment Screen
 */
@Composable
fun VendorProfitabilityRiskScreen(
    snapshot: VendorProfitabilitySnapshotDto?,
    onBack: () -> Unit = {}
) {
    val bgNavy = Color(0xFF0F172A)
    val cardNavy = Color(0xFF1E293B)
    val accentRose = Color(0xFFF43F5E)
    val accentAmber = Color(0xFFF59E0B)
    val accentEmerald = Color(0xFF10B981)

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
                Text(text = "Supplier Risk Assessment", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = "Rework, Disputes, QC Failures & Liability Exposure", color = Color(0xFF94A3B8), fontSize = 12.sp)
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
                    Text(text = "Risk Classification Tier", color = Color(0xFF06B6D4), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    val (riskBg, riskText) = when (snapshot.riskClassification) {
                        "LOW_RISK" -> Pair(Color(0xFF064E3B), accentEmerald)
                        "MODERATE_RISK" -> Pair(Color(0xFF78350F), accentAmber)
                        else -> Pair(Color(0xFF881337), accentRose)
                    }

                    Surface(shape = RoundedCornerShape(8.dp), color = riskBg) {
                        Text(
                            text = snapshot.riskClassification.replace("_", " "),
                            color = riskText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Operational & Financial Risk Indicators", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "• Rework Incidents: ${snapshot.reworkCount} (Cost: BDT ${snapshot.reworkCost})", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                    Text(text = "• Quality Failures: ${snapshot.qualityFailureCount}", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                    Text(text = "• Financial Disputes: ${snapshot.disputeCount}", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                    Text(text = "• Outstanding Exposure: BDT ${snapshot.outstandingExposure}", color = Color(0xFFCBD5E1), fontSize = 13.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    if (snapshot.riskReasons.isNotEmpty()) {
                        Text(text = "Identified Risk Reasons:", color = accentRose, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(snapshot.riskReasons) { reason ->
                                Text(text = "⚠ $reason", color = Color(0xFFFDA4AF), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
