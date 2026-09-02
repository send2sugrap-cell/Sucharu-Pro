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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryIssueType

/**
 * Form screen for creating a new stock-out / issue header (Module 07 Step 04).
 *
 * Captures: reference, warehouse ID, stock-out date, issue type, and notes.
 * Validation and persistence are delegated to the caller via [onSubmit].
 */
@Composable
fun InventoryStockOutFormScreen(
    onSubmit: (
        reference: String,
        warehouseId: String,
        stockOutDate: String,
        issueType: InventoryIssueType,
        sourceReference: String,
        notes: String
    ) -> Unit = { _, _, _, _, _, _ -> },
    onNavigateBack: () -> Unit = {}
) {
    var reference by remember { mutableStateOf("") }
    var warehouseId by remember { mutableStateOf("") }
    var stockOutDate by remember { mutableStateOf("") }
    var issueType by remember { mutableStateOf(InventoryIssueType.PRODUCTION) }
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
            text = "New Stock Issuance",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = reference,
            onValueChange = { reference = it },
            label = { Text("Stock-Out Reference *") },
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
            value = stockOutDate,
            onValueChange = { stockOutDate = it },
            label = { Text("Date (ISO 8601) *") },
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
                onSubmit(reference.trim(), warehouseId.trim(), stockOutDate.trim(), issueType, sourceReference.trim(), notes.trim())
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = reference.isNotBlank() && warehouseId.isNotBlank() && stockOutDate.isNotBlank()
        ) {
            Text("Create Stock-Out")
        }

        Button(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}
