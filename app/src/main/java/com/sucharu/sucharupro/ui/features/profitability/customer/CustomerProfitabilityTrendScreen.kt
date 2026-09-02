package com.sucharu.sucharupro.ui.features.profitability.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 4. Customer Profitability Trend Screen
 */
@Composable
fun CustomerProfitabilityTrendScreen(
    customerId: String,
    trend: String,
    currentMargin: String? = null,
    previousMargin: String? = null,
    onBack: () -> Unit = {}
) {
    val bgNavy = Color(0xFF0F172A)
    val cardNavy = Color(0xFF1E293B)
    val accentCyan = Color(0xFF06B6D4)
    val accentEmerald = Color(0xFF10B981)
    val accentRose = Color(0xFFF43F5E)
    val accentAmber = Color(0xFFF59E0B)

    val trendColor = when (trend) {
        "STRONGLY_IMPROVING", "IMPROVING" -> accentEmerald
        "STABLE" -> accentCyan
        "DECLINING" -> accentAmber
        "STRONGLY_DECLINING" -> accentRose
        else -> Color(0xFF94A3B8)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgNavy)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
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
                    text = "Customer Profitability Trend",
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

        Spacer(modifier = Modifier.height(16.dp))

        // Trend Status Card
        Card(
            colors = CardDefaults.cardColors(containerColor = cardNavy),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Directional Trajectory", color = Color(0xFF94A3B8), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = trendColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = trend.replace("_", " "),
                        color = trendColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Margin Comparison
        Card(
            colors = CardDefaults.cardColors(containerColor = cardNavy),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Period-over-Period Margins", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Current Period Margin", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text(currentMargin ?: "32.5000%", color = accentEmerald, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Previous Period Margin", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text(previousMargin ?: "28.0000%", color = Color(0xFFCBD5E1), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
