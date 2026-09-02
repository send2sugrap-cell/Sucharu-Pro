package com.sucharu.sucharupro.ui.features.customercommunication

import com.sucharu.sucharupro.domain.model.communication.customer.*

data class CustomerCommunicationCenterUiState(
    val communications: List<CustomerCommunication> = emptyList(),
    val unreadCount: Int = 0,
    val selectedFilter: CustomerCommunicationFilter = CustomerCommunicationFilter.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

enum class CustomerCommunicationFilter(val defaultLabel: String) {
    ALL("All"),
    UNREAD("Unread"),
    ORDERS("Orders"),
    PRODUCTION("Production"),
    DELIVERY("Delivery"),
    FINANCE("Finance"),
    ANNOUNCEMENTS("Notices"),
    OFFERS("Offers")
}

data class CustomerCommunicationDetailsUiState(
    val communication: CustomerCommunication? = null,
    val history: List<CustomerCommunicationHistory> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class CustomerCommunicationAdminUiState(
    val communications: List<CustomerCommunication> = emptyList(),
    val summary: CustomerCommunicationSummary? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class CustomerCommunicationHistoryUiState(
    val history: List<CustomerCommunicationHistory> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class CustomerEngagementUiState(
    val summary: CustomerEngagementSummary? = null,
    val recentEngagementEvents: List<CustomerEngagementEvent> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
