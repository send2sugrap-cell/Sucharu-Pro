package com.sucharu.sucharupro.ui.features.profitability.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import com.sucharu.sucharupro.data.api.model.profitability.CustomerProfitabilityComparisonItemDto
import java.math.BigDecimal

/**
 * 9. Customer Profitability Comparison Screen
 */
@Composable
fun CustomerProfitabilityComparisonScreen(
    comparisonItems: List<CustomerProfitabilityComparisonItemDto>,
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
                    text = "Customer Profitability Comparison",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Comparing ${comparisonItems.size} customers",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (comparisonItems.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardNavy),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
            ) {
                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Select at least 2 customers to compare", color = Color(0xFF94A3B8), fontSize = 14.sp)
                }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(comparisonItems) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardNavy),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.width(260.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = item.customerName ?: "Customer ${item.customerId}",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Class: ${item.classification}",
                                color = accentCyan,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                            )

                            ComparisonField("Revenue", "৳ ${item.revenue.toPlainString()}")
                            ComparisonField("Cost", "৳ ${item.totalCost.toPlainString()}")
                            ComparisonField(
                                "Gross Profit",
                                "৳ ${item.grossProfit.toPlainString()}",
                                if (item.grossProfit >= BigDecimal.ZERO) accentEmerald else accentRose
                            )
                            ComparisonField(
                                "Gross Margin",
                                item.grossMarginPercentage?.let { "${it.toPlainString()}%" } ?: "N/A",
                                if ((item.grossMarginPercentage ?: BigDecimal.ZERO) >= BigDecimal.ZERO) accentEmerald else accentRose
                            )
                            ComparisonField("Contribution", "৳ ${item.contributionAmount.toPlainString()}", accentAmber)
                            ComparisonField("Orders", item.orderCount.toString())
                            ComparisonField("Jobs", item.jobCount.toString())
                            ComparisonField("Quantity", "${item.totalQuantity} units")
                            ComparisonField("Trend", item.trend)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonField(label: String, value: String, valueColor: Color = Color.White) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF94A3B8), fontSize = 11.sp)
        Text(value, color = valueColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
