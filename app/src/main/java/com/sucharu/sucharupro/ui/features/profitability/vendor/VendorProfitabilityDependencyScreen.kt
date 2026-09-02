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
import com.sucharu.sucharupro.data.api.model.profitability.VendorConcentrationAnalysisDto

/**
 * 10. Vendor Spend Dependency & Concentration Screen
 */
@Composable
fun VendorProfitabilityDependencyScreen(
    concentration: VendorConcentrationAnalysisDto?,
    onBack: () -> Unit = {}
) {
    val bgNavy = Color(0xFF0F172A)
    val cardNavy = Color(0xFF1E293B)
    val accentCyan = Color(0xFF06B6D4)
    val accentAmber = Color(0xFFF59E0B)

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
                Text(text = "Vendor Spend Dependency", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = "Concentration Risk & Top Supplier Outsource Share", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
            Button(onClick = onBack) { Text("Back") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (concentration != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardNavy)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Overall Spend Concentration Tier", color = accentCyan, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = concentration.concentrationRisk.replace("_", " "),
                        color = accentAmber,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Total System Outsource Spend: BDT ${concentration.totalVendorSpend}", color = Color.White, fontSize = 14.sp)
                    Text(text = "Total Active Vendors: ${concentration.totalVendorCount}", color = Color(0xFF94A3B8), fontSize = 14.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Top 1 Vendor Spend: BDT ${concentration.top1Spend} (${concentration.top1SharePercentage}%)", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                    Text(text = "Top 5 Vendors Spend: BDT ${concentration.top5Spend} (${concentration.top5SharePercentage}%)", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                    Text(text = "Top 10 Vendors Spend: BDT ${concentration.top10Spend} (${concentration.top10SharePercentage}%)", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                }
            }
        }
    }
}
