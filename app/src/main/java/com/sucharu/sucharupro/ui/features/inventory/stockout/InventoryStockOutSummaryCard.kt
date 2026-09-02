package com.sucharu.sucharupro.ui.features.inventory.stockout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutRecord

/**
 * Summary card displaying issued stock-out records for a stock-out operation
 * (Module 07 Step 04).
 *
 * Shows the total issued quantity and breakdown per stock-out record.
 * This is a read-only informational card displayed in the stock-out details screen.
 */
@Composable
fun InventoryStockOutSummaryCard(
    stockOutRecords: List<InventoryStockOutRecord>,
    modifier: Modifier = Modifier
) {
    val totalIssued = stockOutRecords.sumOf { it.quantity }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stock-Out Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A148C)
                )
                Text(
                    text = "Total: $totalIssued units",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF6A1B9A)
                )
            }

            if (stockOutRecords.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No stock-out records yet. Process the issuance to generate records.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = Color(0xFFCE93D8))
                Spacer(modifier = Modifier.height(8.dp))

                stockOutRecords.forEach { record ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Product: ${record.inventoryProductId}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Location: ${record.locationId}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Created: ${record.createdAt} by ${record.createdBy}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Text(
                            text = "-${record.quantity} ${record.unit.defaultLabel}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFFC62828)
                        )
                    }
                    Divider(color = Color(0xFFE1BEE7), thickness = 0.5.dp)
                }
            }
        }
    }
}
