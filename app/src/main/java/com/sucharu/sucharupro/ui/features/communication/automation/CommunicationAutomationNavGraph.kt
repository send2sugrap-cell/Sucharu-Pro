package com.sucharu.sucharupro.ui.features.communication.automation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.sucharu.sucharupro.data.datasource.FakeCommunicationAutomationDataSource
import com.sucharu.sucharupro.data.repository.CommunicationAutomationRepositoryImpl

object AutomationRoutes {
    const val GRAPH_ROOT = "automation_graph"
    const val DASHBOARD = "automation_dashboard"
    const val RULES = "automation_rules"
    const val CREATE_RULE = "automation_create_rule"
    const val RULE_DETAILS = "automation_rule_details/{ruleId}"
    const val EXECUTIONS = "automation_executions"
    const val ANALYTICS = "automation_analytics"

    fun ruleDetails(ruleId: String) = "automation_rule_details/$ruleId"
}

fun NavGraphBuilder.communicationAutomationGraph(navController: NavController) {
    val fakeDataSource = FakeCommunicationAutomationDataSource()
    val repository = CommunicationAutomationRepositoryImpl(fakeDataSource)

    navigation(
        startDestination = AutomationRoutes.DASHBOARD,
        route = AutomationRoutes.GRAPH_ROOT
    ) {
        composable(AutomationRoutes.DASHBOARD) {
            val vm = AutomationDashboardViewModel(repository)
            CommunicationAutomationDashboardScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRules = { navController.navigate(AutomationRoutes.RULES) },
                onNavigateToCreateRule = { navController.navigate(AutomationRoutes.CREATE_RULE) },
                onNavigateToExecutions = { navController.navigate(AutomationRoutes.EXECUTIONS) },
                onNavigateToAnalytics = { navController.navigate(AutomationRoutes.ANALYTICS) },
                onSelectRule = { id -> navController.navigate(AutomationRoutes.ruleDetails(id)) }
            )
        }

        composable(AutomationRoutes.RULES) {
            val vm = AutomationRuleListViewModel(repository)
            AutomationRuleListScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCreate = { navController.navigate(AutomationRoutes.CREATE_RULE) },
                onSelectRule = { id -> navController.navigate(AutomationRoutes.ruleDetails(id)) }
            )
        }

        composable(AutomationRoutes.CREATE_RULE) {
            val vm = AutomationRuleFormViewModel(repository)
            AutomationRuleFormScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }

        composable(AutomationRoutes.RULE_DETAILS) { backStackEntry ->
            val ruleId = backStackEntry.arguments?.getString("ruleId") ?: ""
            val vm = AutomationRuleDetailsViewModel(repository)
            AutomationRuleDetailsScreen(
                ruleId = ruleId,
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AutomationRoutes.EXECUTIONS) {
            val vm = AutomationExecutionViewModel(repository)
            AutomationExecutionScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AutomationRoutes.ANALYTICS) {
            val vm = AutomationAnalyticsViewModel(repository)
            AutomationAnalyticsScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
