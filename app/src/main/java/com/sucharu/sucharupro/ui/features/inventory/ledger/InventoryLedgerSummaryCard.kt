package com.sucharu.sucharupro.ui.features.inventory.ledger

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementDirection
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerEntry

/**
 * Summary card for Inventory Movement Ledger (Module 07 Step 09).
 */
@Composable
fun InventoryLedgerSummaryCard(
    entries: List<InventoryMovementLedgerEntry>
) {
    val totalIn = entries.filter { it.direction == InventoryMovementDirection.IN }.sumOf { it.quantity }
    val totalOut = entries.filter { it.direction == InventoryMovementDirection.OUT }.sumOf { kotlin.math.abs(it.quantity) }
    val netChange = totalIn - totalOut

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Movement Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(label = "Total IN", value = String.format("%.2f", totalIn), color = Color(0xFF2E7D32))
                SummaryItem(label = "Total OUT", value = String.format("%.2f", totalOut), color = Color(0xFFC62828))
                SummaryItem(
                    label = "Net Change", 
                    value = (if (netChange >= 0) "+" else "") + String.format("%.2f", netChange), 
                    color = if (netChange >= 0) MaterialTheme.colorScheme.primary else Color(0xFFC62828)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Based on ${entries.size} ledger entries",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        modifier = Modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = color)
    }
}
