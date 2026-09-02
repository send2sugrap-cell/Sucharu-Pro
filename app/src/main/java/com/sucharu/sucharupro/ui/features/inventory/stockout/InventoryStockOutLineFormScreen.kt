package com.sucharu.sucharupro.ui.features.inventory.stockout

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
 * Form screen for adding a stock-out line to an existing issuance document (Module 07 Step 04).
 *
 * Captures: product ID, location ID, requested quantity, and notes.
 * Features: Displays available quantity for the selected product/location.
 */
@Composable
fun InventoryStockOutLineFormScreen(
    stockOutId: String,
    warehouseId: String,
    availableQuantity: Int? = null,
    onFetchAvailability: (productId: String, locationId: String) -> Unit = { _, _ -> },
    onSubmit: (
        productId: String,
        locationId: String,
        requestedQuantity: Int,
        notes: String
    ) -> Unit = { _, _, _, _ -> },
    onNavigateBack: () -> Unit = {}
) {
    var productId by remember { mutableStateOf("") }
    var locationId by remember { mutableStateOf("") }
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
            text = "Add Stock-Out Item",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Issuance: $stockOutId | Warehouse: $warehouseId",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = productId,
            onValueChange = { 
                productId = it 
                if (locationId.isNotBlank()) onFetchAvailability(it, locationId)
            },
            label = { Text("Inventory Product ID *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = locationId,
            onValueChange = { 
                locationId = it 
                if (productId.isNotBlank()) onFetchAvailability(productId, it)
            },
            label = { Text("Location ID *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Available Quantity Display
        availableQuantity?.let { qty ->
            Text(
                text = "Available Stock: $qty units",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (qty > 0) Color(0xFF2E7D32) else Color(0xFFC62828)
            )
        }

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
                onSubmit(productId.trim(), locationId.trim(), qty, notes.trim())
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = productId.isNotBlank() && locationId.isNotBlank() && requestedQuantityRaw.isNotBlank()
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
