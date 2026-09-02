package com.sucharu.sucharupro.ui.features.inventory.stocktransfer

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
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferLine
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferStatus
import com.sucharu.sucharupro.domain.validation.InventoryStockTransferAuthorizationValidator
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Stock transfer details screen showing header, lines, transfer progress,
 * transfer records summary, and full audit trail (Module 07 Step 05).
 */
@Composable
fun InventoryStockTransferDetailsScreen(
    viewModel: InventoryStockTransferDetailsViewModel,
    userRole: UserRole = UserRole.ADMIN,
    onNavigateBack: () -> Unit = {},
    onAddLine: (String) -> Unit = {},
    onEditLine: (String) -> Unit = {}
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
                text = "Transfer Details",
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

        if (uiState.transfer == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = uiState.errorMessage ?: "Stock transfer record not found.",
                    color = MaterialTheme.colorScheme.error
                )
            }
            return@Column
        }

        val transfer = uiState.transfer!!

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
                                text = transfer.transferReference,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            InventoryStockTransferStatusBadge(status = transfer.status)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("From: ${transfer.fromWarehouseId} → To: ${transfer.toWarehouseId}", fontSize = 14.sp)
                        Text("Date: ${transfer.transferDate}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!transfer.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Notes: ${transfer.notes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Transferred Total: ${transfer.transferredTotalQuantity} / Expected: ${transfer.expectedTotalQuantity}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Created: ${transfer.createdAt} by ${transfer.createdBy}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            // ── Messages ─────────────────────────────────────────
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

            // ── Action Buttons (RBAC Gated) ───────────────────────
            item {
                TransferActionButtons(
                    transfer = transfer,
                    userRole = userRole,
                    viewModel = viewModel,
                    onAddLine = { onAddLine(transfer.transferId) }
                )
            }

            // ── Transfer Lines ──────────────────────────────────
            item {
                Text(
                    text = "Items (${uiState.lines.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.lines.isEmpty()) {
                item {
                    Text(
                        text = "No items added yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            } else {
                items(uiState.lines) { line ->
                    TransferLineCard(
                        line = line,
                        isEditable = transfer.status == InventoryStockTransferStatus.DRAFT,
                        onEdit = { onEditLine(line.transferLineId) }
                    )
                }
            }

            // ── Transfer Summary ──────────────────────────────────
            item {
                Text(
                    text = "Transfer Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                InventoryStockTransferSummaryCard(transferRecords = uiState.transferRecords)
            }

            // ── Audit Trail ───────────────────────────────────────
            if (uiState.auditEvents.isNotEmpty()) {
                item {
                    Text(
                        text = "History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(uiState.auditEvents) { event ->
                    AuditTrailItem(event = event)
                }
            }
        }
    }
}

@Composable
private fun TransferActionButtons(
    transfer: com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransfer,
    userRole: UserRole,
    viewModel: InventoryStockTransferDetailsViewModel,
    onAddLine: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!transfer.isTerminal) {
            when (transfer.status) {
                InventoryStockTransferStatus.DRAFT -> {
                    if (InventoryStockTransferAuthorizationValidator.validateCreateEditPermission(userRole).isSuccess) {
                        Button(onClick = onAddLine, modifier = Modifier.weight(1f)) { Text("Add Item") }
                        Button(
                            onClick = { viewModel.submitTransfer(transfer.transferId, "admin-01", "2026-08-17T12:00:00Z", userRole) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                            modifier = Modifier.weight(1f)
                        ) { Text("Submit") }
                    }
                }
                InventoryStockTransferStatus.PENDING -> {
                    if (InventoryStockTransferAuthorizationValidator.validateApprovePermission(userRole).isSuccess) {
                        Button(
                            onClick = { viewModel.approveTransfer(transfer.transferId, "admin-01", "2026-08-17T12:00:00Z", userRole) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.weight(1f)
                        ) { Text("Approve") }
                    }
                }
                InventoryStockTransferStatus.APPROVED, InventoryStockTransferStatus.TRANSFERRING -> {
                    if (InventoryStockTransferAuthorizationValidator.validateCompletePermission(userRole).isSuccess) {
                        Button(
                            onClick = { viewModel.completeTransfer(transfer.transferId, "manager-01", "2026-08-17T12:00:00Z", userRole) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4527A0)),
                            modifier = Modifier.weight(1f)
                        ) { Text("Complete") }
                    }
                }
                else -> {}
            }
            
            if ((transfer.status == InventoryStockTransferStatus.DRAFT || transfer.status == InventoryStockTransferStatus.PENDING) &&
                InventoryStockTransferAuthorizationValidator.validateCancelPermission(userRole).isSuccess) {
                Button(
                    onClick = { viewModel.cancelTransfer(transfer.transferId, "admin-01", "2026-08-17T12:00:00Z", userRole) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    modifier = Modifier.weight(1f)
                ) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun TransferLineCard(
    line: InventoryStockTransferLine,
    isEditable: Boolean,
    onEdit: () -> Unit
) {
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
                if (isEditable) {
                    Text(
                        text = "EDIT",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
            Text(text = "From: ${line.fromLocationId} → To: ${line.toLocationId}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Divider(modifier = Modifier.padding(vertical = 6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Expected: ${line.expectedQuantity} ${line.unit}", fontSize = 12.sp)
                Text(
                    text = "Transferred: ${line.transferredQuantity}", 
                    fontSize = 12.sp, 
                    color = if (line.isFulfilled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AuditTrailItem(event: com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferActivityEvent) {
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
