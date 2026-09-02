package com.sucharu.sucharupro.ui.features.production.job.details.components

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.job.ProductionStageExecution
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.domain.validation.ProductionStageOutputValidator
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.features.orders.components.DetailSectionCard
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Workspace card focusing on the active/current production stage execution state,
 * showing operator assignment, runtime duration, execution remarks, output quantity progress, and actions.
 */
@Composable
fun StageExecutionWorkspaceCard(
    job: ProductionJob,
    currentStage: ProductionJobStage?,
    currentExecution: ProductionStageExecution?,
    totalOutputQuantity: Int = 0,
    remainingQuantity: Int = job.quantity,
    isActionInProgress: Boolean,
    onStartStageClick: (stageId: String) -> Unit,
    onCompleteStageClick: (stageId: String) -> Unit,
    onRecordOutputClick: (stageId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (currentStage == null) return

    val progressFraction = ProductionStageOutputValidator.calculateProgressFraction(
        totalOutput = totalOutputQuantity,
        plannedQuantity = job.quantity
    )

    DetailSectionCard(
        title = "Stage Execution Workspace",
        icon = Icons.Default.Engineering,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            // Stage Name and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currentStage.sequence}. ${currentStage.stageType.defaultLabel} (${currentStage.stageType.shortCode})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = when (currentStage.status) {
                        ProductionStageStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiaryContainer
                        ProductionStageStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                        ProductionStageStatus.SKIPPED -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = currentStage.status.defaultLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Operator Attribution
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
                Text(
                    text = if (currentStage.assignedUserName != null) {
                        "Assigned Operator: ${currentStage.assignedUserName}"
                    } else {
                        "Operator: Unassigned"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            // Output Quantity Progress Section
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.spacing.small),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Quantity Progress",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${(progressFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Planned: ${job.quantity} ${job.unit}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Produced: $totalOutputQuantity ${job.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Remaining: $remainingQuantity ${job.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Start / Completed Timestamps & Duration
            val startedAt = currentStage.startedAt
            if (startedAt != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Started: ${startedAt.take(16).replace('T', ' ')}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (currentExecution?.formattedDuration != null) {
                        Text(
                            text = "Duration: ${currentExecution.formattedDuration}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            val completedAt = currentStage.completedAt
            if (completedAt != null) {
                Text(
                    text = "Completed: ${completedAt.take(16).replace('T', ' ')}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Start / Completion Remarks
            if (currentExecution?.startRemarks != null) {
                Text(
                    text = "Start Remarks: ${currentExecution.startRemarks}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (currentExecution?.completionRemarks != null) {
                Text(
                    text = "Completion Remarks: ${currentExecution.completionRemarks}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action Buttons
            val canStart = currentStage.status == ProductionStageStatus.PENDING &&
                    job.status != ProductionJobStatus.ON_HOLD &&
                    !job.status.isTerminal &&
                    job.stages.filter { it.sequence < currentStage.sequence }.all {
                        it.status == ProductionStageStatus.COMPLETED || it.status == ProductionStageStatus.SKIPPED
                    }

            val canRecordOutput = currentStage.status == ProductionStageStatus.IN_PROGRESS &&
                    !job.status.isTerminal &&
                    remainingQuantity > 0

            val canComplete = currentStage.status == ProductionStageStatus.IN_PROGRESS &&
                    !job.status.isTerminal

            if (canStart || canRecordOutput || canComplete) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canStart) {
                        AppButton(
                            text = "Start Stage",
                            onClick = { onStartStageClick(currentStage.stageId) },
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

                    if (canRecordOutput) {
                        OutlinedButton(
                            onClick = { onRecordOutputClick(currentStage.stageId) },
                            enabled = !isActionInProgress
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Record Output")
                        }
                    }

                    if (canComplete) {
                        AppButton(
                            text = "Complete Stage",
                            onClick = { onCompleteStageClick(currentStage.stageId) },
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
