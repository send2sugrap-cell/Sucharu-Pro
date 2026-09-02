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
import com.sucharu.sucharupro.data.api.model.profitability.CustomerProfitabilityReconciliationEventDto

/**
 * 10. Customer Profitability Reconciliation Screen
 */
@Composable
fun CustomerProfitabilityReconciliationScreen(
    event: CustomerProfitabilityReconciliationEventDto?,
    onReconcile: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val bgNavy = Color(0xFF0F172A)
    val cardNavy = Color(0xFF1E293B)
    val accentCyan = Color(0xFF06B6D4)
    val accentEmerald = Color(0xFF10B981)
    val accentRose = Color(0xFFF43F5E)

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
                    text = "Customer Profitability Reconciliation",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Non-mutating source verification",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (event == null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardNavy),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No reconciliation event executed yet", color = Color(0xFF94A3B8), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onReconcile, colors = ButtonDefaults.buttonColors(containerColor = accentCyan)) {
                        Text("Run Customer Reconciliation", color = Color.White)
                    }
                }
            }
        } else {
            // Status Card
            Card(
                colors = CardDefaults.cardColors(containerColor = cardNavy),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Reconciliation Status", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    val statusColor = if (event.isReconciled) accentEmerald else accentRose
                    Surface(
                        color = statusColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (event.isReconciled) "100% RECONCILED" else "DISCREPANCY DETECTED",
                            color = statusColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Checklist Card
            Card(
                colors = CardDefaults.cardColors(containerColor = cardNavy),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Mathematical Identities Checklist", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(10.dp))
                    ChecklistItem("Revenue Sum Identity", event.revenueReconciled, "Expected: ৳${event.expectedRevenue} | Actual: ৳${event.actualRevenue}")
                    ChecklistItem("Cost Component Sum Identity", event.costReconciled, "Expected: ৳${event.expectedCost} | Actual: ৳${event.actualCost}")
                    ChecklistItem("Gross Profit Formula Identity", event.profitReconciled, "Expected: ৳${event.expectedGrossProfit} | Actual: ৳${event.actualGrossProfit}")
                    ChecklistItem("Contribution Margin Identity", event.contributionReconciled, "Attributable variable cost verification")
                }
            }

            // Discrepancies if any
            if (event.discrepancies.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardNavy),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Discrepancy Details", color = accentRose, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        event.discrepancies.forEach { disc ->
                            Text("• $disc", color = Color(0xFFFCA5A5), fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistItem(label: String, isPassed: Boolean, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isPassed) "✓" else "✗",
            color = if (isPassed) Color(0xFF10B981) else Color(0xFFF43F5E),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(detail, color = Color(0xFF94A3B8), fontSize = 11.sp)
        }
    }
}
