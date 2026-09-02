package com.sucharu.sucharupro.ui.features.orders.quotation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.order.QuotationStatusType
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.components.AppOutlinedButton
import com.sucharu.sucharupro.ui.components.LoadingIndicator
import com.sucharu.sucharupro.ui.features.orders.quotation.components.QuotationFilterBar
import com.sucharu.sucharupro.ui.features.orders.quotation.components.QuotationListItem
import com.sucharu.sucharupro.ui.features.orders.quotation.components.QuotationSearchBar
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Responsive Commercial Quotation List Screen with integrated search and status filtering.
 */
@Composable
fun QuotationListScreen(
    viewModel: QuotationListViewModel,
    onQuotationClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when (val state = uiState) {
            is QuotationListUiState.Loading -> {
                QuotationLoadingView(modifier = Modifier.fillMaxSize())
            }

            is QuotationListUiState.Error -> {
                QuotationErrorView(
                    errorMessage = state.errorMessage,
                    onRetry = { viewModel.retry() },
                    modifier = Modifier.fillMaxSize()
                )
            }

            is QuotationListUiState.Empty -> {
                QuotationEmptyView(
                    message = state.message,
                    modifier = Modifier.fillMaxSize()
                )
            }

            is QuotationListUiState.Success -> {
                QuotationSuccessContent(
                    state = state,
                    onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                    onClearSearch = { viewModel.onSearchQueryChange("") },
                    onStatusSelect = { viewModel.onStatusFilterChange(it) },
                    onClearAllFilters = { viewModel.clearFilters() },
                    onQuotationClick = onQuotationClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun QuotationSuccessContent(
    state: QuotationListUiState.Success,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onStatusSelect: (QuotationStatusType?) -> Unit,
    onClearAllFilters: () -> Unit,
    onQuotationClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = MaterialTheme.spacing.screenPadding)
            .padding(top = MaterialTheme.spacing.small)
    ) {
        // Summary Count Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Commercial Quotations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${state.visibleCount} / ${state.totalCount} Quotations",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        // Search Field
        QuotationSearchBar(
            query = state.searchQuery,
            onQueryChange = onSearchQueryChange,
            onClearClick = onClearSearch
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        // Filter Chips
        QuotationFilterBar(
            selectedStatus = state.selectedStatus,
            onStatusSelect = onStatusSelect,
            onClearAllFilters = onClearAllFilters
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        // List / Empty Search
        if (state.visibleQuotations.isEmpty()) {
            QuotationSearchEmptyView(
                onClearFilters = onClearAllFilters,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val isTabletOrDesktop = maxWidth >= 600.dp

                if (isTabletOrDesktop) {
                    val chunkedQuotations = state.visibleQuotations.chunked(2)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = MaterialTheme.spacing.xxLarge),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        items(chunkedQuotations, key = { row -> row.first().quotationId }) { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                            ) {
                                row.forEach { quotation ->
                                    QuotationListItem(
                                        quotation = quotation,
                                        onClick = { onQuotationClick(quotation.quotationId) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (row.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = MaterialTheme.spacing.xxLarge),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        items(state.visibleQuotations, key = { it.quotationId }) { quotation ->
                            QuotationListItem(
                                quotation = quotation,
                                onClick = { onQuotationClick(quotation.quotationId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuotationLoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(
            message = "Loading commercial quotations...",
            size = 48.dp
        )
    }
}

@Composable
private fun QuotationSearchEmptyView(
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(MaterialTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
        Text(
            text = "No matching quotations",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Try adjusting your search terms or clearing active status filters.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
        AppOutlinedButton(
            text = "Clear Filters",
            onClick = onClearFilters
        )
    }
}

@Composable
private fun QuotationEmptyView(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(MaterialTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(MaterialTheme.spacing.xxLarge)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.RequestQuote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                Text(
                    text = "No Quotations Yet",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun QuotationErrorView(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(MaterialTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(MaterialTheme.spacing.large)
        ) {
            Text(
                text = "Unable to load quotations",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
            AppButton(
                text = "Try Again",
                onClick = onRetry
            )
        }
    }
}
