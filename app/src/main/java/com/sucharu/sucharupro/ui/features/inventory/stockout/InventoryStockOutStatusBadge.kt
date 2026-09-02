package com.sucharu.sucharupro.ui.features.inventory.stockout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutStatus

/**
 * Colored status badge chip for [InventoryStockOutStatus] (Module 07 Step 04).
 *
 * Displays the status label with a color-coded pill background.
 * Terminal states (COMPLETED, CANCELLED) use muted/distinct tones.
 */
@Composable
fun InventoryStockOutStatusBadge(
    status: InventoryStockOutStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        InventoryStockOutStatus.DRAFT -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        InventoryStockOutStatus.PENDING -> Color(0xFFFFF9C4) to Color(0xFFF57F17)
        InventoryStockOutStatus.ISSUING -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        InventoryStockOutStatus.COMPLETED -> Color(0xFFEDE7F6) to Color(0xFF4527A0)
        InventoryStockOutStatus.CANCELLED -> Color(0xFFF5F5F5) to Color(0xFF757575)
    }

    Box(
        modifier = modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.defaultLabel.uppercase(),
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
