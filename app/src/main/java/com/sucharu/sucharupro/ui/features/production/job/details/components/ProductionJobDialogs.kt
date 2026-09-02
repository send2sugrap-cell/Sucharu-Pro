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
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Confirmation dialog for cancelling a Production Job with mandatory non-blank reason.
 */
@Composable
fun JobCancellationDialog(
    jobNumber: String,
    onConfirm: (reason: String) -> Unit,
    onDismiss: () -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Cancel Production Job $jobNumber") },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to cancel this Job Card? This will mark the Job as cancelled (Terminal state).",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                OutlinedTextField(
                    value = reason,
                    onValueChange = {
                        reason = it
                        if (showError && it.isNotBlank()) showError = false
                    },
                    label = { Text("Cancellation Reason *") },
                    placeholder = { Text("Enter reason for cancellation (e.g. ক্লায়েন্ট অর্ডার বাতিল করেছে)") },
                    isError = showError,
                    supportingText = if (showError) {
                        { Text("Reason is mandatory and cannot be blank.") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            AppButton(
                text = "Confirm Cancel",
                onClick = {
                    if (reason.isBlank()) {
                        showError = true
                    } else {
                        onConfirm(reason.trim())
                    }
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Back")
            }
        }
    )
}

/**
 * Dialog for placing a Job on hold with an optional reason.
 */
@Composable
fun JobHoldDialog(
    jobNumber: String,
    onConfirm: (reason: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Hold Job $jobNumber") },
        text = {
            Column {
                Text(
                    text = "Place this job on hold? Active stage work will be paused.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Hold Reason (Optional)") },
                    placeholder = { Text("e.g. গ্রাহকের নকশা সংশোধনের জন্য স্থগিত") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            AppButton(
                text = "Confirm Hold",
                onClick = { onConfirm(reason.trim().ifBlank { null }) }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog for completing a stage with optional completion notes.
 */
@Composable
fun StageCompletionDialog(
    stageName: String,
    onConfirm: (notes: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Complete Stage: $stageName") },
        text = {
            Column {
                Text(
                    text = "Mark '$stageName' as completed? The job will advance to the next stage.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Completion Remarks (Optional)") },
                    placeholder = { Text("e.g. মুদ্রণ ও কালার ব্যালেন্স সম্পূর্ণ নিখুঁত") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            AppButton(
                text = "Complete",
                onClick = { onConfirm(notes.trim().ifBlank { null }) }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
