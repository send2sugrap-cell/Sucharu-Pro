package com.sucharu.sucharupro.ui.features.customer.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppOutlinedButton
import com.sucharu.sucharupro.ui.components.AppTextField
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Modal dialog for scheduling or updating a customer follow-up target date.
 */
@Composable
fun CustomerFollowUpDialog(
    isVisible: Boolean,
    followUpInput: String,
    onFollowUpInputChange: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Schedule Follow-up",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Record a target date or note for next operational follow-up.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                AppTextField(
                    value = followUpInput,
                    onValueChange = onFollowUpInputChange,
                    label = "Follow-up Date / Target",
                    placeholder = "e.g., 2026-08-25 or Next Monday",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                if (followUpInput.isNotBlank()) {
                    AppOutlinedButton(
                        text = "Clear",
                        onClick = onClear
                    )
                }
                AppButton(
                    text = "Save",
                    onClick = onSave
                )
            }
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
