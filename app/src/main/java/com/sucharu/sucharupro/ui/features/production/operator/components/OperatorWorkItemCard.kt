package com.sucharu.sucharupro.ui.features.production.operator.components

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
import androidx.compose.material.icons.filled.Person
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
import com.sucharu.sucharupro.domain.model.job.StageAssignmentStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.ui.features.production.operator.OperatorWorkItem
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Interactive card displaying an assigned production stage, parent Job summary,
 * assignment metadata, and progress status in the Operator Work Queue.
 */
@Composable
fun OperatorWorkItemCard(
    workItem: OperatorWorkItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stage = workItem.stage
    val job = workItem.job
    val assignment = workItem.assignment

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
            // Header: Stage Name + Stage Status Badge + Priority Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${stage.sequence}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))

                    Text(
                        text = "${stage.stageType.defaultLabel} (${stage.stageType.shortCode})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

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

                    // Stage Status Badge
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = when (stage.status) {
                            ProductionStageStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                            ProductionStageStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiaryContainer
                            ProductionStageStatus.SKIPPED -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = stage.status.defaultLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

            // Job Title & Job #
            Text(
                text = job.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Job: ${job.jobNumber} | Order: ${job.orderNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${job.quantity} ${job.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            // Operator & Assignment Info
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Assigned Operator",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = assignment.operatorName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = when (assignment.status) {
                        StageAssignmentStatus.ASSIGNED -> MaterialTheme.colorScheme.surfaceVariant
                        StageAssignmentStatus.REASSIGNED -> MaterialTheme.colorScheme.errorContainer
                        StageAssignmentStatus.UNASSIGNED -> MaterialTheme.colorScheme.errorContainer
                        StageAssignmentStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                        StageAssignmentStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = assignment.status.defaultLabel,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            // Progress bar
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            LinearProgressIndicator(
                progress = { job.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = MaterialTheme.colorScheme.primary
            )

            // Footer
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Assigned: ${assignment.assignedAt.take(16).replace('T', ' ')}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Open Job Details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
