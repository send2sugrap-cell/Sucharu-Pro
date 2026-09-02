package com.sucharu.sucharupro.ui.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.dashboard.DashboardJobSummary
import com.sucharu.sucharupro.domain.model.dashboard.WorkloadSummary
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.components.OrderStatusBadge
import com.sucharu.sucharupro.ui.components.PaymentStatusBadge
import com.sucharu.sucharupro.ui.components.SectionHeader
import com.sucharu.sucharupro.ui.theme.BadgeShape
import com.sucharu.sucharupro.ui.theme.spacing
import com.sucharu.sucharupro.ui.theme.statusColors

/**
 * Workload & Urgent Deliveries Section highlighting deadlines and priority jobs.
 */
@Composable
fun DashboardWorkloadSection(
    workload: WorkloadSummary,
    onJobClick: (String) -> Unit,
    onViewAllDueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalUrgentOrDue = (workload.priorityJobs + workload.dueTodayJobs).distinctBy { it.orderId }

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Today's Workload & Urgency",
            subtitle = "${totalUrgentOrDue.size} jobs scheduled for completion",
            actionText = "View All",
            onActionClick = onViewAllDueClick
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

        if (totalUrgentOrDue.isEmpty()) {
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(MaterialTheme.spacing.large)
            ) {
                Text(
                    text = "No urgent or pending deliveries scheduled for today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                totalUrgentOrDue.take(4).forEach { job ->
                    WorkloadJobCard(
                        job = job,
                        onClick = { onJobClick(job.orderId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkloadJobCard(
    job: DashboardJobSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColors = MaterialTheme.statusColors

    val cardBorderColor = when {
        job.isDelayed -> statusColors.orderCancelled.border
        job.isPriority -> statusColors.orderOnHold.border
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    AppCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor),
        contentPadding = PaddingValues(MaterialTheme.spacing.medium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = job.orderId,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                if (job.isPriority) {
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                    Box(
                        modifier = Modifier
                            .clip(BadgeShape)
                            .background(statusColors.orderOnHold.container)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = "Priority",
                                tint = statusColors.orderOnHold.content,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Rush Job",
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColors.orderOnHold.content,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (job.isDelayed) {
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                    Box(
                        modifier = Modifier
                            .clip(BadgeShape)
                            .background(statusColors.orderCancelled.container)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Delayed",
                                tint = statusColors.orderCancelled.content,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Delayed",
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColors.orderCancelled.content,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Deadline Time
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = if (job.isDelayed) statusColors.orderCancelled.content else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = job.deliveryDueTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (job.isDelayed) statusColors.orderCancelled.content else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Text(
            text = job.jobTitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "${job.customerName} • Qty: ${job.quantity}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)
            ) {
                OrderStatusBadge(status = job.jobStatus)
                PaymentStatusBadge(status = job.paymentStatus)
            }

            Text(
                text = job.totalAmount.formatted(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
