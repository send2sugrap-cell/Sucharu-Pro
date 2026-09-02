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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Form screen for adding a stock transfer line to an existing transfer document (Module 07 Step 05).
 *
 * Captures: product ID, from location, to location, requested quantity, and notes.
 * Features: Displays available quantity at the SOURCE warehouse/location.
 */
@Composable
fun InventoryStockTransferLineFormScreen(
    transferId: String,
    fromWarehouseId: String,
    toWarehouseId: String,
    availableQuantity: Int? = null,
    onFetchAvailability: (productId: String, locationId: String) -> Unit = { _, _ -> },
    onSubmit: (
        productId: String,
        fromLocationId: String,
        toLocationId: String,
        expectedQuantity: Int,
        notes: String
    ) -> Unit = { _, _, _, _, _ -> },
    onNavigateBack: () -> Unit = {}
) {
    var productId by remember { mutableStateOf("") }
    var fromLocationId by remember { mutableStateOf("") }
    var toLocationId by remember { mutableStateOf("") }
    var requestedQuantityRaw by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Add Transfer Item",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Transfer: $transferId | From: $fromWarehouseId | To: $toWarehouseId",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = productId,
            onValueChange = { 
                productId = it 
                if (fromLocationId.isNotBlank()) onFetchAvailability(it, fromLocationId)
            },
            label = { Text("Inventory Product ID *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = fromLocationId,
            onValueChange = { 
                fromLocationId = it 
                if (productId.isNotBlank()) onFetchAvailability(productId, it)
            },
            label = { Text("From Location ID *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Available Quantity Display
        availableQuantity?.let { qty ->
            Text(
                text = "Available at Source: $qty units",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (qty > 0) Color(0xFF2E7D32) else Color(0xFFC62828)
            )
        }

        OutlinedTextField(
            value = toLocationId,
            onValueChange = { toLocationId = it },
            label = { Text("To Location ID *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = requestedQuantityRaw,
            onValueChange = { requestedQuantityRaw = it.filter { c -> c.isDigit() } },
            label = { Text("Requested Quantity *") },
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
                val qty = requestedQuantityRaw.toIntOrNull() ?: 0
                onSubmit(productId.trim(), fromLocationId.trim(), toLocationId.trim(), qty, notes.trim())
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = productId.isNotBlank() && fromLocationId.isNotBlank() && 
                    toLocationId.isNotBlank() && requestedQuantityRaw.isNotBlank()
        ) {
            Text("Add Item")
        }

        Button(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}
