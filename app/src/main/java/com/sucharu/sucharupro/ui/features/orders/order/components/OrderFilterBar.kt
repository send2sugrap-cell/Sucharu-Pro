package com.sucharu.sucharupro.ui.features.orders.order.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.order.JobHandoffStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Filter bar allowing rapid filtering by [OrderStatusType], [OrderPriority], and [JobHandoffStatus].
 */
@Composable
fun OrderFilterBar(
    selectedStatus: OrderStatusType?,
    selectedPriority: OrderPriority?,
    selectedHandoff: JobHandoffStatus?,
    onStatusSelect: (OrderStatusType?) -> Unit,
    onPrioritySelect: (OrderPriority?) -> Unit,
    onHandoffSelect: (JobHandoffStatus?) -> Unit,
    onClearAllFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAnyFilterActive = selectedStatus != null || selectedPriority != null || selectedHandoff != null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isAnyFilterActive) {
            SuggestionChip(
                onClick = onClearAllFilters,
                label = { Text("Clear", style = MaterialTheme.typography.labelSmall) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear all filters",
                        modifier = Modifier.size(14.dp)
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    labelColor = MaterialTheme.colorScheme.onErrorContainer,
                    iconContentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            )
        }

        // Status: All Chip
        FilterChip(
            selected = selectedStatus == null && selectedPriority == null && selectedHandoff == null,
            onClick = { onClearAllFilters() },
            label = { Text("All", style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        // Commercial Status Chips
        OrderStatusType.entries.forEach { status ->
            val isSelected = selectedStatus == status
            FilterChip(
                selected = isSelected,
                onClick = {
                    onStatusSelect(if (isSelected) null else status)
                },
                label = {
                    Text(status.defaultLabel, style = MaterialTheme.typography.labelSmall)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }

        // Priority Chips
        OrderPriority.entries.forEach { priority ->
            val isSelected = selectedPriority == priority
            FilterChip(
                selected = isSelected,
                onClick = {
                    onPrioritySelect(if (isSelected) null else priority)
                },
                label = {
                    Text(priority.defaultLabel, style = MaterialTheme.typography.labelSmall)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }

        // Job Handoff Readiness Chips
        JobHandoffStatus.entries.forEach { handoff ->
            val isSelected = selectedHandoff == handoff
            FilterChip(
                selected = isSelected,
                onClick = {
                    onHandoffSelect(if (isSelected) null else handoff)
                },
                label = {
                    Text(handoff.defaultLabel, style = MaterialTheme.typography.labelSmall)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            )
        }
    }
}
