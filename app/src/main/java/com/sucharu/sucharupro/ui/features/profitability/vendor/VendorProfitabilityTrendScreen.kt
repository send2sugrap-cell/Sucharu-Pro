package com.sucharu.sucharupro.ui.features.profitability.vendor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
 * 7. Vendor Profitability Trend Screen
 */
@Composable
fun VendorProfitabilityTrendScreen(
    snapshot: VendorProfitabilitySnapshotDto?,
    onBack: () -> Unit = {}
) {
    val bgNavy = Color(0xFF0F172A)
    val cardNavy = Color(0xFF1E293B)
    val accentCyan = Color(0xFF06B6D4)
    val accentEmerald = Color(0xFF10B981)
    val accentAmber = Color(0xFFF59E0B)
    val accentRose = Color(0xFFF43F5E)

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
                Text(text = "Vendor Cost Trajectory", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = "Period-over-Period Performance & Trends", color = Color(0xFF94A3B8), fontSize = 12.sp)
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
                    Text(text = "Directional Trend Status", color = accentCyan, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))

                    val (trendBg, trendText) = when (snapshot.trendDirection) {
                        "STRONGLY_IMPROVING", "IMPROVING" -> Pair(Color(0xFF064E3B), accentEmerald)
                        "STABLE" -> Pair(Color(0xFF1E3A8A), accentCyan)
                        "DECLINING", "STRONGLY_DECLINING" -> Pair(Color(0xFF881337), accentRose)
                        else -> Pair(Color(0xFF334155), Color.LightGray)
                    }

                    Surface(shape = RoundedCornerShape(8.dp), color = trendBg) {
                        Text(
                            text = snapshot.trendDirection.replace("_", " "),
                            color = trendText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Current Spend: ${snapshot.currency} ${snapshot.totalVendorCost}", color = Color.White, fontSize = 14.sp)
                    Text(text = "Baseline Estimate: ${snapshot.baselineCost?.let { "${snapshot.currency} $it" } ?: "N/A"}", color = Color(0xFF94A3B8), fontSize = 14.sp)
                    Text(
                        text = "Variance: ${snapshot.costVariance?.let { "${snapshot.currency} $it (${snapshot.costVariancePercentage}%)" } ?: "Baseline Unavailable"}",
                        color = if ((snapshot.costVariancePercentage ?: java.math.BigDecimal.ZERO) > java.math.BigDecimal.ZERO) accentRose else accentEmerald,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
