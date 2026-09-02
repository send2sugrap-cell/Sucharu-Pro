package com.sucharu.sucharupro.ui.features.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.notification.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.notification.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Notification Center UI (Module 10 Step 01).
 */
class NotificationCenterViewModel(
    private val repository: NotificationRepository,
    private val projectId: String = "PRJ-DEFAULT",
    private val currentUserId: String = "USER-001",
    private val currentUserRole: UserRole = UserRole.ADMIN
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationCenterUiState())
    val uiState: StateFlow<NotificationCenterUiState> = _uiState.asStateFlow()

    init {
        observeNotifications()
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            repository.observeUserNotifications(projectId, currentUserId, currentUserRole).collect { list ->
                _uiState.update { current ->
                    val unread = list.count { !it.isRead && it.status != NotificationStatus.CANCELLED }
                    current.copy(notifications = list, unreadCount = unread)
                }
            }
        }
    }

    fun setFilter(filter: NotificationCenterFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            repository.markRead(projectId, notificationId, currentUserId, currentUserRole)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = repository.markAllAsRead(projectId, currentUserId, currentUserId, currentUserRole)
            if (res is DomainResult.Success) {
                _uiState.update { it.copy(isLoading = false, successMessage = "Marked ${res.data} notifications as read.") }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}

/**
 * ViewModel for Notification Details UI (Module 10 Step 01).
 */
class NotificationDetailsViewModel(
    private val repository: NotificationRepository,
    private val projectId: String = "PRJ-DEFAULT",
    private val currentUserId: String = "USER-001",
    private val currentUserRole: UserRole = UserRole.ADMIN
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationDetailsUiState())
    val uiState: StateFlow<NotificationDetailsUiState> = _uiState.asStateFlow()

    fun loadNotification(notificationId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val notifRes = repository.getNotification(projectId, notificationId, currentUserId, currentUserRole)
            val attemptsRes = repository.getDeliveryAttempts(projectId, notificationId, currentUserId, currentUserRole)
            val eventsRes = repository.getActivityEvents(projectId, notificationId, currentUserId, currentUserRole)

            if (notifRes is DomainResult.Success) {
                val notif = notifRes.data
                // Auto mark as read on view
                if (!notif.isRead) {
                    repository.markRead(projectId, notificationId, currentUserId, currentUserRole)
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        notification = notif,
                        attempts = (attemptsRes as? DomainResult.Success)?.data ?: emptyList(),
                        activityEvents = (eventsRes as? DomainResult.Success)?.data ?: emptyList()
                    )
                }
            } else if (notifRes is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = notifRes.message) }
            }
        }
    }

    fun retryDelivery(notificationId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = repository.retryNotification(projectId, notificationId, currentUserId, currentUserRole)
            if (res is DomainResult.Success) {
                loadNotification(notificationId)
                _uiState.update { it.copy(successMessage = "Delivery retry initiated successfully.") }
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
            }
        }
    }
}

/**
 * ViewModel for Notification Preferences UI (Module 10 Step 01).
 */
class NotificationPreferenceViewModel(
    private val repository: NotificationRepository,
    private val projectId: String = "PRJ-DEFAULT",
    private val currentUserId: String = "USER-001",
    private val currentUserRole: UserRole = UserRole.ADMIN
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationPreferenceUiState())
    val uiState: StateFlow<NotificationPreferenceUiState> = _uiState.asStateFlow()

    init {
        loadPreferences()
    }

    fun loadPreferences() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = repository.getPreferences(projectId, currentUserId, currentUserId, currentUserRole)
            if (res is DomainResult.Success) {
                _uiState.update { it.copy(isLoading = false, preferences = res.data) }
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
            }
        }
    }

    fun updatePreference(preferenceId: String, enabled: Boolean) {
        viewModelScope.launch {
            val res = repository.updatePreference(projectId, preferenceId, enabled, currentUserId, currentUserRole)
            if (res is DomainResult.Success) {
                loadPreferences()
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }
}

/**
 * ViewModel for Notification Templates UI (Module 10 Step 01).
 */
class NotificationTemplateViewModel(
    private val repository: NotificationRepository,
    private val projectId: String = "PRJ-DEFAULT",
    private val currentUserId: String = "USER-001",
    private val currentUserRole: UserRole = UserRole.ADMIN
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationTemplateUiState())
    val uiState: StateFlow<NotificationTemplateUiState> = _uiState.asStateFlow()

    init {
        loadTemplates()
    }

    fun loadTemplates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = repository.getTemplates(projectId, currentUserId, currentUserRole)
            if (res is DomainResult.Success) {
                _uiState.update { it.copy(isLoading = false, templates = res.data) }
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
            }
        }
    }

    fun createTemplateVersion(templateCode: String, title: String, message: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = repository.createTemplateVersion(projectId, templateCode, title, message, currentUserId, currentUserRole)
            if (res is DomainResult.Success) {
                loadTemplates()
                _uiState.update { it.copy(successMessage = "Template version ${res.data.version} created.") }
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
            }
        }
    }
}

/**
 * ViewModel for Notification Admin Dashboard UI (Module 10 Step 01).
 */
class NotificationDashboardViewModel(
    private val repository: NotificationRepository,
    private val projectId: String = "PRJ-DEFAULT",
    private val currentUserId: String = "USER-001",
    private val currentUserRole: UserRole = UserRole.ADMIN
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationDashboardUiState())
    val uiState: StateFlow<NotificationDashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
        observeEvents()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = repository.getSummary(projectId, currentUserId, currentUserRole)
            if (res is DomainResult.Success) {
                _uiState.update { it.copy(isLoading = false, summary = res.data) }
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
            }
        }
    }

    private fun observeEvents() {
        viewModelScope.launch {
            repository.observeActivityEvents(projectId, currentUserRole).collect { events ->
                _uiState.update { it.copy(recentActivityEvents = events.take(15)) }
            }
        }
    }
}
