package com.sucharu.sucharupro.ui.features.orders.inquiry.components

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
import com.sucharu.sucharupro.domain.model.order.InquiryStatusType
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Filter bar allowing rapid filtering by [InquiryStatusType].
 */
@Composable
fun InquiryFilterBar(
    selectedStatus: InquiryStatusType?,
    onStatusSelect: (InquiryStatusType?) -> Unit,
    onClearAllFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAnyFilterActive = selectedStatus != null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Clear chip when filter active
        if (isAnyFilterActive) {
            SuggestionChip(
                onClick = onClearAllFilters,
                label = { Text("Clear", style = MaterialTheme.typography.labelSmall) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear status filter",
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

        // "All" chip
        FilterChip(
            selected = selectedStatus == null,
            onClick = { onStatusSelect(null) },
            label = { Text("All", style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        // Status Filter Chips
        InquiryStatusType.entries.forEach { status ->
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
    }
}
