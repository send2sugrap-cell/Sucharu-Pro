package com.sucharu.sucharupro.ui.features.orders.quotation.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HistoryEdu
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.order.Quotation
import com.sucharu.sucharupro.domain.model.order.QuotationRevision
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppOutlinedButton
import com.sucharu.sucharupro.ui.components.AppTextField
import com.sucharu.sucharupro.ui.theme.spacing
import java.time.Instant
import java.util.UUID

/**
 * Dialog to create a new sequential Revision (e.g., Rev #2 from Rev #1) with revision reason and commercial modifications.
 */
@Composable
fun QuotationRevisionDialog(
    quotation: Quotation,
    baseRevision: QuotationRevision,
    onDismiss: () -> Unit,
    onConfirm: (newRevision: QuotationRevision) -> Unit,
    modifier: Modifier = Modifier
) {
    val nextRevisionNumber = quotation.revisions.maxOfOrNull { it.revisionNumber }?.plus(1) ?: (quotation.currentRevisionNumber + 1)
    var revisionReason by remember { mutableStateOf("") }
    var discountText by remember { mutableStateOf(baseRevision.discount.amount.stripTrailingZeros().toPlainString()) }
    var revisionNotes by remember { mutableStateOf(baseRevision.notes.orEmpty()) }
    var createdBy by remember { mutableStateOf("Sales / Estimator") }
    var reasonError by remember { mutableStateOf<String?>(null) }
    var discountError by remember { mutableStateOf<String?>(null) }

    val currentSubtotal = baseRevision.subtotal
    val parsedDiscount = discountText.toDoubleOrNull()?.let { Money(it) } ?: Money.ZERO
    val calculatedTotal = if (parsedDiscount >= currentSubtotal) Money.ZERO else currentSubtotal - parsedDiscount

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.HistoryEdu,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.small)
            )
        },
        title = {
            Text(
                text = "Create Revision #$nextRevisionNumber",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Creating Revision #$nextRevisionNumber branched from Revision #${baseRevision.revisionNumber}. Historical revisions remain permanently immutable.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Base Subtotal:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currentSubtotal.formatted(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Revised Total:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = calculatedTotal.formatted(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                AppTextField(
                    value = revisionReason,
                    onValueChange = {
                        revisionReason = it
                        if (it.isNotBlank()) reasonError = null
                    },
                    label = "Revision Reason / Justification *",
                    placeholder = "e.g. Client requested 5% commercial discount on negotiation",
                    isError = reasonError != null,
                    errorMessage = reasonError,
                    singleLine = false
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                AppTextField(
                    value = discountText,
                    onValueChange = {
                        discountText = it
                        val d = it.toDoubleOrNull()
                        discountError = if (d != null && d < 0) "Discount cannot be negative" else null
                    },
                    label = "Quotation-Level Discount (৳)",
                    placeholder = "0.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = discountError != null,
                    errorMessage = discountError,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                AppTextField(
                    value = createdBy,
                    onValueChange = { createdBy = it },
                    label = "Created By",
                    placeholder = "e.g. Estimator Desk",
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                AppTextField(
                    value = revisionNotes,
                    onValueChange = { revisionNotes = it },
                    label = "Revision Remarks / Notes",
                    placeholder = "Optional notes for this revision",
                    singleLine = false
                )
            }
        },
        confirmButton = {
            AppButton(
                text = "Save Revision #$nextRevisionNumber",
                onClick = {
                    if (revisionReason.isBlank()) {
                        reasonError = "Revision reason is required."
                    } else if (discountError == null) {
                        val newRev = QuotationRevision(
                            revisionId = "rev-${UUID.randomUUID().toString().take(8)}-v$nextRevisionNumber",
                            quotationId = quotation.quotationId,
                            revisionNumber = nextRevisionNumber,
                            items = baseRevision.items,
                            discount = parsedDiscount,
                            deliveryRequirement = baseRevision.deliveryRequirement,
                            paymentTerms = baseRevision.paymentTerms,
                            notes = revisionNotes.trim().ifBlank { null },
                            revisionReason = revisionReason.trim(),
                            createdAt = Instant.now().toString(),
                            createdBy = createdBy.trim().ifBlank { null },
                            previousRevisionId = baseRevision.revisionId
                        )
                        onConfirm(newRev)
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
