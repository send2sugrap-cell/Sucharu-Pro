package com.sucharu.sucharupro.ui.features.customer.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Filter bar allowing rapid filtering by CustomerType and CustomerStatusType.
 */
@Composable
fun CustomerFilterBar(
    selectedType: CustomerType?,
    selectedStatus: CustomerStatusType?,
    onTypeSelect: (CustomerType?) -> Unit,
    onStatusSelect: (CustomerStatusType?) -> Unit,
    onClearAllFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAnyFilterActive = selectedType != null || selectedStatus != null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reset / Clear pill when any filter is active
        if (isAnyFilterActive) {
            SuggestionChip(
                onClick = onClearAllFilters,
                label = { Text("Clear", style = MaterialTheme.typography.labelSmall) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear filters",
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

        // Status Filter Chips
        CustomerStatusType.entries.forEach { status ->
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

        // Type Filter Chips
        CustomerType.entries.forEach { type ->
            val isSelected = selectedType == type
            FilterChip(
                selected = isSelected,
                onClick = {
                    onTypeSelect(if (isSelected) null else type)
                },
                label = {
                    Text(type.defaultLabel, style = MaterialTheme.typography.labelSmall)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }
    }
}
