package com.sucharu.sucharupro.ui.features.notification

import com.sucharu.sucharupro.domain.model.notification.*

data class NotificationCenterUiState(
    val notifications: List<Notification> = emptyList(),
    val unreadCount: Int = 0,
    val selectedFilter: NotificationCenterFilter = NotificationCenterFilter.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

enum class NotificationCenterFilter(val defaultLabel: String) {
    ALL("All"),
    UNREAD("Unread"),
    HIGH_PRIORITY("High Priority"),
    ORDERS("Orders"),
    PRODUCTION("Production"),
    DELIVERY("Delivery"),
    FINANCE("Finance"),
    INVENTORY("Inventory"),
    SYSTEM("System")
}

data class NotificationDetailsUiState(
    val notification: Notification? = null,
    val attempts: List<NotificationDeliveryAttempt> = emptyList(),
    val activityEvents: List<NotificationActivityEvent> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class NotificationPreferenceUiState(
    val preferences: List<NotificationPreference> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class NotificationTemplateUiState(
    val templates: List<NotificationTemplate> = emptyList(),
    val selectedTemplate: NotificationTemplate? = null,
    val versionHistory: List<NotificationTemplate> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class NotificationDashboardUiState(
    val summary: NotificationSummary? = null,
    val recentActivityEvents: List<NotificationActivityEvent> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
