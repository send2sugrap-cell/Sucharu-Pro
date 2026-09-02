package com.sucharu.sucharupro.ui.features.delivery.partial

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliveryPartialSettlement
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySettlementStatus
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySplitDispatch
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySplitDispatchStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeliverySettlementStatusBadge(
    status: DeliverySettlementStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        DeliverySettlementStatus.OPEN -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        DeliverySettlementStatus.PARTIALLY_DELIVERED -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        DeliverySettlementStatus.FULLY_DELIVERED -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        DeliverySettlementStatus.PARTIALLY_RETURNED -> Color(0xFFFFE0B2) to Color(0xFFE65100)
        DeliverySettlementStatus.SETTLEMENT_PENDING -> Color(0xFFEDE7F6) to Color(0xFF512DA8)
        DeliverySettlementStatus.SETTLED -> Color(0xFFE8F5E9) to Color(0xFF1B5E20)
        DeliverySettlementStatus.DISPUTED -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        DeliverySettlementStatus.CANCELLED -> Color(0xFFF5F5F5) to Color(0xFF757575)
    }

    Box(
        modifier = modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.defaultLabel,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun DeliverySplitDispatchStatusBadge(
    status: DeliverySplitDispatchStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        DeliverySplitDispatchStatus.DRAFT -> Color(0xFFF5F5F5) to Color(0xFF757575)
        DeliverySplitDispatchStatus.PENDING -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
        DeliverySplitDispatchStatus.APPROVED -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        DeliverySplitDispatchStatus.READY -> Color(0xFFE0F2F1) to Color(0xFF00695C)
        DeliverySplitDispatchStatus.EXECUTED -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        DeliverySplitDispatchStatus.DELIVERED -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        DeliverySplitDispatchStatus.CANCELLED -> Color(0xFFFFEBEE) to Color(0xFFC62828)
    }

    Box(
        modifier = modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = status.defaultLabel,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun DeliverySettlementSummaryCard(
    settlement: DeliveryPartialSettlement,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DO: ${settlement.deliveryOrderId.take(12)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                DeliverySettlementStatusBadge(status = settlement.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            val progress = (settlement.totalDeliveredQuantity / settlement.totalOrderedQuantity.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (progress >= 1f) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Delivered: ${settlement.totalDeliveredQuantity.toInt()} / ${settlement.totalOrderedQuantity.toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Pending: ${settlement.totalPendingQuantity.toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (settlement.totalPendingQuantity > 0) Color(0xFFE65100) else Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Updated: ${dateFormat.format(Date(settlement.updatedAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DeliverySplitDispatchCard(
    split: DeliverySplitDispatch,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CallSplit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Split #${split.splitSequence}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                DeliverySplitDispatchStatusBadge(status = split.status)
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (!split.deliveryChallanId.isNullOrBlank()) {
                Text("Challan: ${split.deliveryChallanId}", style = MaterialTheme.typography.bodySmall)
            }
            if (!split.dispatchExecutionId.isNullOrBlank()) {
                Text("Dispatch: ${split.dispatchExecutionId}", style = MaterialTheme.typography.bodySmall)
            }
            if (!split.shipmentId.isNullOrBlank()) {
                Text("Shipment: ${split.shipmentId}", style = MaterialTheme.typography.bodySmall)
            }
            if (!split.notes.isNullOrBlank()) {
                Text("Notes: ${split.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Created: ${dateFormat.format(Date(split.createdAt))}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
