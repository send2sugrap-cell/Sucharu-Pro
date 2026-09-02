package com.sucharu.sucharupro.ui.features.delivery.challan

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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallan
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanStatus
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeliveryChallanStatusBadge(
    status: DeliveryChallanStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        DeliveryChallanStatus.DRAFT -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        DeliveryChallanStatus.PENDING -> Color(0xFFFFF9C4) to Color(0xFFF57F17)
        DeliveryChallanStatus.APPROVED -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        DeliveryChallanStatus.READY_FOR_DISPATCH -> Color(0xFFE0F7FA) to Color(0xFF00838F)
        DeliveryChallanStatus.DISPATCHED -> Color(0xFFEDE7F6) to Color(0xFF4527A0)
        DeliveryChallanStatus.DELIVERED -> Color(0xFFE8F8F5) to Color(0xFF00695C)
        DeliveryChallanStatus.CANCELLED -> Color(0xFFF5F5F5) to Color(0xFF757575)
    }

    Box(
        modifier = modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.name.replace("_", " "),
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun DeliveryChallanTypeBadge(
    type: DeliveryChallanType,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(color = Color(0xFFF3E5F5), shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = type.name.replace("_", " "),
            color = Color(0xFF6A1B9A),
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun DeliveryChallanSummaryCard(
    challan: DeliveryChallan,
    lineCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

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
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = challan.challanNo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                DeliveryChallanStatusBadge(status = challan.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DO Ref: ${challan.deliveryOrderId.take(8)}... • Items: $lineCount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DeliveryChallanTypeBadge(type = challan.challanType)
            }

            if (challan.customerId != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Customer: ${challan.customerId}",
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = "Issued: ${dateFormat.format(Date(challan.issueDate))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
