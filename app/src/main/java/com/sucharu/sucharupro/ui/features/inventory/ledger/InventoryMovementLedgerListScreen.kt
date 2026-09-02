package com.sucharu.sucharupro.ui.features.inventory.ledger

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerEntry
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerType

/**
 * Screen displaying chronological inventory movement ledger (Module 07 Step 09).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryMovementLedgerListScreen(
    viewModel: InventoryMovementLedgerListViewModel,
    onNavigateToBalances: () -> Unit = {},
    onNavigateToValuation: () -> Unit = {},
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
                text = "Movement Ledger",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Row {
                IconButton(onClick = { viewModel.synchronizeLedger() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Sync")
                }
                OutlinedButton(onClick = onNavigateBack) { Text("Back") }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        
        InventoryLedgerSummaryCard(entries = uiState.entries)
        
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onNavigateToBalances, modifier = Modifier.weight(1f)) {
                Text("Balances")
            }
            OutlinedButton(onClick = onNavigateToValuation, modifier = Modifier.weight(1f)) {
                Text("Valuation")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Movement History",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Type Filter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = uiState.selectedTypeFilter == null,
                    onClick = { viewModel.onTypeFilterChanged(null) },
                    label = { Text("All") }
                )
            }
            items(InventoryMovementLedgerType.entries) { type ->
                FilterChip(
                    selected = uiState.selectedTypeFilter == type,
                    onClick = { viewModel.onTypeFilterChanged(type) },
                    label = { Text(type.name.replace("_", " ")) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.filteredEntries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No ledger entries found.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.filteredEntries) { entry ->
                    LedgerEntryListItem(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun LedgerEntryListItem(
    entry: InventoryMovementLedgerEntry
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.referenceId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                InventoryMovementTypeBadge(type = entry.movementType)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Product: ${entry.productId}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = "Location: ${entry.locationId}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Quantity: ${entry.quantity}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.quantity > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                entry.unitCost?.let {
                    Text(
                        text = "@ $it / unit",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Moved at: ${entry.movementAt}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
