package com.sucharu.sucharupro.ui.features.production.job.list.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Interactive card displaying summary metadata, lifecycle status, current stage,
 * and progress fraction for a single Production Job in the Job Queue.
 */
@Composable
fun ProductionJobCard(
    job: ProductionJob,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium)
        ) {
            // Header: Job Number + Status Badge + Priority Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = job.jobNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Priority Badge
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = when (job.priority) {
                            OrderPriority.URGENT -> MaterialTheme.colorScheme.errorContainer
                            OrderPriority.HIGH -> MaterialTheme.colorScheme.tertiaryContainer
                            OrderPriority.NORMAL -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = job.priority.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Status Badge
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = when (job.status) {
                            ProductionJobStatus.READY_FOR_PRODUCTION -> MaterialTheme.colorScheme.primaryContainer
                            ProductionJobStatus.IN_PROGRESS -> MaterialTheme.colorScheme.secondaryContainer
                            ProductionJobStatus.ON_HOLD -> MaterialTheme.colorScheme.errorContainer
                            ProductionJobStatus.READY -> MaterialTheme.colorScheme.tertiaryContainer
                            ProductionJobStatus.DELIVERED -> MaterialTheme.colorScheme.primaryContainer
                            ProductionJobStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant
                            ProductionJobStatus.DRAFT -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = job.status.defaultLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

            // Main: Title
            Text(
                text = job.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

            // Order & Quantity Information
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Order: ${job.orderNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${job.quantity} ${job.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

            // Progress Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentStageName = job.currentStage?.stageType?.defaultLabel ?: "Delivered"
                    Text(
                        text = "Stage: $currentStageName",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "${job.completedStagesCount}/13 (${(job.progressFraction * 100).toInt()}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

                LinearProgressIndicator(
                    progress = { job.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = when (job.status) {
                        ProductionJobStatus.READY, ProductionJobStatus.DELIVERED -> MaterialTheme.colorScheme.primary
                        ProductionJobStatus.ON_HOLD -> MaterialTheme.colorScheme.error
                        ProductionJobStatus.CANCELLED -> MaterialTheme.colorScheme.outline
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }

            // Delivery Requirement & Arrow Footer
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val deliveryAddress = job.deliveryRequirement?.address
                Text(
                    text = if (deliveryAddress != null) {
                        "Delivery: $deliveryAddress"
                    } else {
                        "Created: ${job.createdAt.take(10)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "View Details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
