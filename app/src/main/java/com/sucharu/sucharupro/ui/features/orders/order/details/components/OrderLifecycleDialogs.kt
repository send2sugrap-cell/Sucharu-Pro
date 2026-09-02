package com.sucharu.sucharupro.ui.features.orders.order.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppOutlinedButton
import com.sucharu.sucharupro.ui.components.AppTextField
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Confirmation dialog for marking an order as READY FOR JOB handoff.
 */
@Composable
fun OrderHandoffConfirmationDialog(
    order: Order,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Handyman,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.small)
            )
        },
        title = {
            Text(
                text = "Mark Ready for Job Handoff",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "This order is commercially complete and will be marked ready for Job Card / Production handoff.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                Text(
                    text = "Order Number: ${order.orderNumber}\nCustomer ID: ${order.customerId}\nCommercial Value: ${order.totalAmount.formatted()}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            AppButton(
                text = "Confirm Readiness",
                onClick = onConfirm
            )
        },
        dismissButton = {
            AppOutlinedButton(
                text = "Cancel",
                onClick = onDismiss
            )
        },
        modifier = modifier
    )
}

/**
 * Dialog for cancelling a commercial order, enforcing a cancellation reason.
 */
@Composable
fun OrderCancellationDialog(
    order: Order,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var reason by remember { mutableStateOf("") }
    var reasonError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.small)
            )
        },
        title = {
            Text(
                text = "Cancel Commercial Order",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Are you sure you want to cancel Order ${order.orderNumber}? This will mark the order as CANCELLED. All historical commercial snapshots will be preserved.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                AppTextField(
                    value = reason,
                    onValueChange = {
                        reason = it
                        if (it.isNotBlank()) reasonError = null
                    },
                    label = "Cancellation Reason *",
                    placeholder = "e.g. Client cancelled project, duplicate entry, payment default",
                    isError = reasonError != null,
                    errorMessage = reasonError,
                    singleLine = false
                )
            }
        },
        confirmButton = {
            AppButton(
                text = "Confirm Cancellation",
                onClick = {
                    if (reason.isBlank()) {
                        reasonError = "Cancellation reason is required."
                    } else {
                        onConfirm(reason.trim())
                    }
                }
            )
        },
        dismissButton = {
            AppOutlinedButton(
                text = "Dismiss",
                onClick = onDismiss
            )
        },
        modifier = modifier
    )
}

/**
 * Dialog to change order priority (NORMAL, HIGH, URGENT).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrderPriorityDialog(
    currentPriority: OrderPriority,
    onDismiss: () -> Unit,
    onConfirm: (priority: OrderPriority) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPriority by remember { mutableStateOf(currentPriority) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.small)
            )
        },
        title = {
            Text(
                text = "Set Order Priority",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select commercial operational priority for this order:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)
                ) {
                    OrderPriority.values().forEach { priority ->
                        FilterChip(
                            selected = selectedPriority == priority,
                            onClick = { selectedPriority = priority },
                            label = { Text(priority.defaultLabel) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                text = "Update Priority",
                onClick = { onConfirm(selectedPriority) }
            )
        },
        dismissButton = {
            AppOutlinedButton(
                text = "Cancel",
                onClick = onDismiss
            )
        },
        modifier = modifier
    )
}

/**
 * Confirmation dialog for placing an order ON HOLD or resuming from hold.
 */
@Composable
fun OrderHoldResumeDialog(
    order: Order,
    isPuttingOnHold: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (isPuttingOnHold) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                contentDescription = null,
                tint = if (isPuttingOnHold) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.small)
            )
        },
        title = {
            Text(
                text = if (isPuttingOnHold) "Put Order On Hold" else "Resume Order",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = if (isPuttingOnHold) {
                    "Placing Order ${order.orderNumber} ON HOLD pauses processing while awaiting customer feedback or instructions."
                } else {
                    "Resuming Order ${order.orderNumber} restores it to CONFIRMED status."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            AppButton(
                text = if (isPuttingOnHold) "Put On Hold" else "Resume Order",
                onClick = onConfirm
            )
        },
        dismissButton = {
            AppOutlinedButton(
                text = "Cancel",
                onClick = onDismiss
            )
        },
        modifier = modifier
    )
}

/**
 * Dialog to edit operational order notes.
 */
@Composable
fun OrderNotesDialog(
    currentNotes: String?,
    onDismiss: () -> Unit,
    onConfirm: (notes: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var notesText by remember { mutableStateOf(currentNotes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Notes,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.small)
            )
        },
        title = {
            Text(
                text = "Operational Remarks",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                AppTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = "Order Notes / Remarks",
                    placeholder = "Add operational instructions or remarks",
                    singleLine = false
                )
            }
        },
        confirmButton = {
            AppButton(
                text = "Save Remarks",
                onClick = { onConfirm(notesText.trim().ifBlank { null }) }
            )
        },
        dismissButton = {
            AppOutlinedButton(
                text = "Cancel",
                onClick = onDismiss
            )
        },
        modifier = modifier
    )
}

/**
 * Confirmation dialog for creating a formal OrderJobHandoff snapshot for production intake.
 */
@Composable
fun OrderJobHandoffConfirmationDialog(
    order: Order,
    onDismiss: () -> Unit,
    onConfirm: (notes: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var handoffNotes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Handyman,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.small)
            )
        },
        title = {
            Text(
                text = "Hand Off Order to Production",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                Text(
                    text = "Creating this handoff will capture an immutable commercial snapshot for production intake.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• Order: ${order.orderNumber}\n• Customer: ${order.customerId}\n• Items: ${order.items.size} (Total Qty: ${order.totalQuantity})\n• Commercial Total: ${order.totalAmount.formatted()}\n• Priority: ${order.priority.defaultLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                AppTextField(
                    value = handoffNotes,
                    onValueChange = { handoffNotes = it },
                    label = "Handoff Notes (Optional)",
                    placeholder = "Special production or delivery instructions...",
                    singleLine = false
                )
            }
        },
        confirmButton = {
            AppButton(
                text = "Confirm Handoff",
                onClick = { onConfirm(handoffNotes.trim().ifBlank { null }) }
            )
        },
        dismissButton = {
            AppOutlinedButton(
                text = "Cancel",
                onClick = onDismiss
            )
        },
        modifier = modifier
    )
}
