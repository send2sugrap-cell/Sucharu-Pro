package com.sucharu.sucharupro.ui.features.inventory.stocktransfer

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
 * Form screen for creating a new stock transfer header (Module 07 Step 05).
 *
 * Captures: reference, from warehouse, to warehouse, transfer date, and notes.
 * Validation and persistence are delegated to the caller via [onSubmit].
 */
@Composable
fun InventoryStockTransferFormScreen(
    onSubmit: (
        reference: String,
        fromWarehouseId: String,
        toWarehouseId: String,
        transferDate: String,
        notes: String
    ) -> Unit = { _, _, _, _, _ -> },
    onNavigateBack: () -> Unit = {}
) {
    var reference by remember { mutableStateOf("") }
    var fromWarehouseId by remember { mutableStateOf("") }
    var toWarehouseId by remember { mutableStateOf("") }
    var transferDate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "New Stock Transfer",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = reference,
            onValueChange = { reference = it },
            label = { Text("Transfer Reference *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = fromWarehouseId,
            onValueChange = { fromWarehouseId = it },
            label = { Text("From Warehouse ID *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = toWarehouseId,
            onValueChange = { toWarehouseId = it },
            label = { Text("To Warehouse ID *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = transferDate,
            onValueChange = { transferDate = it },
            label = { Text("Date (ISO 8601) *") },
            placeholder = { Text("e.g. 2026-08-17") },
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
                onSubmit(reference.trim(), fromWarehouseId.trim(), toWarehouseId.trim(), transferDate.trim(), notes.trim())
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = reference.isNotBlank() && fromWarehouseId.isNotBlank() && toWarehouseId.isNotBlank() && 
                    transferDate.isNotBlank() && fromWarehouseId != toWarehouseId
        ) {
            Text("Create Transfer")
        }

        Button(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}
