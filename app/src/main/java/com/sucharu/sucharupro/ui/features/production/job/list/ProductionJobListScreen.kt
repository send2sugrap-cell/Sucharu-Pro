package com.sucharu.sucharupro.ui.features.production.job.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppOutlinedButton
import com.sucharu.sucharupro.ui.features.production.job.list.components.ProductionJobCard
import com.sucharu.sucharupro.ui.features.production.job.list.components.ProductionJobFilterBar
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Production Job Queue / List Screen for discovering, searching, filtering, and opening Job Cards.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionJobListScreen(
    viewModel: ProductionJobListViewModel,
    onJobClick: (jobId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        val success = uiState as? ProductionJobListUiState.Success
        if (success?.actionMessage != null) {
            snackbarHostState.showSnackbar(success.actionMessage)
            viewModel.dismissActionFeedback()
        } else if (success?.actionError != null) {
            snackbarHostState.showSnackbar("Error: ${success.actionError}")
            viewModel.dismissActionFeedback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Production Job Queue") },
                actions = {
                    IconButton(onClick = { viewModel.loadJobs() }) {
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
                is ProductionJobListUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is ProductionJobListUiState.Empty -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(MaterialTheme.spacing.large),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No Production Jobs",
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

                is ProductionJobListUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(MaterialTheme.spacing.large),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Failed to load jobs",
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
                                    onClick = { viewModel.loadJobs() }
                                )
                            }
                        }
                    }
                }

                is ProductionJobListUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Filter and Search Header
                        ProductionJobFilterBar(
                            searchQuery = state.searchQuery,
                            onSearchQueryChange = viewModel::onSearchQueryChange,
                            selectedStatus = state.selectedStatus,
                            onStatusFilterChange = viewModel::onStatusFilterChange,
                            selectedPriority = state.selectedPriority,
                            onPriorityFilterChange = viewModel::onPriorityFilterChange,
                            selectedStage = state.selectedStage,
                            onStageFilterChange = viewModel::onStageFilterChange,
                            sortOrder = state.sortOrder,
                            onSortOrderChange = viewModel::onSortOrderChange,
                            isFiltered = state.isFiltered,
                            onClearFilters = viewModel::clearFilters
                        )

                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                        // Results count
                        Text(
                            text = "Showing ${state.visibleCount} of ${state.totalCount} jobs",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium)
                        )

                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

                        if (state.visibleJobs.isEmpty()) {
                            // Filter returned 0 results
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(MaterialTheme.spacing.large),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "No jobs match the selected filters.",
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
                            // Jobs List
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(MaterialTheme.spacing.medium),
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                            ) {
                                items(
                                    items = state.visibleJobs,
                                    key = { it.jobId }
                                ) { job ->
                                    ProductionJobCard(
                                        job = job,
                                        onClick = { onJobClick(job.jobId) }
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
