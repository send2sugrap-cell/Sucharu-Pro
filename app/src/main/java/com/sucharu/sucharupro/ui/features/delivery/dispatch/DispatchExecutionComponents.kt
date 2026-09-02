package com.sucharu.sucharupro.ui.features.delivery.dispatch

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
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionStatus
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DispatchExecutionStatusBadge(
    status: DispatchExecutionStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        DispatchExecutionStatus.DRAFT -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        DispatchExecutionStatus.PENDING -> Color(0xFFFFF9C4) to Color(0xFFF57F17)
        DispatchExecutionStatus.APPROVED -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        DispatchExecutionStatus.READY_FOR_EXECUTION -> Color(0xFFE0F7FA) to Color(0xFF00838F)
        DispatchExecutionStatus.EXECUTING -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        DispatchExecutionStatus.DISPATCHED -> Color(0xFFEDE7F6) to Color(0xFF4527A0)
        DispatchExecutionStatus.CANCELLED -> Color(0xFFF5F5F5) to Color(0xFF757575)
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
fun DispatchExecutionTypeBadge(
    type: DispatchExecutionType,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(color = Color(0xFFE0F2F1), shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = type.defaultLabel,
            color = Color(0xFF00695C),
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun DispatchExecutionSummaryCard(
    dispatch: DispatchExecution,
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
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dispatch.dispatchNo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                DispatchExecutionStatusBadge(status = dispatch.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Challan: ${dispatch.deliveryChallanId.take(8)}... • Items: $lineCount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DispatchExecutionTypeBadge(type = dispatch.dispatchType)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Warehouse: ${dispatch.sourceWarehouseId} • Loc: ${dispatch.sourceLocationId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

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
                        text = "Date: ${dateFormat.format(Date(dispatch.dispatchDate))}",
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
