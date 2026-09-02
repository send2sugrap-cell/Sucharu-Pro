package com.sucharu.sucharupro.ui.features.communication.campaign

import com.sucharu.sucharupro.domain.model.communication.campaign.*

data class CampaignDashboardUiState(
    val isLoading: Boolean = false,
    val summary: CampaignSummary = CampaignSummary(projectId = "default-project"),
    val recentCampaigns: List<Campaign> = emptyList(),
    val error: String? = null
)

data class CampaignListUiState(
    val isLoading: Boolean = false,
    val campaigns: List<Campaign> = emptyList(),
    val searchQuery: String = "",
    val filterStatus: CampaignStatus? = null,
    val filterType: CampaignType? = null,
    val filterPriority: CampaignPriority? = null,
    val error: String? = null
)

data class CampaignDetailsUiState(
    val isLoading: Boolean = false,
    val campaign: Campaign? = null,
    val recipients: List<CampaignRecipient> = emptyList(),
    val deliverySummary: CampaignDeliverySummary = CampaignDeliverySummary(),
    val engagementSummary: CampaignEngagementSummary = CampaignEngagementSummary(),
    val activityEvents: List<CampaignActivityEvent> = emptyList(),
    val isActionInProgress: Boolean = false,
    val error: String? = null,
    val actionSuccessMessage: String? = null
)

data class CampaignFormUiState(
    val isSubmitting: Boolean = false,
    val title: String = "",
    val description: String = "",
    val campaignType: CampaignType = CampaignType.GENERAL,
    val priority: CampaignPriority = CampaignPriority.NORMAL,
    val audienceType: CampaignAudienceType = CampaignAudienceType.ALL_PROJECT_USERS,
    val targetCriteria: CampaignAudienceCriteria = CampaignAudienceCriteria(),
    val content: String = "",
    val scheduledAt: Long? = null,
    val isSuccess: Boolean = false,
    val error: String? = null
)

data class AnnouncementUiState(
    val isLoading: Boolean = false,
    val announcements: List<Announcement> = emptyList(),
    val error: String? = null
)

data class BroadcastUiState(
    val isLoading: Boolean = false,
    val broadcasts: List<BroadcastMessage> = emptyList(),
    val isSending: Boolean = false,
    val sendSuccess: Boolean = false,
    val error: String? = null
)

data class CampaignAnalyticsUiState(
    val isLoading: Boolean = false,
    val summary: CampaignSummary = CampaignSummary(projectId = "default-project"),
    val deliverySummary: CampaignDeliverySummary = CampaignDeliverySummary(),
    val engagementSummary: CampaignEngagementSummary = CampaignEngagementSummary(),
    val error: String? = null
)
