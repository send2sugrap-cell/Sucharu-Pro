package com.sucharu.sucharupro.ui.features.internalcommunication

import com.sucharu.sucharupro.domain.model.communication.internal.*

data class InternalCommunicationDashboardUiState(
    val summary: InternalCommunicationSummary? = null,
    val recentCommunications: List<InternalCommunication> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class InternalCommunicationInboxUiState(
    val communications: List<InternalCommunication> = emptyList(),
    val unreadCount: Int = 0,
    val selectedFilter: InternalCommunicationInboxFilter = InternalCommunicationInboxFilter.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

enum class InternalCommunicationInboxFilter(val defaultLabel: String) {
    ALL("All"),
    UNREAD("Unread"),
    DIRECT("Direct"),
    TEAM("Team"),
    DEPARTMENT("Department"),
    ANNOUNCEMENTS("Notices"),
    URGENT("Urgent & Critical")
}

data class InternalCommunicationDetailsUiState(
    val communication: InternalCommunication? = null,
    val thread: InternalCommunicationThread? = null,
    val threadMessages: List<InternalCommunication> = emptyList(),
    val history: List<InternalCommunicationActivityEvent> = emptyList(),
    val acknowledgements: List<InternalCommunicationAcknowledgement> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class InternalCommunicationComposeUiState(
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class InternalCommunicationThreadUiState(
    val thread: InternalCommunicationThread? = null,
    val messages: List<InternalCommunication> = emptyList(),
    val replyText: String = "",
    val isLoading: Boolean = false,
    val isSendingReply: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class InternalCommunicationTeamUiState(
    val teamMessages: List<InternalCommunication> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class InternalCommunicationDepartmentUiState(
    val departmentMessages: List<InternalCommunication> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class InternalCommunicationBroadcastUiState(
    val isBroadcasting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
