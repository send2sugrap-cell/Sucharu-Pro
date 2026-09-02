package com.sucharu.sucharupro.ui.features.inventory.receiving

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Form screen for creating a new receiving header (Module 07 Step 03).
 *
 * Captures: reference, warehouse ID, receiving date, source reference, and notes.
 * Validation and persistence are delegated to the caller via [onSubmit].
 */
@Composable
fun InventoryReceivingFormScreen(
    onSubmit: (
        reference: String,
        warehouseId: String,
        receivingDate: String,
        sourceReference: String,
        notes: String
    ) -> Unit = { _, _, _, _, _ -> },
    onNavigateBack: () -> Unit = {}
) {
    var reference by remember { mutableStateOf("") }
    var warehouseId by remember { mutableStateOf("") }
    var receivingDate by remember { mutableStateOf("") }
    var sourceReference by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "New Stock Receiving",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = reference,
            onValueChange = { reference = it },
            label = { Text("Receiving Reference *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = warehouseId,
            onValueChange = { warehouseId = it },
            label = { Text("Warehouse ID *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = receivingDate,
            onValueChange = { receivingDate = it },
            label = { Text("Receiving Date (ISO 8601) *") },
            placeholder = { Text("e.g. 2026-08-17") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = sourceReference,
            onValueChange = { sourceReference = it },
            label = { Text("Source Reference (optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                onSubmit(reference.trim(), warehouseId.trim(), receivingDate.trim(), sourceReference.trim(), notes.trim())
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = reference.isNotBlank() && warehouseId.isNotBlank() && receivingDate.isNotBlank()
        ) {
            Text("Create Receiving")
        }

        Button(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}
