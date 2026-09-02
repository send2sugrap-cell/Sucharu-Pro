package com.sucharu.sucharupro.domain.repository.communication.campaign

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.campaign.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Campaign, Announcement, and Broadcast Communication (Module 10 Step 07).
 */
interface CampaignRepository {

    // ─── Campaigns ───
    fun observeCampaigns(projectId: String, callerRole: UserRole): Flow<List<Campaign>>

    suspend fun getCampaigns(
        projectId: String,
        status: CampaignStatus? = null,
        type: CampaignType? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<Campaign>>

    suspend fun getCampaignById(
        projectId: String,
        campaignId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Campaign>

    suspend fun createCampaign(
        projectId: String,
        title: String,
        description: String = "",
        campaignType: CampaignType,
        priority: CampaignPriority = CampaignPriority.NORMAL,
        audienceType: CampaignAudienceType,
        targetCriteria: CampaignAudienceCriteria = CampaignAudienceCriteria(),
        content: String,
        scheduledAt: Long? = null,
        startsAt: Long? = null,
        endsAt: Long? = null,
        idempotencyKey: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Campaign>

    suspend fun updateDraft(
        campaign: Campaign,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Campaign>

    suspend fun submitForApproval(
        projectId: String,
        campaignId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Campaign>

    suspend fun approveCampaign(
        projectId: String,
        campaignId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Campaign>

    suspend fun rejectCampaign(
        projectId: String,
        campaignId: String,
        rejectionReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Campaign>

    suspend fun scheduleCampaign(
        projectId: String,
        campaignId: String,
        scheduledAt: Long,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Campaign>

    suspend fun publishCampaign(
        projectId: String,
        campaignId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Campaign>

    suspend fun completeCampaign(
        projectId: String,
        campaignId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Campaign>

    suspend fun cancelCampaign(
        projectId: String,
        campaignId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Campaign>

    // ─── Recipients & Engagement ───
    suspend fun getRecipients(
        projectId: String,
        campaignId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<CampaignRecipient>>

    suspend fun recordRecipientRead(
        projectId: String,
        campaignId: String,
        recipientId: String,
        userId: String
    ): DomainResult<CampaignRecipient>

    suspend fun recordRecipientAcknowledged(
        projectId: String,
        campaignId: String,
        recipientId: String,
        userId: String
    ): DomainResult<CampaignRecipient>

    suspend fun getDeliverySummary(
        projectId: String,
        campaignId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CampaignDeliverySummary>

    suspend fun getEngagementSummary(
        projectId: String,
        campaignId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CampaignEngagementSummary>

    suspend fun getProjectSummary(
        projectId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CampaignSummary>

    // ─── Announcements ───
    fun observeAnnouncements(projectId: String, callerRole: UserRole): Flow<List<Announcement>>

    suspend fun getAnnouncements(
        projectId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<Announcement>>

    suspend fun createAnnouncement(
        projectId: String,
        title: String,
        content: String,
        priority: CampaignPriority = CampaignPriority.NORMAL,
        audienceType: CampaignAudienceType = CampaignAudienceType.ALL_PROJECT_USERS,
        targetCriteria: CampaignAudienceCriteria = CampaignAudienceCriteria(),
        expiresAt: Long? = null,
        acknowledgementRequired: Boolean = false,
        idempotencyKey: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Announcement>

    suspend fun publishAnnouncement(
        projectId: String,
        announcementId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Announcement>

    // ─── Broadcasts ───
    fun observeBroadcasts(projectId: String, callerRole: UserRole): Flow<List<BroadcastMessage>>

    suspend fun getBroadcasts(
        projectId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<BroadcastMessage>>

    suspend fun sendBroadcast(
        projectId: String,
        title: String,
        message: String,
        priority: CampaignPriority = CampaignPriority.HIGH,
        audienceType: CampaignAudienceType = CampaignAudienceType.ROLE,
        targetCriteria: CampaignAudienceCriteria = CampaignAudienceCriteria(),
        idempotencyKey: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<BroadcastMessage>

    // ─── Audit Trail ───
    suspend fun getActivityEvents(
        projectId: String,
        campaignId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<CampaignActivityEvent>>

    fun observeActivityEvents(projectId: String, callerRole: UserRole): Flow<List<CampaignActivityEvent>>
}
