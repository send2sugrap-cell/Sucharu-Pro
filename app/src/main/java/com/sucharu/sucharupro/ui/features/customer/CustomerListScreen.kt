package com.sucharu.sucharupro.ui.features.customer

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.components.AppOutlinedButton
import com.sucharu.sucharupro.ui.components.LoadingIndicator
import com.sucharu.sucharupro.ui.features.customer.components.CustomerFilterBar
import com.sucharu.sucharupro.ui.features.customer.components.CustomerListItem
import com.sucharu.sucharupro.ui.features.customer.components.CustomerSearchBar
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Customer Management List & Directory Screen.
 *
 * @param viewModel CustomerListViewModel coordinating search, filter, and repository data.
 * @param onCustomerClick Invoked when a customer record is tapped.
 * @param onAddCustomerClick Invoked when the user taps "+ New Customer".
 */
@Composable
fun CustomerListScreen(
    viewModel: CustomerListViewModel,
    onCustomerClick: (String) -> Unit,
    onAddCustomerClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCustomerClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Customer"
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is CustomerListUiState.Loading -> {
                    CustomerLoadingView(modifier = Modifier.fillMaxSize())
                }

                is CustomerListUiState.Error -> {
                    CustomerErrorView(
                        errorMessage = state.errorMessage,
                        onRetry = { viewModel.retry() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is CustomerListUiState.Empty -> {
                    CustomerEmptyView(
                        message = state.message,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is CustomerListUiState.Success -> {
                    CustomerSuccessContent(
                        state = state,
                        onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                        onClearSearch = { viewModel.onSearchQueryChange("") },
                        onTypeSelect = { viewModel.onTypeFilterChange(it) },
                        onStatusSelect = { viewModel.onStatusFilterChange(it) },
                        onClearAllFilters = { viewModel.clearFilters() },
                        onCustomerClick = onCustomerClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerSuccessContent(
    state: CustomerListUiState.Success,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onTypeSelect: (CustomerType?) -> Unit,
    onStatusSelect: (CustomerStatusType?) -> Unit,
    onClearAllFilters: () -> Unit,
    onCustomerClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = MaterialTheme.spacing.screenPadding)
            .padding(top = MaterialTheme.spacing.medium)
    ) {
        // 1. Header (Title + Count Summary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Customer Management",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage customer profiles, types and contacts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${state.visibleCount} / ${state.totalCount} Customers",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        // 2. Search Field
        CustomerSearchBar(
            query = state.searchQuery,
            onQueryChange = onSearchQueryChange,
            onClearClick = onClearSearch
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        // 3. Horizontal Filter Bar
        CustomerFilterBar(
            selectedType = state.selectedType,
            selectedStatus = state.selectedStatus,
            onTypeSelect = onTypeSelect,
            onStatusSelect = onStatusSelect,
            onClearAllFilters = onClearAllFilters
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        // 4. Customer List / Empty Search View
        if (state.visibleCustomers.isEmpty()) {
            SearchEmptyView(
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
                    // 2-column layout on tablet/desktop
                    val chunkedCustomers = state.visibleCustomers.chunked(2)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = MaterialTheme.spacing.xxLarge),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        items(chunkedCustomers, key = { row -> row.first().customerId }) { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                            ) {
                                row.forEach { customer ->
                                    CustomerListItem(
                                        customer = customer,
                                        onClick = { onCustomerClick(customer.customerId) },
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
                    // 1-column layout on phones
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = MaterialTheme.spacing.xxLarge),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        items(state.visibleCustomers, key = { it.customerId }) { customer ->
                            CustomerListItem(
                                customer = customer,
                                onClick = { onCustomerClick(customer.customerId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerLoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(
            message = "Loading customer directory...",
            size = 48.dp
        )
    }
}

@Composable
private fun SearchEmptyView(
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(MaterialTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .padding(bottom = MaterialTheme.spacing.small),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
        }

        Text(
            text = "No matching customers",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Try a different search query or clear the active filters.",
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
private fun CustomerEmptyView(
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
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                Text(
                    text = "No Customers Yet",
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
private fun CustomerErrorView(
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
                text = "Unable to load customer directory",
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
