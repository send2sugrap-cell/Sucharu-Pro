package com.sucharu.sucharupro.ui.features.inventory.stocktransfer

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferRecord

/**
 * Summary card for stock transfer records (Module 07 Step 05).
 *
 * Groups records by product and shows total transferred vs requested.
 */
@Composable
fun InventoryStockTransferSummaryCard(
    transferRecords: List<InventoryStockTransferRecord>
) {
    if (transferRecords.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                text = "No transfer records available for this document.",
                modifier = Modifier.padding(16.dp),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val groupedByProduct = transferRecords.groupBy { it.inventoryProductId }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            groupedByProduct.forEach { (productId, records) ->
                val totalQty = records.sumOf { it.quantity }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = productId, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "Entries: ${records.size}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    Text(
                        text = "$totalQty Units",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            val grandTotal = transferRecords.sumOf { it.quantity }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Grand Total", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Text(text = "$grandTotal Units", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
