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
 * Registration form for a new inventory lot (Module 07 Step 07).
 */
@Composable
fun InventoryLotFormScreen(
    onSubmit: (
        lotNo: String,
        productId: String,
        batchId: String?,
        notes: String?
    ) -> Unit = { _, _, _, _ -> },
    onNavigateBack: () -> Unit = {}
) {
    var lotNo by remember { mutableStateOf("") }
    var productId by remember { mutableStateOf("") }
    var batchId by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Register Lot",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = lotNo,
            onValueChange = { lotNo = it },
            label = { Text("Lot Number *") },
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
            value = batchId,
            onValueChange = { batchId = it },
            label = { Text("Batch ID (optional)") },
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
                    lotNo.trim(),
                    productId.trim(),
                    batchId.trim().takeIf { it.isNotBlank() },
                    notes.trim().takeIf { it.isNotBlank() }
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = lotNo.isNotBlank() && productId.isNotBlank()
        ) {
            Text("Register Lot")
        }

        Button(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}
