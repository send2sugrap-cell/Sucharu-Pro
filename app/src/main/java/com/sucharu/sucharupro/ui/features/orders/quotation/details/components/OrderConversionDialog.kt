package com.sucharu.sucharupro.ui.features.orders.quotation.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCartCheckout
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
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.order.Quotation
import com.sucharu.sucharupro.domain.model.order.QuotationRevision
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppOutlinedButton
import com.sucharu.sucharupro.ui.components.AppTextField
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Dialog to confirm converting an approved commercial Quotation into a confirmed Customer Order.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrderConversionDialog(
    quotation: Quotation,
    approvedRevision: QuotationRevision,
    onDismiss: () -> Unit,
    onConfirm: (priority: OrderPriority, confirmedBy: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPriority by remember { mutableStateOf(OrderPriority.NORMAL) }
    var confirmedBy by remember { mutableStateOf("Sales / Commercial Desk") }
    var confirmedByError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.ShoppingCartCheckout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.small)
            )
        },
        title = {
            Text(
                text = "Convert to Confirmed Order",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Creating a formal Order from approved Quotation ${quotation.quotationNumber} (Revision #${approvedRevision.revisionNumber}).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                Text(
                    text = "Agreed Value: ${approvedRevision.totalAmount.formatted()} (${approvedRevision.items.size} item${if (approvedRevision.items.size > 1) "s" else ""})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                Text(
                    text = "Order Priority",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

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

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                AppTextField(
                    value = confirmedBy,
                    onValueChange = {
                        confirmedBy = it
                        if (it.isNotBlank()) confirmedByError = null
                    },
                    label = "Confirmed By *",
                    placeholder = "e.g. Sales Desk / Manager",
                    isError = confirmedByError != null,
                    errorMessage = confirmedByError,
                    singleLine = true
                )
            }
        },
        confirmButton = {
            AppButton(
                text = "Confirm & Create Order",
                onClick = {
                    if (confirmedBy.isBlank()) {
                        confirmedByError = "Confirmed By is required."
                    } else {
                        onConfirm(selectedPriority, confirmedBy.trim())
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
