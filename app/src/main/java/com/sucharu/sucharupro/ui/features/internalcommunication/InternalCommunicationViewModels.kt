package com.sucharu.sucharupro.ui.features.internalcommunication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.internal.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.communication.internal.InternalCommunicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Internal Communication Dashboard (Module 10 Step 03).
 */
class InternalCommunicationDashboardViewModel(
    private val repository: InternalCommunicationRepository,
    private val projectId: String = "PRJ-DEFAULT",
    private val currentActorId: String = "STAFF-001",
    private val currentUserRole: UserRole = UserRole.STAFF
) : ViewModel() {

    private val _uiState = MutableStateFlow(InternalCommunicationDashboardUiState())
    val uiState: StateFlow<InternalCommunicationDashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val sumRes = repository.getSummary(projectId, currentActorId, currentUserRole)
            val commsRes = repository.getCommunications(projectId, currentActorId, currentActorId, currentUserRole)

            if (sumRes is DomainResult.Success) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        summary = sumRes.data,
                        recentCommunications = (commsRes as? DomainResult.Success)?.data?.take(10) ?: emptyList()
                    )
                }
            } else if (sumRes is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = sumRes.message) }
            }
        }
    }
}

/**
 * ViewModel for Internal Communication Inbox (Module 10 Step 03).
 */
class InternalCommunicationInboxViewModel(
    private val repository: InternalCommunicationRepository,
    private val projectId: String = "PRJ-DEFAULT",
    private val currentActorId: String = "STAFF-001",
    private val currentUserRole: UserRole = UserRole.STAFF
) : ViewModel() {

    private val _uiState = MutableStateFlow(InternalCommunicationInboxUiState())
    val uiState: StateFlow<InternalCommunicationInboxUiState> = _uiState.asStateFlow()

    init {
        observeInbox()
    }

    private fun observeInbox() {
        viewModelScope.launch {
            repository.observeCommunications(projectId, currentActorId, currentUserRole).collect { list ->
                _uiState.update { current ->
                    val unread = list.count { !it.isRead && it.status != InternalCommunicationStatus.ARCHIVED && it.status != InternalCommunicationStatus.CANCELLED }
                    current.copy(communications = list, unreadCount = unread)
                }
            }
        }
    }

    fun setFilter(filter: InternalCommunicationInboxFilter) {
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
}

/**
 * ViewModel for Internal Communication Details (Module 10 Step 03).
 */
class InternalCommunicationDetailsViewModel(
    private val repository: InternalCommunicationRepository,
    private val projectId: String = "PRJ-DEFAULT",
    private val currentActorId: String = "STAFF-001",
    private val currentUserRole: UserRole = UserRole.STAFF
) : ViewModel() {

    private val _uiState = MutableStateFlow(InternalCommunicationDetailsUiState())
    val uiState: StateFlow<InternalCommunicationDetailsUiState> = _uiState.asStateFlow()

    fun loadDetails(communicationId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val commRes = repository.getCommunication(projectId, communicationId, currentActorId, currentUserRole)
            val histRes = repository.getActivityHistory(projectId, communicationId, currentActorId, currentUserRole)

            if (commRes is DomainResult.Success) {
                val comm = commRes.data
                if (!comm.isRead) {
                    repository.markRead(projectId, communicationId, currentActorId, currentUserRole)
                }

                var thread: InternalCommunicationThread? = null
                var threadMsgs: List<InternalCommunication> = emptyList()
                val threadId = comm.threadId
                if (threadId != null) {
                    val tRes = repository.getThread(projectId, threadId, currentActorId, currentUserRole)
                    if (tRes is DomainResult.Success) thread = tRes.data
                    val tmRes = repository.getThreadMessages(projectId, threadId, currentActorId, currentUserRole)
                    if (tmRes is DomainResult.Success) threadMsgs = tmRes.data
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        communication = comm,
                        thread = thread,
                        threadMessages = threadMsgs,
                        history = (histRes as? DomainResult.Success)?.data ?: emptyList()
                    )
                }
            } else if (commRes is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = commRes.message) }
            }
        }
    }

    fun acknowledge(communicationId: String, notes: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = repository.acknowledge(projectId, communicationId, notes, currentActorId, currentUserRole)
            if (res is DomainResult.Success) {
                loadDetails(communicationId)
                _uiState.update { it.copy(successMessage = "Acknowledged successfully.") }
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
            }
        }
    }
}

/**
 * ViewModel for Composing Internal Communication (Module 10 Step 03).
 */
class InternalCommunicationComposeViewModel(
    private val repository: InternalCommunicationRepository,
    private val projectId: String = "PRJ-DEFAULT",
    private val currentActorId: String = "STAFF-001",
    private val currentUserRole: UserRole = UserRole.STAFF
) : ViewModel() {

    private val _uiState = MutableStateFlow(InternalCommunicationComposeUiState())
    val uiState: StateFlow<InternalCommunicationComposeUiState> = _uiState.asStateFlow()

    fun sendCommunication(
        recipientType: InternalCommunicationRecipientType,
        recipientUserIds: Set<String>,
        recipientRole: UserRole? = null,
        teamId: String? = null,
        departmentId: String? = null,
        type: InternalCommunicationType,
        priority: InternalCommunicationPriority,
        subject: String,
        message: String,
        requiresAcknowledgement: Boolean = false,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, errorMessage = null) }
            val createRes = repository.createCommunication(
                projectId = projectId,
                senderUserId = currentActorId,
                senderRole = currentUserRole,
                recipientType = recipientType,
                recipientUserIds = recipientUserIds,
                recipientRole = recipientRole,
                teamId = teamId,
                departmentId = departmentId,
                communicationType = type,
                priority = priority,
                subject = subject,
                message = message,
                requiresAcknowledgement = requiresAcknowledgement,
                actorId = currentActorId,
                callerRole = currentUserRole
            )

            if (createRes is DomainResult.Success) {
                val comm = createRes.data
                repository.queueCommunication(projectId, comm.communicationId, currentActorId, currentUserRole)
                _uiState.update { it.copy(isSending = false, successMessage = "Message dispatched.") }
                onSuccess()
            } else if (createRes is DomainResult.Error) {
                _uiState.update { it.copy(isSending = false, errorMessage = createRes.message) }
            }
        }
    }
}

