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
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.sucharu.sucharupro.domain.model.order.DeliveryRequirement
import com.sucharu.sucharupro.domain.model.order.DeliveryType
import com.sucharu.sucharupro.ui.components.AppTextField
import com.sucharu.sucharupro.ui.features.orders.components.DetailSectionCard
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Form section card for configuring [DeliveryRequirement] in Quotation.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuotationDeliveryRequirementFormCard(
    deliveryRequirement: DeliveryRequirement?,
    onDeliveryRequirementChange: (DeliveryRequirement?) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentReq = deliveryRequirement ?: DeliveryRequirement(deliveryType = DeliveryType.BUSINESS_DELIVERY)

    DetailSectionCard(
        title = "Delivery Requirement",
        icon = Icons.Default.LocalShipping,
        modifier = modifier
    ) {
        Text(
            text = "Delivery Fulfillment Method",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)
        ) {
            DeliveryType.values().forEach { dType ->
                FilterChip(
                    selected = currentReq.deliveryType == dType,
                    onClick = {
                        onDeliveryRequirementChange(currentReq.copy(deliveryType = dType))
                    },
                    label = { Text(dType.defaultLabel) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        // Target delivery date & Contact Person Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            AppTextField(
                value = currentReq.requiredDate.orEmpty(),
                onValueChange = {
                    onDeliveryRequirementChange(currentReq.copy(requiredDate = it.trim().ifBlank { null }))
                },
                label = "Target Delivery Date",
                placeholder = "YYYY-MM-DD",
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            AppTextField(
                value = currentReq.contactName.orEmpty(),
                onValueChange = {
                    onDeliveryRequirementChange(currentReq.copy(contactName = it.trim().ifBlank { null }))
                },
                label = "Recipient Contact Name",
                placeholder = "e.g. Delivery In-charge",
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        // Phone & Address
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            AppTextField(
                value = currentReq.contactPhone.orEmpty(),
                onValueChange = {
                    onDeliveryRequirementChange(currentReq.copy(contactPhone = it.trim().ifBlank { null }))
                },
                label = "Contact Phone",
                placeholder = "01XXXXXXXXX",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            AppTextField(
                value = currentReq.address.orEmpty(),
                onValueChange = {
                    onDeliveryRequirementChange(currentReq.copy(address = it.trim().ifBlank { null }))
                },
                label = "Delivery Address",
                placeholder = "Physical delivery address (Bangla/English)",
                modifier = Modifier.weight(1.5f),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        AppTextField(
            value = currentReq.instructions.orEmpty(),
            onValueChange = {
                onDeliveryRequirementChange(currentReq.copy(instructions = it.trim().ifBlank { null }))
            },
            label = "Dispatch / Packaging Instructions",
            placeholder = "e.g. Pack in bundles of 50, deliver before 5 PM",
            singleLine = false,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
