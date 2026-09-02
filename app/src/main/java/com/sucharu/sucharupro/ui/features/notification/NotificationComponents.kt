package com.sucharu.sucharupro.ui.features.notification

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.notification.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationItemCard(
    notification: Notification,
    onClick: () -> Unit,
    onMarkAsRead: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUnread = !notification.isRead && notification.status != NotificationStatus.CANCELLED
    val borderColor = if (isUnread) Color(0xFF38BDF8).copy(alpha = 0.5f) else Color(0xFF334155).copy(alpha = 0.5f)
    val containerColor = if (isUnread) Color(0xFF1E293B) else Color(0xFF161E2E)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Category Icon with Status Dot
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(getCategoryColor(notification.notificationType.category).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(notification.notificationType.category),
                    contentDescription = null,
                    tint = getCategoryColor(notification.notificationType.category),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        color = if (isUnread) Color(0xFFF8FAFC) else Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold
                    )
                    Text(
                        text = formatTimestamp(notification.createdAt),
                        color = Color(0xFF64748B),
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.message,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        NotificationPriorityBadge(priority = notification.priority)
                        NotificationStatusBadge(status = notification.status)
                    }

                    if (isUnread) {
                        TextButton(
                            onClick = onMarkAsRead,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Mark Read", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationPriorityBadge(priority: NotificationPriority) {
    val (color, text) = when (priority) {
        NotificationPriority.LOW -> Color(0xFF94A3B8) to "Low"
        NotificationPriority.NORMAL -> Color(0xFF38BDF8) to "Normal"
        NotificationPriority.HIGH -> Color(0xFFFBBF24) to "High"
        NotificationPriority.URGENT -> Color(0xFFF87171) to "Urgent"
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun NotificationStatusBadge(status: NotificationStatus) {
    val color = when (status) {
        NotificationStatus.DRAFT -> Color(0xFF94A3B8)
        NotificationStatus.QUEUED -> Color(0xFFFBBF24)
        NotificationStatus.PROCESSING -> Color(0xFF38BDF8)
        NotificationStatus.SENT -> Color(0xFF60A5FA)
        NotificationStatus.DELIVERED -> Color(0xFF34D399)
        NotificationStatus.READ -> Color(0xFF10B981)
        NotificationStatus.FAILED -> Color(0xFFF87171)
        NotificationStatus.CANCELLED -> Color(0xFF64748B)
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = status.defaultLabel,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun NotificationKpiCard(
    title: String,
    count: Int,
    accentColor: Color = Color(0xFF38BDF8),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Text("$count", color = accentColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun getCategoryColor(category: NotificationCategory): Color {
    return when (category) {
        NotificationCategory.ORDER -> Color(0xFF38BDF8)
        NotificationCategory.DESIGN -> Color(0xFFC084FC)
        NotificationCategory.PRODUCTION -> Color(0xFFF59E0B)
        NotificationCategory.QUALITY -> Color(0xFFEC4899)
        NotificationCategory.DELIVERY -> Color(0xFF10B981)
        NotificationCategory.INVENTORY -> Color(0xFF06B6D4)
        NotificationCategory.FINANCE -> Color(0xFF34D399)
        NotificationCategory.SYSTEM -> Color(0xFFF87171)
        NotificationCategory.GENERAL -> Color(0xFF94A3B8)
    }
}

private fun getCategoryIcon(category: NotificationCategory): ImageVector {
    return when (category) {
        NotificationCategory.ORDER -> Icons.Default.ShoppingCart
        NotificationCategory.DESIGN -> Icons.Default.Brush
        NotificationCategory.PRODUCTION -> Icons.Default.Build
        NotificationCategory.QUALITY -> Icons.Default.CheckCircle
        NotificationCategory.DELIVERY -> Icons.Default.LocalShipping
        NotificationCategory.INVENTORY -> Icons.Default.Inventory
        NotificationCategory.FINANCE -> Icons.Default.AccountBalanceWallet
        NotificationCategory.SYSTEM -> Icons.Default.Warning
        NotificationCategory.GENERAL -> Icons.Default.Notifications
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
