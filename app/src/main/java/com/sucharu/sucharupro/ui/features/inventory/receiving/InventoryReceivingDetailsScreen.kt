package com.sucharu.sucharupro.ui.features.inventory.receiving

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
import androidx.compose.material3.Divider
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
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingLine
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingLineStatus
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingStatus

/**
 * Receiving details screen showing header, lines, verification decisions,
 * stock-in summary, and full audit trail (Module 07 Step 03).
 */
@Composable
fun InventoryReceivingDetailsScreen(
    viewModel: InventoryReceivingDetailsViewModel,
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
                text = "Receiving Details",
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

        if (uiState.receiving == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = uiState.errorMessage ?: "Receiving not found.",
                    color = MaterialTheme.colorScheme.error
                )
            }
            return@Column
        }

        val receiving = uiState.receiving!!

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // ── Header Card ──────────────────────────────────────
            item {
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
                                text = receiving.receivingReference,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            InventoryReceivingStatusBadge(status = receiving.status)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Warehouse: ${receiving.warehouseId}", fontSize = 14.sp)
                        Text("Date: ${receiving.receivingDate}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!receiving.sourceReference.isNullOrBlank()) {
                            Text("Source Ref: ${receiving.sourceReference}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (!receiving.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Notes: ${receiving.notes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Accepted Total: ${receiving.acceptedTotalQuantity}  |  Rejected Total: ${receiving.rejectedTotalQuantity}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Created: ${receiving.createdAt} by ${receiving.createdBy}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        if (!receiving.completedAt.isNullOrBlank()) {
                            Text("Completed: ${receiving.completedAt} by ${receiving.completedBy}", fontSize = 11.sp, color = Color(0xFF2E7D32))
                        }
                        if (!receiving.cancelledAt.isNullOrBlank()) {
                            Text("Cancelled: ${receiving.cancelledAt} by ${receiving.cancelledBy}", fontSize = 11.sp, color = Color(0xFFC62828))
                        }
                    }
                }
            }

            // ── Error/Success Messages ────────────────────────────
            if (!uiState.errorMessage.isNullOrBlank() || !uiState.operationMessage.isNullOrBlank()) {
                item {
                    val isError = !uiState.errorMessage.isNullOrBlank()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isError) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                        )
                    ) {
                        Text(
                            text = uiState.errorMessage ?: uiState.operationMessage ?: "",
                            color = if (isError) Color(0xFFC62828) else Color(0xFF2E7D32),
                            modifier = Modifier.padding(12.dp),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // ── Action Buttons ────────────────────────────────────
            if (!receiving.isTerminal) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (receiving.status) {
                            InventoryReceivingStatus.DRAFT -> {
                                Button(
                                    onClick = { viewModel.submitReceiving(receiving.receivingId, "admin-01", "2026-08-17T12:00:00Z") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Submit") }
                            }
                            InventoryReceivingStatus.PENDING -> {
                                Button(
                                    onClick = { viewModel.startReceiving(receiving.receivingId, "warehouse-01", "2026-08-17T12:00:00Z") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Start Receiving") }
                            }
                            InventoryReceivingStatus.RECEIVING -> {
                                Button(
                                    onClick = { viewModel.completeReceiving(receiving.receivingId, "manager-01", "2026-08-17T12:00:00Z") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4527A0)),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Complete") }
                            }
                            else -> {}
                        }
                        if (receiving.status == InventoryReceivingStatus.DRAFT || receiving.status == InventoryReceivingStatus.PENDING) {
                            Button(
                                onClick = { viewModel.cancelReceiving(receiving.receivingId, "admin-01", "2026-08-17T12:00:00Z") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancel") }
                        }
                    }
                }
            }

            // ── Receiving Lines ───────────────────────────────────
            item {
                Text(
                    text = "Receiving Lines (${uiState.lines.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.lines.isEmpty()) {
                item {
                    Text(
                        text = "No lines added to this receiving.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            } else {
                items(uiState.lines) { line ->
                    ReceivingLineCard(
                        line = line,
                        onAccept = { viewModel.acceptLine(line.receivingLineId, "manager-01", "2026-08-17T12:00:00Z") },
                        onReject = { viewModel.rejectLine(line.receivingLineId, "Quality issue", "manager-01", "2026-08-17T12:00:00Z") }
                    )
                }
            }

            // ── Stock-In Summary ──────────────────────────────────
            item {
                Text(
                    text = "Stock-In Records",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                InventoryStockInSummaryCard(stockInRecords = uiState.stockInRecords)
            }

            // ── Audit Trail ───────────────────────────────────────
            if (uiState.auditEvents.isNotEmpty()) {
                item {
                    Text(
                        text = "Audit Trail (${uiState.auditEvents.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(uiState.auditEvents) { event ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = event.eventType.defaultLabel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(text = event.description, fontSize = 12.sp)
                            Text(
                                text = "${event.timestamp} | by ${event.actorId}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceivingLineCard(
    line: InventoryReceivingLine,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val lineStatusColor = when (line.lineStatus) {
        InventoryReceivingLineStatus.PENDING -> Color(0xFFF57F17)
        InventoryReceivingLineStatus.VERIFIED -> Color(0xFF1565C0)
        InventoryReceivingLineStatus.ACCEPTED -> Color(0xFF2E7D32)
        InventoryReceivingLineStatus.PARTIALLY_ACCEPTED -> Color(0xFF388E3C)
        InventoryReceivingLineStatus.REJECTED -> Color(0xFFC62828)
        InventoryReceivingLineStatus.CANCELLED -> Color(0xFF757575)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Product: ${line.inventoryProductId}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    text = line.lineStatus.defaultLabel.uppercase(),
                    color = lineStatusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
            Text(text = "Location: ${line.locationId}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Divider(modifier = Modifier.padding(vertical = 6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Expected: ${line.expectedQuantity}", fontSize = 12.sp)
                Text("Received: ${line.receivedQuantity}", fontSize = 12.sp)
                Text("Accepted: ${line.acceptedQuantity}", fontSize = 12.sp, color = Color(0xFF2E7D32))
                Text("Rejected: ${line.rejectedQuantity}", fontSize = 12.sp, color = Color(0xFFC62828))
            }
            if (!line.rejectionReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Rejection: ${line.rejectionReason}", fontSize = 11.sp, color = Color(0xFFC62828))
            }
            // Action buttons for VERIFIED lines
            if (line.lineStatus == InventoryReceivingLineStatus.VERIFIED) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) { Text("Accept", fontSize = 12.sp) }
                    Button(
                        onClick = onReject,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                    ) { Text("Reject", fontSize = 12.sp) }
                }
            }
        }
    }
}
