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
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransfer
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferStatus

/**
 * List screen for all stock transfer operations in a project (Module 07 Step 05).
 *
 * Features:
 * - Status filter chips
 * - Transfer list with status badge, reference, from/to warehouses
 * - Displays transferred vs expected quantity summary
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryStockTransferListScreen(
    viewModel: InventoryStockTransferListViewModel,
    onTransferClick: (String) -> Unit = {},
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
                text = "Stock Transfers",
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
            items(InventoryStockTransferStatus.entries) { status ->
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
        } else if (uiState.filteredTransfers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No stock transfer records found.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.filteredTransfers) { transfer ->
                    StockTransferListItem(transfer = transfer, onClick = { onTransferClick(transfer.transferId) })
                }
            }
        }
    }
}

@Composable
private fun StockTransferListItem(
    transfer: InventoryStockTransfer,
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
                    text = transfer.transferReference,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                InventoryStockTransferStatusBadge(status = transfer.status)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "From: ${transfer.fromWarehouseId} → To: ${transfer.toWarehouseId}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "Date: ${transfer.transferDate}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Transferred: ${transfer.transferredTotalQuantity} / Expected: ${transfer.expectedTotalQuantity}",
                fontSize = 12.sp,
                color = if (transfer.transferredTotalQuantity >= transfer.expectedTotalQuantity && transfer.expectedTotalQuantity > 0) 
                    Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "Created: ${transfer.createdAt} by ${transfer.createdBy}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
