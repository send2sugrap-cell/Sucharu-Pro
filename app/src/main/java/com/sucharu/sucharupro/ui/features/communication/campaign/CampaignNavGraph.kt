package com.sucharu.sucharupro.ui.features.communication.campaign

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.sucharu.sucharupro.data.datasource.FakeCampaignDataSource
import com.sucharu.sucharupro.data.repository.CampaignRepositoryImpl

object CampaignRoutes {
    const val GRAPH_ROOT = "campaign_graph"
    const val DASHBOARD = "campaign_dashboard"
    const val LIST = "campaign_list"
    const val CREATE = "campaign_create"
    const val DETAILS = "campaign_details/{campaignId}"
    const val ANNOUNCEMENTS = "campaign_announcements"
    const val BROADCASTS = "campaign_broadcasts"
    const val ANALYTICS = "campaign_analytics"

    fun details(campaignId: String) = "campaign_details/$campaignId"
}

fun NavGraphBuilder.campaignGraph(navController: NavController) {
    // Shared repository instance for navigation graph
    val fakeDataSource = FakeCampaignDataSource()
    val repository = CampaignRepositoryImpl(fakeDataSource)

    navigation(
        startDestination = CampaignRoutes.DASHBOARD,
        route = CampaignRoutes.GRAPH_ROOT
    ) {
        composable(CampaignRoutes.DASHBOARD) {
            val vm = CampaignDashboardViewModel(repository)
            CampaignDashboardScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToList = { navController.navigate(CampaignRoutes.LIST) },
                onNavigateToCreate = { navController.navigate(CampaignRoutes.CREATE) },
                onNavigateToAnnouncements = { navController.navigate(CampaignRoutes.ANNOUNCEMENTS) },
                onNavigateToBroadcasts = { navController.navigate(CampaignRoutes.BROADCASTS) },
                onNavigateToAnalytics = { navController.navigate(CampaignRoutes.ANALYTICS) },
                onSelectCampaign = { id -> navController.navigate(CampaignRoutes.details(id)) }
            )
        }

        composable(CampaignRoutes.LIST) {
            val vm = CampaignListViewModel(repository)
            CampaignListScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCreate = { navController.navigate(CampaignRoutes.CREATE) },
                onSelectCampaign = { id -> navController.navigate(CampaignRoutes.details(id)) }
            )
        }

        composable(CampaignRoutes.CREATE) {
            val vm = CampaignFormViewModel(repository)
            CampaignFormScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }

        composable(CampaignRoutes.DETAILS) { backStackEntry ->
            val campaignId = backStackEntry.arguments?.getString("campaignId") ?: ""
            val vm = CampaignDetailsViewModel(repository)
            CampaignDetailsScreen(
                campaignId = campaignId,
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(CampaignRoutes.ANNOUNCEMENTS) {
            val vm = AnnouncementViewModel(repository)
            AnnouncementScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onSelectAnnouncement = { /* View announcement details */ }
            )
        }

        composable(CampaignRoutes.BROADCASTS) {
            val vm = BroadcastViewModel(repository)
            BroadcastScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(CampaignRoutes.ANALYTICS) {
            val vm = CampaignAnalyticsViewModel(repository)
            CampaignAnalyticsScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
