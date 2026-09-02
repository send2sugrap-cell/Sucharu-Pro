package com.sucharu.sucharupro.ui.features.delivery.shipment

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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipment
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentAttempt
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentAttemptStatus
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentEvent
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentPriority
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeliveryShipmentStatusBadge(
    status: DeliveryShipmentStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        DeliveryShipmentStatus.DRAFT -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        DeliveryShipmentStatus.READY -> Color(0xFFE0F2F1) to Color(0xFF00695C)
        DeliveryShipmentStatus.DISPATCHED -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        DeliveryShipmentStatus.IN_TRANSIT -> Color(0xFFEDE7F6) to Color(0xFF512DA8)
        DeliveryShipmentStatus.OUT_FOR_DELIVERY -> Color(0xFFE1F5FE) to Color(0xFF0288D1)
        DeliveryShipmentStatus.DELIVERY_ATTEMPTED -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
        DeliveryShipmentStatus.DELAYED -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        DeliveryShipmentStatus.ON_HOLD -> Color(0xFFFFE0B2) to Color(0xFFE65100)
        DeliveryShipmentStatus.DELIVERED -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        DeliveryShipmentStatus.CANCELLED -> Color(0xFFF5F5F5) to Color(0xFF757575)
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
fun DeliveryShipmentTypeBadge(
    type: DeliveryShipmentType,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = type.defaultLabel,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun DeliveryShipmentPriorityBadge(
    priority: DeliveryShipmentPriority,
    modifier: Modifier = Modifier
) {
    val color = when (priority) {
        DeliveryShipmentPriority.LOW -> Color(0xFF757575)
        DeliveryShipmentPriority.NORMAL -> Color(0xFF1565C0)
        DeliveryShipmentPriority.HIGH -> Color(0xFFE65100)
        DeliveryShipmentPriority.URGENT -> Color(0xFFD32F2F)
    }

    Box(
        modifier = modifier
            .background(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = priority.defaultLabel,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun DeliveryShipmentAttemptStatusBadge(
    status: DeliveryShipmentAttemptStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        DeliveryShipmentAttemptStatus.SUCCESSFUL -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        DeliveryShipmentAttemptStatus.FAILED -> Color(0xFFFFEBEE) to Color(0xFFD32F2F)
        DeliveryShipmentAttemptStatus.RESCHEDULED -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        DeliveryShipmentAttemptStatus.RECIPIENT_UNAVAILABLE -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
        DeliveryShipmentAttemptStatus.ADDRESS_ISSUE -> Color(0xFFFFE0B2) to Color(0xFFE65100)
        DeliveryShipmentAttemptStatus.OTHER -> Color(0xFFF5F5F5) to Color(0xFF757575)
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
fun DeliveryShipmentSummaryCard(
    shipment: DeliveryShipment,
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
                        imageVector = if (shipment.currentStatus == DeliveryShipmentStatus.DELAYED) Icons.Default.Warning else Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = if (shipment.currentStatus == DeliveryShipmentStatus.DELAYED) Color(0xFFC62828) else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = shipment.shipmentNo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                DeliveryShipmentStatusBadge(status = shipment.currentStatus)
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!shipment.trackingNumber.isNullOrBlank()) {
                Text(
                    text = "Tracking: ${shipment.trackingNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "Carrier: ${shipment.carrierName ?: "Direct / Internal"} • ${shipment.shipmentType.defaultLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!shipment.destinationAddress.isNullOrBlank()) {
                Text(
                    text = "Destination: ${shipment.destinationAddress}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Created: ${dateFormat.format(Date(shipment.createdAt))}",
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
fun DeliveryShipmentTrackingTimeline(
    events: List<DeliveryShipmentEvent>,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Column(modifier = modifier.fillMaxWidth()) {
        events.sortedByDescending { it.eventTime }.forEach { event ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = event.eventType.defaultLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = dateFormat.format(Date(event.eventTime)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                if (!event.locationText.isNullOrBlank()) {
                    Text(
                        text = "Location: ${event.locationText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                val desc = event.description
                if (!desc.isNullOrBlank()) {
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
