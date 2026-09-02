package com.sucharu.sucharupro.ui.features.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.components.SectionHeader
import com.sucharu.sucharupro.ui.features.orders.inquiry.InquiryListScreen
import com.sucharu.sucharupro.ui.features.orders.inquiry.InquiryListUiState
import com.sucharu.sucharupro.ui.features.orders.inquiry.InquiryListViewModel
import com.sucharu.sucharupro.ui.features.orders.order.OrderListScreen
import com.sucharu.sucharupro.ui.features.orders.order.OrderListUiState
import com.sucharu.sucharupro.ui.features.orders.order.OrderListViewModel
import com.sucharu.sucharupro.ui.features.orders.quotation.QuotationListScreen
import com.sucharu.sucharupro.ui.features.orders.quotation.QuotationListUiState
import com.sucharu.sucharupro.ui.features.orders.quotation.QuotationListViewModel
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Module 03 Landing Screen: Quotation & Order Management.
 *
 * Hosts reactive top-level tabs for:
 * 1. Customer Inquiries
 * 2. Commercial Quotations
 * 3. Commercial Orders
 */
@Composable
fun QuotationOrderManagementScreen(
    inquiryViewModel: InquiryListViewModel,
    quotationViewModel: QuotationListViewModel,
    orderViewModel: OrderListViewModel,
    onInquiryClick: (String) -> Unit = {},
    onQuotationClick: (String) -> Unit = {},
    onOrderClick: (String) -> Unit = {},
    onAddInquiryClick: () -> Unit = {},
    onAddQuotationClick: () -> Unit = {},
    initialTabIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(initialTabIndex) }

    val inquiryState by inquiryViewModel.uiState.collectAsState()
    val quotationState by quotationViewModel.uiState.collectAsState()
    val orderState by orderViewModel.uiState.collectAsState()

    val inquiryCount = (inquiryState as? InquiryListUiState.Success)?.totalCount ?: 0
    val quotationCount = (quotationState as? QuotationListUiState.Success)?.totalCount ?: 0
    val orderCount = (orderState as? OrderListUiState.Success)?.totalCount ?: 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = MaterialTheme.spacing.medium)
    ) {
        // Section Header with Action Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.screenPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(
                title = "Quotation & Order Management",
                subtitle = "Inquiries, commercial quotations, and confirmed customer orders",
                modifier = Modifier.weight(1f)
            )
            when (selectedTabIndex) {
                0 -> {
                    com.sucharu.sucharupro.ui.components.AppButton(
                        text = "New Inquiry",
                        onClick = onAddInquiryClick,
                        leadingIcon = {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
                1 -> {
                    com.sucharu.sucharupro.ui.components.AppButton(
                        text = "New Quotation",
                        onClick = onAddQuotationClick,
                        leadingIcon = {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }

        // Navigation Tabs
        PrimaryTabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            // Tab 0: Inquiries
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Inquiries",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                        )
                        if (inquiryCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Badge(
                                containerColor = if (selectedTabIndex == 0) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor = if (selectedTabIndex == 0) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            ) {
                                Text(text = "$inquiryCount", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = "Customer Inquiries"
                    )
                }
            )

            // Tab 1: Quotations
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Quotations",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal
                        )
                        if (quotationCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Badge(
                                containerColor = if (selectedTabIndex == 1) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor = if (selectedTabIndex == 1) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            ) {
                                Text(text = "$quotationCount", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.RequestQuote,
                        contentDescription = "Commercial Quotations"
                    )
                }
            )

            // Tab 2: Orders
            Tab(
                selected = selectedTabIndex == 2,
                onClick = { selectedTabIndex = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Orders",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selectedTabIndex == 2) FontWeight.Bold else FontWeight.Normal
                        )
                        if (orderCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Badge(
                                containerColor = if (selectedTabIndex == 2) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor = if (selectedTabIndex == 2) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            ) {
                                Text(text = "$orderCount", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Customer Orders"
                    )
                }
            )
        }

        // Tab Content
        when (selectedTabIndex) {
            0 -> InquiryListScreen(
                viewModel = inquiryViewModel,
                onInquiryClick = onInquiryClick,
                modifier = Modifier.fillMaxSize()
            )
            1 -> QuotationListScreen(
                viewModel = quotationViewModel,
                onQuotationClick = onQuotationClick,
                modifier = Modifier.fillMaxSize()
            )
            2 -> OrderListScreen(
                viewModel = orderViewModel,
                onOrderClick = onOrderClick,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
