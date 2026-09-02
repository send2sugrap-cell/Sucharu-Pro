package com.sucharu.sucharupro.ui.features.profitability.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.profitability.CustomerProfitabilityRankingItemDto
import java.math.BigDecimal

/**
 * 8. Customer Profitability Ranking Screen
 */
@Composable
fun CustomerProfitabilityRankingScreen(
    rankings: List<CustomerProfitabilityRankingItemDto>,
    onSelectCustomer: (String) -> Unit = {},
    onCriteriaChanged: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val bgNavy = Color(0xFF0F172A)
    val cardNavy = Color(0xFF1E293B)
    val accentCyan = Color(0xFF06B6D4)
    val accentEmerald = Color(0xFF10B981)
    val accentRose = Color(0xFFF43F5E)
    val accentAmber = Color(0xFFF59E0B)

    var selectedTab by remember { mutableStateOf("GROSS_PROFIT") }

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
                    text = "Customer Profitability Rankings",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${rankings.size} customers ranked",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Criteria Tabs
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val tabs = listOf("GROSS_PROFIT" to "Profit", "REVENUE" to "Revenue", "GROSS_MARGIN" to "Margin", "CONTRIBUTION" to "Contrib")
            tabs.forEach { (key, label) ->
                val isSel = selectedTab == key
                Surface(
                    color = if (isSel) accentCyan else cardNavy,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).clickable {
                        selectedTab = key
                        onCriteriaChanged(key)
                    }
                ) {
                    Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text(label, color = if (isSel) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rankings) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardNavy),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onSelectCustomer(item.customerId) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rank Badge
                        Surface(
                            color = when (item.rank) {
                                1 -> accentAmber
                                2 -> Color(0xFF94A3B8)
                                3 -> Color(0xFFB45309)
                                else -> Color(0xFF334155)
                            },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "#${item.rank}",
                                    color = if (item.rank <= 3) Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.customerName ?: "Customer ${item.customerId}",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Rev: ৳${item.revenue.toPlainString()} • Orders: ${item.orderCount}",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            val isProf = item.grossProfit >= BigDecimal.ZERO
                            Text(
                                text = "৳ ${item.grossProfit.toPlainString()}",
                                color = if (isProf) accentEmerald else accentRose,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = item.grossMarginPercentage?.let { "${it.toPlainString()}%" } ?: "N/A",
                                color = accentCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
