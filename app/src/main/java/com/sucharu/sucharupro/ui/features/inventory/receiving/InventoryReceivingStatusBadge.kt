package com.sucharu.sucharupro.ui.features.inventory.receiving

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
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingStatus

/**
 * Colored status badge chip for [InventoryReceivingStatus] (Module 07 Step 03).
 *
 * Displays the status label with a color-coded pill background.
 * Terminal states are shown in muted/neutral tones.
 */
@Composable
fun InventoryReceivingStatusBadge(
    status: InventoryReceivingStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        InventoryReceivingStatus.DRAFT -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        InventoryReceivingStatus.PENDING -> Color(0xFFFFF9C4) to Color(0xFFF57F17)
        InventoryReceivingStatus.RECEIVING -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        InventoryReceivingStatus.PARTIALLY_ACCEPTED -> Color(0xFFE8F5E9) to Color(0xFF388E3C)
        InventoryReceivingStatus.ACCEPTED -> Color(0xFF1B5E20).copy(alpha = 0.15f) to Color(0xFF1B5E20)
        InventoryReceivingStatus.PARTIALLY_REJECTED -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        InventoryReceivingStatus.REJECTED -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        InventoryReceivingStatus.COMPLETED -> Color(0xFFEDE7F6) to Color(0xFF4527A0)
        InventoryReceivingStatus.CANCELLED -> Color(0xFFF5F5F5) to Color(0xFF757575)
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
