package com.sucharu.sucharupro.ui.features.inventory.traceability

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryTraceabilityStatus

/**
 * Visual badge for Batch/Lot status (Module 07 Step 07).
 */
@Composable
fun InventoryTraceabilityStatusBadge(status: InventoryTraceabilityStatus) {
    val (backgroundColor, textColor) = when (status) {
        InventoryTraceabilityStatus.ACTIVE -> Color(0xFFC8E6C9) to Color(0xFF2E7D32)
        InventoryTraceabilityStatus.HOLD -> Color(0xFFFFF9C4) to Color(0xFFFBC02D)
        InventoryTraceabilityStatus.EXHAUSTED -> Color(0xFFE0E0E0) to Color(0xFF616161)
        InventoryTraceabilityStatus.CLOSED -> Color(0xFFB3E5FC) to Color(0xFF0288D1)
        InventoryTraceabilityStatus.CANCELLED -> Color(0xFFFFCDD2) to Color(0xFFC62828)
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
