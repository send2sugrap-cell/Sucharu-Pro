package com.sucharu.sucharupro.ui.features.inventory.analytics

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
import com.sucharu.sucharupro.domain.model.inventory.analytics.InventoryException

/**
 * Status badge for inventory exception severity (Module 07 Step 10).
 */
@Composable
fun InventorySeverityBadge(
    severity: InventoryException.Severity,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (severity) {
        InventoryException.Severity.LOW -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        InventoryException.Severity.MEDIUM -> Color(0xFFFFF3E0) to Color(0xFFEF6C00)
        InventoryException.Severity.HIGH -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        InventoryException.Severity.CRITICAL -> Color(0xFFFFCDD2) to Color(0xFFB71C1C)
    }

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = severity.name,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
