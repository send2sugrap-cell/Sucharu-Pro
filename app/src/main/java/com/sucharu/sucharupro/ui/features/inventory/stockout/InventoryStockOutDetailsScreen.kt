package com.sucharu.sucharupro.ui.features.inventory.stockout

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
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutLine
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutStatus
import com.sucharu.sucharupro.domain.validation.InventoryStockOutAuthorizationValidator
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Stock-out details screen showing header, lines, issuance progress,
 * stock-out records summary, and full audit trail (Module 07 Step 04).
 */
@Composable
fun InventoryStockOutDetailsScreen(
    viewModel: InventoryStockOutDetailsViewModel,
    userRole: UserRole = UserRole.ADMIN, // Mocked or passed from navigation/auth
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
                text = "Stock-Out Details",
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

        if (uiState.stockOut == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = uiState.errorMessage ?: "Stock-out record not found.",
                    color = MaterialTheme.colorScheme.error
                )
            }
            return@Column
        }

        val stockOut = uiState.stockOut!!

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
                                text = stockOut.stockOutReference,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            InventoryStockOutStatusBadge(status = stockOut.status)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Warehouse: ${stockOut.warehouseId}", fontSize = 14.sp)
                        Text("Issue Type: ${stockOut.issueType.name}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Date: ${stockOut.stockOutDate}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!stockOut.sourceReference.isNullOrBlank()) {
                            Text("Source Ref: ${stockOut.sourceReference}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (!stockOut.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Notes: ${stockOut.notes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Issued Total: ${stockOut.issuedTotalQuantity} / Expected: ${stockOut.expectedTotalQuantity}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Created: ${stockOut.createdAt} by ${stockOut.createdBy}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
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
                StockOutActionButtons(
                    stockOut = stockOut,
                    userRole = userRole,
                    viewModel = viewModel,
                    onAddLine = { onAddLine(stockOut.stockOutId) }
                )
            }

            // ── Stock-Out Lines ──────────────────────────────────
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
                    StockOutLineCard(
                        line = line,
                        isEditable = stockOut.status == InventoryStockOutStatus.DRAFT,
                        onEdit = { onEditLine(line.stockOutLineId) }
                    )
                }
            }

            // ── Issuance Summary ──────────────────────────────────
            item {
                Text(
                    text = "Issuance Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                InventoryStockOutSummaryCard(stockOutRecords = uiState.stockOutRecords)
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
private fun StockOutActionButtons(
    stockOut: com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOut,
    userRole: UserRole,
    viewModel: InventoryStockOutDetailsViewModel,
    onAddLine: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!stockOut.isTerminal) {
            when (stockOut.status) {
                InventoryStockOutStatus.DRAFT -> {
                    if (InventoryStockOutAuthorizationValidator.validateCreateEditPermission(userRole).isSuccess) {
                        Button(onClick = onAddLine, modifier = Modifier.weight(1f)) { Text("Add Item") }
                        Button(
                            onClick = { viewModel.submitStockOut(stockOut.stockOutId, "admin-01", "2026-08-17T12:00:00Z", userRole) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                            modifier = Modifier.weight(1f)
                        ) { Text("Submit") }
                    }
                }
                InventoryStockOutStatus.PENDING -> {
                    if (InventoryStockOutAuthorizationValidator.validateApprovePermission(userRole).isSuccess) {
                        Button(
                            onClick = { viewModel.approveStockOut(stockOut.stockOutId, "admin-01", "2026-08-17T12:00:00Z", userRole) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.weight(1f)
                        ) { Text("Approve") }
                    }
                }
                InventoryStockOutStatus.ISSUING -> {
                    if (InventoryStockOutAuthorizationValidator.validateCompletePermission(userRole).isSuccess) {
                        Button(
                            onClick = { viewModel.completeStockOut(stockOut.stockOutId, "manager-01", "2026-08-17T12:00:00Z", userRole) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4527A0)),
                            modifier = Modifier.weight(1f)
                        ) { Text("Complete") }
                    }
                }
                else -> {}
            }
            
            if ((stockOut.status == InventoryStockOutStatus.DRAFT || stockOut.status == InventoryStockOutStatus.PENDING) &&
                InventoryStockOutAuthorizationValidator.validateCancelPermission(userRole).isSuccess) {
                Button(
                    onClick = { viewModel.cancelStockOut(stockOut.stockOutId, "admin-01", "2026-08-17T12:00:00Z", userRole) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    modifier = Modifier.weight(1f)
                ) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun StockOutLineCard(
    line: InventoryStockOutLine,
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
            Text(text = "Location: ${line.locationId}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Divider(modifier = Modifier.padding(vertical = 6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Expected: ${line.expectedQuantity} ${line.unit}", fontSize = 12.sp)
                Text(
                    text = "Issued: ${line.issuedQuantity}", 
                    fontSize = 12.sp, 
                    color = if (line.isFulfilled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AuditTrailItem(event: com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutActivityEvent) {
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
