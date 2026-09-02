package com.sucharu.sucharupro.ui.features.communication.campaign

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.campaign.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.communication.campaign.CampaignRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CampaignDashboardViewModel(
    private val repository: CampaignRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CampaignDashboardUiState())
    val uiState: StateFlow<CampaignDashboardUiState> = _uiState.asStateFlow()

    fun load(projectId: String = "default-project", callerRole: UserRole = UserRole.ADMIN) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val summaryRes = repository.getProjectSummary(projectId, "actor-01", callerRole)
            val campaignsRes = repository.getCampaigns(projectId, actorId = "actor-01", callerRole = callerRole)

            if (summaryRes is DomainResult.Success && campaignsRes is DomainResult.Success) {
                _uiState.value = CampaignDashboardUiState(
                    isLoading = false,
                    summary = summaryRes.data,
                    recentCampaigns = campaignsRes.data.take(5)
                )
            } else {
                _uiState.value = CampaignDashboardUiState(isLoading = false, error = "Failed to load dashboard.")
            }
        }
    }
}

class CampaignListViewModel(
    private val repository: CampaignRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CampaignListUiState())
    val uiState: StateFlow<CampaignListUiState> = _uiState.asStateFlow()

    fun load(projectId: String = "default-project", callerRole: UserRole = UserRole.ADMIN) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.getCampaigns(
                projectId = projectId,
                status = _uiState.value.filterStatus,
                type = _uiState.value.filterType,
                actorId = "actor-01",
                callerRole = callerRole
            )
            if (result is DomainResult.Success) {
                _uiState.value = _uiState.value.copy(isLoading = false, campaigns = result.data)
            } else if (result is DomainResult.Error) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun setFilterStatus(status: CampaignStatus?) {
        _uiState.value = _uiState.value.copy(filterStatus = status)
        load()
    }

    fun setFilterType(type: CampaignType?) {
        _uiState.value = _uiState.value.copy(filterType = type)
        load()
    }
}

class CampaignDetailsViewModel(
    private val repository: CampaignRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CampaignDetailsUiState())
    val uiState: StateFlow<CampaignDetailsUiState> = _uiState.asStateFlow()

    fun load(projectId: String = "default-project", campaignId: String, callerRole: UserRole = UserRole.ADMIN) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val campRes = repository.getCampaignById(projectId, campaignId, "actor-01", callerRole)
            if (campRes is DomainResult.Success) {
                val recipientsRes = repository.getRecipients(projectId, campaignId, "actor-01", callerRole)
                val delSummaryRes = repository.getDeliverySummary(projectId, campaignId, "actor-01", callerRole)
                val engSummaryRes = repository.getEngagementSummary(projectId, campaignId, "actor-01", callerRole)
                val auditRes = repository.getActivityEvents(projectId, campaignId, "actor-01", callerRole)

                _uiState.value = CampaignDetailsUiState(
                    isLoading = false,
                    campaign = campRes.data,
                    recipients = (recipientsRes as? DomainResult.Success)?.data ?: emptyList(),
                    deliverySummary = (delSummaryRes as? DomainResult.Success)?.data ?: CampaignDeliverySummary(),
                    engagementSummary = (engSummaryRes as? DomainResult.Success)?.data ?: CampaignEngagementSummary(),
                    activityEvents = (auditRes as? DomainResult.Success)?.data ?: emptyList()
                )
            } else if (campRes is DomainResult.Error) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = campRes.message)
            }
        }
    }

    fun submitForApproval(projectId: String, campaignId: String, actorId: String = "user-staff-01", callerRole: UserRole = UserRole.STAFF) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionInProgress = true)
            val res = repository.submitForApproval(projectId, campaignId, actorId, callerRole)
            if (res is DomainResult.Success) {
                load(projectId, campaignId, callerRole)
            } else if (res is DomainResult.Error) {
                _uiState.value = _uiState.value.copy(isActionInProgress = false, error = res.message)
            }
        }
    }

    fun approve(projectId: String, campaignId: String, actorId: String = "user-manager-01", callerRole: UserRole = UserRole.MANAGER) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionInProgress = true)
            val res = repository.approveCampaign(projectId, campaignId, actorId, callerRole)
            if (res is DomainResult.Success) {
                load(projectId, campaignId, callerRole)
            } else if (res is DomainResult.Error) {
                _uiState.value = _uiState.value.copy(isActionInProgress = false, error = res.message)
            }
        }
    }

    fun publish(projectId: String, campaignId: String, actorId: String = "user-admin-01", callerRole: UserRole = UserRole.ADMIN) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionInProgress = true)
            val res = repository.publishCampaign(projectId, campaignId, actorId, callerRole)
            if (res is DomainResult.Success) {
                load(projectId, campaignId, callerRole)
            } else if (res is DomainResult.Error) {
                _uiState.value = _uiState.value.copy(isActionInProgress = false, error = res.message)
            }
        }
    }

    fun cancel(projectId: String, campaignId: String, actorId: String = "user-admin-01", callerRole: UserRole = UserRole.ADMIN) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionInProgress = true)
            val res = repository.cancelCampaign(projectId, campaignId, actorId, callerRole)
            if (res is DomainResult.Success) {
                load(projectId, campaignId, callerRole)
            } else if (res is DomainResult.Error) {
                _uiState.value = _uiState.value.copy(isActionInProgress = false, error = res.message)
            }
        }
    }
}

