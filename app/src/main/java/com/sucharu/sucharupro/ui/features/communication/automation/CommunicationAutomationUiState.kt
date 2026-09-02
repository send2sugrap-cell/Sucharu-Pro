package com.sucharu.sucharupro.ui.features.communication.automation

import com.sucharu.sucharupro.domain.model.communication.automation.*

data class AutomationDashboardUiState(
    val isLoading: Boolean = false,
    val summary: CommunicationAutomationSummary = CommunicationAutomationSummary(projectId = "default-project"),
    val recentRules: List<CommunicationAutomationRule> = emptyList(),
    val recentExecutions: List<CommunicationAutomationExecution> = emptyList(),
    val error: String? = null
)

data class AutomationRuleListUiState(
    val isLoading: Boolean = false,
    val rules: List<CommunicationAutomationRule> = emptyList(),
    val selectedEventType: CommunicationAutomationEventType? = null,
    val enabledOnly: Boolean = false,
    val searchQuery: String = "",
    val error: String? = null
)

data class AutomationRuleDetailsUiState(
    val isLoading: Boolean = false,
    val rule: CommunicationAutomationRule? = null,
    val executions: List<CommunicationAutomationExecution> = emptyList(),
    val activityEvents: List<CommunicationAutomationActivityEvent> = emptyList(),
    val isToggling: Boolean = false,
    val error: String? = null
)

data class AutomationRuleFormUiState(
    val isSubmitting: Boolean = false,
    val name: String = "",
    val description: String = "",
    val eventType: CommunicationAutomationEventType = CommunicationAutomationEventType.ORDER_STATUS_CHANGED,
    val titleTemplate: String = "",
    val messageTemplate: String = "",
    val isSuccess: Boolean = false,
    val error: String? = null
)

data class AutomationExecutionUiState(
    val isLoading: Boolean = false,
    val executions: List<CommunicationAutomationExecution> = emptyList(),
    val error: String? = null
)

data class AutomationAnalyticsUiState(
    val isLoading: Boolean = false,
    val summary: CommunicationAutomationSummary = CommunicationAutomationSummary(projectId = "default-project"),
    val error: String? = null
)
