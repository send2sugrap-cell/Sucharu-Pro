package com.sucharu.sucharupro.ui.features.customerfinancial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.data.api.model.CollectionQueueItemDto
import com.sucharu.sucharupro.data.api.model.CustomerCollectionActionDto
import com.sucharu.sucharupro.data.api.model.CustomerPaymentPromiseDto
import com.sucharu.sucharupro.data.api.model.CustomerReceivableCollectionSummaryDto
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.theme.spacing
import java.math.BigDecimal

/**
 * Customer Payment Due Scheduling & Collection Management Screen (Module 14 Step 08).
 */
@Composable
fun CustomerCollectionManagementScreen(
    summary: CustomerReceivableCollectionSummaryDto?,
    queue: List<CollectionQueueItemDto>,
    actions: List<CustomerCollectionActionDto>,
    promises: List<CustomerPaymentPromiseDto>,
    onCreateActionClick: () -> Unit = {},
    onPromiseClick: () -> Unit = {},
    onActionClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        item {
            Text(
                text = "Receivable Collection & Due Scheduling",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // --- KPI Cards Grid ---
        item {
            summary?.let {
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        CollectionKpiCard(
                            title = "Total Outstanding",
                            value = "৳ ${it.totalOutstanding}",
                            subtitle = "Canonical Ledger Due",
                            icon = Icons.Default.DateRange,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        CollectionKpiCard(
                            title = "Due Today",
                            value = "৳ ${it.dueTodayAmount}",
                            subtitle = "Immediate Follow-up",
                            icon = Icons.Default.CalendarToday,
                            color = Color(0xFFF57C00),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        CollectionKpiCard(
                            title = "Total Overdue",
                            value = "৳ ${it.overdueAmount}",
                            subtitle = "${it.overdueInvoiceCount} Overdue Invoices",
                            icon = Icons.Default.Warning,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        CollectionKpiCard(
                            title = "Payment Promises",
                            value = "৳ ${it.activePromisedAmount}",
                            subtitle = "${it.activePromiseCount} Active Promises",
                            icon = Icons.Default.Handshake,
                            color = Color(0xFF388E3C),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // --- Action Buttons ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                Button(
                    onClick = onCreateActionClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Schedule Follow-Up")
                }

                OutlinedButton(
                    onClick = onPromiseClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Handshake, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Record Promise")
                }
            }
        }

        // --- Collection Queue Section ---
        if (queue.isNotEmpty()) {
            item {
                Text(
                    text = "Operational Collection Queue",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(queue) { item ->
                CollectionQueueCard(item = item, onClick = { item.latestActionId?.let(onActionClick) })
            }
        }

        // --- Recent Actions History Section ---
        if (actions.isNotEmpty()) {
            item {
                Text(
                    text = "Collection Follow-up Actions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(actions) { action ->
                CollectionActionHistoryCard(action = action)
            }
        }
    }
}

@Composable
private fun CollectionKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CollectionQueueCard(
    item: CollectionQueueItemDto,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = item.customerDisplayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Code: ${item.customerCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                PriorityBadge(priority = item.priority)
            }

            Spacer(Modifier.height(MaterialTheme.spacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Outstanding: ৳ ${item.totalOutstanding}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Overdue: ৳ ${item.overdueAmount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Aging: ${item.agingBucket} (${item.maxDaysOverdue} days) | Status: ${item.creditRiskStatus}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CollectionActionHistoryCard(
    action: CustomerCollectionActionDto
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${action.actionType} (${action.status})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Priority: ${action.priority}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            action.outcome?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Outcome: $it - ${action.outcomeNotes ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2E7D32)
                )
            }

            action.notes?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Notes: $it",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun PriorityBadge(priority: String) {
    val (bgColor, textColor) = when (priority) {
        "CRITICAL" -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        "HIGH" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        "NORMAL" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        else -> Color(0xFFF5F5F5) to Color(0xFF616161)
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = priority,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
