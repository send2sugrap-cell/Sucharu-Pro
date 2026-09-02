package com.sucharu.sucharupro.ui.features.production.monitoring

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.features.production.monitoring.components.ActiveProductionWorkCard
import com.sucharu.sucharupro.ui.features.production.monitoring.components.OperatorWorkloadOverviewCard
import com.sucharu.sucharupro.ui.features.production.monitoring.components.ProductionAttentionQueueCard
import com.sucharu.sucharupro.ui.features.production.monitoring.components.ProductionMetricsSummarySection
import com.sucharu.sucharupro.ui.features.production.monitoring.components.ProductionStatusDistributionCard
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Live Production Monitoring and Supervisor Dashboard Screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionMonitoringDashboardScreen(
    viewModel: ProductionMonitoringDashboardViewModel,
    onOpenJobDetails: (jobId: String) -> Unit,
    onOpenOperatorQueue: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Production Monitoring") },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is ProductionMonitoringDashboardUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is ProductionMonitoringDashboardUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(MaterialTheme.spacing.large),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Error Loading Monitoring Data",
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
                                    onClick = { viewModel.clearFilters() }
                                )
                            }
                        }
                    }
                }

                is ProductionMonitoringDashboardUiState.Success -> {
                    if (state.snapshot.totalJobs == 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(MaterialTheme.spacing.large),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "কোনো Production Job এখনো নেই",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                                Text(
                                    text = "নতুন অর্ডার হ্যান্ডঅফ গ্রহণ করে জব তৈরি করুন।",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else {
                        MonitoringDashboardContent(
                            state = state,
                            onSearchQueryChanged = { viewModel.setSearchQuery(it) },
                            onFilterSelected = { viewModel.setFilter(it) },
                            onOpenJobDetails = onOpenJobDetails,
                            onOpenOperatorQueue = onOpenOperatorQueue
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonitoringDashboardContent(
    state: ProductionMonitoringDashboardUiState.Success,
    onSearchQueryChanged: (String) -> Unit,
    onFilterSelected: (ProductionMonitoringFilter) -> Unit,
    onOpenJobDetails: (jobId: String) -> Unit,
    onOpenOperatorQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        // Search Input
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChanged,
            label = { Text("Search Jobs, Stages, Operators...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            trailingIcon = {
                if (state.searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search"
                        )
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)
        ) {
            ProductionMonitoringFilter.entries.forEach { filter ->
                FilterChip(
                    selected = state.filter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(filter.label) }
                )
            }
        }

        // Key Production KPI Summary Cards
        ProductionMetricsSummarySection(snapshot = state.snapshot)

        // Job Status Distribution
        ProductionStatusDistributionCard(snapshot = state.snapshot)

        // Supervisor Attention Queue
        ProductionAttentionQueueCard(
            attentionItems = state.filteredAttentionItems,
            onOpenJobDetails = onOpenJobDetails
        )

        // Active Production Stages
        ActiveProductionWorkCard(
            activeStages = state.filteredActiveStages,
            onOpenJobDetails = onOpenJobDetails
        )

        // Operator Workload Summary
        OperatorWorkloadOverviewCard(
            workloads = state.operatorWorkloads,
            onOpenOperatorQueue = onOpenOperatorQueue
        )
    }
}
