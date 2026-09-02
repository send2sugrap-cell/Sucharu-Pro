package com.sucharu.sucharupro.ui.features.profitability.customer

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
import com.sucharu.sucharupro.data.api.model.profitability.CustomerConcentrationAnalysisDto

/**
 * 11. Customer Concentration & Dependency Intelligence Screen
 */
@Composable
fun CustomerProfitabilityConcentrationScreen(
    analysis: CustomerConcentrationAnalysisDto?,
    onBack: () -> Unit = {}
) {
    val bgNavy = Color(0xFF0F172A)
    val cardNavy = Color(0xFF1E293B)
    val accentCyan = Color(0xFF06B6D4)
    val accentEmerald = Color(0xFF10B981)
    val accentRose = Color(0xFFF43F5E)
    val accentAmber = Color(0xFFF59E0B)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgNavy)
            .padding(16.dp)
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
                    text = "Customer Concentration Intelligence",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Revenue & Profit Dependency Analysis",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (analysis == null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardNavy),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
            ) {
                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No concentration analysis data available", color = Color(0xFF94A3B8), fontSize = 14.sp)
                }
            }
        } else {
            // Risk Badge
            Card(
                colors = CardDefaults.cardColors(containerColor = cardNavy),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Business Concentration Risk Indicator", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    val riskColor = when (analysis.concentrationRisk) {
                        "CONCENTRATION_LOW" -> accentEmerald
                        "CONCENTRATION_MODERATE" -> accentAmber
                        else -> accentRose
                    }
                    Surface(
                        color = riskColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = analysis.concentrationRisk.replace("_", " "),
                            color = riskColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Concentration Stats Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Top 1 Revenue", "${analysis.top1RevenueSharePercentage.toPlainString()}%", accentCyan, Modifier.weight(1f))
                StatCard("Top 5 Revenue", "${analysis.top5RevenueSharePercentage.toPlainString()}%", accentAmber, Modifier.weight(1f))
                StatCard("Top 10 Revenue", "${analysis.top10RevenueSharePercentage.toPlainString()}%", accentEmerald, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Top 1 Profit", "${analysis.top1ProfitSharePercentage.toPlainString()}%", accentCyan, Modifier.weight(1f))
                StatCard("Top 5 Profit", "${analysis.top5ProfitSharePercentage.toPlainString()}%", accentAmber, Modifier.weight(1f))
                StatCard("Top 10 Profit", "${analysis.top10ProfitSharePercentage.toPlainString()}%", accentEmerald, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("Top Contributing Customers", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(analysis.topCustomers) { cust ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardNavy),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#${cust.rank}", color = accentCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cust.customerName ?: "Customer ${cust.customerId}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Rev: ৳${cust.revenue.toPlainString()} • Profit: ৳${cust.grossProfit.toPlainString()}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = Color(0xFF94A3B8), fontSize = 11.sp)
            Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
