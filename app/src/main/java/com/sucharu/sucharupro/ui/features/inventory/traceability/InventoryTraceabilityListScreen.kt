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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryBatch
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryLot
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryTraceabilityStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.validation.InventoryTraceabilityAuthorizationValidator

/**
 * List screen for Batch and Lot traceability records (Module 07 Step 07).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryTraceabilityListScreen(
    viewModel: InventoryTraceabilityListViewModel,
    userRole: UserRole = UserRole.ADMIN,
    onBatchClick: (String) -> Unit = {},
    onLotClick: (String) -> Unit = {},
    onRegisterBatch: () -> Unit = {},
    onRegisterLot: () -> Unit = {},
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
                text = "Traceability",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(onClick = onNavigateBack) { Text("Back") }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons (RBAC Gated)
        if (InventoryTraceabilityAuthorizationValidator.validateRegisterPermission(userRole).isSuccess) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRegisterBatch,
                    modifier = Modifier.weight(1f)
                ) { Text("Register Batch") }
                Button(
                    onClick = onRegisterLot,
                    modifier = Modifier.weight(1f)
                ) { Text("Register Lot") }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // View Mode Toggle (Batch vs Lot)
        TabRow(selectedTabIndex = if (uiState.viewMode == TraceabilityViewMode.BATCH) 0 else 1) {
            Tab(
                selected = uiState.viewMode == TraceabilityViewMode.BATCH,
                onClick = { viewModel.setViewMode(TraceabilityViewMode.BATCH) },
                text = { Text("Batches") }
            )
            Tab(
                selected = uiState.viewMode == TraceabilityViewMode.LOT,
                onClick = { viewModel.setViewMode(TraceabilityViewMode.LOT) },
                text = { Text("Lots") }
            )
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
            items(InventoryTraceabilityStatus.entries) { status ->
                FilterChip(
                    selected = uiState.selectedStatusFilter == status,
                    onClick = { viewModel.onStatusFilterChanged(status) },
                    label = { Text(status.defaultLabel) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val isEmpty = if (uiState.viewMode == TraceabilityViewMode.BATCH) uiState.filteredBatches.isEmpty() else uiState.filteredLots.isEmpty()
            
            if (isEmpty) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No records found.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (uiState.viewMode == TraceabilityViewMode.BATCH) {
                        items(uiState.filteredBatches) { batch ->
                            BatchListItem(batch = batch, onClick = { onBatchClick(batch.batchId) })
                        }
                    } else {
                        items(uiState.filteredLots) { lot ->
                            LotListItem(lot = lot, onClick = { onLotClick(lot.lotId) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BatchListItem(batch: InventoryBatch, onClick: () -> Unit) {
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
                    text = "Batch: ${batch.batchNo}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                InventoryTraceabilityStatusBadge(status = batch.status)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Product: ${batch.productId}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!batch.productionReferenceId.isNullOrBlank()) {
                Text(text = "Prod Ref: ${batch.productionReferenceId}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(text = "Created: ${batch.createdAt}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun LotListItem(lot: InventoryLot, onClick: () -> Unit) {
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
                    text = "Lot: ${lot.lotNo}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                InventoryTraceabilityStatusBadge(status = lot.status)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Product: ${lot.productId}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!lot.batchId.isNullOrBlank()) {
                Text(text = "Parent Batch: ${lot.batchId}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(text = "Created: ${lot.createdAt}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}
