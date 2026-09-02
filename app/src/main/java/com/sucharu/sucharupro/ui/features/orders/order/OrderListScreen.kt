package com.sucharu.sucharupro.ui.features.orders.order

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
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.ShoppingCart
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
import com.sucharu.sucharupro.domain.model.order.JobHandoffStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.components.AppOutlinedButton
import com.sucharu.sucharupro.ui.components.LoadingIndicator
import com.sucharu.sucharupro.ui.features.orders.order.components.OrderFilterBar
import com.sucharu.sucharupro.ui.features.orders.order.components.OrderListItem
import com.sucharu.sucharupro.ui.features.orders.order.components.OrderSearchBar
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Responsive Commercial Order List Screen with integrated search and status/priority filtering.
 */
@Composable
fun OrderListScreen(
    viewModel: OrderListViewModel,
    onOrderClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when (val state = uiState) {
            is OrderListUiState.Loading -> {
                OrderLoadingView(modifier = Modifier.fillMaxSize())
            }

            is OrderListUiState.Error -> {
                OrderErrorView(
                    errorMessage = state.errorMessage,
                    onRetry = { viewModel.retry() },
                    modifier = Modifier.fillMaxSize()
                )
            }

            is OrderListUiState.Empty -> {
                OrderEmptyView(
                    message = state.message,
                    modifier = Modifier.fillMaxSize()
                )
            }

            is OrderListUiState.Success -> {
                OrderSuccessContent(
                    state = state,
                    onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                    onClearSearch = { viewModel.onSearchQueryChange("") },
                    onStatusSelect = { viewModel.onStatusFilterChange(it) },
                    onPrioritySelect = { viewModel.onPriorityFilterChange(it) },
                    onHandoffSelect = { viewModel.onHandoffFilterChange(it) },
                    onClearAllFilters = { viewModel.clearFilters() },
                    onOrderClick = onOrderClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun OrderSuccessContent(
    state: OrderListUiState.Success,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onStatusSelect: (OrderStatusType?) -> Unit,
    onPrioritySelect: (OrderPriority?) -> Unit,
    onHandoffSelect: (JobHandoffStatus?) -> Unit,
    onClearAllFilters: () -> Unit,
    onOrderClick: (String) -> Unit,
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
                text = "Commercial Orders",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${state.visibleCount} / ${state.totalCount} Orders",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        // Search Field
        OrderSearchBar(
            query = state.searchQuery,
            onQueryChange = onSearchQueryChange,
            onClearClick = onClearSearch
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        // Filter Chips (Status, Priority & Handoff)
        OrderFilterBar(
            selectedStatus = state.selectedStatus,
            selectedPriority = state.selectedPriority,
            selectedHandoff = state.selectedHandoff,
            onStatusSelect = onStatusSelect,
            onPrioritySelect = onPrioritySelect,
            onHandoffSelect = onHandoffSelect,
            onClearAllFilters = onClearAllFilters
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        // List / Empty Search
        if (state.visibleOrders.isEmpty()) {
            OrderSearchEmptyView(
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
                    val chunkedOrders = state.visibleOrders.chunked(2)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = MaterialTheme.spacing.xxLarge),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        items(chunkedOrders, key = { row -> row.first().orderId }) { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                            ) {
                                row.forEach { order ->
                                    OrderListItem(
                                        order = order,
                                        onClick = { onOrderClick(order.orderId) },
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
                        items(state.visibleOrders, key = { it.orderId }) { order ->
                            OrderListItem(
                                order = order,
                                onClick = { onOrderClick(order.orderId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderLoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(
            message = "Loading commercial orders...",
            size = 48.dp
        )
    }
}

@Composable
private fun OrderSearchEmptyView(
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
            text = "No matching orders",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Try adjusting your search terms or clearing active filters.",
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
private fun OrderEmptyView(
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
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                Text(
                    text = "No Orders Yet",
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
private fun OrderErrorView(
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
                text = "Unable to load orders",
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
