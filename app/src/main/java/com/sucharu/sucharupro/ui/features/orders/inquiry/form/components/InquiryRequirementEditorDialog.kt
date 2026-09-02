package com.sucharu.sucharupro.ui.features.orders.inquiry.form.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
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
import com.sucharu.sucharupro.domain.model.order.InquiryRequirement
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppTextField
import com.sucharu.sucharupro.ui.theme.spacing
import java.util.UUID

/**
 * Modal Dialog for adding or editing an [InquiryRequirement] specification item.
 */
@Composable
fun InquiryRequirementEditorDialog(
    initialItem: InquiryRequirement?,
    onDismiss: () -> Unit,
    onSaveItem: (InquiryRequirement) -> Unit
) {
    var productName by remember { mutableStateOf(initialItem?.productName.orEmpty()) }
    var description by remember { mutableStateOf(initialItem?.description.orEmpty()) }
    var quantityText by remember { mutableStateOf(initialItem?.quantity?.toString() ?: "1000") }
    var unit by remember { mutableStateOf(initialItem?.unit ?: "Pcs") }
    var size by remember { mutableStateOf(initialItem?.size.orEmpty()) }
    var paperMaterial by remember { mutableStateOf(initialItem?.paperMaterial.orEmpty()) }
    var gsmText by remember { mutableStateOf(initialItem?.gsm?.toString().orEmpty()) }
    var colorSpecification by remember { mutableStateOf(initialItem?.colorSpecification.orEmpty()) }
    var printingMethod by remember { mutableStateOf(initialItem?.printingMethod.orEmpty()) }
    var finishing by remember { mutableStateOf(initialItem?.finishing.orEmpty()) }
    var isDesignRequired by remember { mutableStateOf(initialItem?.isDesignRequired ?: false) }
    var notes by remember { mutableStateOf(initialItem?.notes.orEmpty()) }

    var productNameError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }
    var quantityError by remember { mutableStateOf<String?>(null) }
    var gsmError by remember { mutableStateOf<String?>(null) }

    val isEditing = initialItem != null
    val dialogTitle = if (isEditing) "Edit Requirement" else "Add Requirement"

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
                // Product Name
                AppTextField(
                    value = productName,
                    onValueChange = {
                        productName = it
                        productNameError = if (it.isBlank()) "Product name is required" else null
                    },
                    label = "Product / Service *",
                    placeholder = "e.g. Brochure, Book, Packaging Box",
                    errorMessage = productNameError,
                    singleLine = true
                )

                // Description
                AppTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        descriptionError = if (it.isBlank()) "Description is required" else null
                    },
                    label = "Description *",
                    placeholder = "Detailed product description",
                    errorMessage = descriptionError,
                    singleLine = false,
                    maxLines = 3
                )

                // Quantity & Unit Row
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

                // Size & Paper Material Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                ) {
                    AppTextField(
                        value = size,
                        onValueChange = { size = it },
                        label = "Size",
                        placeholder = "e.g. A4, 8x6 in",
                        modifier = Modifier.weight(1f)
                    )
                    AppTextField(
                        value = paperMaterial,
                        onValueChange = { paperMaterial = it },
                        label = "Paper / Material",
                        placeholder = "e.g. Art Paper",
                        modifier = Modifier.weight(1f)
                    )
                }

                // GSM & Color Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                ) {
                    AppTextField(
                        value = gsmText,
                        onValueChange = {
                            gsmText = it
                            if (it.isNotBlank()) {
                                val parsed = it.toIntOrNull()
                                gsmError = if (parsed == null || parsed <= 0) "Must be > 0" else null
                            } else {
                                gsmError = null
                            }
                        },
                        label = "GSM",
                        placeholder = "e.g. 150",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        errorMessage = gsmError,
                        modifier = Modifier.weight(1f)
                    )
                    AppTextField(
                        value = colorSpecification,
                        onValueChange = { colorSpecification = it },
                        label = "Color",
                        placeholder = "e.g. 4 Color (CMYK)",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Printing Method & Finishing Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                ) {
                    AppTextField(
                        value = printingMethod,
                        onValueChange = { printingMethod = it },
                        label = "Method",
                        placeholder = "Offset / Digital",
                        modifier = Modifier.weight(1f)
                    )
                    AppTextField(
                        value = finishing,
                        onValueChange = { finishing = it },
                        label = "Finishing",
                        placeholder = "Gloss / Matt",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Design Required Checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isDesignRequired,
                        onCheckedChange = { isDesignRequired = it }
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
                    Text(
                        text = "Design service required by client",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Item Notes
                AppTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Specification Notes",
                    placeholder = "Additional instructions for this item",
                    singleLine = false,
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            AppButton(
                text = if (isEditing) "Update" else "Add Item",
                onClick = {
                    val pNameTrimmed = productName.trim()
                    val descTrimmed = description.trim()
                    val qtyParsed = quantityText.trim().toIntOrNull()
                    val gsmParsed = gsmText.trim().toIntOrNull()

                    var hasError = false
                    if (pNameTrimmed.isBlank()) {
                        productNameError = "Product name is required"
                        hasError = true
                    }
                    if (descTrimmed.isBlank()) {
                        descriptionError = "Description is required"
                        hasError = true
                    }
                    if (qtyParsed == null || qtyParsed <= 0) {
                        quantityError = "Must be > 0"
                        hasError = true
                    }
                    if (gsmText.isNotBlank() && (gsmParsed == null || gsmParsed <= 0)) {
                        gsmError = "Must be > 0"
                        hasError = true
                    }

                    if (!hasError) {
                        val item = InquiryRequirement(
                            itemId = initialItem?.itemId ?: "req-${UUID.randomUUID().toString().take(8)}",
                            productName = pNameTrimmed,
                            description = descTrimmed,
                            quantity = qtyParsed ?: 1,
                            unit = unit.trim().ifBlank { "Pcs" },
                            size = size.trim().ifBlank { null },
                            paperMaterial = paperMaterial.trim().ifBlank { null },
                            gsm = gsmParsed,
                            colorSpecification = colorSpecification.trim().ifBlank { null },
                            printingMethod = printingMethod.trim().ifBlank { null },
                            finishing = finishing.trim().ifBlank { null },
                            isDesignRequired = isDesignRequired,
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
