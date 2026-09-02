package com.sucharu.sucharupro.ui.features.communication.vendor.document

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation

// =========================================================================
// Route Constants
// =========================================================================
object VendorDocumentRoutes {
    const val GRAPH_ROOT = "vendor_document_graph"
    const val DASHBOARD = "vendor_document_dashboard"
    const val REQUEST_LIST = "vendor_document_request_list"
    const val REQUEST_DETAILS = "vendor_document_request_details/{requestId}"
    const val SUBMIT_DOCUMENT = "vendor_document_submit?requestId={requestId}"
    const val DOCUMENT_LIST = "vendor_document_list"
    const val DOCUMENT_DETAILS = "vendor_document_details/{documentId}"
    const val DOCUMENT_REVIEW = "vendor_document_review/{documentId}"
    const val VERSION_HISTORY = "vendor_document_version_history/{documentId}"
    const val COMPLIANCE_DASHBOARD = "vendor_document_compliance"
    const val EXPIRY_TRACKER = "vendor_document_expiry"
    const val ACTIVITY_LOG = "vendor_document_activity"

    fun requestDetails(requestId: String) = "vendor_document_request_details/$requestId"
    fun submitDocument(requestId: String? = null) =
        if (requestId != null) "vendor_document_submit?requestId=$requestId"
        else "vendor_document_submit?requestId="
    fun documentDetails(documentId: String) = "vendor_document_details/$documentId"
    fun documentReview(documentId: String) = "vendor_document_review/$documentId"
    fun versionHistory(documentId: String) = "vendor_document_version_history/$documentId"
}

// =========================================================================
// Nav Graph Builder
// =========================================================================
fun NavGraphBuilder.vendorDocumentGraph(navController: NavController) {
    navigation(
        startDestination = VendorDocumentRoutes.DASHBOARD,
        route = VendorDocumentRoutes.GRAPH_ROOT
    ) {
        composable(VendorDocumentRoutes.DASHBOARD) {
            val vm: VendorDocumentDashboardViewModel = viewModel()
            VendorDocumentDashboardScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRequests = { navController.navigate(VendorDocumentRoutes.REQUEST_LIST) },
                onNavigateToDocuments = { navController.navigate(VendorDocumentRoutes.DOCUMENT_LIST) },
                onNavigateToCompliance = { navController.navigate(VendorDocumentRoutes.COMPLIANCE_DASHBOARD) },
                onNavigateToExpiry = { navController.navigate(VendorDocumentRoutes.EXPIRY_TRACKER) }
            )
        }

        composable(VendorDocumentRoutes.REQUEST_LIST) {
            val vm: VendorDocumentRequestListViewModel = viewModel()
            VendorDocumentRequestListScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetails = { navController.navigate(VendorDocumentRoutes.requestDetails(it)) },
                onNavigateToNewRequest = { navController.navigate(VendorDocumentRoutes.submitDocument()) }
            )
        }

        composable(
            route = VendorDocumentRoutes.REQUEST_DETAILS,
            arguments = listOf(navArgument("requestId") { type = NavType.StringType })
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId") ?: return@composable
            val vm: VendorDocumentRequestDetailsViewModel = viewModel()
            VendorDocumentRequestDetailsScreen(
                requestId = requestId,
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSubmit = { navController.navigate(VendorDocumentRoutes.submitDocument(it)) }
            )
        }

        composable(
            route = VendorDocumentRoutes.SUBMIT_DOCUMENT,
            arguments = listOf(navArgument("requestId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId")
            val vm: VendorDocumentSubmitViewModel = viewModel()
            VendorDocumentSubmitScreen(
                requestId = requestId,
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onSuccess = {
                    navController.navigate(VendorDocumentRoutes.DOCUMENT_LIST) {
                        popUpTo(VendorDocumentRoutes.SUBMIT_DOCUMENT) { inclusive = true }
                    }
                }
            )
        }

        composable(VendorDocumentRoutes.DOCUMENT_LIST) {
            val vm: VendorDocumentListViewModel = viewModel()
            VendorDocumentListScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetails = { navController.navigate(VendorDocumentRoutes.documentDetails(it)) }
            )
        }

        composable(
            route = VendorDocumentRoutes.DOCUMENT_DETAILS,
            arguments = listOf(navArgument("documentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId") ?: return@composable
            val vm: VendorDocumentDetailsViewModel = viewModel()
            VendorDocumentDetailsScreen(
                documentId = documentId,
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToReview = { navController.navigate(VendorDocumentRoutes.documentReview(it)) },
                onNavigateToVersionHistory = { navController.navigate(VendorDocumentRoutes.versionHistory(it)) }
            )
        }

        composable(
            route = VendorDocumentRoutes.DOCUMENT_REVIEW,
            arguments = listOf(navArgument("documentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId") ?: return@composable
            val vm: VendorDocumentReviewViewModel = viewModel()
            VendorDocumentReviewScreen(
                documentId = documentId,
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onReviewComplete = { navController.popBackStack() }
            )
        }

        composable(
            route = VendorDocumentRoutes.VERSION_HISTORY,
            arguments = listOf(navArgument("documentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId") ?: return@composable
            val vm: VendorDocumentVersionHistoryViewModel = viewModel()
            VendorDocumentVersionHistoryScreen(
                documentId = documentId,
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(VendorDocumentRoutes.COMPLIANCE_DASHBOARD) {
            val vm: VendorComplianceViewModel = viewModel()
            VendorComplianceDashboardScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(VendorDocumentRoutes.EXPIRY_TRACKER) {
            val vm: VendorDocumentExpiryViewModel = viewModel()
            VendorDocumentExpiryScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(VendorDocumentRoutes.ACTIVITY_LOG) {
            val vm: VendorDocumentActivityViewModel = viewModel()
            VendorDocumentActivityScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
