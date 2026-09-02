package com.sucharu.sucharupro.ui.features.inventory.reorder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.inventory.reorder.InventoryReorderAlert
import com.sucharu.sucharupro.domain.model.inventory.reorder.InventoryReorderAlertStatus
import com.sucharu.sucharupro.domain.model.inventory.reorder.InventoryStockLevelPolicy
import com.sucharu.sucharupro.domain.validation.InventoryReorderAuthorizationValidator

/**
 * Details screen for a specific reorder alert (Module 07 Step 08).
 *
 * Displays current stock vs thresholds, policy information, and life-cycle actions.
 * Actions are RBAC-gated.
 */
@Composable
fun InventoryReorderDetailsScreen(
    viewModel: InventoryReorderDetailsViewModel,
    currentUserId: String,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Alert Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.alert != null) {
                AlertDetailsContent(
                    alert = uiState.alert!!,
                    policy = uiState.policy,
                    uiState = uiState,
                    currentUserId = currentUserId,
                    onAcknowledge = { viewModel.acknowledgeAlert(uiState.alert!!.alertId, currentUserId) },
                    onResolve = { viewModel.resolveAlert(uiState.alert!!.alertId, currentUserId) }
                )
            } else if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AlertDetailsContent(
    alert: InventoryReorderAlert,
    policy: InventoryStockLevelPolicy?,
    uiState: InventoryReorderDetailsUiState,
    currentUserId: String,
    onAcknowledge: () -> Unit,
    onResolve: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = alert.alertType.defaultLabel,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (alert.alertType.priority) {
                            4 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    InventoryReorderAlertStatusBadge(status = alert.status)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "ID: ${alert.alertId}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                Text(text = "Product: ${alert.productId}", fontWeight = FontWeight.SemiBold)
                Text(text = "Location: ${alert.locationId}", fontSize = 14.sp)
                Text(text = "Detected: ${alert.detectedAt}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stock Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Stock Comparison", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Available Quantity")
                    Text(text = "${alert.availableQuantity} Units", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Trigger Threshold")
                    Text(text = "${alert.thresholdQuantity} Units", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Policy Info Card
        if (policy != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Stock Policy Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    PolicyDetailRow(label = "Minimum Level", value = policy.minimumStockLevel.toString())
                    PolicyDetailRow(label = "Critical Level", value = policy.criticalStockLevel.toString())
                    PolicyDetailRow(label = "Reorder Point", value = policy.reorderPoint.toString())
                    PolicyDetailRow(label = "Target Level", value = policy.targetStockLevel.toString())
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.operationMessage != null) {
            Text(
                text = uiState.operationMessage,
                color = Color(0xFF2E7D32),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Actions
        if (!alert.status.isTerminal) {
            val canAcknowledge = InventoryReorderAuthorizationValidator.validateAcknowledgePermission(uiState.currentUserRole) is com.sucharu.sucharupro.domain.model.common.DomainResult.Success
            val canResolve = InventoryReorderAuthorizationValidator.validateManageAlertsPermission(uiState.currentUserRole) is com.sucharu.sucharupro.domain.model.common.DomainResult.Success

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (alert.status == InventoryReorderAlertStatus.OPEN && canAcknowledge) {
                    Button(
                        onClick = onAcknowledge,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Acknowledge Alert")
                    }
                }
                
                if (canResolve) {
                    OutlinedButton(
                        onClick = onResolve,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Resolve Alert")
                    }
                }
                
                if (canResolve) {
                    OutlinedButton(
                        onClick = { /* Dismiss logic */ },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Dismiss Alert")
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text(
                    text = "This alert is ${alert.status.defaultLabel}.",
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun PolicyDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
