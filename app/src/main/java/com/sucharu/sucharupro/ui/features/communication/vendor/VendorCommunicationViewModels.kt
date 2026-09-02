package com.sucharu.sucharupro.ui.features.communication.vendor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.datasource.FakeVendorCommunicationDataSource
import com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource
import com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorCommunicationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.vendor.*
import com.sucharu.sucharupro.domain.model.notification.NotificationPriority
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.communication.vendor.VendorCommunicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// =========================================================================
// Dashboard ViewModel
// =========================================================================

/**
 * ViewModel for the Vendor Communication Dashboard screen (Module 10 Step 05).
 */
class VendorCommunicationDashboardViewModel(
    private val repository: VendorCommunicationRepository = defaultRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(VendorCommunicationUiState.DashboardState(isLoading = true))
    val uiState: StateFlow<VendorCommunicationUiState.DashboardState> = _state.asStateFlow()

    private val projectId = "default-project"
    private val actorId = "current-user"
    private val callerRole = UserRole.ADMIN

    init { loadDashboard() }

    fun loadDashboard() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val summaryResult = repository.getProjectSummary(projectId, actorId, callerRole)
            val engagementResult = repository.getEngagementSummary(projectId, null, actorId, callerRole)
            _state.update {
                it.copy(
                    isLoading = false,
                    summary = (summaryResult as? DomainResult.Success)?.data,
                    engagementSummary = (engagementResult as? DomainResult.Success)?.data,
                    error = (summaryResult as? DomainResult.Error)?.message
                )
            }
        }
    }
}

// =========================================================================
// List ViewModel
// =========================================================================

/**
 * ViewModel for the Vendor Communication Center/List screen (Module 10 Step 05).
 */
class VendorCommunicationListViewModel(
    private val repository: VendorCommunicationRepository = defaultRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(VendorCommunicationUiState.ListState(isLoading = true))
    val uiState: StateFlow<VendorCommunicationUiState.ListState> = _state.asStateFlow()

    private val projectId = "default-project"
    private val actorId = "current-user"
    private val callerRole = UserRole.ADMIN

    init { loadCommunications() }

    fun loadCommunications(vendorId: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = repository.listCommunications(projectId, vendorId, null, null, actorId, callerRole)
            _state.update {
                it.copy(
                    isLoading = false,
                    communications = (result as? DomainResult.Success)?.data ?: emptyList(),
                    error = (result as? DomainResult.Error)?.message
                )
            }
        }
    }

    fun updateSearch(query: String) { _state.update { it.copy(searchQuery = query) } }
    fun filterByType(type: VendorCommunicationType?) { _state.update { it.copy(filterType = type) } }
    fun filterByStatus(status: VendorCommunicationStatus?) { _state.update { it.copy(filterStatus = status) } }
}

// =========================================================================
// Details ViewModel
// =========================================================================

/**
 * ViewModel for the Vendor Communication Details screen (Module 10 Step 05).
 */
class VendorCommunicationDetailsViewModel(
    private val repository: VendorCommunicationRepository = defaultRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(VendorCommunicationUiState.DetailsState(isLoading = true))
    val uiState: StateFlow<VendorCommunicationUiState.DetailsState> = _state.asStateFlow()

    private val projectId = "default-project"
    private val actorId = "current-user"
    private val callerRole = UserRole.ADMIN

    fun loadCommunication(communicationId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val commResult = repository.getCommunication(projectId, communicationId, actorId, callerRole)
            val comm = (commResult as? DomainResult.Success)?.data
            val historyResult = if (comm != null) repository.getHistory(projectId, communicationId, actorId, callerRole) else null
            val ackResult = if (comm != null) repository.getAcknowledgement(projectId, communicationId, actorId, callerRole) else null
            val receiptResult = if (comm != null) repository.getReadReceipt(projectId, communicationId, comm.vendorId, actorId, callerRole) else null

            _state.update {
                it.copy(
                    isLoading = false,
                    communication = comm,
                    history = (historyResult as? DomainResult.Success)?.data ?: emptyList(),
                    acknowledgement = (ackResult as? DomainResult.Success)?.data,
                    readReceipt = (receiptResult as? DomainResult.Success)?.data,
                    error = (commResult as? DomainResult.Error)?.message
                )
            }
        }
    }
}

// =========================================================================
// Compose ViewModel
// =========================================================================

/**
 * ViewModel for the Vendor Communication Compose screen (Module 10 Step 05).
 */
