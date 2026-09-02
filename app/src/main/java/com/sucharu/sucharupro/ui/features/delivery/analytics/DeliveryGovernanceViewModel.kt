package com.sucharu.sucharupro.ui.features.delivery.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlertCategory
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlertSeverity
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlertStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryAnalyticsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeliveryGovernanceViewModel(
    private val repository: DeliveryAnalyticsRepository,
    private val projectId: String,
    private val currentUserRole: UserRole = UserRole.ADMIN,
    private val currentActorId: String = "admin-1"
) : ViewModel() {

    private val _uiState = MutableStateFlow<DeliveryGovernanceUiState>(DeliveryGovernanceUiState.Loading)
    val uiState: StateFlow<DeliveryGovernanceUiState> = _uiState.asStateFlow()

    init {
        refreshAlerts()
    }

    fun refreshAlerts() {
        viewModelScope.launch {
            _uiState.value = DeliveryGovernanceUiState.Loading
            val refreshRes = repository.refreshGovernanceAlerts(projectId, currentActorId, currentUserRole)
            if (refreshRes is DomainResult.Error) {
                _uiState.value = DeliveryGovernanceUiState.Error(refreshRes.message)
                return@launch
            }

            val alerts = (refreshRes as DomainResult.Success).data
            if (alerts.isEmpty()) {
                _uiState.value = DeliveryGovernanceUiState.Empty
            } else {
                _uiState.value = DeliveryGovernanceUiState.Success(
                    alerts = alerts,
                    openCount = alerts.count { it.status == DeliveryGovernanceAlertStatus.OPEN },
                    criticalCount = alerts.count { it.severity == DeliveryGovernanceAlertSeverity.CRITICAL },
                    warningCount = alerts.count { it.severity == DeliveryGovernanceAlertSeverity.WARNING }
                )
            }
        }
    }

    fun acknowledgeAlert(alertId: String) {
        viewModelScope.launch {
            val result = repository.acknowledgeAlert(alertId, currentActorId, currentUserRole)
            if (result is DomainResult.Success) {
                refreshAlerts()
            }
        }
    }

    fun resolveAlert(alertId: String, notes: String) {
        viewModelScope.launch {
            val result = repository.resolveAlert(alertId, currentActorId, notes, currentUserRole)
            if (result is DomainResult.Success) {
                refreshAlerts()
            }
        }
    }

    fun dismissAlert(alertId: String, reason: String) {
        viewModelScope.launch {
            val result = repository.dismissAlert(alertId, currentActorId, reason, currentUserRole)
            if (result is DomainResult.Success) {
                refreshAlerts()
            }
        }
    }
}
