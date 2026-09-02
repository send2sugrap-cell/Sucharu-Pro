package com.sucharu.sucharupro.ui.features.delivery.reconciliation

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
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySettlementStatus
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationDiscrepancySeverity
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationItemStatus
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationStatus

@Composable
fun DeliveryReconciliationStatusBadge(
    status: DeliveryReconciliationStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status) {
        DeliveryReconciliationStatus.OPEN -> Color(0xFFF5F5F5) to Color(0xFF616161)
        DeliveryReconciliationStatus.IN_PROGRESS -> Color(0xFFE3F2FD) to Color(0xFF1976D2)
        DeliveryReconciliationStatus.PARTIALLY_RECONCILED -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
        DeliveryReconciliationStatus.REQUIRES_REVIEW -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        DeliveryReconciliationStatus.RECONCILED -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        DeliveryReconciliationStatus.DISPUTED -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        DeliveryReconciliationStatus.RESOLVED -> Color(0xFFEDE7F6) to Color(0xFF512DA8)
        DeliveryReconciliationStatus.CLOSED -> Color(0xFFECEFF1) to Color(0xFF37474F)
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
fun DeliveryReconciliationSettlementBadge(
    status: DeliverySettlementStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status) {
        DeliverySettlementStatus.OPEN -> Color(0xFFF5F5F5) to Color(0xFF616161)
        DeliverySettlementStatus.PARTIALLY_DELIVERED -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
        DeliverySettlementStatus.FULLY_DELIVERED -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        DeliverySettlementStatus.PARTIALLY_RETURNED -> Color(0xFFFBE9E7) to Color(0xFFD84315)
        DeliverySettlementStatus.SETTLEMENT_PENDING -> Color(0xFFE1F5FE) to Color(0xFF0277BD)
        DeliverySettlementStatus.SETTLED -> Color(0xFFE8F5E9) to Color(0xFF1B5E20)
        DeliverySettlementStatus.DISPUTED -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        DeliverySettlementStatus.CANCELLED -> Color(0xFFFFCDD2) to Color(0xFFB71C1C)
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
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DeliveryReconciliationDiscrepancySeverityBadge(
    severity: DeliveryReconciliationDiscrepancySeverity,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (severity) {
        DeliveryReconciliationDiscrepancySeverity.LOW -> Color(0xFFF5F5F5) to Color(0xFF757575)
        DeliveryReconciliationDiscrepancySeverity.MEDIUM -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
        DeliveryReconciliationDiscrepancySeverity.HIGH -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        DeliveryReconciliationDiscrepancySeverity.CRITICAL -> Color(0xFFFFEBEE) to Color(0xFFC62828)
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = severity.defaultLabel,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DeliveryReconciliationItemStatusBadge(
    status: DeliveryReconciliationItemStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status) {
        DeliveryReconciliationItemStatus.PENDING -> Color(0xFFF5F5F5) to Color(0xFF757575)
        DeliveryReconciliationItemStatus.MATCHED -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        DeliveryReconciliationItemStatus.DISCREPANCY -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        DeliveryReconciliationItemStatus.RECONCILED -> Color(0xFFE0F2F1) to Color(0xFF00695C)
        DeliveryReconciliationItemStatus.RESOLVED -> Color(0xFFEDE7F6) to Color(0xFF512DA8)
        DeliveryReconciliationItemStatus.CLOSED -> Color(0xFFECEFF1) to Color(0xFF455A64)
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = status.defaultLabel,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}
