package com.sucharu.sucharupro.ui.features.production.job.list.components

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
import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoff
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Confirmation dialog for creating an isolated Production Job Card from an OrderJobHandoff snapshot.
 */
@Composable
fun ProductionJobHandoffDialog(
    handoff: OrderJobHandoff,
    onConfirm: (title: String?, description: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(handoff.items.firstOrNull()?.description ?: "Job for ${handoff.orderNumber}") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Create Production Job") },
        text = {
            Column {
                Text(
                    text = "Convert Handoff (${handoff.orderNumber}) into an active Production Job Card. All 13 stages will be initialized to Pending.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                Text(
                    text = "Customer: ${handoff.customerId} | Items: ${handoff.itemCount} | Qty: ${handoff.totalQuantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Job Title *") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Production Notes / Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            AppButton(
                text = "Create Job Card",
                onClick = {
                    onConfirm(
                        title.trim().ifBlank { null },
                        description.trim().ifBlank { null }
                    )
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
