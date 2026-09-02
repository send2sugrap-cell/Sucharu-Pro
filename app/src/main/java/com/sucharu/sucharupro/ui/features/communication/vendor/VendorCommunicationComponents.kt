package com.sucharu.sucharupro.ui.features.communication.vendor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.communication.vendor.*
import com.sucharu.sucharupro.domain.model.notification.NotificationPriority

// =========================================================================
// Design tokens (matching established dark ERP premium design system)
// =========================================================================
private val VcBg = Color(0xFF0F172A)
private val VcSurface = Color(0xFF1E293B)
private val VcBorder = Color(0xFF334155)
private val VcAccent = Color(0xFF38BDF8)
private val VcAccentGreen = Color(0xFF4ADE80)
private val VcAccentAmber = Color(0xFFFBBF24)
private val VcAccentRed = Color(0xFFF87171)
private val VcAccentPurple = Color(0xFFC084FC)
private val VcTextPrimary = Color(0xFFF8FAFC)
private val VcTextSecondary = Color(0xFF94A3B8)

// =========================================================================
// VendorCommunicationCard
// =========================================================================

@Composable
fun VendorCommunicationCard(
    communication: VendorCommunication,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = VcSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = communication.subject,
                        color = VcTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = communication.communicationNo,
                        color = VcTextSecondary,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                VendorCommunicationStatusChip(status = communication.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VendorCommunicationTypeChip(type = communication.communicationType)
                VendorCommunicationPriorityChip(priority = communication.priority)
                if (communication.requiresAcknowledgement && !communication.isAcknowledged) {
                    Surface(
                        color = VcAccentAmber.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Ack Required",
                            color = VcAccentAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = communication.message,
                color = VcTextSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// =========================================================================
// VendorCommunicationStatusChip
// =========================================================================

@Composable
fun VendorCommunicationStatusChip(status: VendorCommunicationStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor, label) = when (status) {
        VendorCommunicationStatus.DRAFT -> Triple(Color(0xFF334155), VcTextSecondary, "Draft")
        VendorCommunicationStatus.SCHEDULED -> Triple(VcAccentPurple.copy(alpha = 0.15f), VcAccentPurple, "Scheduled")
        VendorCommunicationStatus.QUEUED -> Triple(VcAccentAmber.copy(alpha = 0.15f), VcAccentAmber, "Queued")
        VendorCommunicationStatus.SENT -> Triple(VcAccent.copy(alpha = 0.15f), VcAccent, "Sent")
        VendorCommunicationStatus.DELIVERED -> Triple(Color(0xFF60A5FA).copy(alpha = 0.15f), Color(0xFF60A5FA), "Delivered")
        VendorCommunicationStatus.READ -> Triple(VcAccentGreen.copy(alpha = 0.15f), VcAccentGreen, "Read")
        VendorCommunicationStatus.ACKNOWLEDGED -> Triple(VcAccentGreen.copy(alpha = 0.2f), VcAccentGreen, "Acknowledged")
        VendorCommunicationStatus.DECLINED -> Triple(VcAccentRed.copy(alpha = 0.15f), VcAccentRed, "Declined")
        VendorCommunicationStatus.FAILED -> Triple(VcAccentRed.copy(alpha = 0.15f), VcAccentRed, "Failed")
        VendorCommunicationStatus.CANCELLED -> Triple(Color(0xFF475569).copy(alpha = 0.4f), VcTextSecondary, "Cancelled")
    }
    Surface(color = bgColor, shape = RoundedCornerShape(6.dp), modifier = modifier) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// =========================================================================
// VendorCommunicationTypeChip
// =========================================================================

@Composable
fun VendorCommunicationTypeChip(type: VendorCommunicationType, modifier: Modifier = Modifier) {
    val color = when {
        type.name.startsWith("PAYMENT") || type.name.startsWith("PAYABLE") -> VcAccentAmber
        type.name.startsWith("QUALITY") || type.name.startsWith("RETURN") -> VcAccentRed
        type.name.startsWith("DELIVERY") || type.name.startsWith("RECEIVING") -> Color(0xFF60A5FA)
        type.name.startsWith("PURCHASE") || type.name.startsWith("SUPPLY") -> VcAccent
        type.name.startsWith("DOCUMENT") -> VcAccentPurple
        else -> VcTextSecondary
    }
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp), modifier = modifier) {
        Text(
            text = type.defaultLabel,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// =========================================================================
// VendorCommunicationPriorityChip
// =========================================================================

@Composable
fun VendorCommunicationPriorityChip(priority: NotificationPriority, modifier: Modifier = Modifier) {
    val (color, label) = when (priority) {
        NotificationPriority.LOW -> Pair(VcTextSecondary, "Low")
        NotificationPriority.NORMAL -> Pair(VcAccent, "Normal")
        NotificationPriority.HIGH -> Pair(VcAccentAmber, "High")
        NotificationPriority.URGENT -> Pair(VcAccentRed, "Urgent")
    }
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp), modifier = modifier) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

// =========================================================================
// VendorCommunicationSummaryCard
// =========================================================================

@Composable
fun VendorCommunicationSummaryCard(
    label: String,
    count: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = VcSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = "$count", color = accentColor, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = label, color = VcTextSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

// =========================================================================
// VendorCommunicationHistoryRow
// =========================================================================

@Composable
fun VendorCommunicationHistoryRow(history: VendorCommunicationHistory, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(VcSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.History, contentDescription = null, tint = VcAccent, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = history.action, color = VcTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                history.previousStatus?.let { prev ->
                    Text(text = prev.defaultLabel, color = VcTextSecondary, fontSize = 11.sp)
                    Text(text = "→", color = VcAccent, fontSize = 11.sp)
                }
                Text(text = history.newStatus.defaultLabel, color = VcAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(text = "By: ${history.performedBy}", color = VcTextSecondary, fontSize = 10.sp)
        }
    }
}

// =========================================================================
// VendorCommunicationEngagementEventRow
// =========================================================================

@Composable
fun VendorCommunicationEngagementEventRow(event: VendorEngagementEvent, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val color = when (event.eventType) {
            VendorEngagementEventType.ACKNOWLEDGED -> VcAccentGreen
            VendorEngagementEventType.DECLINED -> VcAccentRed
            VendorEngagementEventType.READ -> VcAccent
            else -> VcTextSecondary
        }
        Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
            Text(
                text = event.eventType.defaultLabel,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Text(text = "Actor: ${event.actorId}", color = VcTextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
    }
}

// =========================================================================
// VendorEngagementStatRow
// =========================================================================

@Composable
fun VendorEngagementStatRow(label: String, value: String, accentColor: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = VcTextSecondary, fontSize = 13.sp)
        Text(text = value, color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

// =========================================================================
// EmptyVendorCommunicationState
// =========================================================================

@Composable
fun EmptyVendorCommunicationState(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.MarkEmailUnread, contentDescription = null, tint = VcTextSecondary, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = message, color = VcTextSecondary, fontSize = 14.sp)
        }
    }
}
