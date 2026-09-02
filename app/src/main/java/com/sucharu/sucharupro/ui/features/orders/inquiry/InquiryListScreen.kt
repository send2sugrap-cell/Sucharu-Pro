package com.sucharu.sucharupro.ui.features.orders.inquiry

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
import androidx.compose.material.icons.filled.Assignment
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
import com.sucharu.sucharupro.domain.model.order.InquiryStatusType
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.components.AppOutlinedButton
import com.sucharu.sucharupro.ui.components.LoadingIndicator
import com.sucharu.sucharupro.ui.features.orders.inquiry.components.InquiryFilterBar
import com.sucharu.sucharupro.ui.features.orders.inquiry.components.InquiryListItem
import com.sucharu.sucharupro.ui.features.orders.inquiry.components.InquirySearchBar
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Responsive Inquiry List Screen with integrated search and status filtering.
 */
@Composable
fun InquiryListScreen(
    viewModel: InquiryListViewModel,
    onInquiryClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when (val state = uiState) {
            is InquiryListUiState.Loading -> {
                InquiryLoadingView(modifier = Modifier.fillMaxSize())
            }

            is InquiryListUiState.Error -> {
                InquiryErrorView(
                    errorMessage = state.errorMessage,
                    onRetry = { viewModel.retry() },
                    modifier = Modifier.fillMaxSize()
                )
            }

            is InquiryListUiState.Empty -> {
                InquiryEmptyView(
                    message = state.message,
                    modifier = Modifier.fillMaxSize()
                )
            }

            is InquiryListUiState.Success -> {
                InquirySuccessContent(
                    state = state,
                    onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                    onClearSearch = { viewModel.onSearchQueryChange("") },
                    onStatusSelect = { viewModel.onStatusFilterChange(it) },
                    onClearAllFilters = { viewModel.clearFilters() },
                    onInquiryClick = onInquiryClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun InquirySuccessContent(
    state: InquiryListUiState.Success,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onStatusSelect: (InquiryStatusType?) -> Unit,
    onClearAllFilters: () -> Unit,
    onInquiryClick: (String) -> Unit,
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
                text = "Customer Inquiries",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${state.visibleCount} / ${state.totalCount} Inquiries",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        // Search Field
        InquirySearchBar(
            query = state.searchQuery,
            onQueryChange = onSearchQueryChange,
            onClearClick = onClearSearch
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        // Filter Chips
        InquiryFilterBar(
            selectedStatus = state.selectedStatus,
            onStatusSelect = onStatusSelect,
            onClearAllFilters = onClearAllFilters
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        // List / Empty Search
        if (state.visibleInquiries.isEmpty()) {
            InquirySearchEmptyView(
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
                    val chunkedInquiries = state.visibleInquiries.chunked(2)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = MaterialTheme.spacing.xxLarge),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        items(chunkedInquiries, key = { row -> row.first().inquiryId }) { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                            ) {
                                row.forEach { inquiry ->
                                    InquiryListItem(
                                        inquiry = inquiry,
                                        onClick = { onInquiryClick(inquiry.inquiryId) },
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
                        items(state.visibleInquiries, key = { it.inquiryId }) { inquiry ->
                            InquiryListItem(
                                inquiry = inquiry,
                                onClick = { onInquiryClick(inquiry.inquiryId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InquiryLoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(
            message = "Loading customer inquiries...",
            size = 48.dp
        )
    }
}

@Composable
private fun InquirySearchEmptyView(
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
            text = "No matching inquiries",
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
private fun InquiryEmptyView(
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
                    imageVector = Icons.Default.Assignment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                Text(
                    text = "No Inquiries Yet",
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
private fun InquiryErrorView(
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
                text = "Unable to load inquiries",
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
