package com.sucharu.sucharupro.ui.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.dashboard.DashboardSummary
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.components.LoadingIndicator
import com.sucharu.sucharupro.ui.features.dashboard.components.DashboardHeader
import com.sucharu.sucharupro.ui.features.dashboard.components.DashboardInventoryAlerts
import com.sucharu.sucharupro.ui.features.dashboard.components.DashboardKpiGrid
import com.sucharu.sucharupro.ui.features.dashboard.components.DashboardOperationalAlertsSection
import com.sucharu.sucharupro.ui.features.dashboard.components.DashboardPaymentSummary
import com.sucharu.sucharupro.ui.features.dashboard.components.DashboardQuickActions
import com.sucharu.sucharupro.ui.features.dashboard.components.DashboardRecentOrders
import com.sucharu.sucharupro.ui.features.dashboard.components.DashboardWorkflowPipeline
import com.sucharu.sucharupro.ui.features.dashboard.components.DashboardWorkloadSection
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Main Entry Point for the Sucharu Pro Dashboard Screen (Admin / Manager Control Center).
 *
 * @param onNavigateToNewOrder Opens new order creation flow.
 * @param onNavigateToOrders Navigate to orders list, optionally filtered by [OrderStatusType].
 * @param onNavigateToOrderDetail Navigate to order ticket details.
 * @param onNavigateToProductionStage Navigate to production list filtered by [ProductionStageType].
 * @param onNavigateToPrintingCalculator Opens price/printing calculator.
 * @param onNavigateToCustomers Opens customer directory.
 * @param onNavigateToInvoices Opens billing/invoices management.
 * @param onNavigateToInventory Opens finished product inventory management.
 * @param userRole Optional user role for role-aware visibility preparation.
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToNewOrder: () -> Unit,
    onNavigateToOrders: (OrderStatusType?) -> Unit,
    onNavigateToOrderDetail: (String) -> Unit,
    onNavigateToProductionStage: (ProductionStageType) -> Unit,
    onNavigateToPrintingCalculator: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    onNavigateToInventory: () -> Unit,
    modifier: Modifier = Modifier,
    userRole: UserRole? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is DashboardUiState.Loading -> {
                    DashboardLoadingView(
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is DashboardUiState.Error -> {
                    DashboardErrorView(
                        errorMessage = state.errorMessage,
                        onRetry = { viewModel.retry() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is DashboardUiState.Empty -> {
                    DashboardEmptyView(
                        message = state.message,
                        onNewOrderClick = onNavigateToNewOrder,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is DashboardUiState.Success -> {
                    DashboardSuccessContent(
                        summary = state.summary,
                        isRefreshing = state.isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        onNewOrderClick = onNavigateToNewOrder,
                        onNewCustomerClick = onNavigateToCustomers,
                        onPrintJobClick = onNavigateToPrintingCalculator,
                        onRecordPaymentClick = onNavigateToInvoices,
                        onCreateInvoiceClick = onNavigateToInvoices,
                        onProductionStageClick = onNavigateToProductionStage,
                        onJobClick = onNavigateToOrderDetail,
                        onViewAllOrdersClick = { onNavigateToOrders(null) },
                        onViewAllDueClick = { onNavigateToOrders(OrderStatusType.READY) },
                        onViewInvoicesClick = onNavigateToInvoices,
                        onViewInventoryClick = onNavigateToInventory,
                        onOrdersWithStatusClick = onNavigateToOrders,
                        userRole = userRole,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardSuccessContent(
    summary: DashboardSummary,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onNewOrderClick: () -> Unit,
    onNewCustomerClick: () -> Unit,
    onPrintJobClick: () -> Unit,
    onRecordPaymentClick: () -> Unit,
    onCreateInvoiceClick: () -> Unit,
    onProductionStageClick: (ProductionStageType) -> Unit,
    onJobClick: (String) -> Unit,
    onViewAllOrdersClick: () -> Unit,
    onViewAllDueClick: () -> Unit,
    onViewInvoicesClick: () -> Unit,
    onViewInventoryClick: () -> Unit,
    onOrdersWithStatusClick: (OrderStatusType?) -> Unit,
    modifier: Modifier = Modifier,
    userRole: UserRole? = null
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.spacing.screenPadding)
            .padding(bottom = MaterialTheme.spacing.xxLarge),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large)
    ) {
        // 1. Header (Shop Profile, Date, Shift, Quick Refresh)
        DashboardHeader(
            headerInfo = summary.shopHeader,
            onRefreshClick = onRefresh,
            isRefreshing = isRefreshing
        )

        // 2. Executive KPI Summary (Sales, Volume, Finance, Auxiliary)
        DashboardKpiGrid(
            kpis = summary.kpis,
            userRole = userRole
        )

        // 3. Quick Action Shortcuts (Role-aware)
        DashboardQuickActions(
            onNewOrderClick = onNewOrderClick,
            onNewCustomerClick = onNewCustomerClick,
            onPrintJobClick = onPrintJobClick,
            onRecordPaymentClick = onRecordPaymentClick,
            onCreateInvoiceClick = onCreateInvoiceClick,
            userRole = userRole
        )

        // 4. Real-time Operational Alerts with full click navigation routing
        DashboardOperationalAlertsSection(
            alerts = summary.operationalAlerts,
            onPendingApprovalClick = { onOrdersWithStatusClick(OrderStatusType.CONFIRMED) },
            onQcPendingClick = { onProductionStageClick(ProductionStageType.QC) },
            onDeliveryPendingClick = { onOrdersWithStatusClick(OrderStatusType.READY) },
            onDelayedJobsClick = { onOrdersWithStatusClick(OrderStatusType.IN_PRODUCTION) },
            onLowStockClick = onViewInventoryClick,
            onUnpaidInvoicesClick = onViewInvoicesClick,
            onVendorDueClick = onViewInvoicesClick,
            onReplacementClick = { onOrdersWithStatusClick(null) },
            onViewAllAlertsClick = onViewAllOrdersClick,
            userRole = userRole
        )

        // 5. Today's Workload & Urgency Section (Rush, Priority, Due Today)
        DashboardWorkloadSection(
            workload = summary.workloadSummary,
            onJobClick = onJobClick,
            onViewAllDueClick = onViewAllDueClick
        )

        // 6. Production Workflow Pipeline (13 canonical stages)
        DashboardWorkflowPipeline(
            stageCounts = summary.stageCounts,
            onStageClick = onProductionStageClick
        )

        // 7. Payment & Receivables Financial Snapshot (Role-aware)
        DashboardPaymentSummary(
            paymentBreakdown = summary.paymentBreakdown,
            onViewInvoicesClick = onViewInvoicesClick,
            userRole = userRole
        )

        // 8. Recent Orders & Jobs (explicitly showing OrderStatus + ProductionStage)
        DashboardRecentOrders(
            orders = summary.recentOrders,
            onOrderClick = onJobClick,
            onViewAllOrdersClick = onViewAllOrdersClick
        )

        // 9. Finished Product Stock Alerts (Finished goods only, Role-aware)
        DashboardInventoryAlerts(
            alerts = summary.inventoryAlerts,
            onViewInventoryClick = onViewInventoryClick,
            userRole = userRole
        )
    }
}

@Composable
private fun DashboardLoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(
            message = "Loading operational dashboard...",
            size = 48.dp
        )
    }
}

@Composable
private fun DashboardEmptyView(
    message: String,
    onNewOrderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(MaterialTheme.spacing.large),
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
                Text(
                    text = "Welcome to Sucharu Pro",
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
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
                AppButton(
                    text = "+ Create First Order",
                    onClick = onNewOrderClick
                )
            }
        }
    }
}

@Composable
private fun DashboardErrorView(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(MaterialTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(MaterialTheme.spacing.large)
        ) {
            Text(
                text = "Dashboard Unavailable",
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
                text = "Retry",
                onClick = onRetry
            )
        }
    }
}
