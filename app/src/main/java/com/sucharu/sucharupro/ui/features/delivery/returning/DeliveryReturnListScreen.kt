package com.sucharu.sucharupro.ui.features.delivery.returning

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturn
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnStatus
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnType
import com.sucharu.sucharupro.domain.model.user.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryReturnListScreen(
    projectId: String,
    viewModel: DeliveryReturnListViewModel,
    currentUserRole: UserRole,
    onReturnClick: (String) -> Unit,
    onCreateReturnClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.loadReturns(projectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Delivery Returns & Reverse Logistics") }
            )
        },
        floatingActionButton = {
            if (currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.WAREHOUSE, UserRole.STAFF)) {
                FloatingActionButton(onClick = onCreateReturnClick) {
                    Icon(Icons.Default.Add, contentDescription = "Create Return")
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                label = { Text("Search by Return No / Order / Customer") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            // Status Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedStatusFilter == null,
                        onClick = { viewModel.onStatusFilterSelected(null) },
                        label = { Text("All Statuses") }
                    )
                }
                items(DeliveryReturnStatus.values()) { status ->
                    FilterChip(
                        selected = uiState.selectedStatusFilter == status,
                        onClick = { viewModel.onStatusFilterSelected(status) },
                        label = { Text(status.defaultLabel) }
                    )
                }
            }

            // Return Type Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedTypeFilter == null,
                        onClick = { viewModel.onTypeFilterSelected(null) },
                        label = { Text("All Types") }
                    )
                }
                items(DeliveryReturnType.values()) { type ->
                    FilterChip(
                        selected = uiState.selectedTypeFilter == type,
                        onClick = { viewModel.onTypeFilterSelected(type) },
                        label = { Text(type.defaultLabel) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.filteredReturns.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (uiState.searchQuery.isNotBlank() || uiState.selectedStatusFilter != null)
                                "No delivery returns match the selected filters."
                            else "No delivery returns found for this project.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.filteredReturns, key = { it.returnId }) { ret ->
                            DeliveryReturnCard(
                                item = ret,
                                onClick = { onReturnClick(ret.returnId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeliveryReturnCard(
    item: DeliveryReturn,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.returnNo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DeliveryReturnPriorityBadge(priority = item.priority)
                    DeliveryReturnStatusBadge(status = item.status)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Order: ${item.deliveryOrderId} • Customer: ${item.customerId ?: "N/A"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Type: ${item.returnType.defaultLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Reason: ${item.returnReason.defaultLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
