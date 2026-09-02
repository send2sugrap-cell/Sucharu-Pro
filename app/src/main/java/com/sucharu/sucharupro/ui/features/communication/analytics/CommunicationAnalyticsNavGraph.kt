package com.sucharu.sucharupro.ui.features.communication.analytics

import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.sucharu.sucharupro.data.datasource.FakeCampaignDataSource
import com.sucharu.sucharupro.data.datasource.FakeCommunicationAnalyticsDataSource
import com.sucharu.sucharupro.data.datasource.FakeCommunicationAutomationDataSource
import com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource
import com.sucharu.sucharupro.data.repository.CommunicationAnalyticsRepositoryImpl
import com.sucharu.sucharupro.domain.model.user.UserRole

fun NavGraphBuilder.communicationAnalyticsNavGraph(
    navController: NavHostController,
    projectId: String,
    currentUserId: String,
    currentUserRole: UserRole,
    analyticsDataSource: FakeCommunicationAnalyticsDataSource,
    notificationDataSource: FakeNotificationDataSource,
    automationDataSource: FakeCommunicationAutomationDataSource,
    campaignDataSource: FakeCampaignDataSource
) {
    navigation(
        startDestination = "communication_analytics_dashboard",
        route = "communication_analytics"
    ) {
        composable("communication_analytics_dashboard") {
            val repository = remember {
                CommunicationAnalyticsRepositoryImpl(
                    analyticsDataSource = analyticsDataSource,
                    notificationDataSource = notificationDataSource,
                    automationDataSource = automationDataSource,
                    campaignDataSource = campaignDataSource
                )
            }
            
            val viewModel = remember {
                CommunicationAnalyticsViewModel(
                    repository = repository,
                    currentUserRole = currentUserRole,
                    currentUserId = currentUserId,
                    projectId = projectId
                )
            }
            
            CommunicationAnalyticsDashboardScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
