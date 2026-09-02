package com.sucharu.sucharupro.ui.features.production.operator

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppOutlinedButton
import com.sucharu.sucharupro.ui.features.production.operator.components.OperatorWorkItemCard
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Operator Work Queue Screen showing assigned stages, progress, priority filters,
 * and seamless navigation to Job Details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionOperatorWorkQueueScreen(
    viewModel: ProductionOperatorWorkQueueViewModel,
    onOpenJobDetails: (jobId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Operator Work Queue") },
                actions = {
                    IconButton(onClick = { viewModel.loadWorkQueue() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is ProductionOperatorWorkQueueUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is ProductionOperatorWorkQueueUiState.Empty -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(MaterialTheme.spacing.large),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No Work Assigned",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is ProductionOperatorWorkQueueUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(MaterialTheme.spacing.large),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Error Loading Work Queue",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                            Text(
                                text = state.errorMessage,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                            if (state.canRetry) {
                                AppButton(
                                    text = "Retry",
                                    onClick = { viewModel.loadWorkQueue() }
                                )
                            }
                        }
                    }
                }

                is ProductionOperatorWorkQueueUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Search bar
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            placeholder = { Text("Search by Job #, Stage, Operator, Order #...") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                            },
                            trailingIcon = if (state.searchQuery.isNotBlank()) {
                                {
                                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            } else null,
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = MaterialTheme.spacing.medium)
                        )

                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                        // Operator Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = MaterialTheme.spacing.medium),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (state.isFiltered) {
                                TextButton(onClick = viewModel::clearFilters) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear All")
                                }
                            }

                            FilterChip(
                                selected = state.selectedOperatorId == null,
                                onClick = { viewModel.onOperatorSelect(null) },
                                label = { Text("All Operators") }
                            )

                            state.availableOperators.forEach { operator ->
                                FilterChip(
                                    selected = state.selectedOperatorId == operator.operatorId,
                                    onClick = {
                                        viewModel.onOperatorSelect(
                                            if (state.selectedOperatorId == operator.operatorId) null else operator.operatorId
                                        )
                                    },
                                    label = { Text(operator.operatorName) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

                        // Priority and Stage Status Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = MaterialTheme.spacing.medium),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = state.selectedPriority == null,
                                onClick = { viewModel.onPriorityFilterChange(null) },
                                label = { Text("All Priorities") }
                            )
                            OrderPriority.entries.forEach { priority ->
                                FilterChip(
                                    selected = state.selectedPriority == priority,
                                    onClick = {
                                        viewModel.onPriorityFilterChange(
                                            if (state.selectedPriority == priority) null else priority
                                        )
                                    },
                                    label = { Text(priority.name) }
                                )
                            }

                            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))

                            FilterChip(
                                selected = state.selectedStageStatus == null,
                                onClick = { viewModel.onStageStatusFilterChange(null) },
                                label = { Text("All Stage Statuses") }
                            )
                            ProductionStageStatus.entries.forEach { status ->
                                FilterChip(
                                    selected = state.selectedStageStatus == status,
                                    onClick = {
                                        viewModel.onStageStatusFilterChange(
                                            if (state.selectedStageStatus == status) null else status
                                        )
                                    },
                                    label = { Text(status.defaultLabel) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                        // Results counter
                        Text(
                            text = "Showing ${state.visibleCount} of ${state.totalCount} assigned tasks",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium)
                        )

                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

                        if (state.visibleWorkItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(MaterialTheme.spacing.large),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "আপনার অনুসন্ধানের সাথে মিলে কোনো কাজ পাওয়া যায়নি।",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                                    AppOutlinedButton(
                                        text = "Clear Filters",
                                        onClick = viewModel::clearFilters
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(MaterialTheme.spacing.medium),
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                            ) {
                                items(
                                    items = state.visibleWorkItems,
                                    key = { "${it.assignment.assignmentId}_${it.stage.stageId}" }
                                ) { workItem ->
                                    OperatorWorkItemCard(
                                        workItem = workItem,
                                        onClick = { onOpenJobDetails(workItem.job.jobId) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
