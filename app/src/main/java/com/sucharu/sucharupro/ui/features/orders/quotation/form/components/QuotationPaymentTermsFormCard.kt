package com.sucharu.sucharupro.ui.features.orders.quotation.form.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.sucharu.sucharupro.domain.model.order.PaymentTermType
import com.sucharu.sucharupro.domain.model.order.PaymentTerms
import com.sucharu.sucharupro.ui.components.AppTextField
import com.sucharu.sucharupro.ui.features.orders.components.DetailSectionCard
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Form section card for selecting and configuring commercial [PaymentTerms].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuotationPaymentTermsFormCard(
    paymentTerms: PaymentTerms,
    onPaymentTermsChange: (PaymentTerms) -> Unit,
    modifier: Modifier = Modifier
) {
    DetailSectionCard(
        title = "Payment Terms",
        icon = Icons.Default.Payment,
        modifier = modifier
    ) {
        Text(
            text = "Payment Term Structure",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)
        ) {
            PaymentTermType.values().forEach { termType ->
                FilterChip(
                    selected = paymentTerms.type == termType,
                    onClick = {
                        onPaymentTermsChange(
                            when (termType) {
                                PaymentTermType.FULL_ADVANCE -> PaymentTerms(type = PaymentTermType.FULL_ADVANCE)
                                PaymentTermType.PARTIAL_ADVANCE -> PaymentTerms(type = PaymentTermType.PARTIAL_ADVANCE, advancePercentage = 50)
                                PaymentTermType.ON_DELIVERY -> PaymentTerms(type = PaymentTermType.ON_DELIVERY)
                                PaymentTermType.CREDIT -> PaymentTerms(type = PaymentTermType.CREDIT, dueDays = 30)
                                PaymentTermType.CUSTOM -> PaymentTerms(type = PaymentTermType.CUSTOM, customDescription = "Custom payment schedule")
                            }
                        )
                    },
                    label = { Text(termType.defaultLabel) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        // Contextual inputs based on selected term type
        when (paymentTerms.type) {
            PaymentTermType.PARTIAL_ADVANCE -> {
                AppTextField(
                    value = paymentTerms.advancePercentage.toString(),
                    onValueChange = {
                        val parsed = it.toIntOrNull()
                        onPaymentTermsChange(paymentTerms.copy(advancePercentage = parsed ?: 0))
                    },
                    label = "Advance Percentage (%)",
                    placeholder = "50",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
            PaymentTermType.CREDIT -> {
                AppTextField(
                    value = paymentTerms.dueDays.toString(),
                    onValueChange = {
                        val parsed = it.toIntOrNull()
                        onPaymentTermsChange(paymentTerms.copy(dueDays = parsed ?: 0))
                    },
                    label = "Credit Due Days",
                    placeholder = "30",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
            PaymentTermType.CUSTOM -> {
                AppTextField(
                    value = paymentTerms.customDescription.orEmpty(),
                    onValueChange = {
                        onPaymentTermsChange(paymentTerms.copy(customDescription = it))
                    },
                    label = "Custom Payment Terms Description",
                    placeholder = "e.g. 30% advance, 50% on sample approval, 20% on delivery",
                    singleLine = false,
                    maxLines = 2
                )
            }
            else -> {
                // FULL_ADVANCE & ON_DELIVERY need no extra fields
            }
        }
    }
}
