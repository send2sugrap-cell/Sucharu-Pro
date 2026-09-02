package com.sucharu.sucharupro.ui.features.inventory.adjustment

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
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentLine
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentStatus
import com.sucharu.sucharupro.domain.validation.InventoryStockAdjustmentAuthorizationValidator
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Stock adjustment details screen showing header, lines, adjustment progress,
 * adjustment records summary, and full audit trail (Module 07 Step 06).
 */
@Composable
fun InventoryStockAdjustmentDetailsScreen(
    viewModel: InventoryStockAdjustmentDetailsViewModel,
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
                text = "Adjustment Details",
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

        if (uiState.adjustment == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = uiState.errorMessage ?: "Stock adjustment record not found.",
                    color = MaterialTheme.colorScheme.error
                )
            }
            return@Column
        }

        val adjustment = uiState.adjustment!!

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
                                text = adjustment.adjustmentReference,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            InventoryStockAdjustmentStatusBadge(status = adjustment.status)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Warehouse: ${adjustment.warehouseId}", fontSize = 14.sp)
                        Text("Date: ${adjustment.adjustmentDate}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!adjustment.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Notes: ${adjustment.notes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Items: ${adjustment.totalItemsAdjusted} | Total Change: ${adjustment.totalQuantityChange}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Created: ${adjustment.createdAt} by ${adjustment.createdBy}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
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
                AdjustmentActionButtons(
                    adjustment = adjustment,
                    userRole = userRole,
                    viewModel = viewModel,
                    onAddLine = { onAddLine(adjustment.adjustmentId) }
                )
            }

            // ── Adjustment Lines ──────────────────────────────────
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
                    AdjustmentLineCard(
                        line = line,
                        isEditable = adjustment.status == InventoryStockAdjustmentStatus.DRAFT,
                        onEdit = { onEditLine(line.adjustmentLineId) }
                    )
                }
            }

            // ── Adjustment Summary ──────────────────────────────────
            item {
                Text(
                    text = "Adjustment Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                InventoryStockAdjustmentSummaryCard(adjustmentRecords = uiState.adjustmentRecords)
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
private fun AdjustmentActionButtons(
    adjustment: com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustment,
    userRole: UserRole,
    viewModel: InventoryStockAdjustmentDetailsViewModel,
    onAddLine: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!adjustment.isTerminal) {
            when (adjustment.status) {
                InventoryStockAdjustmentStatus.DRAFT -> {
                    if (InventoryStockAdjustmentAuthorizationValidator.validateCreateEditPermission(userRole).isSuccess) {
                        Button(onClick = onAddLine, modifier = Modifier.weight(1f)) { Text("Add Item") }
                        Button(
                            onClick = { viewModel.submitAdjustment(adjustment.adjustmentId, "admin-01", "2026-08-17T12:00:00Z", userRole) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                            modifier = Modifier.weight(1f)
                        ) { Text("Submit") }
                    }
                }
                InventoryStockAdjustmentStatus.PENDING -> {
                    if (InventoryStockAdjustmentAuthorizationValidator.validateApprovePermission(userRole).isSuccess) {
                        Button(
                            onClick = { viewModel.approveAdjustment(adjustment.adjustmentId, "admin-01", "2026-08-17T12:00:00Z", userRole) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.weight(1f)
                        ) { Text("Approve") }
                    }
                }
                InventoryStockAdjustmentStatus.APPROVED, InventoryStockAdjustmentStatus.ADJUSTING -> {
                    if (InventoryStockAdjustmentAuthorizationValidator.validateProcessPermission(userRole).isSuccess) {
                        Button(
                            onClick = { viewModel.completeAdjustment(adjustment.adjustmentId, "manager-01", "2026-08-17T12:00:00Z", userRole) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4527A0)),
                            modifier = Modifier.weight(1f)
                        ) { Text("Complete") }
                    }
                }
                else -> {}
            }
            
            if ((adjustment.status == InventoryStockAdjustmentStatus.DRAFT || adjustment.status == InventoryStockAdjustmentStatus.PENDING) &&
                InventoryStockAdjustmentAuthorizationValidator.validateCancelPermission(userRole).isSuccess) {
                Button(
                    onClick = { viewModel.cancelAdjustment(adjustment.adjustmentId, "admin-01", "2026-08-17T12:00:00Z", userRole) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    modifier = Modifier.weight(1f)
                ) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun AdjustmentLineCard(
    line: InventoryStockAdjustmentLine,
    isEditable: Boolean,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = { if (isEditable) onEdit() }
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
            Text(text = "Location: ${line.locationId} | Reason: ${line.adjustmentReason.defaultLabel}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Current: ${line.currentQuantity} ${line.unit}", fontSize = 12.sp)
                Text(
                    text = "Adjusted: ${line.adjustedQuantity}", 
                    fontSize = 12.sp, 
                    color = if (line.quantityChange > 0) Color(0xFF2E7D32) else if (line.quantityChange < 0) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "(${if (line.quantityChange > 0) "+" else ""}${line.quantityChange})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun AuditTrailItem(event: com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentActivityEvent) {
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
