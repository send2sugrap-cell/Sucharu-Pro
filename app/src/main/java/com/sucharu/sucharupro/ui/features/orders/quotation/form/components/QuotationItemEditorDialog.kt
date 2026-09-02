package com.sucharu.sucharupro.ui.features.orders.quotation.form.components

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.sucharu.sucharupro.domain.model.order.QuotationItem
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.components.AppTextField
import com.sucharu.sucharupro.ui.theme.spacing
import java.util.UUID

/**
 * Modal Dialog for adding or editing a [QuotationItem] with live line pricing subtotal.
 */
@Composable
fun QuotationItemEditorDialog(
    initialItem: QuotationItem?,
    onDismiss: () -> Unit,
    onSaveItem: (QuotationItem) -> Unit
) {
    var description by remember { mutableStateOf(initialItem?.description.orEmpty()) }
    var specification by remember { mutableStateOf(initialItem?.specification.orEmpty()) }
    var quantityText by remember { mutableStateOf(initialItem?.quantity?.toString() ?: "1000") }
    var unit by remember { mutableStateOf(initialItem?.unit ?: "Pcs") }
    var unitPriceText by remember { mutableStateOf(initialItem?.unitPrice?.amount?.stripTrailingZeros()?.toPlainString() ?: "50") }
    var discountText by remember { mutableStateOf(initialItem?.discount?.amount?.stripTrailingZeros()?.toPlainString() ?: "0") }
    var notes by remember { mutableStateOf(initialItem?.notes.orEmpty()) }

    var descriptionError by remember { mutableStateOf<String?>(null) }
    var quantityError by remember { mutableStateOf<String?>(null) }
    var unitPriceError by remember { mutableStateOf<String?>(null) }
    var discountError by remember { mutableStateOf<String?>(null) }

    val isEditing = initialItem != null
    val dialogTitle = if (isEditing) "Edit Commercial Item" else "Add Commercial Item"

    // Live calculations
    val quantity = quantityText.trim().toIntOrNull() ?: 0
    val unitPriceDouble = unitPriceText.trim().toDoubleOrNull() ?: 0.0
    val discountDouble = discountText.trim().toDoubleOrNull() ?: 0.0

    val unitPriceMoney = if (unitPriceDouble >= 0) Money(unitPriceDouble) else Money.ZERO
    val discountMoney = if (discountDouble >= 0) Money(discountDouble) else Money.ZERO
    val grossTotal = unitPriceMoney * quantity
    val lineSubtotal = if (discountMoney >= grossTotal) Money.ZERO else grossTotal - discountMoney

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = dialogTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                // Description
                AppTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        descriptionError = if (it.isBlank()) "Item description is required" else null
                    },
                    label = "Item Description *",
                    placeholder = "e.g. Brochure Printing - A4, 4 Color",
                    errorMessage = descriptionError,
                    singleLine = true
                )

                // Specification
                AppTextField(
                    value = specification,
                    onValueChange = { specification = it },
                    label = "Technical Specification",
                    placeholder = "e.g. 150 GSM Art paper, Gloss lamination",
                    singleLine = false,
                    maxLines = 2
                )

                // Quantity & Unit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                ) {
                    AppTextField(
                        value = quantityText,
                        onValueChange = {
                            quantityText = it
                            val parsed = it.toIntOrNull()
                            quantityError = if (parsed == null || parsed <= 0) "Must be > 0" else null
                        },
                        label = "Quantity *",
                        placeholder = "1000",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        errorMessage = quantityError,
                        modifier = Modifier.weight(1.5f)
                    )
                    AppTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = "Unit",
                        placeholder = "Pcs",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Unit Price & Line Discount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                ) {
                    AppTextField(
                        value = unitPriceText,
                        onValueChange = {
                            unitPriceText = it
                            val parsed = it.toDoubleOrNull()
                            unitPriceError = if (parsed == null || parsed < 0) "Must be >= 0" else null
                        },
                        label = "Unit Price (৳) *",
                        placeholder = "50.00",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        errorMessage = unitPriceError,
                        modifier = Modifier.weight(1f)
                    )
                    AppTextField(
                        value = discountText,
                        onValueChange = {
                            discountText = it
                            val parsed = it.toDoubleOrNull()
                            discountError = if (parsed == null || parsed < 0) "Must be >= 0" else null
                        },
                        label = "Line Discount (৳)",
                        placeholder = "0.00",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        errorMessage = discountError,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Live Line Subtotal Card
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.small),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Line Subtotal:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = lineSubtotal.formatted(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Notes
                AppTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Item Notes",
                    placeholder = "Special processing or packing notes",
                    singleLine = false,
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            AppButton(
                text = if (isEditing) "Update" else "Add Item",
                onClick = {
                    val descTrimmed = description.trim()
                    val qtyParsed = quantityText.trim().toIntOrNull()
                    val priceParsed = unitPriceText.trim().toDoubleOrNull()
                    val discParsed = discountText.trim().toDoubleOrNull() ?: 0.0

                    var hasError = false
                    if (descTrimmed.isBlank()) {
                        descriptionError = "Item description is required"
                        hasError = true
                    }
                    if (qtyParsed == null || qtyParsed <= 0) {
                        quantityError = "Must be > 0"
                        hasError = true
                    }
                    if (priceParsed == null || priceParsed < 0) {
                        unitPriceError = "Unit price must be >= 0"
                        hasError = true
                    }
                    if (discParsed < 0) {
                        discountError = "Discount cannot be negative"
                        hasError = true
                    }

                    if (!hasError) {
                        val item = QuotationItem(
                            itemId = initialItem?.itemId ?: "qitem-${UUID.randomUUID().toString().take(8)}",
                            description = descTrimmed,
                            specification = specification.trim().ifBlank { null },
                            quantity = qtyParsed ?: 1,
                            unit = unit.trim().ifBlank { "Pcs" },
                            unitPrice = Money(priceParsed ?: 0.0),
                            discount = Money(discParsed),
                            notes = notes.trim().ifBlank { null }
                        )
                        onSaveItem(item)
                    }
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
