package com.sucharu.sucharupro.ui.features.inventory.reorder

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
import com.sucharu.sucharupro.domain.model.inventory.reorder.InventoryReorderAlertStatus

/**
 * Visual badge for Reorder Alert status (Module 07 Step 08).
 */
@Composable
fun InventoryReorderAlertStatusBadge(status: InventoryReorderAlertStatus) {
    val (backgroundColor, textColor) = when (status) {
        InventoryReorderAlertStatus.OPEN -> Color(0xFFFFF9C4) to Color(0xFFFBC02D)
        InventoryReorderAlertStatus.ACKNOWLEDGED -> Color(0xFFE1BEE7) to Color(0xFF7B1FA2)
        InventoryReorderAlertStatus.RESOLVED -> Color(0xFFC8E6C9) to Color(0xFF2E7D32)
        InventoryReorderAlertStatus.DISMISSED -> Color(0xFFFFCDD2) to Color(0xFFC62828)
    }

    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = status.defaultLabel.uppercase(),
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