class VendorCommunicationComposeViewModel(
    private val repository: VendorCommunicationRepository = defaultRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(VendorCommunicationUiState.ComposeState())
    val uiState: StateFlow<VendorCommunicationUiState.ComposeState> = _state.asStateFlow()

    private val projectId = "default-project"
    private val actorId = "current-user"
    private val callerRole = UserRole.ADMIN

    fun updateVendorId(id: String) { _state.update { it.copy(vendorId = id) } }
    fun updateSubject(s: String) { _state.update { it.copy(subject = s) } }
    fun updateMessage(m: String) { _state.update { it.copy(message = m) } }
    fun updateType(t: VendorCommunicationType) { _state.update { it.copy(communicationType = t) } }

    fun sendCommunication() {
        viewModelScope.launch {
            val s = _state.value
            if (s.vendorId.isBlank() || s.subject.isBlank() || s.message.isBlank()) {
                _state.update { it.copy(error = "Vendor, subject and message are required.") }
                return@launch
            }
            _state.update { it.copy(isSubmitting = true, error = null) }
            val result = repository.createCommunication(
                projectId = projectId,
                vendorId = s.vendorId,
                communicationType = s.communicationType,
                subject = s.subject,
                message = s.message,
                actorId = actorId,
                callerRole = callerRole
            )
            _state.update {
                it.copy(
                    isSubmitting = false,
                    isSuccess = result is DomainResult.Success,
                    error = (result as? DomainResult.Error)?.message
                )
            }
        }
    }

    fun reset() { _state.value = VendorCommunicationUiState.ComposeState() }
}

// =========================================================================
// Engagement ViewModel
// =========================================================================

/**
 * ViewModel for the Vendor Communication Engagement Analytics screen (Module 10 Step 05).
 */
class VendorCommunicationEngagementViewModel(
    private val repository: VendorCommunicationRepository = defaultRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(VendorCommunicationUiState.EngagementState(isLoading = true))
    val uiState: StateFlow<VendorCommunicationUiState.EngagementState> = _state.asStateFlow()

    private val projectId = "default-project"
    private val actorId = "current-user"
    private val callerRole = UserRole.ADMIN

    fun loadEngagement(vendorId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val summaryResult = repository.getEngagementSummary(projectId, vendorId, actorId, callerRole)
            val eventsResult = repository.getEngagementEvents(projectId, vendorId, actorId, callerRole)
            _state.update {
                it.copy(
                    isLoading = false,
                    summary = (summaryResult as? DomainResult.Success)?.data,
                    events = (eventsResult as? DomainResult.Success)?.data ?: emptyList(),
                    error = (summaryResult as? DomainResult.Error)?.message
                )
            }
        }
    }
}

// =========================================================================
// Acknowledgement ViewModel
// =========================================================================

/**
 * ViewModel for the Vendor Communication Acknowledgement screen (Module 10 Step 05).
 */
class VendorCommunicationAcknowledgementViewModel(
    private val repository: VendorCommunicationRepository = defaultRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(VendorCommunicationUiState.AcknowledgementState())
    val uiState: StateFlow<VendorCommunicationUiState.AcknowledgementState> = _state.asStateFlow()

    private val projectId = "default-project"
    private val actorId = "current-user"
    private val callerRole = UserRole.VENDOR
    private val callerVendorId = "vendor-001"

    fun loadCommunication(communicationId: String) {
        viewModelScope.launch {
            val result = repository.getCommunication(projectId, communicationId, actorId, UserRole.ADMIN)
            _state.update { it.copy(communication = (result as? DomainResult.Success)?.data) }
        }
    }

    fun acknowledge(communicationId: String, message: String?) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            val result = repository.acknowledge(projectId, communicationId, message, actorId, callerRole, callerVendorId)
            _state.update {
                it.copy(
                    isSubmitting = false,
                    isSuccess = result is DomainResult.Success,
                    acknowledgement = (result as? DomainResult.Success)?.data,
                    error = (result as? DomainResult.Error)?.message
                )
            }
        }
    }

    fun decline(communicationId: String, reason: String?) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            val result = repository.decline(projectId, communicationId, reason, actorId, callerRole, callerVendorId)
            _state.update {
                it.copy(
                    isSubmitting = false,
                    isSuccess = result is DomainResult.Success,
                    acknowledgement = (result as? DomainResult.Success)?.data,
                    error = (result as? DomainResult.Error)?.message
                )
            }
        }
    }
}

// =========================================================================
// Factory helpers
// =========================================================================

private fun defaultRepository(): VendorCommunicationRepository {
    val notificationDataSource = com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource()
    val notificationRepo = NotificationRepositoryImpl(notificationDataSource)
    return VendorCommunicationRepositoryImpl(FakeVendorCommunicationDataSource(), notificationRepo)
}
