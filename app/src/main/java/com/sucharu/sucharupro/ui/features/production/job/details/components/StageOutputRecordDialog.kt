package com.sucharu.sucharupro.ui.features.production.job.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Dialog for recording an operational output quantity against a production stage.
 */
@Composable
fun StageOutputRecordDialog(
    stage: ProductionJobStage,
    plannedQuantity: Int,
    producedQuantity: Int,
    remainingQuantity: Int,
    unit: String,
    onConfirm: (quantity: Int, remarks: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var quantityText by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Record Stage Output") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                Text(
                    text = "Stage: ${stage.sequence}. ${stage.stageType.defaultLabel} (${stage.stageType.shortCode})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Quantity Metrics Summary Box
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Planned:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "$plannedQuantity $unit",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Already Produced:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "$producedQuantity $unit",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Remaining:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "$remainingQuantity $unit",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            quantityText = input
                            validationError = null
                        }
                    },
                    label = { Text("Output Quantity ($unit)") },
                    placeholder = { Text("e.g. 250") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = validationError != null,
                    supportingText = validationError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks (Optional)") },
                    placeholder = { Text("e.g. ১ম ব্যাচ সম্পন্ন") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            AppButton(
                text = "Record Output",
                onClick = {
                    val qty = quantityText.toIntOrNull()
                    if (qty == null || qty <= 0) {
                        validationError = "উৎপাদন পরিমাণ অবশ্যই ০-এর বেশি হতে হবে।"
                        return@AppButton
                    }
                    if (qty > remainingQuantity) {
                        validationError = "অবশিষ্ট পরিমাণ $remainingQuantity $unit। এর বেশি আউটপুট রেকর্ড করা যাবে না।"
                        return@AppButton
                    }
                    onConfirm(qty, remarks.trim().ifBlank { null })
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
