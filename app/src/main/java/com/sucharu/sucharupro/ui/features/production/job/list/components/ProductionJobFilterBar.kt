package com.sucharu.sucharupro.ui.features.production.job.list.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.ui.features.production.job.list.ProductionJobSortOrder
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Filter bar component providing search input, status chips, priority chips,
 * and clear filter actions for the Production Job Queue.
 */
@Composable
fun ProductionJobFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedStatus: ProductionJobStatus?,
    onStatusFilterChange: (ProductionJobStatus?) -> Unit,
    selectedPriority: OrderPriority?,
    onPriorityFilterChange: (OrderPriority?) -> Unit,
    selectedStage: ProductionStageType?,
    onStageFilterChange: (ProductionStageType?) -> Unit,
    sortOrder: ProductionJobSortOrder,
    onSortOrderChange: (ProductionJobSortOrder) -> Unit,
    isFiltered: Boolean,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.medium)
    ) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search by Job #, Order #, Title, Customer...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            trailingIcon = if (searchQuery.isNotBlank()) {
                {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Search"
                        )
                    }
                }
            } else null,
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        // Horizontal Scrollable Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clear Filters Button (when filtered)
            if (isFiltered) {
                TextButton(onClick = onClearFilters) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
                    Text("Clear All")
                }
            }

            // Status Filter Chips
            FilterChip(
                selected = selectedStatus == null,
                onClick = { onStatusFilterChange(null) },
                label = { Text("All Statuses") }
            )

            ProductionJobStatus.entries.forEach { status ->
                FilterChip(
                    selected = selectedStatus == status,
                    onClick = {
                        onStatusFilterChange(if (selectedStatus == status) null else status)
                    },
                    label = { Text(status.defaultLabel) }
                )
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

        // Priority Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedPriority == null,
                onClick = { onPriorityFilterChange(null) },
                label = { Text("All Priorities") }
            )

            OrderPriority.entries.forEach { priority ->
                FilterChip(
                    selected = selectedPriority == priority,
                    onClick = {
                        onPriorityFilterChange(if (selectedPriority == priority) null else priority)
                    },
                    label = { Text(priority.name) }
                )
            }
        }
    }
}
