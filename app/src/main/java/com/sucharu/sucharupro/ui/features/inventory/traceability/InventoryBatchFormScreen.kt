package com.sucharu.sucharupro.ui.features.inventory.traceability

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
 * Registration form for a new production batch (Module 07 Step 07).
 */
@Composable
fun InventoryBatchFormScreen(
    onSubmit: (
        batchNo: String,
        productId: String,
        prodRefId: String?,
        prodRefType: String?,
        notes: String?
    ) -> Unit = { _, _, _, _, _ -> },
    onNavigateBack: () -> Unit = {}
) {
    var batchNo by remember { mutableStateOf("") }
    var productId by remember { mutableStateOf("") }
    var prodRefId by remember { mutableStateOf("") }
    var prodRefType by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Register Batch",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = batchNo,
            onValueChange = { batchNo = it },
            label = { Text("Batch Number *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = productId,
            onValueChange = { productId = it },
            label = { Text("Product ID *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = prodRefId,
            onValueChange = { prodRefId = it },
            label = { Text("Production Ref ID (optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = prodRefType,
            onValueChange = { prodRefType = it },
            label = { Text("Production Ref Type (optional)") },
            placeholder = { Text("e.g. MO, PO") },
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
                onSubmit(
                    batchNo.trim(),
                    productId.trim(),
                    prodRefId.trim().takeIf { it.isNotBlank() },
                    prodRefType.trim().takeIf { it.isNotBlank() },
                    notes.trim().takeIf { it.isNotBlank() }
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = batchNo.isNotBlank() && productId.isNotBlank()
        ) {
            Text("Register Batch")
        }

        Button(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}
