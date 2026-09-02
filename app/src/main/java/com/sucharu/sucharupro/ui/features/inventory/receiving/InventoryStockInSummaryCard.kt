package com.sucharu.sucharupro.ui.features.inventory.receiving

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
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryStockInRecord

/**
 * Summary card displaying accepted stock-in records for a receiving operation
 * (Module 07 Step 03).
 *
 * Shows the total accepted quantity and breakdown per stock-in record.
 * This is a read-only informational card displayed in the receiving details screen.
 */
@Composable
fun InventoryStockInSummaryCard(
    stockInRecords: List<InventoryStockInRecord>,
    modifier: Modifier = Modifier
) {
    val totalAccepted = stockInRecords.sumOf { it.quantity }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stock-In Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
                Text(
                    text = "Total: $totalAccepted units",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF2E7D32)
                )
            }

            if (stockInRecords.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No stock-in records yet. Complete the receiving to generate stock-in records.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = Color(0xFFA5D6A7))
                Spacer(modifier = Modifier.height(8.dp))

                stockInRecords.forEach { record ->
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
                            text = "+${record.quantity} ${record.unit.defaultLabel}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                    Divider(color = Color(0xFFC8E6C9), thickness = 0.5.dp)
                }
            }
        }
    }
}
