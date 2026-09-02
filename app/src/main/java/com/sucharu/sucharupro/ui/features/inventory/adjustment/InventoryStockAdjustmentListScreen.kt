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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustment
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentStatus

/**
 * List screen for all stock adjustment operations in a project (Module 07 Step 06).
 *
 * Features:
 * - Status filter chips
 * - Adjustment list with status badge, reference, and warehouse
 * - Displays total items and quantity change summary
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryStockAdjustmentListScreen(
    viewModel: InventoryStockAdjustmentListViewModel,
    onAdjustmentClick: (String) -> Unit = {},
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
                text = "Stock Adjustments",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(onClick = onNavigateBack) { Text("Back") }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Status Filter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = uiState.selectedStatusFilter == null,
                    onClick = { viewModel.onStatusFilterChanged(null) },
                    label = { Text("All") }
                )
            }
            items(InventoryStockAdjustmentStatus.entries) { status ->
                FilterChip(
                    selected = uiState.selectedStatusFilter == status,
                    onClick = { viewModel.onStatusFilterChanged(status) },
                    label = { Text(status.defaultLabel) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!uiState.errorMessage.isNullOrBlank()) {
            Text(
                text = uiState.errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.filteredAdjustments.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No stock adjustment records found.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.filteredAdjustments) { adjustment ->
                    StockAdjustmentListItem(adjustment = adjustment, onClick = { onAdjustmentClick(adjustment.adjustmentId) })
                }
            }
        }
    }
}

@Composable
private fun StockAdjustmentListItem(
    adjustment: InventoryStockAdjustment,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = adjustment.adjustmentReference,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                InventoryStockAdjustmentStatusBadge(status = adjustment.status)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Warehouse: ${adjustment.warehouseId}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "Date: ${adjustment.adjustmentDate}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(modifier = Modifier.height(4.dp))
            val changeColor = if (adjustment.totalQuantityChange > 0) Color(0xFF2E7D32) else if (adjustment.totalQuantityChange < 0) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant
            Text(
                text = "Items: ${adjustment.totalItemsAdjusted} | Change: ${if (adjustment.totalQuantityChange > 0) "+" else ""}${adjustment.totalQuantityChange}",
                fontSize = 12.sp,
                color = changeColor,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "Created: ${adjustment.createdAt} by ${adjustment.createdBy}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
