package com.sucharu.sucharupro.ui.features.finance.receivable

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivableStatus
import com.sucharu.sucharupro.domain.model.finance.ReceivableAgingBucket

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerReceivableListScreen(
    viewModel: CustomerReceivableListViewModel,
    onNavigateToDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customer Receivables & Due", fontWeight = FontWeight.Bold) }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            uiState.summary?.let { summary ->
                CustomerDueSummaryCard(summary = summary)
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                placeholder = { Text("Search by Receivable No, Customer, Ref...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Status Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedStatusFilter == null,
                    onClick = { viewModel.onStatusFilterSelected(null) },
                    label = { Text("All Status") }
                )
                CustomerReceivableStatus.entries.forEach { status ->
                    FilterChip(
                        selected = uiState.selectedStatusFilter == status,
                        onClick = { viewModel.onStatusFilterSelected(status) },
                        label = { Text(status.defaultLabel) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Aging Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedAgingFilter == null,
                    onClick = { viewModel.onAgingFilterSelected(null) },
                    label = { Text("All Aging") }
                )
                ReceivableAgingBucket.entries.forEach { aging ->
                    FilterChip(
                        selected = uiState.selectedAgingFilter == aging,
                        onClick = { viewModel.onAgingFilterSelected(aging) },
                        label = { Text(aging.defaultLabel) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val filtered = uiState.receivables.filter { rec ->
                    val matchesStatus = uiState.selectedStatusFilter == null || rec.status == uiState.selectedStatusFilter
                    val matchesAging = uiState.selectedAgingFilter == null || rec.agingBucket == uiState.selectedAgingFilter
                    val matchesQuery = uiState.searchQuery.isBlank() ||
                            rec.receivableNo.contains(uiState.searchQuery, ignoreCase = true) ||
                            rec.customerId.contains(uiState.searchQuery, ignoreCase = true) ||
                            rec.referenceId.contains(uiState.searchQuery, ignoreCase = true)
                    matchesStatus && matchesAging && matchesQuery
                }

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No customer receivables found.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered, key = { it.receivableId }) { receivable ->
                            CustomerReceivableCard(
                                receivable = receivable,
                                onClick = { onNavigateToDetails(receivable.receivableId) }
                            )
                        }
                    }
                }
            }
        }
    }
}
