package com.sucharu.sucharupro.ui.features.finance.adjustment

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
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentDirection
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentType
import com.sucharu.sucharupro.domain.model.user.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialAdjustmentListScreen(
    viewModel: FinancialAdjustmentListViewModel,
    callerRole: UserRole,
    onAdjustmentClick: (String) -> Unit,
    onCreateAdjustmentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Financial Adjustments & Notes") }
            )
        },
        floatingActionButton = {
            if (callerRole.isInternal) {
                FloatingActionButton(
                    onClick = onCreateAdjustmentClick,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Adjustment")
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
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                placeholder = { Text("Search adjustment #, reason, customer/vendor...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Type Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FinancialAdjustmentType.values().forEach { type ->
                    FilterChip(
                        selected = state.selectedType == type,
                        onClick = { viewModel.onTypeSelected(type) },
                        label = { Text(type.defaultLabel, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Status & Direction Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FinancialAdjustmentStatus.values().forEach { status ->
                    FilterChip(
                        selected = state.selectedStatus == status,
                        onClick = { viewModel.onStatusSelected(status) },
                        label = { Text(status.defaultLabel, fontSize = 11.sp) }
                    )
                }

                FinancialAdjustmentDirection.values().forEach { dir ->
                    FilterChip(
                        selected = state.selectedDirection == dir,
                        onClick = { viewModel.onDirectionSelected(dir) },
                        label = { Text(dir.defaultLabel, fontSize = 11.sp) }
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
                state.filteredAdjustments.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (state.adjustments.isEmpty()) "No financial adjustments recorded." else "No matching adjustments found.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        state.summary?.let { sum ->
                            item {
                                FinancialAdjustmentSummaryCard(summary = sum)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }

                        items(state.filteredAdjustments, key = { it.adjustmentId }) { adjustment ->
                            FinancialAdjustmentCard(
                                adjustment = adjustment,
                                onClick = { onAdjustmentClick(adjustment.adjustmentId) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(64.dp)) }
                    }
                }
            }
        }
    }
}
