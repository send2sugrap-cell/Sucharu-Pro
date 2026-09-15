package com.sucharu.sucharupro.ui.customer.myarea

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.ui.customer.components.CardSkeletonLoader
import com.sucharu.sucharupro.ui.customer.components.CustomerBottomNavigation
import com.sucharu.sucharupro.ui.customer.components.CustomerBottomTab
import com.sucharu.sucharupro.ui.customer.components.CustomerEmptyState
import com.sucharu.sucharupro.ui.customer.components.CustomerErrorState
import com.sucharu.sucharupro.ui.customer.components.MobileTopBar
import com.sucharu.sucharupro.ui.customer.components.QuickActionCard
import com.sucharu.sucharupro.ui.customer.components.StatusChip
import com.sucharu.sucharupro.ui.customer.components.SucharuWallCard
import com.sucharu.sucharupro.ui.customer.theme.CustomerTheme
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Mobile-First Customer My Area / Account Experience Screen.
 *
 * Renders authenticated customer relationship workspace:
 * - Customer Profile & Contact Details
 * - Financial Snapshot (What I Owe vs What I Paid)
 * - My Orders & Customer-Facing Order Progress
 * - My Quotations & Approvals
 * - My Documents & Tax Invoices
 * - Customer Support & Helpline
 */
@Composable
fun CustomerMyAreaScreen(
    viewModel: CustomerMyAreaViewModel,
    principal: AuthenticatedPrincipal?,
    modifier: Modifier = Modifier,
    onNavigateToDestination: (destination: AppDestination) -> Unit = {},
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(CustomerBottomTab.ACCOUNT) }

    LaunchedEffect(principal) {
        viewModel.loadMyArea(principal)
    }

    CustomerTheme {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = CustomerTheme.colors.background,
            topBar = {
                MobileTopBar(
                    title = "Customer My Area",
                    onBackClick = onNavigateBack
                )
            },
            bottomBar = {
                CustomerBottomNavigation(
                    selectedTab = selectedTab,
                    onTabSelect = { tab ->
                        selectedTab = tab
                        when (tab) {
                            CustomerBottomTab.HOME -> onNavigateToDestination(AppDestination.Public.Home)
                            CustomerBottomTab.ACCOUNT -> { /* Stay on My Area */ }
                        }
                    },
                    userRole = principal?.role
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = CustomerTheme.spacing.lg)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(CustomerTheme.spacing.md))

                when (val state = uiState) {
                    is CustomerMyAreaUiState.Loading -> {
                        CardSkeletonLoader()
                        Spacer(modifier = Modifier.height(CustomerTheme.spacing.md))
                        CardSkeletonLoader()
                    }

                    is CustomerMyAreaUiState.Error -> {
                        CustomerErrorState(
                            errorMessage = state.errorMessage,
                            onRetry = { viewModel.refresh(principal) }
                        )
                    }

                    is CustomerMyAreaUiState.Empty -> {
                        CustomerEmptyState(
                            message = state.message,
                            subtitle = "You do not have any orders or invoices on record."
                        )
                    }

                    is CustomerMyAreaUiState.Success -> {
                        val summary = state.summary

                        // 1. Customer Profile Card
                        CustomerProfileCard(profile = summary.profile)

                        Spacer(modifier = Modifier.height(CustomerTheme.spacing.lg))

                        // 2. Financial Snapshot (What I Owe vs What I Paid)
                        CustomerFinancialSnapshotCard(financials = summary.financials)

                        Spacer(modifier = Modifier.height(CustomerTheme.spacing.lg))

                        // 3. Quick Action Buttons
                        CustomerQuickActionsSection(onNavigateToDestination = onNavigateToDestination)

                        Spacer(modifier = Modifier.height(CustomerTheme.spacing.lg))

                        // 4. My Orders & Customer-Facing Progress
                        CustomerMyOrdersSection(
                            orders = summary.activeOrders,
                            onNavigateToDestination = onNavigateToDestination
                        )

                        Spacer(modifier = Modifier.height(CustomerTheme.spacing.lg))

                        // 5. My Quotations Section
                        CustomerMyQuotationsSection(
                            quotations = summary.quotations,
                            onNavigateToDestination = onNavigateToDestination
                        )

                        Spacer(modifier = Modifier.height(CustomerTheme.spacing.lg))

                        // 6. My Documents & Tax Invoices
                        CustomerMyDocumentsSection(
                            documents = summary.documents,
                            onNavigateToDestination = onNavigateToDestination
                        )

                        Spacer(modifier = Modifier.height(CustomerTheme.spacing.lg))

                        // 7. Customer Support & Helpline Card
                        CustomerSupportCard()

                        Spacer(modifier = Modifier.height(CustomerTheme.spacing.xxl))
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerProfileCard(profile: CustomerProfileInfo) {
    SucharuWallCard(
        containerColor = CustomerTheme.colors.surface,
        borderColor = CustomerTheme.colors.border
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = CustomerTheme.colors.accentPrimary)
                    Spacer(modifier = Modifier.width(CustomerTheme.spacing.sm))
                    Text(text = profile.displayName, style = CustomerTheme.typography.title, color = CustomerTheme.colors.primaryText)
                }
                StatusChip(label = profile.accountStatus, dotColor = CustomerTheme.colors.success)
            }
            Spacer(modifier = Modifier.height(CustomerTheme.spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = CustomerTheme.colors.secondaryText)
                Spacer(modifier = Modifier.width(CustomerTheme.spacing.xs))
                Text(text = profile.primaryPhone, style = CustomerTheme.typography.caption, color = CustomerTheme.colors.secondaryText)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Email, contentDescription = null, tint = CustomerTheme.colors.secondaryText)
                Spacer(modifier = Modifier.width(CustomerTheme.spacing.xs))
                Text(text = profile.email, style = CustomerTheme.typography.caption, color = CustomerTheme.colors.secondaryText)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = CustomerTheme.colors.secondaryText)
                Spacer(modifier = Modifier.width(CustomerTheme.spacing.xs))
                Text(text = profile.address, style = CustomerTheme.typography.caption, color = CustomerTheme.colors.secondaryText)
            }
        }
    }
}

