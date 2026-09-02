package com.sucharu.sucharupro.ui.features.orders.order.details.components

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
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.order.JobHandoffStatus
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppOutlinedButton
import com.sucharu.sucharupro.ui.features.orders.components.DetailSectionCard
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Operational lifecycle actions card on the Order Details screen.
 * Displays contextual commercial actions based on the current order status.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrderOperationalActionsCard(
    order: Order,
    isActionInProgress: Boolean,
    onConfirmOrderClick: () -> Unit,
    onMarkReadyForJobClick: () -> Unit,
    onSetPriorityClick: () -> Unit,
    onPutOnHoldClick: () -> Unit,
    onResumeOrderClick: () -> Unit,
    onCancelOrderClick: () -> Unit,
    onEditNotesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DetailSectionCard(
        title = "Operational Controls & Lifecycle",
        icon = Icons.Default.Timeline,
        modifier = modifier
    ) {
        when (order.status) {
            OrderStatusType.PENDING -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Order is in PENDING state awaiting formal customer confirmation or deposit.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        AppButton(
                            text = "Confirm Order",
                            onClick = onConfirmOrderClick,
                            leadingIcon = {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            enabled = !isActionInProgress
                        )

                        AppOutlinedButton(
                            text = "Set Priority (${order.priority.defaultLabel})",
                            onClick = onSetPriorityClick,
                            leadingIcon = {
                                Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            enabled = !isActionInProgress
                        )

                        AppOutlinedButton(
                            text = "Edit Remarks",
                            onClick = onEditNotesClick,
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            enabled = !isActionInProgress
                        )

                        AppOutlinedButton(
                            text = "Cancel Order",
                            onClick = onCancelOrderClick,
                            leadingIcon = {
                                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            enabled = !isActionInProgress
                        )
                    }
                }
            }

            OrderStatusType.CONFIRMED -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (order.jobHandoffStatus == JobHandoffStatus.READY_FOR_JOB) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(MaterialTheme.spacing.medium),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Handyman,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                                Column {
                                    Text(
                                        text = "Ready for Job Handoff",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Commercial requirements complete. Eligible for Module 04 Job Card creation.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Commercial order is confirmed. When specifications and customer approvals are verified, mark ready for production handoff.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        if (order.jobHandoffStatus == JobHandoffStatus.NOT_READY) {
                            AppButton(
                                text = "Mark Ready for Job",
                                onClick = onMarkReadyForJobClick,
                                leadingIcon = {
                                    Icon(Icons.Default.Handyman, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                enabled = !isActionInProgress
                            )
                        }

                        AppOutlinedButton(
                            text = "Priority: ${order.priority.defaultLabel}",
                            onClick = onSetPriorityClick,
                            leadingIcon = {
                                Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            enabled = !isActionInProgress
                        )

                        AppOutlinedButton(
                            text = "Put On Hold",
                            onClick = onPutOnHoldClick,
                            leadingIcon = {
                                Icon(Icons.Default.PauseCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            enabled = !isActionInProgress
                        )

                        AppOutlinedButton(
                            text = "Edit Remarks",
                            onClick = onEditNotesClick,
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            enabled = !isActionInProgress
                        )

                        AppOutlinedButton(
                            text = "Cancel Order",
                            onClick = onCancelOrderClick,
                            leadingIcon = {
                                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            enabled = !isActionInProgress
                        )
                    }
                }
            }

            OrderStatusType.ON_HOLD -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Order is currently ON HOLD. Resume order when client instructions or requirements are resolved.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        AppButton(
                            text = "Resume Order",
                            onClick = onResumeOrderClick,
                            leadingIcon = {
                                Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            enabled = !isActionInProgress
                        )

                        AppOutlinedButton(
                            text = "Priority: ${order.priority.defaultLabel}",
                            onClick = onSetPriorityClick,
                            leadingIcon = {
                                Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            enabled = !isActionInProgress
                        )

                        AppOutlinedButton(
                            text = "Edit Remarks",
                            onClick = onEditNotesClick,
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            enabled = !isActionInProgress
                        )

                        AppOutlinedButton(
                            text = "Cancel Order",
                            onClick = onCancelOrderClick,
                            leadingIcon = {
                                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            enabled = !isActionInProgress
                        )
                    }
                }
            }

            OrderStatusType.IN_PRODUCTION -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Order is in active production. Commercial snapshot is locked. Factory workflow stages are tracked in production management.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        AppOutlinedButton(
                            text = "Priority: ${order.priority.defaultLabel}",
                            onClick = onSetPriorityClick,
                            leadingIcon = {
                                Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            enabled = !isActionInProgress
                        )

                        AppOutlinedButton(
                            text = "Edit Remarks",
                            onClick = onEditNotesClick,
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            enabled = !isActionInProgress
                        )
                    }
                }
            }

            OrderStatusType.READY -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Production is completed. Order is ready for dispatch or client pickup.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                    AppOutlinedButton(
                        text = "Edit Remarks",
                        onClick = onEditNotesClick,
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        enabled = !isActionInProgress
                    )
                }
            }

            OrderStatusType.DELIVERED -> {
                Text(
                    text = "Order has been delivered to customer (Terminal State). Read-only commercial archive.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OrderStatusType.CANCELLED -> {
                Text(
                    text = "Order is CANCELLED. All historical snapshots remain permanently archived as read-only.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
