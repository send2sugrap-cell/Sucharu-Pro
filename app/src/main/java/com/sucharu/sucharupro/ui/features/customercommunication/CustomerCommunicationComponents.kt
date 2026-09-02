package com.sucharu.sucharupro.ui.features.customercommunication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.communication.customer.CustomerCommunication
import com.sucharu.sucharupro.domain.model.communication.customer.CustomerCommunicationStatus
import com.sucharu.sucharupro.domain.model.communication.customer.CustomerCommunicationType
import com.sucharu.sucharupro.domain.model.notification.NotificationPriority
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomerCommunicationItemCard(
    communication: CustomerCommunication,
    onClick: () -> Unit,
    onMarkReadClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                1.dp,
                if (!communication.isRead) Color(0xFF38BDF8).copy(alpha = 0.6f) else Color(0xFF334155).copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!communication.isRead) Color(0xFF1E293B).copy(alpha = 0.95f) else Color(0xFF1E293B).copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(getCommunicationTypeColor(communication.communicationType).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCommunicationTypeIcon(communication.communicationType),
                            contentDescription = null,
                            tint = getCommunicationTypeColor(communication.communicationType),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column {
                        Text(
                            text = communication.communicationType.defaultLabel,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = communication.communicationNo,
                            color = Color(0xFF64748B),
                            fontSize = 10.sp
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomerCommunicationPriorityBadge(communication.priority)
                    CustomerCommunicationStatusBadge(communication.status)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = communication.title,
                color = if (!communication.isRead) Color(0xFFF8FAFC) else Color(0xFFCBD5E1),
                fontSize = 13.sp,
                fontWeight = if (!communication.isRead) FontWeight.Bold else FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = communication.message,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDateTime(communication.createdAt),
                    color = Color(0xFF64748B),
                    fontSize = 10.sp
                )

                if (!communication.isRead && onMarkReadClick != null) {
                    TextButton(
                        onClick = onMarkReadClick,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Mark Read", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerCommunicationPriorityBadge(priority: NotificationPriority) {
    val (bgColor, textColor) = when (priority) {
        NotificationPriority.LOW -> Color(0xFF64748B).copy(alpha = 0.2f) to Color(0xFF94A3B8)
        NotificationPriority.NORMAL -> Color(0xFF38BDF8).copy(alpha = 0.15f) to Color(0xFF38BDF8)
        NotificationPriority.HIGH -> Color(0xFFFBBF24).copy(alpha = 0.2f) to Color(0xFFFBBF24)
        NotificationPriority.URGENT -> Color(0xFFF87171).copy(alpha = 0.2f) to Color(0xFFF87171)
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bgColor
    ) {
        Text(
            text = priority.name,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun CustomerCommunicationStatusBadge(status: CustomerCommunicationStatus) {
    val (bgColor, textColor) = when (status) {
        CustomerCommunicationStatus.DRAFT -> Color(0xFF64748B).copy(alpha = 0.2f) to Color(0xFF94A3B8)
        CustomerCommunicationStatus.SCHEDULED -> Color(0xFF60A5FA).copy(alpha = 0.2f) to Color(0xFF60A5FA)
        CustomerCommunicationStatus.QUEUED -> Color(0xFF38BDF8).copy(alpha = 0.2f) to Color(0xFF38BDF8)
        CustomerCommunicationStatus.SENT -> Color(0xFF818CF8).copy(alpha = 0.2f) to Color(0xFF818CF8)
        CustomerCommunicationStatus.DELIVERED -> Color(0xFF34D399).copy(alpha = 0.15f) to Color(0xFF34D399)
        CustomerCommunicationStatus.READ -> Color(0xFF10B981).copy(alpha = 0.2f) to Color(0xFF10B981)
        CustomerCommunicationStatus.ACKNOWLEDGED -> Color(0xFFC084FC).copy(alpha = 0.2f) to Color(0xFFC084FC)
        CustomerCommunicationStatus.FAILED -> Color(0xFFF87171).copy(alpha = 0.2f) to Color(0xFFF87171)
        CustomerCommunicationStatus.CANCELLED -> Color(0xFF64748B).copy(alpha = 0.2f) to Color(0xFF64748B)
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bgColor
    ) {
        Text(
            text = status.defaultLabel,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun CustomerEngagementMetricCard(
    label: String,
    value: String,
    accentColor: Color = Color(0xFF38BDF8),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, Color(0xFF334155).copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, color = Color(0xFF94A3B8), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = accentColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun getCommunicationTypeIcon(type: CustomerCommunicationType): ImageVector {
    return when (type) {
        CustomerCommunicationType.ORDER_UPDATE -> Icons.Default.ShoppingCart
        CustomerCommunicationType.DESIGN_UPDATE,
        CustomerCommunicationType.APPROVAL_REQUEST -> Icons.Default.ThumbUp
        CustomerCommunicationType.PRODUCTION_UPDATE -> Icons.Default.Build
        CustomerCommunicationType.QUALITY_UPDATE -> Icons.Default.CheckCircle
        CustomerCommunicationType.DELIVERY_UPDATE -> Icons.Default.Send
        CustomerCommunicationType.PAYMENT_RECEIVED,
        CustomerCommunicationType.PAYMENT_DUE,
        CustomerCommunicationType.PAYMENT_OVERDUE -> Icons.Default.ShoppingCart
        CustomerCommunicationType.OFFER,
        CustomerCommunicationType.PROMOTION -> Icons.Default.Star
        CustomerCommunicationType.SERVICE_ANNOUNCEMENT,
        CustomerCommunicationType.IMPORTANT_NOTICE -> Icons.Default.Info
        else -> Icons.Default.Notifications
    }
}

private fun getCommunicationTypeColor(type: CustomerCommunicationType): Color {
    return when (type) {
        CustomerCommunicationType.ORDER_UPDATE -> Color(0xFF38BDF8)
        CustomerCommunicationType.DESIGN_UPDATE,
        CustomerCommunicationType.APPROVAL_REQUEST -> Color(0xFFC084FC)
        CustomerCommunicationType.PRODUCTION_UPDATE -> Color(0xFFFB923C)
        CustomerCommunicationType.QUALITY_UPDATE -> Color(0xFF34D399)
        CustomerCommunicationType.DELIVERY_UPDATE -> Color(0xFF60A5FA)
        CustomerCommunicationType.PAYMENT_RECEIVED -> Color(0xFF10B981)
        CustomerCommunicationType.PAYMENT_DUE -> Color(0xFFFBBF24)
        CustomerCommunicationType.PAYMENT_OVERDUE -> Color(0xFFF87171)
        CustomerCommunicationType.OFFER,
        CustomerCommunicationType.PROMOTION -> Color(0xFFF472B6)
        else -> Color(0xFF94A3B8)
    }
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
