package com.sucharu.sucharupro.ui.features.finance.receivable

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.History
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.finance.CustomerDueSummary
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivable
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivableActivityEvent
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivableStatus
import com.sucharu.sucharupro.domain.model.finance.ReceivableAgingBucket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomerReceivableStatusBadge(status: CustomerReceivableStatus) {
    val (bgColor, textColor) = when (status) {
        CustomerReceivableStatus.OPEN -> Color(0xFFEFF6FF) to Color(0xFF1D4ED8)
        CustomerReceivableStatus.PARTIALLY_SETTLED -> Color(0xFFFEF3C7) to Color(0xFF92400E)
        CustomerReceivableStatus.SETTLED -> Color(0xFFDCFCE7) to Color(0xFF166534)
        CustomerReceivableStatus.OVERDUE -> Color(0xFFFEE2E2) to Color(0xFF991B1B)
        CustomerReceivableStatus.CANCELLED -> Color(0xFFF1F5F9) to Color(0xFF475569)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.defaultLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor
        )
    }
}

@Composable
fun ReceivableAgingBadge(aging: ReceivableAgingBucket) {
    val (bgColor, textColor) = when (aging) {
        ReceivableAgingBucket.CURRENT -> Color(0xFFF0FDF4) to Color(0xFF15803D)
        ReceivableAgingBucket.DAYS_1_TO_30 -> Color(0xFFFEF9C3) to Color(0xFF854D0E)
        ReceivableAgingBucket.DAYS_31_TO_60 -> Color(0xFFFFEDD5) to Color(0xFF9A3412)
        ReceivableAgingBucket.DAYS_61_TO_90 -> Color(0xFFFEE2E2) to Color(0xFF991B1B)
        ReceivableAgingBucket.DAYS_OVER_90 -> Color(0xFF7F1D1D) to Color(0xFFFFFFFF)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = aging.defaultLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = textColor
        )
    }
}

@Composable
fun CustomerDueSummaryCard(summary: CustomerDueSummary, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalanceWallet,
                        contentDescription = "Due Summary",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Receivables & Due Summary",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Text(
                    text = "${summary.openReceivablesCount} Open / ${summary.overdueReceivablesCount} Overdue",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (summary.overdueReceivablesCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Total Outstanding Due", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = summary.totalOutstandingDue.formatted(),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Total Overdue Amount", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    Text(
                        text = summary.totalOverdueAmount.formatted(),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (summary.totalOverdueAmount.isPositive()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total Invoiced: ${summary.totalOriginalAmount.formatted()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Total Settled: ${summary.totalSettledAmount.formatted()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF166534)
                )
            }
        }
    }
}

@Composable
fun CustomerReceivableCard(
    receivable: CustomerReceivable,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dueStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(receivable.dueDate))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = receivable.receivableNo,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Customer: ${receivable.customerId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                CustomerReceivableStatusBadge(status = receivable.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Due: $dueStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (receivable.status == CustomerReceivableStatus.OVERDUE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                    )
                    ReceivableAgingBadge(aging = receivable.agingBucket)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Outstanding: ${receivable.outstandingAmount.formatted()}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Original: ${receivable.originalAmount.formatted()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ref: ${receivable.referenceType.name} #${receivable.referenceId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "View Details",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun CustomerReceivableActivityTimelineItem(event: CustomerReceivableActivityEvent) {
    val timeStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(event.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Filled.History,
            contentDescription = "Activity",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = event.activityType.defaultLabel,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = event.details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$timeStr • Actor: ${event.actorId}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
