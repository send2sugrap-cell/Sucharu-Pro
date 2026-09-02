package com.sucharu.sucharupro.ui.features.inventory.traceability

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryTraceabilityActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryTraceabilityRecord
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryTraceabilityStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.validation.InventoryTraceabilityAuthorizationValidator

/**
 * Details screen for Batch and Lot traceability (Module 07 Step 07).
 */
@Composable
fun InventoryTraceabilityDetailsScreen(
    viewModel: InventoryTraceabilityDetailsViewModel,
    userRole: UserRole = UserRole.ADMIN,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Traceability Details",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(onClick = onNavigateBack) { Text("Back") }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        val title = uiState.batch?.let { "Batch: ${it.batchNo}" } ?: uiState.lot?.let { "Lot: ${it.lotNo}" } ?: "Unknown"
        val status = uiState.batch?.status ?: uiState.lot?.status ?: InventoryTraceabilityStatus.CLOSED
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            
            // ── Summary Card ──────────────────────────────────────
            item {
                InventoryTraceabilitySummaryCard(
                    title = title,
                    status = status,
                    totalQuantity = uiState.totalQuantity,
                    unit = uiState.unit
                )
            }

            // ── Identity & Metadata ────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Identity", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (uiState.batch != null) {
                            MetadataRow(label = "Batch ID", value = uiState.batch!!.batchId)
                            MetadataRow(label = "Product ID", value = uiState.batch!!.productId)
                            MetadataRow(label = "Production Ref", value = uiState.batch!!.productionReferenceId ?: "N/A")
                            MetadataRow(label = "Created At", value = uiState.batch!!.createdAt)
                        } else if (uiState.lot != null) {
                            MetadataRow(label = "Lot ID", value = uiState.lot!!.lotId)
                            MetadataRow(label = "Product ID", value = uiState.lot!!.productId)
                            MetadataRow(label = "Parent Batch", value = uiState.lot!!.batchId ?: "None")
                            MetadataRow(label = "Created At", value = uiState.lot!!.createdAt)
                        }
                    }
                }
            }

            // ── Action Buttons ────────────────────────────────────
            item {
                TraceabilityActionButtons(
                    status = status,
                    userRole = userRole,
                    onUpdateStatus = { newStatus -> 
                        viewModel.updateStatus(newStatus, "user-01", "System User")
                    }
                )
            }

            // ── Trace History ─────────────────────────────────────
            item {
                Text(
                    text = "Trace History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.traceHistory.isEmpty()) {
                item {
                    Text(text = "No history recorded.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(uiState.traceHistory) { entry ->
                    when (entry) {
                        is InventoryTraceabilityRecord -> TraceRecordCard(record = entry)
                        is InventoryTraceabilityActivityEvent -> TraceActivityCard(event = entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun TraceabilityActionButtons(
    status: InventoryTraceabilityStatus,
    userRole: UserRole,
    onUpdateStatus: (InventoryTraceabilityStatus) -> Unit
) {
    val canModify = InventoryTraceabilityAuthorizationValidator.validateStatusChangePermission(userRole).isSuccess
    
    if (canModify && !status.isTerminal) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (status == InventoryTraceabilityStatus.ACTIVE) {
                Button(
                    onClick = { onUpdateStatus(InventoryTraceabilityStatus.HOLD) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBC02D)),
                    modifier = Modifier.weight(1f)
                ) { Text("Place Hold") }
            } else if (status == InventoryTraceabilityStatus.HOLD) {
                Button(
                    onClick = { onUpdateStatus(InventoryTraceabilityStatus.ACTIVE) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.weight(1f)
                ) { Text("Release Hold") }
            }

            Button(
                onClick = { onUpdateStatus(InventoryTraceabilityStatus.CLOSED) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                modifier = Modifier.weight(1f)
            ) { Text("Close") }
        }
    }
}

@Composable
private fun TraceRecordCard(record: InventoryTraceabilityRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = record.movementType.defaultLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${record.quantity} ${record.unit.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Text(text = "Location: ${record.locationId}", fontSize = 11.sp)
            Text(text = "${record.timestamp} | by ${record.actorId}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun TraceActivityCard(event: InventoryTraceabilityActivityEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = event.eventType.defaultLabel,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(text = event.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "${event.timestamp} | by ${event.actorId}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}
