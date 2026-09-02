package com.sucharu.sucharupro.ui.features.inventory.reorder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.inventory.reorder.InventoryStockLevelPolicy

/**
 * Form screen for configuring Stock Level Policies (Module 07 Step 08).
 *
 * Allows setting thresholds: Minimum, Reorder, Critical, Target, and Maximum.
 */
@Composable
fun InventoryStockLevelPolicyFormScreen(
    initialPolicy: InventoryStockLevelPolicy? = null,
    onSubmit: (InventoryStockLevelPolicy) -> Unit,
    onNavigateBack: () -> Unit
) {
    var productId by remember { mutableStateOf(initialPolicy?.productId ?: "") }
    var locationId by remember { mutableStateOf(initialPolicy?.locationId ?: "") }
    var minLevel by remember { mutableStateOf(initialPolicy?.minimumStockLevel?.toString() ?: "0.0") }
    var criticalLevel by remember { mutableStateOf(initialPolicy?.criticalStockLevel?.toString() ?: "0.0") }
    var reorderPoint by remember { mutableStateOf(initialPolicy?.reorderPoint?.toString() ?: "0.0") }
    var targetLevel by remember { mutableStateOf(initialPolicy?.targetStockLevel?.toString() ?: "0.0") }
    var maxLevel by remember { mutableStateOf(initialPolicy?.maximumStockLevel?.toString() ?: "0.0") }
    var enabled by remember { mutableStateOf(initialPolicy?.enabled ?: true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (initialPolicy == null) "New Stock Policy" else "Edit Stock Policy",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = productId,
            onValueChange = { productId = it },
            label = { Text("Product ID *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = initialPolicy == null
        )

        OutlinedTextField(
            value = locationId,
            onValueChange = { locationId = it },
            label = { Text("Location ID (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = initialPolicy == null
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "Policy Enabled", fontWeight = FontWeight.Medium)
                Text(text = "Whether to trigger alerts for this product", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            }
            Switch(checked = enabled, onCheckedChange = { enabled = it })
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Threshold Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        PolicyNumberField(label = "Minimum Stock Level", value = minLevel, onValueChange = { minLevel = it })
        PolicyNumberField(label = "Critical Stock Level", value = criticalLevel, onValueChange = { criticalLevel = it })
        PolicyNumberField(label = "Reorder Point", value = reorderPoint, onValueChange = { reorderPoint = it })
        PolicyNumberField(label = "Target Stock Level", value = targetLevel, onValueChange = { targetLevel = it })
        PolicyNumberField(label = "Maximum Stock Level", value = maxLevel, onValueChange = { maxLevel = it })

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val policy = InventoryStockLevelPolicy(
                    policyId = initialPolicy?.policyId ?: "POLICY-${System.currentTimeMillis()}",
                    projectId = initialPolicy?.projectId ?: "PROJECT-001", // Placeholder
                    productId = productId.trim(),
                    locationId = locationId.trim().ifBlank { null },
                    minimumStockLevel = minLevel.toDoubleOrNull() ?: 0.0,
                    criticalStockLevel = criticalLevel.toDoubleOrNull() ?: 0.0,
                    reorderPoint = reorderPoint.toDoubleOrNull() ?: 0.0,
                    targetStockLevel = targetLevel.toDoubleOrNull() ?: 0.0,
                    maximumStockLevel = maxLevel.toDoubleOrNull() ?: 0.0,
                    enabled = enabled
                )
                onSubmit(policy)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = productId.isNotBlank()
        ) {
            Text(if (initialPolicy == null) "Create Policy" else "Update Policy")
        }

        OutlinedButton(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}

@Composable
private fun PolicyNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true
    )
}
