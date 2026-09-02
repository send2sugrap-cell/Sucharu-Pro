package com.sucharu.sucharupro.ui.features.production.job.details.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppOutlinedButton
import com.sucharu.sucharupro.ui.features.orders.components.DetailSectionCard
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * 13-Stage Production Timeline card rendering each stage's status, assigned operator,
 * sequence, and operational controls.
 */
@Composable
fun ProductionStageTimelineCard(
    job: ProductionJob,
    isActionInProgress: Boolean,
    onStartStageClick: (stageId: String) -> Unit,
    onCompleteStageClick: (stageId: String) -> Unit,
    onSkipStageClick: (stageId: String) -> Unit,
    onAssignOperatorClick: ((stage: ProductionJobStage) -> Unit)? = null,
    onReassignOperatorClick: ((stage: ProductionJobStage) -> Unit)? = null,
    onUnassignOperatorClick: ((stage: ProductionJobStage) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    DetailSectionCard(
        title = "13-Stage Production Timeline",
        icon = Icons.Default.Timeline,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            job.stages.forEach { stage ->
                val canStart = stage.status == ProductionStageStatus.PENDING &&
                        job.status != ProductionJobStatus.ON_HOLD &&
                        !job.status.isTerminal &&
                        job.stages.filter { it.sequence < stage.sequence }.all {
                            it.status == ProductionStageStatus.COMPLETED || it.status == ProductionStageStatus.SKIPPED
                        }

                val canComplete = stage.status == ProductionStageStatus.IN_PROGRESS &&
                        !job.status.isTerminal

                val canSkip = stage.stageType.canBeSkipped &&
                        stage.status == ProductionStageStatus.PENDING &&
                        canStart

                val canAssign = !job.status.isTerminal &&
                        stage.status != ProductionStageStatus.COMPLETED &&
                        stage.status != ProductionStageStatus.SKIPPED

                StageRowItem(
                    stage = stage,
                    canStart = canStart,
                    canComplete = canComplete,
                    canSkip = canSkip,
                    canAssign = canAssign,
                    isActionInProgress = isActionInProgress,
                    onStartClick = { onStartStageClick(stage.stageId) },
                    onCompleteClick = { onCompleteStageClick(stage.stageId) },
                    onSkipClick = { onSkipStageClick(stage.stageId) },
                    onAssignClick = { onAssignOperatorClick?.invoke(stage) },
                    onReassignClick = { onReassignOperatorClick?.invoke(stage) },
                    onUnassignClick = { onUnassignOperatorClick?.invoke(stage) }
                )
            }
        }
    }
}

@Composable
private fun StageRowItem(
    stage: ProductionJobStage,
    canStart: Boolean,
    canComplete: Boolean,
    canSkip: Boolean,
    canAssign: Boolean,
    isActionInProgress: Boolean,
    onStartClick: () -> Unit,
    onCompleteClick: () -> Unit,
    onSkipClick: () -> Unit,
    onAssignClick: () -> Unit,
    onReassignClick: () -> Unit,
    onUnassignClick: () -> Unit
) {
    val isCurrent = stage.status == ProductionStageStatus.IN_PROGRESS

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isCurrent) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sequence & Stage Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
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
                            text = "${stage.sequence}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))

                    Text(
                        text = stage.stageType.defaultLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                    )

                    if (stage.stageType.isQcStage) {
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "QC Point",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                // Status Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (stage.status) {
                            ProductionStageStatus.COMPLETED -> Icons.Default.CheckCircle
                            ProductionStageStatus.IN_PROGRESS -> Icons.Default.PlayCircle
                            ProductionStageStatus.SKIPPED -> Icons.Default.SkipNext
                            else -> Icons.Default.HourglassEmpty
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = when (stage.status) {
                            ProductionStageStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                            ProductionStageStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
                    Text(
                        text = stage.status.defaultLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = when (stage.status) {
                            ProductionStageStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                            ProductionStageStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // Operator Assignment Row
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (stage.assignedUserName != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Assigned Operator",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Operator: ${stage.assignedUserName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (canAssign) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = onReassignClick,
                                enabled = !isActionInProgress
                            ) {
                                Text("Reassign", style = MaterialTheme.typography.labelSmall)
                            }
                            if (stage.status == ProductionStageStatus.PENDING) {
                                TextButton(
                                    onClick = onUnassignClick,
                                    enabled = !isActionInProgress
                                ) {
                                    Text("Unassign", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Operator: Unassigned",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    if (canAssign) {
                        TextButton(
                            onClick = onAssignClick,
                            enabled = !isActionInProgress
                        ) {
                            Text("Assign Operator", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Timestamps or Notes
            if (stage.startedAt != null || stage.completedAt != null || stage.notes != null) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (stage.startedAt != null) {
                        Text(
                            text = "Started: ${stage.startedAt}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (stage.completedAt != null) {
                        Text(
                            text = "Completed: ${stage.completedAt}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (stage.notes != null) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                Text(
                    text = "Notes: ${stage.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action Buttons
            if (canStart || canComplete || canSkip) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canSkip) {
                        AppOutlinedButton(
                            text = "Skip",
                            onClick = onSkipClick,
                            enabled = !isActionInProgress
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                    }

                    if (canStart) {
                        AppButton(
                            text = "Start Stage",
                            onClick = onStartClick,
                            enabled = !isActionInProgress,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }

                    if (canComplete) {
                        AppButton(
                            text = "Complete Stage",
                            onClick = onCompleteClick,
                            enabled = !isActionInProgress,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