/**
 * ViewModel for Thread Discussions (Module 10 Step 03).
 */
class InternalCommunicationThreadViewModel(
    private val repository: InternalCommunicationRepository,
    private val projectId: String = "PRJ-DEFAULT",
    private val currentActorId: String = "STAFF-001",
    private val currentUserRole: UserRole = UserRole.STAFF
) : ViewModel() {

    private val _uiState = MutableStateFlow(InternalCommunicationThreadUiState())
    val uiState: StateFlow<InternalCommunicationThreadUiState> = _uiState.asStateFlow()

    fun loadThread(threadId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val tRes = repository.getThread(projectId, threadId, currentActorId, currentUserRole)
            val msgsRes = repository.getThreadMessages(projectId, threadId, currentActorId, currentUserRole)

            if (tRes is DomainResult.Success && msgsRes is DomainResult.Success) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        thread = tRes.data,
                        messages = msgsRes.data
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load thread.") }
            }
        }
    }

    fun setReplyText(text: String) {
        _uiState.update { it.copy(replyText = text) }
    }

    fun sendReply(threadId: String) {
        val reply = _uiState.value.replyText
        if (reply.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingReply = true) }
            val res = repository.replyToThread(
                projectId = projectId,
                threadId = threadId,
                replyMessage = reply,
                senderUserId = currentActorId,
                senderRole = currentUserRole,
                actorId = currentActorId,
                callerRole = currentUserRole
            )
            if (res is DomainResult.Success) {
                _uiState.update { it.copy(replyText = "", isSendingReply = false) }
                loadThread(threadId)
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(isSendingReply = false, errorMessage = res.message) }
            }
        }
    }
}

/**
 * ViewModel for Team Communications (Module 10 Step 03).
 */
class InternalCommunicationTeamViewModel(
    private val repository: InternalCommunicationRepository,
    private val projectId: String = "PRJ-DEFAULT",
    private val currentActorId: String = "STAFF-001",
    private val currentUserRole: UserRole = UserRole.STAFF
) : ViewModel() {

    private val _uiState = MutableStateFlow(InternalCommunicationTeamUiState())
    val uiState: StateFlow<InternalCommunicationTeamUiState> = _uiState.asStateFlow()

    fun loadTeamMessages(teamId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = repository.getCommunications(projectId, currentActorId, currentActorId, currentUserRole)
            if (res is DomainResult.Success) {
                val teamMsgs = res.data.filter { it.teamId == teamId || it.communicationType == InternalCommunicationType.TEAM_MESSAGE }
                _uiState.update { it.copy(isLoading = false, teamMessages = teamMsgs) }
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
            }
        }
    }
}

/**
 * ViewModel for Department Communications (Module 10 Step 03).
 */
class InternalCommunicationDepartmentViewModel(
    private val repository: InternalCommunicationRepository,
    private val projectId: String = "PRJ-DEFAULT",
    private val currentActorId: String = "STAFF-001",
    private val currentUserRole: UserRole = UserRole.STAFF
) : ViewModel() {

    private val _uiState = MutableStateFlow(InternalCommunicationDepartmentUiState())
    val uiState: StateFlow<InternalCommunicationDepartmentUiState> = _uiState.asStateFlow()

    fun loadDepartmentMessages(departmentId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = repository.getCommunications(projectId, currentActorId, currentActorId, currentUserRole)
            if (res is DomainResult.Success) {
                val deptMsgs = res.data.filter { it.departmentId == departmentId || it.communicationType == InternalCommunicationType.DEPARTMENT_MESSAGE }
                _uiState.update { it.copy(isLoading = false, departmentMessages = deptMsgs) }
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
            }
        }
    }
}

/**
 * ViewModel for Administrative Broadcast (Module 10 Step 03).
 */
class InternalCommunicationBroadcastViewModel(
    private val repository: InternalCommunicationRepository,
    private val projectId: String = "PRJ-DEFAULT",
    private val currentActorId: String = "ADMIN-001",
    private val currentUserRole: UserRole = UserRole.ADMIN
) : ViewModel() {

    private val _uiState = MutableStateFlow(InternalCommunicationBroadcastUiState())
    val uiState: StateFlow<InternalCommunicationBroadcastUiState> = _uiState.asStateFlow()

    fun broadcast(
        recipientType: InternalCommunicationRecipientType,
        recipientRole: UserRole? = null,
        teamId: String? = null,
        departmentId: String? = null,
        priority: InternalCommunicationPriority = InternalCommunicationPriority.HIGH,
        subject: String,
        message: String,
        requiresAcknowledgement: Boolean = false,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBroadcasting = true, errorMessage = null) }
            val res = repository.broadcastCommunication(
                projectId = projectId,
                recipientType = recipientType,
                recipientRole = recipientRole,
                teamId = teamId,
                departmentId = departmentId,
                priority = priority,
                subject = subject,
                message = message,
                requiresAcknowledgement = requiresAcknowledgement,
                actorId = currentActorId,
                callerRole = currentUserRole
            )
            if (res is DomainResult.Success) {
                _uiState.update { it.copy(isBroadcasting = false, successMessage = "Broadcast transmitted.") }
                onSuccess()
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(isBroadcasting = false, errorMessage = res.message) }
            }
        }
    }
}
