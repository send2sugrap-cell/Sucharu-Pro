package com.sucharu.sucharupro.ui.features.finance.payable

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.finance.VendorPayableAgingBucket
import com.sucharu.sucharupro.domain.model.finance.VendorPayableStatus
import com.sucharu.sucharupro.domain.model.user.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPayableListScreen(
    viewModel: VendorPayableListViewModel,
    callerRole: UserRole,
    onPayableClick: (String) -> Unit,
    onCreatePayableClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vendor Payables & Bills") }
            )
        },
        floatingActionButton = {
            if (callerRole.isInternal) {
                FloatingActionButton(
                    onClick = onCreatePayableClick,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Vendor Payable")
                }
            }
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            state.summary?.let { summary ->
                VendorPayableSummaryCard(summary = summary)
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                placeholder = { Text("Search by payable #, vendor, invoice...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VendorPayableStatus.values().forEach { status ->
                    FilterChip(
                        selected = state.selectedStatus == status,
                        onClick = { viewModel.onStatusSelected(status) },
                        label = { Text(status.defaultLabel, fontSize = 12.sp) }
                    )
                }
            }

            // Aging Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VendorPayableAgingBucket.values().forEach { bucket ->
                    FilterChip(
                        selected = state.selectedAgingBucket == bucket,
                        onClick = { viewModel.onAgingBucketSelected(bucket) },
                        label = { Text(bucket.defaultLabel, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.errorMessage != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.errorMessage ?: "Unknown error", color = MaterialTheme.colorScheme.error)
                    }
                }
                state.filteredPayables.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (state.payables.isEmpty()) "No vendor payables recorded." else "No matching payables found.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.filteredPayables, key = { it.payableId }) { payable ->
                            VendorPayableCard(
                                payable = payable,
                                onClick = { onPayableClick(payable.payableId) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(64.dp)) }
                    }
                }
            }
        }
    }
}
