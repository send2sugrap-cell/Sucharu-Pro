package com.sucharu.sucharupro.ui.features.delivery.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryAnalyticsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeliveryGovernanceAlertDetailsViewModel(
    private val repository: DeliveryAnalyticsRepository,
    private val alertId: String,
    private val currentUserRole: UserRole = UserRole.ADMIN,
    private val currentActorId: String = "admin-1"
) : ViewModel() {

    private val _uiState = MutableStateFlow<DeliveryGovernanceAlertDetailsUiState>(DeliveryGovernanceAlertDetailsUiState.Loading)
    val uiState: StateFlow<DeliveryGovernanceAlertDetailsUiState> = _uiState.asStateFlow()

    init {
        loadDetails()
    }

    fun loadDetails() {
        viewModelScope.launch {
            _uiState.value = DeliveryGovernanceAlertDetailsUiState.Loading
            val alertRes = repository.getAlertById(alertId, currentUserRole)
            if (alertRes is DomainResult.Error) {
                _uiState.value = DeliveryGovernanceAlertDetailsUiState.Error(alertRes.message)
                return@launch
            }

            val eventsRes = repository.getActivityEvents(alertId, currentUserRole)
            val alert = (alertRes as DomainResult.Success).data
            val events = (eventsRes as? DomainResult.Success)?.data ?: emptyList()

            _uiState.value = DeliveryGovernanceAlertDetailsUiState.Success(alert, events)
        }
    }

    fun acknowledge() {
        viewModelScope.launch {
            val res = repository.acknowledgeAlert(alertId, currentActorId, currentUserRole)
            if (res is DomainResult.Success) {
                loadDetails()
            }
        }
    }

    fun resolve(notes: String) {
        viewModelScope.launch {
            val res = repository.resolveAlert(alertId, currentActorId, notes, currentUserRole)
            if (res is DomainResult.Success) {
                loadDetails()
            }
        }
    }

    fun dismiss(reason: String) {
        viewModelScope.launch {
            val res = repository.dismissAlert(alertId, currentActorId, reason, currentUserRole)
            if (res is DomainResult.Success) {
                loadDetails()
            }
        }
    }
}
