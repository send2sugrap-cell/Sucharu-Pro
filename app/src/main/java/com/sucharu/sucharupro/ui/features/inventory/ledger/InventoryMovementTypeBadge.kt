package com.sucharu.sucharupro.ui.features.inventory.ledger

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
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerType

/**
 * Visual badge for Inventory Movement types (Module 07 Step 09).
 */
@Composable
fun InventoryMovementTypeBadge(type: InventoryMovementLedgerType) {
    val (backgroundColor, textColor) = when (type) {
        InventoryMovementLedgerType.STOCK_IN -> Color(0xFFC8E6C9) to Color(0xFF2E7D32)
        InventoryMovementLedgerType.STOCK_OUT -> Color(0xFFFFCDD2) to Color(0xFFC62828)
        InventoryMovementLedgerType.TRANSFER_IN -> Color(0xFFE1BEE7) to Color(0xFF7B1FA2)
        InventoryMovementLedgerType.TRANSFER_OUT -> Color(0xFFF3E5F5) to Color(0xFF8E24AA)
        InventoryMovementLedgerType.ADJUSTMENT_IN -> Color(0xFFBBDEFB) to Color(0xFF1976D2)
        InventoryMovementLedgerType.ADJUSTMENT_OUT -> Color(0xFFE3F2FD) to Color(0xFF1E88E5)
    }

    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = type.name.replace("_", " "),
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
