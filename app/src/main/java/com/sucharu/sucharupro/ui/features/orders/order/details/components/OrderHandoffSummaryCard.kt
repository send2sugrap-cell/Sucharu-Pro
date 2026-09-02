package com.sucharu.sucharupro.ui.features.orders.order.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.DoubleArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoff
import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoffStatus
import com.sucharu.sucharupro.domain.model.order.JobHandoffStatus
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Commercial Order → Job Handoff Summary Card.
 *
 * Shows the commercial readiness state of an Order, the sealed handoff snapshot record (if created),
 * and provides verified actions for advancing the commercial handoff boundary without crossing into Module 04 production execution.
 */
@Composable
fun OrderHandoffSummaryCard(
    order: Order,
    handoff: OrderJobHandoff? = null,
    onInitiateHandoff: () -> Unit = {},
    onConfirmHandoff: (String) -> Unit = {},
    onMarkReadyForProduction: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isHandoffReady = order.jobHandoffStatus == JobHandoffStatus.READY_FOR_JOB
    val isCancelled = order.status == OrderStatusType.CANCELLED || handoff?.handoffStatus == OrderJobHandoffStatus.CANCELLED
    val isDelivered = order.status == OrderStatusType.DELIVERED

    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {

            // ── Header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AssignmentTurnedIn,
                        contentDescription = null,
                        tint = if (handoff != null || isHandoffReady) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (handoff != null) "Job Handoff Snapshot" else "Handoff Readiness Summary",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                HandoffStatusBadge(
                    status = handoff?.handoffStatus,
                    isReady = isHandoffReady,
                    isCancelled = isCancelled
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // ── Snapshot Details (If Handoff Exists) ──
            if (handoff != null) {
                HandoffInfoRow(
                    label = "Handoff ID",
                    value = handoff.handoffId
                )
                HandoffInfoRow(
                    label = "Handoff Status",
                    value = handoff.handoffStatus.defaultLabel
                )
                HandoffInfoRow(
                    label = "Snapshot Total",
                    value = handoff.commercialTotal.formatted()
                )
                HandoffInfoRow(
                    label = "Items Snapshot",
                    value = "${handoff.itemCount} item(s), Qty: ${handoff.totalQuantity}"
                )
                if (!handoff.createdBy.isNullOrBlank()) {
                    HandoffInfoRow(
                        label = "Created By",
                        value = "${handoff.createdBy} (${handoff.createdAt.take(10)})",
                        icon = Icons.Default.Person
                    )
                }
                if (!handoff.confirmedBy.isNullOrBlank()) {
                    HandoffInfoRow(
                        label = "Confirmed By",
                        value = "${handoff.confirmedBy} (${handoff.confirmedAt?.take(10) ?: ""})",
                        icon = Icons.Default.CheckCircle
                    )
                }
                val jobRef = handoff.jobReferenceId
                if (!jobRef.isNullOrBlank()) {
                    HandoffInfoRow(
                        label = "Job Reference",
                        value = jobRef
                    )
                }
                val hNotes = handoff.notes
                if (!hNotes.isNullOrBlank()) {
                    HandoffInfoRow(
                        label = "Handoff Notes",
                        value = hNotes
                    )
                }
            } else {
                // ── Order Identification ──
                HandoffInfoRow(
                    label = "Order Number",
                    value = order.orderNumber,
                    icon = Icons.Default.ShoppingCart
                )
                HandoffInfoRow(
                    label = "Commercial Status",
                    value = order.status.defaultLabel
                )
                HandoffInfoRow(
                    label = "Priority",
                    value = order.priority.defaultLabel
                )
                HandoffInfoRow(
                    label = "Line Items",
                    value = "${order.items.size} item(s), Qty: ${order.totalQuantity}"
                )
                HandoffInfoRow(
                    label = "Order Total",
                    value = order.totalAmount.formatted()
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // ── Commercial Readiness Checklist ──
            Text(
                text = "Commercial Readiness Checks",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(2.dp))

            ReadinessCheckRow(
                label = "Customer Assigned",
                passed = order.customerId.isNotBlank()
            )
            ReadinessCheckRow(
                label = "Has Line Items",
                passed = order.items.isNotEmpty()
            )
            ReadinessCheckRow(
                label = "Positive Order Total",
                passed = !order.totalAmount.isNegative()
            )
            ReadinessCheckRow(
                label = "Not Cancelled",
                passed = !isCancelled
            )
            ReadinessCheckRow(
                label = "Job Handoff Ready",
                passed = isHandoffReady || handoff != null
            )

            // ── Action Buttons ──
            if (!isCancelled && !isDelivered) {
                Spacer(modifier = Modifier.height(4.dp))
                when {
                    handoff == null && isHandoffReady -> {
                        Button(
                            onClick = onInitiateHandoff,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Hand Off to Production")
                        }
                    }
                    handoff != null && handoff.handoffStatus == OrderJobHandoffStatus.READY_FOR_HANDOFF -> {
                        Button(
                            onClick = { onConfirmHandoff(handoff.handoffId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Confirm Handoff to Production")
                        }
                    }
                    handoff != null && handoff.handoffStatus == OrderJobHandoffStatus.HANDED_OFF -> {
                        Button(
                            onClick = { onMarkReadyForProduction(handoff.handoffId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.DoubleArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Mark Ready for Production")
                        }
                    }
                    handoff != null && handoff.handoffStatus == OrderJobHandoffStatus.READY_FOR_PRODUCTION -> {
                        Text(
                            text = "✓ Handoff sealed and ready for production intake.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Private sub-components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HandoffStatusBadge(
    status: OrderJobHandoffStatus?,
    isReady: Boolean,
    isCancelled: Boolean,
    modifier: Modifier = Modifier
) {
    val (icon, text, tint) = when {
        status != null -> when (status) {
            OrderJobHandoffStatus.PENDING -> Triple(Icons.Default.Schedule, status.defaultLabel, MaterialTheme.colorScheme.onSurfaceVariant)
            OrderJobHandoffStatus.READY_FOR_HANDOFF -> Triple(Icons.Default.CheckCircle, status.defaultLabel, MaterialTheme.colorScheme.primary)
            OrderJobHandoffStatus.HANDED_OFF -> Triple(Icons.Default.Send, status.defaultLabel, MaterialTheme.colorScheme.tertiary)
            OrderJobHandoffStatus.READY_FOR_PRODUCTION -> Triple(Icons.Default.AssignmentTurnedIn, status.defaultLabel, MaterialTheme.colorScheme.primary)
            OrderJobHandoffStatus.CANCELLED -> Triple(Icons.Default.Cancel, status.defaultLabel, MaterialTheme.colorScheme.error)
        }
        isCancelled -> Triple(Icons.Default.Cancel, "Cancelled", MaterialTheme.colorScheme.error)
        isReady -> Triple(Icons.Default.CheckCircle, "Ready", MaterialTheme.colorScheme.primary)
        else -> Triple(Icons.Default.Schedule, "Not Ready", MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HandoffInfoRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ReadinessCheckRow(
    label: String,
    passed: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (passed) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = if (passed) "Pass" else "Fail",
            tint = if (passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (passed) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
