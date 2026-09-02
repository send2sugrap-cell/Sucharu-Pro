package com.sucharu.sucharupro.ui.features.inventory.adjustment

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryAdjustmentType
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentRecord

/**
 * Summary card for stock adjustment records (Module 07 Step 06).
 *
 * Groups records by product and shows net quantity change.
 */
@Composable
fun InventoryStockAdjustmentSummaryCard(
    adjustmentRecords: List<InventoryStockAdjustmentRecord>
) {
    if (adjustmentRecords.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                text = "No adjustment records available for this document.",
                modifier = Modifier.padding(16.dp),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val groupedByProduct = adjustmentRecords.groupBy { it.inventoryProductId }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            groupedByProduct.forEach { (productId, records) ->
                val netChange = records.sumOf { 
                    if (it.adjustmentType == InventoryAdjustmentType.INCREASE) it.quantity else -it.quantity 
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = productId, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "Entries: ${records.size}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    Text(
                        text = "${if (netChange > 0) "+" else ""}$netChange Units",
                        fontWeight = FontWeight.Bold,
                        color = if (netChange > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            val grandTotalChange = adjustmentRecords.sumOf { 
                if (it.adjustmentType == InventoryAdjustmentType.INCREASE) it.quantity else -it.quantity
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Net Total Change", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Text(
                    text = "${if (grandTotalChange > 0) "+" else ""}$grandTotalChange Units", 
                    fontWeight = FontWeight.ExtraBold, 
                    fontSize = 15.sp, 
                    color = if (grandTotalChange >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
