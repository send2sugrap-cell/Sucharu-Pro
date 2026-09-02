package com.sucharu.sucharupro.ui.features.communication.automation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.automation.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.communication.automation.CommunicationAutomationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AutomationDashboardViewModel(
    private val repository: CommunicationAutomationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AutomationDashboardUiState())
    val uiState: StateFlow<AutomationDashboardUiState> = _uiState.asStateFlow()

    fun load(projectId: String = "default-project", callerRole: UserRole = UserRole.ADMIN) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val sumRes = repository.getProjectSummary(projectId, "actor-01", callerRole)
            val rulesRes = repository.getRules(projectId, actorId = "actor-01", callerRole = callerRole)
            val execsRes = repository.getExecutions(projectId, actorId = "actor-01", callerRole = callerRole)

            if (sumRes is DomainResult.Success && rulesRes is DomainResult.Success && execsRes is DomainResult.Success) {
                _uiState.value = AutomationDashboardUiState(
                    isLoading = false,
                    summary = sumRes.data,
                    recentRules = rulesRes.data.take(5),
                    recentExecutions = execsRes.data.take(5)
                )
            } else {
                _uiState.value = AutomationDashboardUiState(isLoading = false, error = "Failed to load automation dashboard.")
            }
        }
    }
}

class AutomationRuleListViewModel(
    private val repository: CommunicationAutomationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AutomationRuleListUiState())
    val uiState: StateFlow<AutomationRuleListUiState> = _uiState.asStateFlow()

    fun load(projectId: String = "default-project", callerRole: UserRole = UserRole.ADMIN) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val res = repository.getRules(
                projectId = projectId,
                eventType = _uiState.value.selectedEventType,
                enabledOnly = _uiState.value.enabledOnly,
                actorId = "actor-01",
                callerRole = callerRole
            )
            if (res is DomainResult.Success) {
                _uiState.value = _uiState.value.copy(isLoading = false, rules = res.data)
            } else if (res is DomainResult.Error) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = res.message)
            }
        }
    }

    fun setFilterEventType(type: CommunicationAutomationEventType?) {
        _uiState.value = _uiState.value.copy(selectedEventType = type)
        load()
    }
}

class AutomationRuleDetailsViewModel(
    private val repository: CommunicationAutomationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AutomationRuleDetailsUiState())
    val uiState: StateFlow<AutomationRuleDetailsUiState> = _uiState.asStateFlow()

    fun load(projectId: String = "default-project", ruleId: String, callerRole: UserRole = UserRole.ADMIN) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val ruleRes = repository.getRuleById(projectId, ruleId, "actor-01", callerRole)
            if (ruleRes is DomainResult.Success) {
                val execsRes = repository.getExecutions(projectId, ruleId, "actor-01", callerRole)
                _uiState.value = AutomationRuleDetailsUiState(
                    isLoading = false,
                    rule = ruleRes.data,
                    executions = (execsRes as? DomainResult.Success)?.data ?: emptyList()
                )
            } else if (ruleRes is DomainResult.Error) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = ruleRes.message)
            }
        }
    }

    fun toggleEnabled(projectId: String, ruleId: String, enabled: Boolean, callerRole: UserRole = UserRole.ADMIN) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isToggling = true)
            val res = repository.toggleRuleStatus(projectId, ruleId, enabled, "actor-01", callerRole)
            if (res is DomainResult.Success) {
                _uiState.value = _uiState.value.copy(isToggling = false, rule = res.data)
            } else if (res is DomainResult.Error) {
                _uiState.value = _uiState.value.copy(isToggling = false, error = res.message)
            }
        }
    }
}

class AutomationRuleFormViewModel(
    private val repository: CommunicationAutomationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AutomationRuleFormUiState())
    val uiState: StateFlow<AutomationRuleFormUiState> = _uiState.asStateFlow()

    fun updateName(value: String) { _uiState.value = _uiState.value.copy(name = value) }
    fun updateDescription(value: String) { _uiState.value = _uiState.value.copy(description = value) }
    fun updateEventType(value: CommunicationAutomationEventType) { _uiState.value = _uiState.value.copy(eventType = value) }
    fun updateTitleTemplate(value: String) { _uiState.value = _uiState.value.copy(titleTemplate = value) }
    fun updateMessageTemplate(value: String) { _uiState.value = _uiState.value.copy(messageTemplate = value) }

    fun submit(projectId: String = "default-project", callerRole: UserRole = UserRole.ADMIN) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
            val rule = CommunicationAutomationRule(
                ruleId = "aut-${java.util.UUID.randomUUID().toString().take(8)}",
                ruleNo = "AUT-2026-${(10000..99999).random()}",
                projectId = projectId,
                name = _uiState.value.name,
                description = _uiState.value.description,
                eventType = _uiState.value.eventType,
                titleTemplate = _uiState.value.titleTemplate,
                messageTemplate = _uiState.value.messageTemplate,
                createdBy = "user-admin-01"
            )
            val res = repository.createRule(rule, "user-admin-01", callerRole)
            if (res is DomainResult.Success) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, isSuccess = true)
            } else if (res is DomainResult.Error) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, error = res.message)
            }
        }
    }
}

class AutomationExecutionViewModel(
    private val repository: CommunicationAutomationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AutomationExecutionUiState())
    val uiState: StateFlow<AutomationExecutionUiState> = _uiState.asStateFlow()

    fun load(projectId: String = "default-project", callerRole: UserRole = UserRole.ADMIN) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val res = repository.getExecutions(projectId, actorId = "actor-01", callerRole = callerRole)
            if (res is DomainResult.Success) {
                _uiState.value = AutomationExecutionUiState(isLoading = false, executions = res.data)
            } else if (res is DomainResult.Error) {
                _uiState.value = AutomationExecutionUiState(isLoading = false, error = res.message)
            }
        }
    }
}

class AutomationAnalyticsViewModel(
    private val repository: CommunicationAutomationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AutomationAnalyticsUiState())
    val uiState: StateFlow<AutomationAnalyticsUiState> = _uiState.asStateFlow()

    fun load(projectId: String = "default-project", callerRole: UserRole = UserRole.ADMIN) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val res = repository.getProjectSummary(projectId, "actor-01", callerRole)
            if (res is DomainResult.Success) {
                _uiState.value = AutomationAnalyticsUiState(isLoading = false, summary = res.data)
            } else if (res is DomainResult.Error) {
                _uiState.value = AutomationAnalyticsUiState(isLoading = false, error = res.message)
            }
        }
    }
}
