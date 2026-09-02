package com.sucharu.sucharupro.ui.features.production.job.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.job.ProductionActivityEvent
import com.sucharu.sucharupro.domain.model.job.ProductionActivityType
import com.sucharu.sucharupro.ui.features.orders.components.DetailSectionCard
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Visual timeline card rendering chronological production activities, stage executions,
 * hold/resume events, and operator attribution on a Production Job Card.
 */
@Composable
fun ProductionActivityTimeline(
    activities: List<ProductionActivityEvent>,
    modifier: Modifier = Modifier
) {
    DetailSectionCard(
        title = "Production Activity Timeline",
        icon = Icons.Default.History,
        modifier = modifier
    ) {
        if (activities.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.medium),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No production activity recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                activities.forEachIndexed { index, event ->
                    ActivityEventRow(event = event)
                    if (index < activities.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityEventRow(
    event: ProductionActivityEvent,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Event Icon Badge
        Surface(
            shape = MaterialTheme.shapes.small,
            color = when (event.eventType) {
                ProductionActivityType.STAGE_STARTED -> MaterialTheme.colorScheme.primaryContainer
                ProductionActivityType.STAGE_COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                ProductionActivityType.STAGE_ASSIGNED -> MaterialTheme.colorScheme.secondaryContainer
                ProductionActivityType.STAGE_REASSIGNED -> MaterialTheme.colorScheme.tertiaryContainer
                ProductionActivityType.STAGE_UNASSIGNED -> MaterialTheme.colorScheme.errorContainer
                ProductionActivityType.STAGE_SKIPPED -> MaterialTheme.colorScheme.surfaceVariant
                ProductionActivityType.STAGE_EXECUTION_NOTE -> MaterialTheme.colorScheme.surfaceVariant
                ProductionActivityType.JOB_HELD -> MaterialTheme.colorScheme.errorContainer
                ProductionActivityType.JOB_RESUMED -> MaterialTheme.colorScheme.tertiaryContainer
                ProductionActivityType.JOB_CANCELLED -> MaterialTheme.colorScheme.errorContainer
                ProductionActivityType.JOB_READY -> MaterialTheme.colorScheme.primaryContainer
                ProductionActivityType.JOB_DELIVERED -> MaterialTheme.colorScheme.primaryContainer
                ProductionActivityType.STAGE_OUTPUT_RECORDED -> MaterialTheme.colorScheme.secondaryContainer
            },
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = when (event.eventType) {
                        ProductionActivityType.STAGE_STARTED -> Icons.Default.PlayArrow
                        ProductionActivityType.STAGE_COMPLETED -> Icons.Default.CheckCircle
                        ProductionActivityType.STAGE_ASSIGNED -> Icons.Default.AssignmentInd
                        ProductionActivityType.STAGE_REASSIGNED -> Icons.Default.SwapHoriz
                        ProductionActivityType.STAGE_UNASSIGNED -> Icons.Default.Person
                        ProductionActivityType.STAGE_SKIPPED -> Icons.Default.SkipNext
                        ProductionActivityType.STAGE_EXECUTION_NOTE -> Icons.Default.Comment
                        ProductionActivityType.JOB_HELD -> Icons.Default.PauseCircle
                        ProductionActivityType.JOB_RESUMED -> Icons.Default.PlayCircle
                        ProductionActivityType.JOB_CANCELLED -> Icons.Default.Cancel
                        ProductionActivityType.JOB_READY -> Icons.Default.Done
                        ProductionActivityType.JOB_DELIVERED -> Icons.Default.CheckCircleOutline
                        ProductionActivityType.STAGE_OUTPUT_RECORDED -> Icons.Default.CheckCircle
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.eventType.defaultLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = event.timestamp.take(16).replace('T', ' '),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            val stageType = event.stageType
            if (stageType != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Stage: ${stageType.defaultLabel} (${stageType.shortCode})",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (event.operatorName != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Operator: ${event.operatorName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            val msg = event.message
            if (!msg.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
