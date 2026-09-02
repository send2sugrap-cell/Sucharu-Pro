package com.sucharu.sucharupro.ui.features.orders.quotation.details.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.order.Quotation
import com.sucharu.sucharupro.domain.model.order.QuotationRevision
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppOutlinedButton
import com.sucharu.sucharupro.ui.components.AppTextField
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Confirmation dialog for formally approving a specific quotation revision.
 */
@Composable
fun QuotationApprovalDialog(
    quotation: Quotation,
    revision: QuotationRevision,
    onDismiss: () -> Unit,
    onConfirm: (approvedBy: String, notes: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var approverName by remember { mutableStateOf("Sales / Commercial Desk") }
    var approvalNotes by remember { mutableStateOf("") }
    var approverNameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.small)
            )
        },
        title = {
            Text(
                text = "Approve Quotation",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "You are about to formally approve Quotation ${quotation.quotationNumber} with Revision #${revision.revisionNumber}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                Text(
                    text = "Commercial Value: ${revision.totalAmount.formatted()} (${revision.items.size} item${if (revision.items.size > 1) "s" else ""})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                AppTextField(
                    value = approverName,
                    onValueChange = {
                        approverName = it
                        if (it.isNotBlank()) approverNameError = null
                    },
                    label = "Approved By *",
                    placeholder = "Enter approver name / title",
                    isError = approverNameError != null,
                    errorMessage = approverNameError,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                AppTextField(
                    value = approvalNotes,
                    onValueChange = { approvalNotes = it },
                    label = "Approval Remarks (Optional)",
                    placeholder = "e.g. Approved as per client confirmation on phone/email",
                    singleLine = false
                )
            }
        },
        confirmButton = {
            AppButton(
                text = "Confirm Approval",
                onClick = {
                    if (approverName.isBlank()) {
                        approverNameError = "Approver name is required."
                    } else {
                        onConfirm(approverName.trim(), approvalNotes.trim().ifBlank { null })
                    }
                }
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