@Composable
private fun CustomerFinancialSnapshotCard(financials: MyAccountFinancialSnapshot) {
    SucharuWallCard(
        containerColor = CustomerTheme.colors.elevatedSurface,
        borderColor = CustomerTheme.colors.border
    ) {
        Column {
            Text("My Account Summary", style = CustomerTheme.typography.sectionHeader, color = CustomerTheme.colors.primaryText)
            Spacer(modifier = Modifier.height(CustomerTheme.spacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Invoiced", style = CustomerTheme.typography.caption, color = CustomerTheme.colors.secondaryText)
                    Text(financials.totalInvoicedFormatted, style = CustomerTheme.typography.title, color = CustomerTheme.colors.primaryText)
                }
                Column {
                    Text("Total Paid", style = CustomerTheme.typography.caption, color = CustomerTheme.colors.secondaryText)
                    Text(financials.totalPaidFormatted, style = CustomerTheme.typography.title, color = CustomerTheme.colors.success)
                }
                Column {
                    Text("Outstanding Due", style = CustomerTheme.typography.caption, color = CustomerTheme.colors.secondaryText)
                    Text(financials.totalOutstandingDueFormatted, style = CustomerTheme.typography.title, color = CustomerTheme.colors.warning)
                }
            }
        }
    }
}

