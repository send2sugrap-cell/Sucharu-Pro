package com.sucharu.sucharupro.ui.features.inventory.adjustment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryAdjustmentReason
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryAdjustmentType

/**
 * Form screen for adding a stock adjustment line to an existing adjustment document (Module 07 Step 06).
 *
 * Captures: product ID, location, adjustment type, reason, adjusted quantity, and notes.
 * Features: Displays available quantity for DECREASE operations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryStockAdjustmentLineFormScreen(
    adjustmentId: String,
    warehouseId: String,
    availableQuantity: Int? = null,
    onFetchAvailability: (productId: String, locationId: String) -> Unit = { _, _ -> },
    onSubmit: (
        productId: String,
        locationId: String,
        adjustmentType: InventoryAdjustmentType,
        adjustmentReason: InventoryAdjustmentReason,
        adjustedQuantity: Int,
        notes: String
    ) -> Unit = { _, _, _, _, _, _ -> },
    onNavigateBack: () -> Unit = {}
) {
    var productId by remember { mutableStateOf("") }
    var locationId by remember { mutableStateOf("") }
    var adjustmentType by remember { mutableStateOf(InventoryAdjustmentType.INCREASE) }
    var adjustmentReason by remember { mutableStateOf(InventoryAdjustmentReason.PHYSICAL_COUNT) }
    var adjustedQuantityRaw by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Add Adjustment Item",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Adjustment: $adjustmentId | Warehouse: $warehouseId",
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

        // Adjustment Type selection
        Text(text = "Adjustment Type *", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InventoryAdjustmentType.entries.forEach { type ->
                FilterChip(
                    selected = adjustmentType == type,
                    onClick = { adjustmentType = type },
                    label = { Text(type.defaultLabel) }
                )
            }
        }

        // Available Quantity Display (Mandatory for DECREASE as per requirements)
        if (adjustmentType == InventoryAdjustmentType.DECREASE || availableQuantity != null) {
            availableQuantity?.let { qty ->
                Text(
                    text = "Current Availability: $qty units",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (qty > 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }
        }

        // Adjustment Reason selection
        Text(text = "Reason *", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(InventoryAdjustmentReason.entries) { reason ->
                FilterChip(
                    selected = adjustmentReason == reason,
                    onClick = { adjustmentReason = reason },
                    label = { Text(reason.defaultLabel) }
                )
            }
        }

        OutlinedTextField(
            value = adjustedQuantityRaw,
            onValueChange = { adjustedQuantityRaw = it.filter { c -> c.isDigit() } },
            label = { Text("New Adjusted Quantity *") },
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
                val qty = adjustedQuantityRaw.toIntOrNull() ?: 0
                onSubmit(productId.trim(), locationId.trim(), adjustmentType, adjustmentReason, qty, notes.trim())
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = productId.isNotBlank() && locationId.isNotBlank() && adjustedQuantityRaw.isNotBlank()
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
