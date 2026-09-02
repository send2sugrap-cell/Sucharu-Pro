package com.sucharu.sucharupro.ui.features.inventory.reorder

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
import com.sucharu.sucharupro.domain.model.inventory.reorder.InventoryReorderAlert
import com.sucharu.sucharupro.domain.model.inventory.reorder.InventoryReorderAlertType

/**
 * Summary card for Reorder Alerts (Module 07 Step 08).
 *
 * Displays alert distribution by severity (Critical, Reorder, Low Stock, Out of Stock).
 */
@Composable
fun InventoryReorderSummaryCard(
    alerts: List<InventoryReorderAlert>
) {
    val total = alerts.size
    val critical = alerts.count { it.alertType == InventoryReorderAlertType.CRITICAL }
    val outOfStock = alerts.count { it.alertType == InventoryReorderAlertType.OUT_OF_STOCK }
    val reorder = alerts.count { it.alertType == InventoryReorderAlertType.REORDER_REQUIRED }
    val lowStock = alerts.count { it.alertType == InventoryReorderAlertType.LOW_STOCK }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Alert Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(label = "Total", value = total.toString(), color = MaterialTheme.colorScheme.onSurface)
                SummaryItem(label = "Critical", value = critical.toString(), color = Color(0xFFC62828))
                SummaryItem(label = "OOS", value = outOfStock.toString(), color = Color(0xFFB71C1C))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(label = "Reorder", value = reorder.toString(), color = Color(0xFFF57F17))
                SummaryItem(label = "Low Stock", value = lowStock.toString(), color = Color(0xFFFBC02D))
                // Placeholder to keep spacing
                Spacer(modifier = Modifier.weight(1f))
            }
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
