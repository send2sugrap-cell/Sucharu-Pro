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
import com.sucharu.sucharupro.data.api.model.profitability.VendorReconciliationEventDto

/**
 * 13. Vendor Non-Mutating Financial Reconciliation Screen
 */
@Composable
fun VendorProfitabilityReconciliationScreen(
    vendorId: String,
    event: VendorReconciliationEventDto?,
    onRunReconciliation: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val bgNavy = Color(0xFF0F172A)
    val cardNavy = Color(0xFF1E293B)
    val accentEmerald = Color(0xFF10B981)
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
                Text(text = "Vendor Financial Reconciliation", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = "Non-Mutating Verification Checklist for Vendor: $vendorId", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
            Button(onClick = onBack) { Text("Back") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRunReconciliation,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4))
        ) {
            Text("Execute Reconciliation Check", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (event != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardNavy)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (event.isBalanced) "✔ Reconciliation Balanced" else "⚠ Discrepancies Detected",
                        color = if (event.isBalanced) accentEmerald else accentRose,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "• Cost Variance Diff: BDT ${event.totalCostDifference}", color = Color.White, fontSize = 13.sp)
                    Text(text = "• Component Breakdown Diff: BDT ${event.componentDifference}", color = Color.White, fontSize = 13.sp)
                    Text(text = "• Provenance Attribution Diff: BDT ${event.provenanceDifference}", color = Color.White, fontSize = 13.sp)
                    Text(text = "• Paid vs Liability Invariant: ${if (event.paidVsLiabilityValid) "Passed" else "Failed"}", color = Color.White, fontSize = 13.sp)

                    if (event.errorDetails.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Discrepancy Details:", color = accentRose, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        event.errorDetails.forEach { err ->
                            Text(text = "• $err", color = Color(0xFFFDA4AF), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
