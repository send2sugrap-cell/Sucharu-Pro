package com.sucharu.sucharupro.ui.features.communication.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationExportType

@Composable
fun CommunicationExportDialog(
    isLoading: Boolean,
    availableSnapshotId: String? = null,
    onDismiss: () -> Unit,
    onExportRequested: (CommunicationExportType, String?) -> Unit
) {
    var selectedType by remember { mutableStateOf(CommunicationExportType.FULL_REPORT) }
    var useSnapshot by remember { mutableStateOf(availableSnapshotId != null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Analytics Data") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select the type of report you want to generate. This will produce a secure, tamper-evident export payload.")
                
                Column {
                    CommunicationExportType.values().forEach { type ->
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = { selectedType = type }
                            )
                            Text(type.name.replace("_", " "))
                        }
                    }
                }
                
                if (availableSnapshotId != null) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(
                            checked = useSnapshot,
                            onCheckedChange = { useSnapshot = it }
                        )
                        Text("Include verified Snapshot data (ID: ${availableSnapshotId.take(8)}...)")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onExportRequested(selectedType, if (useSnapshot) availableSnapshotId else null) 
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Generate Export")
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
