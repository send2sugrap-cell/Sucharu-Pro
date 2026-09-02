package com.sucharu.sucharupro.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.foundation.layout.size
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.components.AppOutlinedButton
import com.sucharu.sucharupro.ui.components.SectionHeader
import com.sucharu.sucharupro.ui.components.StatusBadge
import com.sucharu.sucharupro.ui.features.communication.vendor.vendorCommunicationGraph
import com.sucharu.sucharupro.ui.features.communication.vendor.document.vendorDocumentGraph
import com.sucharu.sucharupro.ui.features.communication.campaign.campaignGraph
import com.sucharu.sucharupro.ui.features.communication.automation.communicationAutomationGraph
import com.sucharu.sucharupro.ui.features.communication.analytics.communicationAnalyticsNavGraph
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.ui.features.dashboard.DashboardScreen
import com.sucharu.sucharupro.ui.features.dashboard.DashboardViewModel
import com.sucharu.sucharupro.ui.theme.spacing
import com.sucharu.sucharupro.ui.theme.statusColors

/**
 * Main NavHost for Sucharu Pro connecting all top-level destinations.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Dashboard.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(route = Screen.Dashboard.route) {
            val dashboardViewModel = remember { DashboardViewModel() }
            DashboardScreen(
                viewModel = dashboardViewModel,
                onNavigateToNewOrder = { navController.navigateToTopLevelDestination(Screen.Orders) },
                onNavigateToOrders = { _ -> navController.navigateToTopLevelDestination(Screen.Orders) },
                onNavigateToOrderDetail = { _ -> navController.navigateToTopLevelDestination(Screen.Orders) },
                onNavigateToProductionStage = { _ -> navController.navigateToTopLevelDestination(Screen.Printing) },
                onNavigateToPrintingCalculator = { navController.navigateToTopLevelDestination(Screen.Printing) },
                onNavigateToCustomers = { navController.navigateToTopLevelDestination(Screen.Customers) },
                onNavigateToInvoices = { navController.navigateToTopLevelDestination(Screen.Reports) },
                onNavigateToInventory = { navController.navigateToTopLevelDestination(Screen.Printing) }
            )
        }
        composable(route = Screen.Orders.route) {
            val inquiryViewModel = remember {
                com.sucharu.sucharupro.ui.features.orders.inquiry.InquiryListViewModel()
            }
            val quotationViewModel = remember {
                com.sucharu.sucharupro.ui.features.orders.quotation.QuotationListViewModel()
            }
            val orderViewModel = remember {
                com.sucharu.sucharupro.ui.features.orders.order.OrderListViewModel()
            }
            com.sucharu.sucharupro.ui.features.orders.QuotationOrderManagementScreen(
                inquiryViewModel = inquiryViewModel,
                quotationViewModel = quotationViewModel,
                orderViewModel = orderViewModel,
                onInquiryClick = { inquiryId ->
                    navController.navigate(Screen.InquiryDetails.createRoute(inquiryId))
                },
                onQuotationClick = { quotationId ->
                    navController.navigate(Screen.QuotationDetails.createRoute(quotationId))
                },
                onOrderClick = { orderId ->
                    navController.navigate(Screen.OrderDetails.createRoute(orderId))
                },
                onAddInquiryClick = {
                    navController.navigate(Screen.InquiryCreate.route)
                },
                onAddQuotationClick = {
                    navController.navigate(Screen.QuotationCreate.createRoute())
                }
            )
        }
        composable(
            route = Screen.InquiryDetails.route,
            arguments = listOf(
                androidx.navigation.navArgument(Screen.InquiryDetails.ARG_INQUIRY_ID) {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val inquiryId = backStackEntry.arguments?.getString(Screen.InquiryDetails.ARG_INQUIRY_ID).orEmpty()
            val detailsViewModel = remember(inquiryId) {
                com.sucharu.sucharupro.ui.features.orders.inquiry.details.InquiryDetailsViewModel(inquiryId = inquiryId)
            }
            com.sucharu.sucharupro.ui.features.orders.inquiry.details.InquiryDetailsScreen(
                viewModel = detailsViewModel,
                onBackClick = { navController.popBackStack() },
                onEditClick = { id ->
                    navController.navigate(Screen.InquiryEdit.createRoute(id))
                }
            )
        }
        composable(route = Screen.InquiryCreate.route) {
            val formViewModel = remember {
                com.sucharu.sucharupro.ui.features.orders.inquiry.form.InquiryFormViewModel()
            }
            com.sucharu.sucharupro.ui.features.orders.inquiry.form.InquiryFormScreen(
                viewModel = formViewModel,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { inqId ->
                    navController.navigate(Screen.InquiryDetails.createRoute(inqId)) {
                        popUpTo(Screen.InquiryCreate.route) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Screen.InquiryEdit.route,
            arguments = listOf(
                androidx.navigation.navArgument(Screen.InquiryEdit.ARG_INQUIRY_ID) {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val inquiryId = backStackEntry.arguments?.getString(Screen.InquiryEdit.ARG_INQUIRY_ID).orEmpty()
            val formViewModel = remember(inquiryId) {
                com.sucharu.sucharupro.ui.features.orders.inquiry.form.InquiryFormViewModel(inquiryId = inquiryId)
            }
            com.sucharu.sucharupro.ui.features.orders.inquiry.form.InquiryFormScreen(
                viewModel = formViewModel,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = {
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = Screen.QuotationDetails.route,
            arguments = listOf(
                androidx.navigation.navArgument(Screen.QuotationDetails.ARG_QUOTATION_ID) {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val quotationId = backStackEntry.arguments?.getString(Screen.QuotationDetails.ARG_QUOTATION_ID).orEmpty()
            val detailsViewModel = remember(quotationId) {
                com.sucharu.sucharupro.ui.features.orders.quotation.details.QuotationDetailsViewModel(quotationId = quotationId)
            }
            com.sucharu.sucharupro.ui.features.orders.quotation.details.QuotationDetailsScreen(
                viewModel = detailsViewModel,
                onBackClick = { navController.popBackStack() },
                onEditClick = { id ->
                    navController.navigate(Screen.QuotationEdit.createRoute(id))
                },
                onNavigateToOrder = { orderId ->
                    navController.navigate(Screen.OrderDetails.createRoute(orderId))
                }
            )
        }
        composable(
            route = Screen.QuotationCreate.route,
            arguments = listOf(
                androidx.navigation.navArgument(Screen.QuotationCreate.ARG_INQUIRY_ID) {
                    type = androidx.navigation.NavType.StringType
                    defaultValue = ""
                },
                androidx.navigation.navArgument(Screen.QuotationCreate.ARG_CUSTOMER_ID) {
                    type = androidx.navigation.NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val inquiryId = backStackEntry.arguments?.getString(Screen.QuotationCreate.ARG_INQUIRY_ID).orEmpty().ifBlank { null }
            val customerId = backStackEntry.arguments?.getString(Screen.QuotationCreate.ARG_CUSTOMER_ID).orEmpty().ifBlank { null }
            val formViewModel = remember(inquiryId, customerId) {
                com.sucharu.sucharupro.ui.features.orders.quotation.form.QuotationFormViewModel(
                    initialInquiryId = inquiryId,
                    initialCustomerId = customerId
                )
            }
            com.sucharu.sucharupro.ui.features.orders.quotation.form.QuotationFormScreen(
                viewModel = formViewModel,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { quoId ->
                    navController.navigate(Screen.QuotationDetails.createRoute(quoId)) {
                        popUpTo(Screen.QuotationCreate.route) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Screen.QuotationEdit.route,
            arguments = listOf(
                androidx.navigation.navArgument(Screen.QuotationEdit.ARG_QUOTATION_ID) {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val quotationId = backStackEntry.arguments?.getString(Screen.QuotationEdit.ARG_QUOTATION_ID).orEmpty()
            val formViewModel = remember(quotationId) {
                com.sucharu.sucharupro.ui.features.orders.quotation.form.QuotationFormViewModel(quotationId = quotationId)
            }
            com.sucharu.sucharupro.ui.features.orders.quotation.form.QuotationFormScreen(
                viewModel = formViewModel,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = {
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = Screen.OrderDetails.route,
            arguments = listOf(
                androidx.navigation.navArgument(Screen.OrderDetails.ARG_ORDER_ID) {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString(Screen.OrderDetails.ARG_ORDER_ID).orEmpty()
            val detailsViewModel = remember(orderId) {
                com.sucharu.sucharupro.ui.features.orders.order.details.OrderDetailsViewModel(orderId = orderId)
            }
            com.sucharu.sucharupro.ui.features.orders.order.details.OrderDetailsScreen(
                viewModel = detailsViewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToCustomer = { customerId ->
                    navController.navigate(Screen.CustomerDetails.createRoute(customerId))
                },
                onNavigateToQuotation = { quotationId ->
                    navController.navigate(Screen.QuotationDetails.createRoute(quotationId))
                }
            )
        }
        composable(route = Screen.Printing.route) {
            com.sucharu.sucharupro.ui.features.printing.quotation.PrintingQuotationWorkspaceScreen(
                onNavigateBack = { navController.navigateToTopLevelDestination(Screen.Dashboard) }
            )
        }
        composable(route = Screen.PrintingCalculatorWorkspace.route) {
            com.sucharu.sucharupro.ui.features.printing.calculator.PrintingCalculatorScreen()
        }
        composable(route = Screen.PrintingQuotationWorkspace.route) {
            com.sucharu.sucharupro.ui.features.printing.quotation.PrintingQuotationWorkspaceScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(route = Screen.Customers.route) {
            val customerViewModel = remember {
                com.sucharu.sucharupro.ui.features.customer.CustomerListViewModel()
            }
            com.sucharu.sucharupro.ui.features.customer.CustomerListScreen(
                viewModel = customerViewModel,
                onCustomerClick = { customerId ->
                    navController.navigate(Screen.CustomerDetails.createRoute(customerId))
                },
                onAddCustomerClick = {
                    navController.navigate(Screen.CustomerCreate.route)
                }
            )
        }
        composable(
            route = Screen.CustomerDetails.route,
            arguments = listOf(
                androidx.navigation.navArgument(Screen.CustomerDetails.ARG_CUSTOMER_ID) {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString(Screen.CustomerDetails.ARG_CUSTOMER_ID).orEmpty()
            val detailsViewModel = remember(customerId) {
                com.sucharu.sucharupro.ui.features.customer.details.CustomerDetailsViewModel(customerId = customerId)
            }
            com.sucharu.sucharupro.ui.features.customer.details.CustomerDetailsScreen(
                viewModel = detailsViewModel,
                onBackClick = { navController.popBackStack() },
                onEditClick = { id ->
                    navController.navigate(Screen.CustomerEdit.createRoute(id))
                }
            )
        }
        composable(route = Screen.CustomerCreate.route) {
            val formViewModel = remember {
                com.sucharu.sucharupro.ui.features.customer.form.CustomerFormViewModel()
            }
            com.sucharu.sucharupro.ui.features.customer.form.CustomerFormScreen(
                viewModel = formViewModel,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { _ ->
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = Screen.CustomerEdit.route,
            arguments = listOf(
                androidx.navigation.navArgument(Screen.CustomerEdit.ARG_CUSTOMER_ID) {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString(Screen.CustomerEdit.ARG_CUSTOMER_ID).orEmpty()
            val formViewModel = remember(customerId) {
                com.sucharu.sucharupro.ui.features.customer.form.CustomerFormViewModel(customerId = customerId)
            }
            com.sucharu.sucharupro.ui.features.customer.form.CustomerFormScreen(
                viewModel = formViewModel,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { _ ->
                    navController.popBackStack()
                }
            )
        }
        composable(route = Screen.FinancialReconciliationDashboard.route) {
            val periodDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeAccountingPeriodDataSource() }
            val snapshotDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeFinancialClosingSnapshotDataSource() }
            val recDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeFinancialReconciliationDataSource() }
            val discDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeFinancialDiscrepancyDataSource() }
            val periodRepo = remember {
                com.sucharu.sucharupro.data.repository.AccountingPeriodRepositoryImpl(
                    periodDataSource = periodDataSource,
                    snapshotDataSource = snapshotDataSource,
                    discrepancyDataSource = discDataSource,
                    reconciliationDataSource = recDataSource
                )
            }
            val recRepo = remember {
                com.sucharu.sucharupro.data.repository.FinancialReconciliationRepositoryImpl(
                    reconciliationDataSource = recDataSource,
                    discrepancyDataSource = discDataSource,
                    periodDataSource = periodDataSource
                )
            }
            val viewModel = remember {
                com.sucharu.sucharupro.ui.features.finance.reconciliation.FinancialReconciliationViewModel(
                    reconciliationRepository = recRepo,
                    periodRepository = periodRepo
                )
            }
            com.sucharu.sucharupro.ui.features.finance.reconciliation.FinancialReconciliationDashboardScreen(
                viewModel = viewModel,
                onNavigateToExecute = { periodId ->
                    navController.navigate(Screen.FinancialReconciliationExecution.createRoute(periodId))
                },
                onNavigateToDiscrepancies = {
                    navController.navigate(Screen.FinancialDiscrepancies.route)
                },
                onNavigateToPeriodClose = {
                    navController.navigate(Screen.AccountingPeriods.route)
                }
            )
        }
        composable(
            route = Screen.FinancialReconciliationExecution.route,
            arguments = listOf(
                androidx.navigation.navArgument(Screen.FinancialReconciliationExecution.ARG_PERIOD_ID) {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val periodId = backStackEntry.arguments?.getString(Screen.FinancialReconciliationExecution.ARG_PERIOD_ID).orEmpty()
            val periodDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeAccountingPeriodDataSource() }
            val snapshotDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeFinancialClosingSnapshotDataSource() }
            val recDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeFinancialReconciliationDataSource() }
            val discDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeFinancialDiscrepancyDataSource() }
            val periodRepo = remember {
                com.sucharu.sucharupro.data.repository.AccountingPeriodRepositoryImpl(
                    periodDataSource = periodDataSource,
                    snapshotDataSource = snapshotDataSource,
                    discrepancyDataSource = discDataSource,
                    reconciliationDataSource = recDataSource
                )
            }
            val recRepo = remember {
                com.sucharu.sucharupro.data.repository.FinancialReconciliationRepositoryImpl(
                    reconciliationDataSource = recDataSource,
                    discrepancyDataSource = discDataSource,
                    periodDataSource = periodDataSource
                )
            }
            val viewModel = remember {
                com.sucharu.sucharupro.ui.features.finance.reconciliation.FinancialReconciliationViewModel(
                    reconciliationRepository = recRepo,
                    periodRepository = periodRepo
                )
            }
            com.sucharu.sucharupro.ui.features.finance.reconciliation.FinancialReconciliationExecutionScreen(
                periodId = periodId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(route = Screen.FinancialDiscrepancies.route) {
            val periodDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeAccountingPeriodDataSource() }
            val snapshotDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeFinancialClosingSnapshotDataSource() }
            val recDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeFinancialReconciliationDataSource() }
            val discDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeFinancialDiscrepancyDataSource() }
            val periodRepo = remember {
                com.sucharu.sucharupro.data.repository.AccountingPeriodRepositoryImpl(
                    periodDataSource = periodDataSource,
                    snapshotDataSource = snapshotDataSource,
                    discrepancyDataSource = discDataSource,
                    reconciliationDataSource = recDataSource
                )
            }
            val recRepo = remember {
                com.sucharu.sucharupro.data.repository.FinancialReconciliationRepositoryImpl(
                    reconciliationDataSource = recDataSource,
                    discrepancyDataSource = discDataSource,
                    periodDataSource = periodDataSource
                )
            }
            val viewModel = remember {
                com.sucharu.sucharupro.ui.features.finance.reconciliation.FinancialReconciliationViewModel(
                    reconciliationRepository = recRepo,
                    periodRepository = periodRepo
                )
            }
            com.sucharu.sucharupro.ui.features.finance.reconciliation.FinancialDiscrepancyScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(route = Screen.AccountingPeriods.route) {
            val periodDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeAccountingPeriodDataSource() }
            val snapshotDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeFinancialClosingSnapshotDataSource() }
            val recDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeFinancialReconciliationDataSource() }
            val discDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeFinancialDiscrepancyDataSource() }
            val periodRepo = remember {
                com.sucharu.sucharupro.data.repository.AccountingPeriodRepositoryImpl(
                    periodDataSource = periodDataSource,
                    snapshotDataSource = snapshotDataSource,
                    discrepancyDataSource = discDataSource,
                    reconciliationDataSource = recDataSource
                )
            }
            val viewModel = remember {
                com.sucharu.sucharupro.ui.features.finance.periodclose.AccountingPeriodViewModel(
                    periodRepository = periodRepo
                )
            }
            com.sucharu.sucharupro.ui.features.finance.periodclose.AccountingPeriodScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChecklist = { periodId ->
                    navController.navigate(Screen.ClosingChecklist.createRoute(periodId))
                },
                onNavigateToSnapshot = { periodId ->
                    navController.navigate(Screen.ClosingSnapshot.createRoute(periodId))
                },
                onNavigateToReopenRequests = { periodId ->
                    navController.navigate(Screen.PeriodReopenRequests.createRoute(periodId))
                }
            )
        }
        composable(
            route = Screen.ClosingChecklist.route,
            arguments = listOf(
                androidx.navigation.navArgument(Screen.ClosingChecklist.ARG_PERIOD_ID) {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val periodId = backStackEntry.arguments?.getString(Screen.ClosingChecklist.ARG_PERIOD_ID).orEmpty()
            val periodDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeAccountingPeriodDataSource() }
            val snapshotDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeFinancialClosingSnapshotDataSource() }
            val recDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeFinancialReconciliationDataSource() }
            val discDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeFinancialDiscrepancyDataSource() }
            val periodRepo = remember {
                com.sucharu.sucharupro.data.repository.AccountingPeriodRepositoryImpl(
                    periodDataSource = periodDataSource,
                    snapshotDataSource = snapshotDataSource,
                    discrepancyDataSource = discDataSource,
                    reconciliationDataSource = recDataSource
                )
            }
            val viewModel = remember {
                com.sucharu.sucharupro.ui.features.finance.periodclose.AccountingPeriodViewModel(
                    periodRepository = periodRepo
                )
            }
            com.sucharu.sucharupro.ui.features.finance.periodclose.ClosingChecklistScreen(
                periodId = periodId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSnapshot = { pId ->
                    navController.navigate(Screen.ClosingSnapshot.createRoute(pId))
                }
            )
        }
        composable(
            route = Screen.ClosingSnapshot.route,
            arguments = listOf(
                androidx.navigation.navArgument(Screen.ClosingSnapshot.ARG_PERIOD_ID) {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val periodId = backStackEntry.arguments?.getString(Screen.ClosingSnapshot.ARG_PERIOD_ID).orEmpty()
            val periodDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeAccountingPeriodDataSource() }
            val snapshotDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeFinancialClosingSnapshotDataSource() }
            val recDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeFinancialReconciliationDataSource() }
            val discDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeFinancialDiscrepancyDataSource() }
            val periodRepo = remember {
                com.sucharu.sucharupro.data.repository.AccountingPeriodRepositoryImpl(
                    periodDataSource = periodDataSource,
                    snapshotDataSource = snapshotDataSource,
                    discrepancyDataSource = discDataSource,
                    reconciliationDataSource = recDataSource
                )
            }
            val viewModel = remember {
                com.sucharu.sucharupro.ui.features.finance.periodclose.AccountingPeriodViewModel(
                    periodRepository = periodRepo
                )
            }
            com.sucharu.sucharupro.ui.features.finance.periodclose.ClosingSnapshotScreen(
                periodId = periodId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.PeriodReopenRequests.route,
            arguments = listOf(
                androidx.navigation.navArgument(Screen.PeriodReopenRequests.ARG_PERIOD_ID) {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val periodId = backStackEntry.arguments?.getString(Screen.PeriodReopenRequests.ARG_PERIOD_ID).orEmpty()
            val periodDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeAccountingPeriodDataSource() }
            val snapshotDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeFinancialClosingSnapshotDataSource() }
            val recDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeFinancialReconciliationDataSource() }
            val discDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeFinancialDiscrepancyDataSource() }
            val periodRepo = remember {
                com.sucharu.sucharupro.data.repository.AccountingPeriodRepositoryImpl(
                    periodDataSource = periodDataSource,
                    snapshotDataSource = snapshotDataSource,
                    discrepancyDataSource = discDataSource,
                    reconciliationDataSource = recDataSource
                )
            }
            val viewModel = remember {
                com.sucharu.sucharupro.ui.features.finance.periodclose.AccountingPeriodViewModel(
                    periodRepository = periodRepo
                )
            }
            com.sucharu.sucharupro.ui.features.finance.periodclose.PeriodReopenRequestScreen(
                periodId = periodId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(route = Screen.Reports.route) {
            DestinationPlaceholderScreen(
                screen = Screen.Reports,
                onBackToDashboard = { navController.navigateToTopLevelDestination(Screen.Dashboard) }
            )
        }
        composable(route = Screen.Settings.route) {
            DestinationPlaceholderScreen(
                screen = Screen.Settings,
                onBackToDashboard = { navController.navigateToTopLevelDestination(Screen.Dashboard) }
            )
        }

        // Module 10 Step 03 Internal Communication Composables
        composable(route = Screen.InternalCommunicationDashboard.route) {
            val commDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeInternalCommunicationDataSource() }
            val notifDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource() }
            val notifRepo = remember { com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl(notifDataSource) }
            val repo = remember { com.sucharu.sucharupro.data.repository.InternalCommunicationRepositoryImpl(commDataSource, notifRepo) }
            val viewModel = remember { com.sucharu.sucharupro.ui.features.internalcommunication.InternalCommunicationDashboardViewModel(repo) }
            com.sucharu.sucharupro.ui.features.internalcommunication.InternalCommunicationDashboardScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToInbox = { navController.navigate(Screen.InternalCommunicationInbox.route) },
                onNavigateToCompose = { navController.navigate(Screen.InternalCommunicationCompose.route) },
                onNavigateToDetails = { id -> navController.navigate(Screen.InternalCommunicationDetails.createRoute(id)) }
            )
        }
        composable(route = Screen.InternalCommunicationInbox.route) {
            val commDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeInternalCommunicationDataSource() }
            val notifDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource() }
            val notifRepo = remember { com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl(notifDataSource) }
            val repo = remember { com.sucharu.sucharupro.data.repository.InternalCommunicationRepositoryImpl(commDataSource, notifRepo) }
            val viewModel = remember { com.sucharu.sucharupro.ui.features.internalcommunication.InternalCommunicationInboxViewModel(repo) }
            com.sucharu.sucharupro.ui.features.internalcommunication.InternalCommunicationInboxScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetails = { id -> navController.navigate(Screen.InternalCommunicationDetails.createRoute(id)) }
            )
        }
        composable(
            route = Screen.InternalCommunicationDetails.route,
            arguments = listOf(
                androidx.navigation.navArgument(Screen.InternalCommunicationDetails.ARG_COMMUNICATION_ID) {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val communicationId = backStackEntry.arguments?.getString(Screen.InternalCommunicationDetails.ARG_COMMUNICATION_ID).orEmpty()
            val commDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeInternalCommunicationDataSource() }
            val notifDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource() }
            val notifRepo = remember { com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl(notifDataSource) }
            val repo = remember { com.sucharu.sucharupro.data.repository.InternalCommunicationRepositoryImpl(commDataSource, notifRepo) }
            val viewModel = remember(communicationId) { com.sucharu.sucharupro.ui.features.internalcommunication.InternalCommunicationDetailsViewModel(repo, communicationId) }
            com.sucharu.sucharupro.ui.features.internalcommunication.InternalCommunicationDetailsScreen(
                communicationId = communicationId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToThread = { threadId -> navController.navigate(Screen.InternalCommunicationThread.createRoute(threadId)) }
            )
        }
        composable(route = Screen.InternalCommunicationCompose.route) {
            val commDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeInternalCommunicationDataSource() }
            val notifDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource() }
            val notifRepo = remember { com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl(notifDataSource) }
            val repo = remember { com.sucharu.sucharupro.data.repository.InternalCommunicationRepositoryImpl(commDataSource, notifRepo) }
            val viewModel = remember { com.sucharu.sucharupro.ui.features.internalcommunication.InternalCommunicationComposeViewModel(repo) }
            com.sucharu.sucharupro.ui.features.internalcommunication.InternalCommunicationComposeScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.InternalCommunicationThread.route,
            arguments = listOf(
                androidx.navigation.navArgument(Screen.InternalCommunicationThread.ARG_THREAD_ID) {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val threadId = backStackEntry.arguments?.getString(Screen.InternalCommunicationThread.ARG_THREAD_ID).orEmpty()
            val commDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeInternalCommunicationDataSource() }
            val notifDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource() }
            val notifRepo = remember { com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl(notifDataSource) }
            val repo = remember { com.sucharu.sucharupro.data.repository.InternalCommunicationRepositoryImpl(commDataSource, notifRepo) }
            val viewModel = remember(threadId) { com.sucharu.sucharupro.ui.features.internalcommunication.InternalCommunicationThreadViewModel(repo, threadId) }
            com.sucharu.sucharupro.ui.features.internalcommunication.InternalCommunicationThreadScreen(
                threadId = threadId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(route = Screen.InternalCommunicationTeam.route) {
            val teamId = "TEAM-PRINTING"
            val commDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeInternalCommunicationDataSource() }
            val notifDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource() }
            val notifRepo = remember { com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl(notifDataSource) }
            val repo = remember { com.sucharu.sucharupro.data.repository.InternalCommunicationRepositoryImpl(commDataSource, notifRepo) }
            val viewModel = remember { com.sucharu.sucharupro.ui.features.internalcommunication.InternalCommunicationTeamViewModel(repo) }
            com.sucharu.sucharupro.ui.features.internalcommunication.InternalCommunicationTeamScreen(
                teamId = teamId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetails = { id -> navController.navigate(Screen.InternalCommunicationDetails.createRoute(id)) }
            )
        }
        composable(route = Screen.InternalCommunicationDepartment.route) {
            val deptId = "DEPT-PRODUCTION"
            val commDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeInternalCommunicationDataSource() }
            val notifDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource() }
            val notifRepo = remember { com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl(notifDataSource) }
            val repo = remember { com.sucharu.sucharupro.data.repository.InternalCommunicationRepositoryImpl(commDataSource, notifRepo) }
            val viewModel = remember { com.sucharu.sucharupro.ui.features.internalcommunication.InternalCommunicationDepartmentViewModel(repo) }
            com.sucharu.sucharupro.ui.features.internalcommunication.InternalCommunicationDepartmentScreen(
                departmentId = deptId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetails = { id -> navController.navigate(Screen.InternalCommunicationDetails.createRoute(id)) }
            )
        }
        composable(route = Screen.InternalCommunicationBroadcast.route) {
            val commDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeInternalCommunicationDataSource() }
            val notifDataSource = remember { com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource() }
            val notifRepo = remember { com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl(notifDataSource) }
            val repo = remember { com.sucharu.sucharupro.data.repository.InternalCommunicationRepositoryImpl(commDataSource, notifRepo) }
            val viewModel = remember { com.sucharu.sucharupro.ui.features.internalcommunication.InternalCommunicationBroadcastViewModel(repo) }
            com.sucharu.sucharupro.ui.features.internalcommunication.InternalCommunicationBroadcastScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Module 10 Step 04 Staff Task Composables
        composable(route = Screen.TaskDashboard.route) {
            val taskDataSource = remember { com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource() }
            val repo = remember { com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl(taskDataSource) }
            val viewModel = remember { com.sucharu.sucharupro.ui.features.task.TaskDashboardViewModel(repo) }
            com.sucharu.sucharupro.ui.features.task.screens.TaskDashboardScreen(
                viewModel = viewModel,
                onNavigateToTasks = { navController.navigate(Screen.TaskList.route) },
                onNavigateToCreate = { navController.navigate(Screen.TaskCreate.route) },
                onNavigateToBoard = { navController.navigate(Screen.TaskBoard.route) },
                onSelectTask = { id -> navController.navigate(Screen.TaskDetails.createRoute(id)) }
            )
        }

        composable(route = Screen.TaskList.route) {
            val taskDataSource = remember { com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource() }
            val repo = remember { com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl(taskDataSource) }
            val viewModel = remember { com.sucharu.sucharupro.ui.features.task.TaskListViewModel(repo) }
            com.sucharu.sucharupro.ui.features.task.screens.TaskListScreen(
                viewModel = viewModel,
                onSelectTask = { id -> navController.navigate(Screen.TaskDetails.createRoute(id)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.TaskDetails.route,
            arguments = listOf(
                androidx.navigation.navArgument(Screen.TaskDetails.ARG_TASK_ID) {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString(Screen.TaskDetails.ARG_TASK_ID).orEmpty()
            val taskDataSource = remember { com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource() }
            val repo = remember { com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl(taskDataSource) }
            val viewModel = remember(taskId) { com.sucharu.sucharupro.ui.features.task.TaskDetailsViewModel(repo) }
            com.sucharu.sucharupro.ui.features.task.screens.TaskDetailsScreen(
                taskId = taskId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.TaskCreate.route) {
            val taskDataSource = remember { com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource() }
            val repo = remember { com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl(taskDataSource) }
            val viewModel = remember { com.sucharu.sucharupro.ui.features.task.TaskFormViewModel(repo) }
            com.sucharu.sucharupro.ui.features.task.screens.TaskFormScreen(
                viewModel = viewModel,
                onTaskCreated = { id ->
                    navController.navigate(Screen.TaskDetails.createRoute(id)) {
                        popUpTo(Screen.TaskCreate.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.TaskBoard.route) {
            val taskDataSource = remember { com.sucharu.sucharupro.data.datasource.task.FakeTaskDataSource() }
            val repo = remember { com.sucharu.sucharupro.data.repository.task.TaskRepositoryImpl(taskDataSource) }
            val viewModel = remember { com.sucharu.sucharupro.ui.features.task.TaskBoardViewModel(repo) }
            com.sucharu.sucharupro.ui.features.task.screens.TaskBoardScreen(
                viewModel = viewModel,
                onSelectTask = { id -> navController.navigate(Screen.TaskDetails.createRoute(id)) },
                onBack = { navController.popBackStack() }
            )
        }

        // Module 10 Step 05 - Vendor & Supplier Communication Routes
        vendorCommunicationGraph(navController)

        // Module 10 Step 06 - Vendor Document, Compliance & Record Communication
        vendorDocumentGraph(navController)

        // Module 10 Step 07 - Campaign, Announcement & Broadcast Communication
        campaignGraph(navController)

        // Module 10 Step 08 - Communication Automation, Event Triggers & Smart Notification Orchestration
        communicationAutomationGraph(navController)

        // Module 10 Step 09 - Communication Analytics, Engagement Intelligence & Governance
        val analyticsDataSource = com.sucharu.sucharupro.data.datasource.FakeCommunicationAnalyticsDataSource()
        val analyticsNotifDataSource = com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource()
        val analyticsAutomationDataSource = com.sucharu.sucharupro.data.datasource.FakeCommunicationAutomationDataSource()
        val analyticsCampaignDataSource = com.sucharu.sucharupro.data.datasource.FakeCampaignDataSource()
        communicationAnalyticsNavGraph(
            navController = navController,
            projectId = "PROJ-001",
            currentUserId = "USR-001",
            currentUserRole = UserRole.ADMIN,
            analyticsDataSource = analyticsDataSource,
            notificationDataSource = analyticsNotifDataSource,
            automationDataSource = analyticsAutomationDataSource,
            campaignDataSource = analyticsCampaignDataSource
        )
    }
}


/**
 * Reusable Material 3 Bottom Navigation Bar for top-level destinations.
 */
@Composable
fun AppBottomNavBar(
    currentRoute: String?,
    onNavigateToDestination: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    items: List<BottomNavItem> = bottomNavItems
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.screen.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigateToDestination(item.screen) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.contentDescription
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/**
 * Helper to navigate to top-level destinations with proper backstack handling.
 *
 * Enforces single-top and popUpTo start destination to prevent navigation loops and backstack leaks.
 */
fun NavHostController.navigateToTopLevelDestination(screen: Screen) {
    navigate(screen.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Clean placeholder UI for modular feature screens before future feature module implementation.
 */
@Composable
fun DestinationPlaceholderScreen(
    screen: Screen,
    onBackToDashboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.screenPadding),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        SectionHeader(
            title = screen.title,
            subtitle = "Sucharu Pro — Full-Service Printing ERP"
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

        AppCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(MaterialTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                    Text(
                        text = "${screen.title} Module",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "Navigation route '${screen.route}' is connected. This module will be implemented in its designated step.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

                StatusBadge(
                    label = "Foundation Ready",
                    statusColor = MaterialTheme.statusColors.orderReady
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                AppOutlinedButton(
                    text = "Back to Dashboard",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = onBackToDashboard,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