@Composable
private fun CustomerQuickActionsSection(onNavigateToDestination: (AppDestination) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CustomerTheme.spacing.sm)
    ) {
        QuickActionCard(
            label = "New Order",
            icon = Icons.Default.Add,
            onClick = { onNavigateToDestination(AppDestination.Customer.Quotations) },
            modifier = Modifier.weight(1f)
        )
        QuickActionCard(
            label = "My Orders",
            icon = Icons.Default.ShoppingCart,
            onClick = { onNavigateToDestination(AppDestination.Customer.Orders) },
            modifier = Modifier.weight(1f)
        )
        QuickActionCard(
            label = "Invoices",
            icon = Icons.Default.AccountBalance,
            onClick = { onNavigateToDestination(AppDestination.Customer.Invoices) },
            modifier = Modifier.weight(1f)
        )
        QuickActionCard(
            label = "Support",
            icon = Icons.Default.Call,
            onClick = { onNavigateToDestination(AppDestination.Customer.Support) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CustomerMyOrdersSection(
    orders: List<MyOrderSummary>,
    onNavigateToDestination: (AppDestination) -> Unit
) {
    Column {
        Text("My Orders & Progress", style = CustomerTheme.typography.sectionHeader, color = CustomerTheme.colors.primaryText)
        Spacer(modifier = Modifier.height(CustomerTheme.spacing.sm))

        orders.forEach { ord ->
            SucharuWallCard {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(ord.orderNumber, style = CustomerTheme.typography.bodyBold, color = CustomerTheme.colors.primaryText)
                        StatusChip(label = ord.statusLabel, dotColor = CustomerTheme.colors.accentPrimary)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(ord.title, style = CustomerTheme.typography.caption, color = CustomerTheme.colors.secondaryText)
                    Spacer(modifier = Modifier.height(CustomerTheme.spacing.xs))
                    Text("Order Progress: ${ord.customerProgressLabel}", style = CustomerTheme.typography.caption, color = CustomerTheme.colors.accentPrimary)
                }
            }
            Spacer(modifier = Modifier.height(CustomerTheme.spacing.sm))
        }
    }
}

@Composable
private fun CustomerMyQuotationsSection(
    quotations: List<MyQuotationSummary>,
    onNavigateToDestination: (AppDestination) -> Unit
) {
    Column {
        Text("My Quotations", style = CustomerTheme.typography.sectionHeader, color = CustomerTheme.colors.primaryText)
        Spacer(modifier = Modifier.height(CustomerTheme.spacing.sm))

        quotations.forEach { quote ->
            SucharuWallCard {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(quote.quoteNumber, style = CustomerTheme.typography.bodyBold, color = CustomerTheme.colors.primaryText)
                        StatusChip(label = quote.statusLabel, dotColor = CustomerTheme.colors.warning)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(quote.title, style = CustomerTheme.typography.caption, color = CustomerTheme.colors.secondaryText)
                    Spacer(modifier = Modifier.height(CustomerTheme.spacing.xs))
                    Text("Amount: ${quote.totalAmountFormatted} • Valid Until: ${quote.validUntilFormatted}", style = CustomerTheme.typography.caption, color = CustomerTheme.colors.mutedText)
                }
            }
            Spacer(modifier = Modifier.height(CustomerTheme.spacing.sm))
        }
    }
}

@Composable
private fun CustomerMyDocumentsSection(
    documents: List<MyDocumentItem>,
    onNavigateToDestination: (AppDestination) -> Unit
) {
    Column {
        Text("My Documents & Tax Invoices", style = CustomerTheme.typography.sectionHeader, color = CustomerTheme.colors.primaryText)
        Spacer(modifier = Modifier.height(CustomerTheme.spacing.sm))

        documents.forEach { doc ->
            SucharuWallCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = CustomerTheme.colors.accentPrimary)
                        Spacer(modifier = Modifier.width(CustomerTheme.spacing.sm))
                        Column {
                            Text(doc.title, style = CustomerTheme.typography.bodyBold, color = CustomerTheme.colors.primaryText)
                            Text("Type: ${doc.documentType} • Issued: ${doc.dateFormatted}", style = CustomerTheme.typography.caption, color = CustomerTheme.colors.secondaryText)
                        }
                    }
                    Icon(Icons.Default.Download, contentDescription = "Download", tint = CustomerTheme.colors.accentPrimary)
                }
            }
            Spacer(modifier = Modifier.height(CustomerTheme.spacing.sm))
        }
    }
}

@Composable
private fun CustomerSupportCard() {
    SucharuWallCard(
        containerColor = CustomerTheme.colors.surface,
        borderColor = CustomerTheme.colors.border
    ) {
        Column {
            Text("Customer Support & Helpline", style = CustomerTheme.typography.sectionHeader, color = CustomerTheme.colors.primaryText)
            Spacer(modifier = Modifier.height(CustomerTheme.spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Call, contentDescription = null, tint = CustomerTheme.colors.success)
                Spacer(modifier = Modifier.width(CustomerTheme.spacing.sm))
                Text("Helpline: +880 1700-000000 (9 AM - 8 PM)", style = CustomerTheme.typography.body, color = CustomerTheme.colors.primaryText)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Email, contentDescription = null, tint = CustomerTheme.colors.accentPrimary)
                Spacer(modifier = Modifier.width(CustomerTheme.spacing.sm))
                Text("Support Email: support@sucharu.com", style = CustomerTheme.typography.body, color = CustomerTheme.colors.primaryText)
            }
        }
    }
}
