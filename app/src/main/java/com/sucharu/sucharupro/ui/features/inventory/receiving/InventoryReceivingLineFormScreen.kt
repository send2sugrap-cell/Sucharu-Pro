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
 * Form screen for adding a receiving line to an existing receiving document (Module 07 Step 03).
 *
 * Captures: product ID, location ID, expected quantity, unit, and notes.
 * Validation and persistence are delegated to the caller via [onSubmit].
 */
@Composable
fun InventoryReceivingLineFormScreen(
    receivingId: String,
    warehouseId: String,
    onSubmit: (
        productId: String,
        locationId: String,
        expectedQuantity: Int,
        notes: String
    ) -> Unit = { _, _, _, _ -> },
    onNavigateBack: () -> Unit = {}
) {
    var productId by remember { mutableStateOf("") }
    var locationId by remember { mutableStateOf("") }
    var expectedQuantityRaw by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Add Receiving Line",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Receiving: $receivingId | Warehouse: $warehouseId",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = productId,
            onValueChange = { productId = it },
            label = { Text("Inventory Product ID *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = locationId,
            onValueChange = { locationId = it },
            label = { Text("Location ID *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = expectedQuantityRaw,
            onValueChange = { expectedQuantityRaw = it.filter { c -> c.isDigit() } },
            label = { Text("Expected Quantity (0 = unknown)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val qty = expectedQuantityRaw.toIntOrNull() ?: 0
                onSubmit(productId.trim(), locationId.trim(), qty, notes.trim())
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = productId.isNotBlank() && locationId.isNotBlank()
        ) {
            Text("Add Line")
        }

        Button(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}
