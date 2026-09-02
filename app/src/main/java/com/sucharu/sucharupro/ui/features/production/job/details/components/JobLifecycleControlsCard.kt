package com.sucharu.sucharupro.ui.features.production.job.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PrecisionManufacturing
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
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppOutlinedButton
import com.sucharu.sucharupro.ui.features.orders.components.DetailSectionCard
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Operational controls and overall lifecycle card for a Production Job.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JobLifecycleControlsCard(
    job: ProductionJob,
    isActionInProgress: Boolean,
    onHoldClick: () -> Unit,
    onResumeClick: () -> Unit,
    onCancelClick: () -> Unit,
    onMarkReadyClick: () -> Unit,
    onDeliverClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DetailSectionCard(
        title = "Job Lifecycle & Progress",
        icon = Icons.Default.PrecisionManufacturing,
        modifier = modifier
    ) {
        // Overall Progress Bar
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Overall Progress",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${job.completedStagesCount} of ${job.stages.size} Stages (${(job.progressFraction * 100).toInt()}%)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

            LinearProgressIndicator(
                progress = { job.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when (job.status) {
                    ProductionJobStatus.READY, ProductionJobStatus.DELIVERED -> MaterialTheme.colorScheme.primary
                    ProductionJobStatus.ON_HOLD -> MaterialTheme.colorScheme.error
                    ProductionJobStatus.CANCELLED -> MaterialTheme.colorScheme.outline
                    else -> MaterialTheme.colorScheme.primary
                }
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        // Status & Priority Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
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
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = when (job.status) {
                        ProductionJobStatus.ON_HOLD -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            Surface(
                shape = MaterialTheme.shapes.small,
                color = when (job.priority) {
                    OrderPriority.URGENT -> MaterialTheme.colorScheme.errorContainer
                    OrderPriority.HIGH -> MaterialTheme.colorScheme.tertiaryContainer
                    OrderPriority.NORMAL -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = "Priority: ${job.priority.name}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        // Contextual Lifecycle Action Buttons
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            when (job.status) {
                ProductionJobStatus.READY_FOR_PRODUCTION,
                ProductionJobStatus.IN_PROGRESS -> {
                    AppOutlinedButton(
                        text = "Hold Job",
                        onClick = onHoldClick,
                        enabled = !isActionInProgress,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PauseCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )

                    // Ready button enabled when all stages through sequence 12 are completed
                    val canMarkReady = job.stages.filter { it.sequence < 13 }.all {
                        it.status == com.sucharu.sucharupro.domain.model.production.ProductionStageStatus.COMPLETED ||
                                it.status == com.sucharu.sucharupro.domain.model.production.ProductionStageStatus.SKIPPED
                    }
                    if (canMarkReady) {
                        AppButton(
                            text = "Mark Ready",
                            onClick = onMarkReadyClick,
                            enabled = !isActionInProgress,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }

                ProductionJobStatus.ON_HOLD -> {
                    AppButton(
                        text = "Resume Job",
                        onClick = onResumeClick,
                        enabled = !isActionInProgress,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }

                ProductionJobStatus.READY -> {
                    AppButton(
                        text = "Deliver Job",
                        onClick = onDeliverClick,
                        enabled = !isActionInProgress,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )

                    AppOutlinedButton(
                        text = "Hold Job",
                        onClick = onHoldClick,
                        enabled = !isActionInProgress,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PauseCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }

                ProductionJobStatus.DELIVERED -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
                        Text(
                            text = "Job is delivered (Terminal State).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                ProductionJobStatus.CANCELLED -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
                        Text(
                            text = "Job is cancelled (Terminal State).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                ProductionJobStatus.DRAFT -> Unit
            }

            // Cancel Job action for non-terminal jobs
            if (!job.status.isTerminal) {
                AppOutlinedButton(
                    text = "Cancel Job",
                    onClick = onCancelClick,
                    enabled = !isActionInProgress,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}
