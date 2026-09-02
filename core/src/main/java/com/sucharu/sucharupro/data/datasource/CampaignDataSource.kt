package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.communication.campaign.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Source contract for Campaign, Announcement, Broadcast, and Audit persistence (Module 10 Step 07).
 */
interface CampaignDataSource {

    // Campaigns
    fun observeCampaigns(projectId: String): Flow<List<Campaign>>
    suspend fun getCampaigns(projectId: String): List<Campaign>
    suspend fun getCampaignById(projectId: String, campaignId: String): Campaign?
    suspend fun getCampaignByNo(projectId: String, campaignNo: String): Campaign?
    suspend fun getCampaignByIdempotencyKey(projectId: String, idempotencyKey: String): Campaign?
    suspend fun saveCampaign(campaign: Campaign): Campaign
    suspend fun deleteCampaign(projectId: String, campaignId: String): Boolean

    // Recipients
    suspend fun saveRecipients(recipients: List<CampaignRecipient>): List<CampaignRecipient>
    suspend fun getRecipients(projectId: String, campaignId: String): List<CampaignRecipient>
    suspend fun updateRecipient(recipient: CampaignRecipient): CampaignRecipient

    // Announcements
    fun observeAnnouncements(projectId: String): Flow<List<Announcement>>
    suspend fun getAnnouncements(projectId: String): List<Announcement>
    suspend fun getAnnouncementById(projectId: String, announcementId: String): Announcement?
    suspend fun saveAnnouncement(announcement: Announcement): Announcement

    // Broadcasts
    fun observeBroadcasts(projectId: String): Flow<List<BroadcastMessage>>
    suspend fun getBroadcasts(projectId: String): List<BroadcastMessage>
    suspend fun getBroadcastById(projectId: String, broadcastId: String): BroadcastMessage?
    suspend fun saveBroadcast(broadcast: BroadcastMessage): BroadcastMessage

    // Activity Events / Audit Trail
    suspend fun recordActivity(event: CampaignActivityEvent): CampaignActivityEvent
    suspend fun getActivityEvents(projectId: String, campaignId: String): List<CampaignActivityEvent>
    fun observeActivityEvents(projectId: String): Flow<List<CampaignActivityEvent>>

    // Candidate Recipients for audience resolution
    suspend fun getCandidateRecipients(projectId: String): List<com.sucharu.sucharupro.domain.validation.communication.campaign.CampaignAudienceResolver.CandidateRecipient>
}
