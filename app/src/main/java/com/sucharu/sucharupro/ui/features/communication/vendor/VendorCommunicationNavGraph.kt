package com.sucharu.sucharupro.ui.features.communication.vendor

import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sucharu.sucharupro.navigation.Screen

/**
 * Extension for NavGraphBuilder to include Module 10 Step 05 Vendor & Supplier Communication routes.
 */
fun NavGraphBuilder.vendorCommunicationGraph(navController: NavHostController) {
    composable(route = Screen.VendorCommunicationDashboard.route) {
        val viewModel = remember { VendorCommunicationDashboardViewModel() }
        VendorCommunicationDashboardScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToCenter = { navController.navigate(Screen.VendorCommunicationCenter.route) },
            onNavigateToCompose = { navController.navigate(Screen.VendorCommunicationCompose.route) }
        )
    }

    composable(route = Screen.VendorCommunicationCenter.route) {
        val viewModel = remember { VendorCommunicationListViewModel() }
        VendorCommunicationCenterScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToDetails = { id -> navController.navigate(Screen.VendorCommunicationDetails.createRoute(id)) },
            onNavigateToCompose = { navController.navigate(Screen.VendorCommunicationCompose.route) }
        )
    }

    composable(route = Screen.VendorCommunicationDetails.route) { backStackEntry ->
        val communicationId = backStackEntry.arguments?.getString(Screen.VendorCommunicationDetails.ARG_COMMUNICATION_ID) ?: ""
        val viewModel = remember { VendorCommunicationDetailsViewModel() }
        VendorCommunicationDetailsScreen(
            communicationId = communicationId,
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToHistory = { id -> navController.navigate(Screen.VendorCommunicationHistory.createRoute(id)) },
            onNavigateToAcknowledge = { id -> navController.navigate(Screen.VendorCommunicationAcknowledgement.createRoute(id)) }
        )
    }

    composable(route = Screen.VendorCommunicationCompose.route) {
        val viewModel = remember { VendorCommunicationComposeViewModel() }
        VendorCommunicationComposeScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() },
            onSuccess = { navController.popBackStack() }
        )
    }

    composable(route = Screen.VendorCommunicationThread.route) { backStackEntry ->
        val communicationId = backStackEntry.arguments?.getString(Screen.VendorCommunicationThread.ARG_COMMUNICATION_ID) ?: ""
        val viewModel = remember { VendorCommunicationDetailsViewModel() }
        VendorCommunicationThreadScreen(
            communicationId = communicationId,
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(route = Screen.VendorCommunicationHistory.route) { backStackEntry ->
        val communicationId = backStackEntry.arguments?.getString(Screen.VendorCommunicationHistory.ARG_COMMUNICATION_ID) ?: ""
        val viewModel = remember { VendorCommunicationDetailsViewModel() }
        VendorCommunicationHistoryScreen(
            communicationId = communicationId,
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(route = Screen.VendorCommunicationEngagement.route) { backStackEntry ->
        val vendorId = backStackEntry.arguments?.getString(Screen.VendorCommunicationEngagement.ARG_VENDOR_ID) ?: ""
        val viewModel = remember { VendorCommunicationEngagementViewModel() }
        VendorCommunicationEngagementScreen(
            vendorId = vendorId,
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(route = Screen.VendorCommunicationAdmin.route) {
        val viewModel = remember { VendorCommunicationListViewModel() }
        VendorCommunicationAdminScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToDetails = { id -> navController.navigate(Screen.VendorCommunicationDetails.createRoute(id)) }
        )
    }

    composable(route = Screen.VendorCommunicationSchedule.route) {
        val viewModel = remember { VendorCommunicationListViewModel() }
        VendorCommunicationScheduleScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(route = Screen.VendorCommunicationAcknowledgement.route) { backStackEntry ->
        val communicationId = backStackEntry.arguments?.getString(Screen.VendorCommunicationAcknowledgement.ARG_COMMUNICATION_ID) ?: ""
        val viewModel = remember { VendorCommunicationAcknowledgementViewModel() }
        VendorCommunicationAcknowledgementScreen(
            communicationId = communicationId,
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() },
            onSuccess = { navController.popBackStack() }
        )
    }
}