class CampaignFormViewModel(
    private val repository: CampaignRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CampaignFormUiState())
    val uiState: StateFlow<CampaignFormUiState> = _uiState.asStateFlow()

    fun updateTitle(value: String) { _uiState.value = _uiState.value.copy(title = value) }
    fun updateDescription(value: String) { _uiState.value = _uiState.value.copy(description = value) }
    fun updateType(value: CampaignType) { _uiState.value = _uiState.value.copy(campaignType = value) }
    fun updatePriority(value: CampaignPriority) { _uiState.value = _uiState.value.copy(priority = value) }
    fun updateAudienceType(value: CampaignAudienceType) { _uiState.value = _uiState.value.copy(audienceType = value) }
    fun updateContent(value: String) { _uiState.value = _uiState.value.copy(content = value) }

    fun submit(projectId: String = "default-project", actorId: String = "user-admin-01", callerRole: UserRole = UserRole.ADMIN) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
            val res = repository.createCampaign(
                projectId = projectId,
                title = _uiState.value.title,
                description = _uiState.value.description,
                campaignType = _uiState.value.campaignType,
                priority = _uiState.value.priority,
                audienceType = _uiState.value.audienceType,
                targetCriteria = _uiState.value.targetCriteria,
                content = _uiState.value.content,
                actorId = actorId,
                callerRole = callerRole
            )
            if (res is DomainResult.Success) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, isSuccess = true)
            } else if (res is DomainResult.Error) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, error = res.message)
            }
        }
    }
}

class AnnouncementViewModel(
    private val repository: CampaignRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AnnouncementUiState())
    val uiState: StateFlow<AnnouncementUiState> = _uiState.asStateFlow()

    fun load(projectId: String = "default-project", callerRole: UserRole = UserRole.ADMIN) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val res = repository.getAnnouncements(projectId, "actor-01", callerRole)
            if (res is DomainResult.Success) {
                _uiState.value = AnnouncementUiState(isLoading = false, announcements = res.data)
            } else if (res is DomainResult.Error) {
                _uiState.value = AnnouncementUiState(isLoading = false, error = res.message)
            }
        }
    }
}

class BroadcastViewModel(
    private val repository: CampaignRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(BroadcastUiState())
    val uiState: StateFlow<BroadcastUiState> = _uiState.asStateFlow()

    fun load(projectId: String = "default-project", callerRole: UserRole = UserRole.ADMIN) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val res = repository.getBroadcasts(projectId, "actor-01", callerRole)
            if (res is DomainResult.Success) {
                _uiState.value = _uiState.value.copy(isLoading = false, broadcasts = res.data)
            } else if (res is DomainResult.Error) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = res.message)
            }
        }
    }

    fun sendBroadcast(
        projectId: String = "default-project",
        title: String,
        message: String,
        priority: CampaignPriority = CampaignPriority.HIGH,
        audienceType: CampaignAudienceType = CampaignAudienceType.ROLE,
        actorId: String = "user-admin-01",
        callerRole: UserRole = UserRole.ADMIN
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true, error = null)
            val res = repository.sendBroadcast(
                projectId = projectId,
                title = title,
                message = message,
                priority = priority,
                audienceType = audienceType,
                actorId = actorId,
                callerRole = callerRole
            )
            if (res is DomainResult.Success) {
                _uiState.value = _uiState.value.copy(isSending = false, sendSuccess = true)
                load(projectId, callerRole)
            } else if (res is DomainResult.Error) {
                _uiState.value = _uiState.value.copy(isSending = false, error = res.message)
            }
        }
    }
}

class CampaignAnalyticsViewModel(
    private val repository: CampaignRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CampaignAnalyticsUiState())
    val uiState: StateFlow<CampaignAnalyticsUiState> = _uiState.asStateFlow()

    fun load(projectId: String = "default-project", callerRole: UserRole = UserRole.ADMIN) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val summaryRes = repository.getProjectSummary(projectId, "actor-01", callerRole)
            if (summaryRes is DomainResult.Success) {
                _uiState.value = CampaignAnalyticsUiState(
                    isLoading = false,
                    summary = summaryRes.data,
                    deliverySummary = CampaignDeliverySummary(
                        totalRecipients = summaryRes.data.totalRecipients,
                        sent = summaryRes.data.sent,
                        delivered = summaryRes.data.delivered,
                        read = summaryRes.data.read,
                        acknowledged = summaryRes.data.acknowledged,
                        failed = summaryRes.data.failed
                    ),
                    engagementSummary = CampaignEngagementSummary(
                        deliveryRate = summaryRes.data.deliveryRate,
                        readRate = summaryRes.data.readRate,
                        acknowledgementRate = summaryRes.data.acknowledgementRate,
                        engagementRate = summaryRes.data.engagementRate
                    )
                )
            } else if (summaryRes is DomainResult.Error) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = summaryRes.message)
            }
        }
    }
}
