package com.sucharu.sucharupro.ui.features.inventory.stocktransfer

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
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferStatus

/**
 * Visual badge for Stock Transfer status (Module 07 Step 05).
 */
@Composable
fun InventoryStockTransferStatusBadge(status: InventoryStockTransferStatus) {
    val (backgroundColor, textColor) = when (status) {
        InventoryStockTransferStatus.DRAFT -> Color(0xFFE0E0E0) to Color(0xFF616161)
        InventoryStockTransferStatus.PENDING -> Color(0xFFFFF9C4) to Color(0xFFFBC02D)
        InventoryStockTransferStatus.APPROVED -> Color(0xFFC8E6C9) to Color(0xFF2E7D32)
        InventoryStockTransferStatus.TRANSFERRING -> Color(0xFFE1BEE7) to Color(0xFF7B1FA2)
        InventoryStockTransferStatus.COMPLETED -> Color(0xFFB3E5FC) to Color(0xFF0288D1)
        InventoryStockTransferStatus.CANCELLED -> Color(0xFFFFCDD2) to Color(0xFFC62828)
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
