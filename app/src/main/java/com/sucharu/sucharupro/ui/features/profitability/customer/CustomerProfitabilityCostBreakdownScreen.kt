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
import com.sucharu.sucharupro.data.api.model.profitability.CustomerCostBreakdownItemDto
import java.math.BigDecimal

/**
 * 3. Customer Cost Breakdown Screen (12 Cost Components)
 */
@Composable
fun CustomerProfitabilityCostBreakdownScreen(
    customerId: String,
    components: List<CustomerCostBreakdownItemDto>,
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
                    text = "12 Cost Components Breakdown",
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

        val totalCost = components.fold(BigDecimal.ZERO) { acc, c -> acc.add(c.amount) }

        Card(
            colors = CardDefaults.cardColors(containerColor = cardNavy),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total Attributed Cost", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("৳ ${totalCost.toPlainString()}", color = accentAmber, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(components) { comp ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardNavy),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = comp.componentType.replace("_", " "),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "৳ ${comp.amount.toPlainString()}",
                                color = accentCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = {
                                val pct = comp.percentageOfTotalCost.toFloat() / 100f
                                pct.coerceIn(0f, 1f)
                            },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = accentCyan,
                            trackColor = Color(0xFF334155),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${comp.percentageOfTotalCost.toPlainString()}% of total • ${if (comp.isVariableCost) "Variable" else "Fixed"}",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Basis: ${comp.allocationBasis} (${comp.sourceCount} sources)",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
