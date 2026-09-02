package com.sucharu.sucharupro.ui.features.customercommunication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.customer.*
import com.sucharu.sucharupro.domain.model.notification.NotificationChannel
import com.sucharu.sucharupro.domain.model.notification.NotificationPriority
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.communication.customer.CustomerCommunicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Customer Communication Center (Module 10 Step 02).
 */
class CustomerCommunicationCenterViewModel(
    private val repository: CustomerCommunicationRepository,
    private val projectId: String = "PRJ-DEFAULT",
    private val currentCustomerId: String = "CUST-001",
    private val currentActorId: String = "CUST-001",
    private val currentUserRole: UserRole = UserRole.CUSTOMER
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerCommunicationCenterUiState())
    val uiState: StateFlow<CustomerCommunicationCenterUiState> = _uiState.asStateFlow()

    init {
        observeCommunications()
    }

    private fun observeCommunications() {
        viewModelScope.launch {
            repository.observeCustomerCommunications(projectId, currentCustomerId, currentUserRole).collect { list ->
                _uiState.update { current ->
                    val unread = list.count { !it.isRead && it.status != CustomerCommunicationStatus.CANCELLED }
                    current.copy(communications = list, unreadCount = unread)
                }
            }
        }
    }

    fun setFilter(filter: CustomerCommunicationFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun markAsRead(communicationId: String) {
        viewModelScope.launch {
            repository.markRead(projectId, communicationId, currentActorId, currentUserRole)
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}

/**
 * ViewModel for Customer Communication Details (Module 10 Step 02).
 */
class CustomerCommunicationDetailsViewModel(
    private val repository: CustomerCommunicationRepository,
    private val projectId: String = "PRJ-DEFAULT",
    private val currentActorId: String = "CUST-001",
    private val currentUserRole: UserRole = UserRole.CUSTOMER
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerCommunicationDetailsUiState())
    val uiState: StateFlow<CustomerCommunicationDetailsUiState> = _uiState.asStateFlow()

    fun loadDetails(communicationId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val commRes = repository.getCommunication(projectId, communicationId, currentActorId, currentUserRole)
            val histRes = repository.getHistory(projectId, communicationId, currentActorId, currentUserRole)

            if (commRes is DomainResult.Success) {
                val comm = commRes.data
                if (!comm.isRead) {
                    repository.markRead(projectId, communicationId, currentActorId, currentUserRole)
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        communication = comm,
                        history = (histRes as? DomainResult.Success)?.data ?: emptyList()
                    )
                }
            } else if (commRes is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = commRes.message) }
            }
        }
    }

    fun acknowledge(communicationId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = repository.markAcknowledged(projectId, communicationId, currentActorId, currentUserRole)
            if (res is DomainResult.Success) {
                loadDetails(communicationId)
                _uiState.update { it.copy(successMessage = "Communication acknowledged successfully.") }
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
            }
        }
    }
}

/**
 * ViewModel for Customer Communication Admin (Module 10 Step 02).
 */
class CustomerCommunicationAdminViewModel(
    private val repository: CustomerCommunicationRepository,
    private val projectId: String = "PRJ-DEFAULT",
    private val currentActorId: String = "ADMIN-001",
    private val currentUserRole: UserRole = UserRole.ADMIN
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerCommunicationAdminUiState())
    val uiState: StateFlow<CustomerCommunicationAdminUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val summaryRes = repository.getSummary(projectId, null, currentActorId, currentUserRole)
            if (summaryRes is DomainResult.Success) {
                _uiState.update { it.copy(isLoading = false, summary = summaryRes.data) }
            } else if (summaryRes is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = summaryRes.message) }
            }
        }
    }

    fun sendCommunication(
        customerId: String,
        type: CustomerCommunicationType,
        channel: NotificationChannel,
        priority: NotificationPriority,
        title: String,
        message: String,
        scheduledAt: Long? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val createRes = repository.createCommunication(
                projectId = projectId,
                customerId = customerId,
                communicationType = type,
                channel = channel,
                priority = priority,
                title = title,
                message = message,
                scheduledAt = scheduledAt,
                actorId = currentActorId,
                callerRole = currentUserRole
            )
            if (createRes is DomainResult.Success) {
                val comm = createRes.data
                if (scheduledAt == null || scheduledAt <= System.currentTimeMillis()) {
                    repository.queueCommunication(projectId, comm.communicationId, currentActorId, currentUserRole)
                }
                loadData()
                _uiState.update { it.copy(successMessage = "Customer communication ${comm.communicationNo} dispatched.") }
            } else if (createRes is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = createRes.message) }
            }
        }
    }
}

/**
 * ViewModel for Customer Communication History (Module 10 Step 02).
 */
class CustomerCommunicationHistoryViewModel(
    private val repository: CustomerCommunicationRepository,
    private val projectId: String = "PRJ-DEFAULT",
    private val currentActorId: String = "ADMIN-001",
    private val currentUserRole: UserRole = UserRole.ADMIN
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerCommunicationHistoryUiState())
    val uiState: StateFlow<CustomerCommunicationHistoryUiState> = _uiState.asStateFlow()

    fun loadHistory(communicationId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = repository.getHistory(projectId, communicationId, currentActorId, currentUserRole)
            if (res is DomainResult.Success) {
                _uiState.update { it.copy(isLoading = false, history = res.data) }
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
            }
        }
    }
}

/**
 * ViewModel for Customer Engagement Analytics (Module 10 Step 02).
 */
class CustomerEngagementViewModel(
    private val repository: CustomerCommunicationRepository,
    private val projectId: String = "PRJ-DEFAULT",
    private val currentCustomerId: String? = null,
    private val currentActorId: String = "ADMIN-001",
    private val currentUserRole: UserRole = UserRole.ADMIN
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerEngagementUiState())
    val uiState: StateFlow<CustomerEngagementUiState> = _uiState.asStateFlow()

    init {
        loadEngagement()
    }

    fun loadEngagement() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val sumRes = repository.getEngagementSummary(projectId, currentCustomerId, currentActorId, currentUserRole)
            if (sumRes is DomainResult.Success) {
                _uiState.update { it.copy(isLoading = false, summary = sumRes.data) }
            } else if (sumRes is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = sumRes.message) }
            }
        }
    }
}
