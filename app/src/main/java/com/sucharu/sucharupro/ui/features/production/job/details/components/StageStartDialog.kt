package com.sucharu.sucharupro.ui.features.production.job.details.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Confirmation dialog for starting a production stage with optional execution start remarks.
 */
@Composable
fun StageStartDialog(
    stage: ProductionJobStage,
    jobNumber: String,
    onConfirm: (startRemarks: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var remarks by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Start Stage: ${stage.stageType.defaultLabel}") },
        text = {
            Column {
                Text(
                    text = "Job: $jobNumber | Stage: ${stage.sequence}. ${stage.stageType.defaultLabel} (${stage.stageType.shortCode})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                if (stage.assignedUserName != null) {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                    Text(
                        text = "Assigned Operator: ${stage.assignedUserName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Start Remarks (Optional)") },
                    placeholder = { Text("e.g. মেশিন রেডি, প্লেট লোড করা হয়েছে") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            AppButton(
                text = "Start Execution",
                onClick = {
                    onConfirm(remarks.trim().ifBlank { null })
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
