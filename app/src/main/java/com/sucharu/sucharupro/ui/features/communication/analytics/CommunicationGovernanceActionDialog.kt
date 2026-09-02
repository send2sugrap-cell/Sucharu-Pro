package com.sucharu.sucharupro.ui.features.communication.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationGovernanceActionType

@Composable
fun CommunicationGovernanceActionDialog(
    targetType: String,
    targetId: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (CommunicationGovernanceActionType, String, String) -> Unit
) {
    var selectedAction by remember { mutableStateOf(CommunicationGovernanceActionType.ACKNOWLEDGE_RISK) }
    var resultingState by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Take Governance Action")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Target: $targetType ($targetId)", style = MaterialTheme.typography.bodySmall)
                
                Text("Action Type:")
                // Simplified selection for brevity
                Row {
                    RadioButton(
                        selected = selectedAction == CommunicationGovernanceActionType.ACKNOWLEDGE_RISK,
                        onClick = { selectedAction = CommunicationGovernanceActionType.ACKNOWLEDGE_RISK }
                    )
                    Text("Acknowledge Risk", modifier = Modifier.padding(top = 12.dp))
                }
                Row {
                    RadioButton(
                        selected = selectedAction == CommunicationGovernanceActionType.DISMISS_ALERT,
                        onClick = { selectedAction = CommunicationGovernanceActionType.DISMISS_ALERT }
                    )
                    Text("Dismiss Alert", modifier = Modifier.padding(top = 12.dp))
                }
                
                OutlinedTextField(
                    value = resultingState,
                    onValueChange = { resultingState = it },
                    label = { Text("Resulting State (e.g. ACCEPTED)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Justification / Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedAction, resultingState, notes) },
                enabled = !isLoading && resultingState.isNotBlank() && notes.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Confirm Action")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}
