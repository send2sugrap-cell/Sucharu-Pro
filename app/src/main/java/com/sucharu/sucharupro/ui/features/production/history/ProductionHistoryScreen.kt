package com.sucharu.sucharupro.ui.features.production.history

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.job.CompletionFilter
import com.sucharu.sucharupro.domain.model.job.ProductionHistorySortBy
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.ui.features.production.history.components.ProductionHistoryJobCard

/**
 * Main Production History Screen displaying list of executed and completed jobs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionHistoryScreen(
    viewModel: ProductionHistoryViewModel,
    onOpenJobHistory: (jobId: String) -> Unit,
    onOpenPerformanceAnalytics: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Production History",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenPerformanceAnalytics) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Performance Analytics",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is ProductionHistoryUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ProductionHistoryUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.retry() }) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Retry")
                        }
                    }
                }
                is ProductionHistoryUiState.Success -> {
                    if (state.isEmpty) {
                        EmptyHistoryView(modifier = Modifier.align(Alignment.Center))
                    } else {
                        HistoryContent(
                            state = state,
                            onSearchQueryChanged = viewModel::setSearchQuery,
                            onStatusFilterChanged = viewModel::setStatusFilter,
                            onPriorityFilterChanged = viewModel::setPriorityFilter,
                            onCompletionFilterChanged = viewModel::setCompletionFilter,
                            onSortChanged = viewModel::setSortBy,
                            onClearFilters = viewModel::clearFilters,
                            onOpenJobHistory = onOpenJobHistory
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryContent(
    state: ProductionHistoryUiState.Success,
    onSearchQueryChanged: (String) -> Unit,
    onStatusFilterChanged: (ProductionJobStatus?) -> Unit,
    onPriorityFilterChanged: (OrderPriority?) -> Unit,
    onCompletionFilterChanged: (CompletionFilter) -> Unit,
    onSortChanged: (ProductionHistorySortBy) -> Unit,
    onClearFilters: () -> Unit,
    onOpenJobHistory: (jobId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Job #, Title, Order, Customer খুঁজুন...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search"
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Filter chips horizontal scroll
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.filter.status == null && state.filter.priority == null && state.filter.completion == CompletionFilter.ALL,
                onClick = { onClearFilters() },
                label = { Text("All") }
            )
            FilterChip(
                selected = state.filter.status == ProductionJobStatus.DELIVERED,
                onClick = {
                    onStatusFilterChanged(if (state.filter.status == ProductionJobStatus.DELIVERED) null else ProductionJobStatus.DELIVERED)
                },
                label = { Text("Delivered") }
            )
            FilterChip(
                selected = state.filter.status == ProductionJobStatus.READY,
                onClick = {
                    onStatusFilterChanged(if (state.filter.status == ProductionJobStatus.READY) null else ProductionJobStatus.READY)
                },
                label = { Text("Ready") }
            )
            FilterChip(
                selected = state.filter.status == ProductionJobStatus.IN_PROGRESS,
                onClick = {
                    onStatusFilterChanged(if (state.filter.status == ProductionJobStatus.IN_PROGRESS) null else ProductionJobStatus.IN_PROGRESS)
                },
                label = { Text("In Progress") }
            )
            FilterChip(
                selected = state.filter.priority == OrderPriority.URGENT,
                onClick = {
                    onPriorityFilterChanged(if (state.filter.priority == OrderPriority.URGENT) null else OrderPriority.URGENT)
                },
                label = { Text("জরুরি (Urgent)") }
            )
            FilterChip(
                selected = state.filter.status == ProductionJobStatus.CANCELLED,
                onClick = {
                    onStatusFilterChanged(if (state.filter.status == ProductionJobStatus.CANCELLED) null else ProductionJobStatus.CANCELLED)
                },
                label = { Text("Cancelled") }
            )
        }

        // Sort Options Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Sort:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ProductionHistorySortBy.entries.forEach { sortBy ->
                FilterChip(
                    selected = state.filter.sortBy == sortBy,
                    onClick = { onSortChanged(sortBy) },
                    label = { Text(sortBy.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // History Job List or Filtered Empty
        if (state.isFilteredEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "আপনার অনুসন্ধানের সাথে মিলে কোনো Job পাওয়া যায়নি",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = onClearFilters) {
                        Text("Clear Filters")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = state.filteredSummaries,
                    key = { it.jobId }
                ) { summary ->
                    ProductionHistoryJobCard(
                        summary = summary,
                        onOpenJobHistory = onOpenJobHistory
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "কোনো Production History পাওয়া যায়নি",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "উৎপাদন কার্যক্রম ও জব কার্ড সম্পন্ন হলে তা এখানে প্রদর্শিত হবে।",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
