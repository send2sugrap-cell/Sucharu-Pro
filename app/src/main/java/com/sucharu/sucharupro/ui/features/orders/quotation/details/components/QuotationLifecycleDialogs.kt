package com.sucharu.sucharupro.ui.features.orders.quotation.details.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material3.AlertDialog
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
import com.sucharu.sucharupro.domain.model.order.Quotation
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppOutlinedButton
import com.sucharu.sucharupro.ui.components.AppTextField
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Confirmation dialog for moving quotation to SENT status.
 */
@Composable
fun QuotationSendConfirmDialog(
    quotation: Quotation,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.small)
            )
        },
        title = {
            Text(
                text = "Send Quotation",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Mark Quotation ${quotation.quotationNumber} as SENT to client? This records official proposal delivery.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            AppButton(
                text = "Confirm Send",
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
 * Confirmation dialog for moving quotation to NEGOTIATION status.
 */
@Composable
fun QuotationStartNegotiationDialog(
    quotation: Quotation,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Handshake,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.small)
            )
        },
        title = {
            Text(
                text = "Start Negotiation",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Transition Quotation ${quotation.quotationNumber} to NEGOTIATION status? You can discuss terms and create subsequent revisions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            AppButton(
                text = "Start Negotiation",
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
 * Dialog for rejecting quotation with optional reason.
 */
@Composable
fun QuotationRejectDialog(
    quotation: Quotation,
    onDismiss: () -> Unit,
    onConfirm: (reason: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.ThumbDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.small)
            )
        },
        title = {
            Text(
                text = "Reject Quotation",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Are you sure you want to mark Quotation ${quotation.quotationNumber} as REJECTED?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                AppTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = "Rejection Reason (Optional)",
                    placeholder = "e.g. Budget constraints, competitor chose, canceled requirement",
                    singleLine = false
                )
            }
        },
        confirmButton = {
            AppButton(
                text = "Reject Quotation",
                onClick = { onConfirm(reason.trim().ifBlank { null }) }
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
 * Confirmation dialog for cancelling a quotation.
 */
@Composable
fun QuotationCancelDialog(
    quotation: Quotation,
    onDismiss: () -> Unit,
    onConfirm: (reason: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var reason by remember { mutableStateOf("") }

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
                text = "Cancel Quotation",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Quotation ${quotation.quotationNumber} will be marked as CANCELLED. All revision history will be preserved as read-only.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                AppTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = "Cancellation Reason (Optional)",
                    placeholder = "e.g. Inquired by mistake, client project aborted",
                    singleLine = false
                )
            }
        },
        confirmButton = {
            AppButton(
                text = "Confirm Cancellation",
                onClick = { onConfirm(reason.trim().ifBlank { null }) }
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
