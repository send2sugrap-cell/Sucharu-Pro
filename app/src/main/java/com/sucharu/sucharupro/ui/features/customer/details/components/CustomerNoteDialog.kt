package com.sucharu.sucharupro.ui.features.customer.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppOutlinedButton
import com.sucharu.sucharupro.ui.components.AppTextField
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Modal dialog for creating or editing an internal customer note.
 */
@Composable
fun CustomerNoteDialog(
    isVisible: Boolean,
    noteText: String,
    isImportant: Boolean,
    isEditing: Boolean,
    errorMessage: String?,
    isSaving: Boolean,
    onNoteTextChange: (String) -> Unit,
    onImportanceChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Internal Note" else "Add Internal Note",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Internal notes are visible to company staff only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                AppTextField(
                    value = noteText,
                    onValueChange = onNoteTextChange,
                    label = "Note Content *",
                    placeholder = "e.g., Customer prefers proofs in Bengali or delivery before noon",
                    errorMessage = errorMessage,
                    singleLine = false,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isImportant,
                        onCheckedChange = onImportanceChange
                    )
                    Text(
                        text = "Mark as Important Note",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            AppButton(
                text = if (isEditing) "Update Note" else "Save Note",
                onClick = onSave,
                isLoading = isSaving,
                enabled = !isSaving
            )
        },
        dismissButton = {
            AppOutlinedButton(
                text = "Cancel",
                onClick = onDismiss,
                enabled = !isSaving
            )
        },
        modifier = modifier
    )
}
