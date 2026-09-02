package com.sucharu.sucharupro.ui.features.delivery.returning

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
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnDisposition
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLineCondition
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnPriority
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnStatus

@Composable
fun DeliveryReturnStatusBadge(
    status: DeliveryReturnStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status) {
        DeliveryReturnStatus.DRAFT -> Color(0xFFF5F5F5) to Color(0xFF616161)
        DeliveryReturnStatus.PENDING -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
        DeliveryReturnStatus.APPROVED -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        DeliveryReturnStatus.RECEIVING -> Color(0xFFE1F5FE) to Color(0xFF0277BD)
        DeliveryReturnStatus.RECEIVED -> Color(0xFFEDE7F6) to Color(0xFF512DA8)
        DeliveryReturnStatus.INSPECTING -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        DeliveryReturnStatus.INSPECTED -> Color(0xFFE0F2F1) to Color(0xFF00695C)
        DeliveryReturnStatus.DISPOSITION_PENDING -> Color(0xFFFBE9E7) to Color(0xFFD84315)
        DeliveryReturnStatus.PROCESSING -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        DeliveryReturnStatus.COMPLETED -> Color(0xFFE8F5E9) to Color(0xFF1B5E20)
        DeliveryReturnStatus.CANCELLED -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        DeliveryReturnStatus.REJECTED -> Color(0xFFFFCDD2) to Color(0xFFB71C1C)
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = status.defaultLabel,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun DeliveryReturnPriorityBadge(
    priority: DeliveryReturnPriority,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (priority) {
        DeliveryReturnPriority.LOW -> Color(0xFFF5F5F5) to Color(0xFF757575)
        DeliveryReturnPriority.NORMAL -> Color(0xFFE3F2FD) to Color(0xFF1976D2)
        DeliveryReturnPriority.HIGH -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        DeliveryReturnPriority.URGENT -> Color(0xFFFFEBEE) to Color(0xFFC62828)
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = priority.defaultLabel,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DeliveryReturnConditionBadge(
    condition: DeliveryReturnLineCondition,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (condition) {
        DeliveryReturnLineCondition.GOOD -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        DeliveryReturnLineCondition.DAMAGED -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        DeliveryReturnLineCondition.DEFECTIVE -> Color(0xFFFBE9E7) to Color(0xFFD84315)
        DeliveryReturnLineCondition.OPENED -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
        DeliveryReturnLineCondition.USED -> Color(0xFFECEFF1) to Color(0xFF455A64)
        DeliveryReturnLineCondition.MISSING -> Color(0xFFFFCDD2) to Color(0xFFB71C1C)
        DeliveryReturnLineCondition.UNKNOWN -> Color(0xFFEEEEEE) to Color(0xFF757575)
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = condition.defaultLabel,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DeliveryReturnDispositionBadge(
    disposition: DeliveryReturnDisposition,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (disposition) {
        DeliveryReturnDisposition.RESTOCK -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        DeliveryReturnDisposition.QUARANTINE -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
        DeliveryReturnDisposition.REWORK -> Color(0xFFE1F5FE) to Color(0xFF0277BD)
        DeliveryReturnDisposition.REPLACEMENT -> Color(0xFFEDE7F6) to Color(0xFF512DA8)
        DeliveryReturnDisposition.SCRAP -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        DeliveryReturnDisposition.CUSTOMER_HOLD -> Color(0xFFECEFF1) to Color(0xFF455A64)
        DeliveryReturnDisposition.NON_STOCK -> Color(0xFFEEEEEE) to Color(0xFF616161)
        DeliveryReturnDisposition.PENDING_DECISION -> Color(0xFFF5F5F5) to Color(0xFF9E9E9E)
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = disposition.defaultLabel,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}
