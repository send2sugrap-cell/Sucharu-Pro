package com.sucharu.sucharupro.ui.features.customerportal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sucharu.sucharupro.data.api.client.BackendApiClient

object CustomerPortalRoutes {
    const val DASHBOARD = "customer_portal_dashboard"
    const val PROFILE = "customer_portal_profile"
    const val ORDERS = "customer_portal_orders"
    const val ORDER_DETAILS = "customer_portal_order_details/{orderId}"
    const val TRACK_PRODUCTION = "customer_portal_track_production/{orderId}"
    const val TRACK_DELIVERY = "customer_portal_track_delivery/{orderId}"

    fun orderDetailsRoute(orderId: String) = "customer_portal_order_details/$orderId"
    fun trackProductionRoute(orderId: String) = "customer_portal_track_production/$orderId"
    fun trackDeliveryRoute(orderId: String) = "customer_portal_track_delivery/$orderId"
}

@Composable
fun CustomerPortalNavGraph(
    apiClient: BackendApiClient,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val dashboardViewModel = remember { CustomerPortalDashboardViewModel(apiClient) }
    val ordersViewModel = remember { CustomerPortalOrdersViewModel(apiClient) }
    val productionViewModel = remember { CustomerPortalProductionViewModel(apiClient) }

    NavHost(
        navController = navController,
        startDestination = CustomerPortalRoutes.DASHBOARD,
        modifier = modifier
    ) {
        composable(CustomerPortalRoutes.DASHBOARD) {
            CustomerPortalDashboardScreen(
                viewModel = dashboardViewModel,
                onNavigateOrders = { navController.navigate(CustomerPortalRoutes.ORDERS) },
                onNavigateOrderDetail = { orderId -> navController.navigate(CustomerPortalRoutes.orderDetailsRoute(orderId)) },
                onNavigateInvoices = { navController.navigate(CustomerPortalRoutes.ORDERS) },
                onNavigatePayments = { navController.navigate(CustomerPortalRoutes.ORDERS) },
                onNavigateProfile = { navController.navigate(CustomerPortalRoutes.PROFILE) },
                onNavigateSupport = { navController.navigate(CustomerPortalRoutes.ORDERS) },
                onNavigateReturns = { navController.navigate(CustomerPortalRoutes.ORDERS) },
                onNavigateNotifications = { navController.navigate(CustomerPortalRoutes.ORDERS) },
                onLogout = onLogout
            )
        }

        composable(CustomerPortalRoutes.PROFILE) {
            val dashState by dashboardViewModel.uiState.collectAsState()
            val profile = (dashState as? CustomerPortalUiState.Success<CustomerDashboardSummary>)?.data?.profile
            CustomerPortalProfileScreen(
                profile = profile,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(CustomerPortalRoutes.ORDERS) {
            CustomerPortalOrderListScreen(
                viewModel = ordersViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateOrderDetail = { orderId -> navController.navigate(CustomerPortalRoutes.orderDetailsRoute(orderId)) }
            )
        }

        composable(CustomerPortalRoutes.ORDER_DETAILS) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            CustomerPortalOrderDetailsScreen(
                orderId = orderId,
                viewModel = ordersViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateTrackProduction = { id -> navController.navigate(CustomerPortalRoutes.trackProductionRoute(id)) },
                onNavigateTrackDelivery = { id -> navController.navigate(CustomerPortalRoutes.trackDeliveryRoute(id)) }
            )
        }

        composable(CustomerPortalRoutes.TRACK_PRODUCTION) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            CustomerPortalProductionTrackingScreen(
                orderId = orderId,
                viewModel = productionViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(CustomerPortalRoutes.TRACK_DELIVERY) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            CustomerPortalProductionTrackingScreen(
                orderId = orderId,
                viewModel = productionViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
