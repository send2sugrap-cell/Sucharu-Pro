package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.communication.campaign.*
import com.sucharu.sucharupro.domain.model.notification.NotificationChannel
import com.sucharu.sucharupro.domain.validation.communication.campaign.CampaignAudienceResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger

/**
 * Production-grade in-memory thread-safe fake implementation of [CampaignDataSource] (Module 10 Step 07).
 */
class FakeCampaignDataSource : CampaignDataSource {

    private val mutex = Mutex()

    private val campaignsState = MutableStateFlow<Map<String, Campaign>>(emptyMap())
    private val recipientsState = MutableStateFlow<Map<String, CampaignRecipient>>(emptyMap())
    private val announcementsState = MutableStateFlow<Map<String, Announcement>>(emptyMap())
    private val broadcastsState = MutableStateFlow<Map<String, BroadcastMessage>>(emptyMap())
    private val activityEventsState = MutableStateFlow<List<CampaignActivityEvent>>(emptyList())

    private val campaignSeq = AtomicInteger(100)
    private val announcementSeq = AtomicInteger(100)
    private val broadcastSeq = AtomicInteger(100)
    private val eventSeq = AtomicInteger(100)

    init {
        seedInitialData()
    }

    private fun seedInitialData() {
        val now = System.currentTimeMillis()

        val seedCampaign1 = Campaign(
            campaignId = "cmp-001",
            campaignNo = "CMP-2026-00001",
            projectId = "default-project",
            title = "Boishakhi Printing Discount 2026",
            description = "Special 15% discount on bulk book printing orders during Pohela Boishakh festival.",
            campaignType = CampaignType.OFFER,
            status = CampaignStatus.PUBLISHED,
            priority = CampaignPriority.HIGH,
            audienceType = CampaignAudienceType.CUSTOMER_SEGMENT,
            targetCriteria = CampaignAudienceCriteria(),
            communicationChannel = NotificationChannel.IN_APP,
            content = "Celebrate Pohela Boishakh with a 15% discount on all book and brochure printing orders placed this month!",
            createdBy = "user-admin-01",
            approvedBy = "user-admin-01",
            publishedBy = "user-admin-01",
            createdAt = now - 86400000L * 3,
            updatedAt = now - 86400000L * 3,
            publishedAt = now - 86400000L * 2
        )

        val seedCampaign2 = Campaign(
            campaignId = "cmp-002",
            campaignNo = "CMP-2026-00002",
            projectId = "default-project",
            title = "Paper Supply Quality Policy Update",
            description = "Notification to raw material vendors regarding new GSM and moisture tolerance rules.",
            campaignType = CampaignType.SERVICE_UPDATE,
            status = CampaignStatus.APPROVED,
            priority = CampaignPriority.NORMAL,
            audienceType = CampaignAudienceType.VENDOR_SEGMENT,
            targetCriteria = CampaignAudienceCriteria(),
            communicationChannel = NotificationChannel.IN_APP,
            content = "All paper suppliers are requested to comply with the updated ISO moisture and GSM compliance requirements effective next quarter.",
            createdBy = "user-manager-01",
            approvedBy = "user-admin-01",
            createdAt = now - 86400000L,
            updatedAt = now - 86400000L,
            approvedAt = now - 3600000L
        )

        campaignsState.value = mapOf(
            seedCampaign1.campaignId to seedCampaign1,
            seedCampaign2.campaignId to seedCampaign2
        )

        val seedAnnouncement = Announcement(
            announcementId = "ann-001",
            announcementNo = "ANN-2026-00001",
            projectId = "default-project",
            title = "Annual Press Maintenance Schedule",
            content = "Printing Press Unit 02 and 03 will undergo routine calibration this coming Friday from 8:00 PM to Saturday 6:00 AM.",
            priority = CampaignPriority.HIGH,
            audienceType = CampaignAudienceType.ALL_PROJECT_USERS,
            status = CampaignStatus.PUBLISHED,
            createdBy = "user-admin-01",
            publishedBy = "user-admin-01",
            createdAt = now - 86400000L,
            updatedAt = now - 86400000L,
            publishedAt = now - 86400000L
        )
        announcementsState.value = mapOf(seedAnnouncement.announcementId to seedAnnouncement)

        val seedBroadcast = BroadcastMessage(
            broadcastId = "brd-001",
            broadcastNo = "BRD-2026-00001",
            projectId = "default-project",
            title = "Urgent: QC Shift Handoff Reminder",
            message = "All incoming shift inspectors must verify paper coating thickness before releasing Job #504.",
            priority = CampaignPriority.URGENT,
            audienceType = CampaignAudienceType.ROLE,
            targetCriteria = CampaignAudienceCriteria(),
            channels = setOf(NotificationChannel.IN_APP),
            status = CampaignStatus.PUBLISHED,
            sentAt = now - 3600000L,
            createdBy = "user-manager-01",
            publishedBy = "user-manager-01"
        )
        broadcastsState.value = mapOf(seedBroadcast.broadcastId to seedBroadcast)
    }

    override fun observeCampaigns(projectId: String): Flow<List<Campaign>> {
        return campaignsState.map { map ->
            map.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
        }
    }

    override suspend fun getCampaigns(projectId: String): List<Campaign> = mutex.withLock {
        campaignsState.value.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
    }

    override suspend fun getCampaignById(projectId: String, campaignId: String): Campaign? = mutex.withLock {
        val c = campaignsState.value[campaignId]
        if (c?.projectId == projectId) c else null
    }

    override suspend fun getCampaignByNo(projectId: String, campaignNo: String): Campaign? = mutex.withLock {
        campaignsState.value.values.firstOrNull { it.projectId == projectId && it.campaignNo == campaignNo }
    }

    override suspend fun getCampaignByIdempotencyKey(projectId: String, idempotencyKey: String): Campaign? = mutex.withLock {
        campaignsState.value.values.firstOrNull { it.projectId == projectId && it.idempotencyKey == idempotencyKey }
    }

    override suspend fun saveCampaign(campaign: Campaign): Campaign = mutex.withLock {
        val current = campaignsState.value.toMutableMap()
        current[campaign.campaignId] = campaign
        campaignsState.value = current
        campaign
    }

    override suspend fun deleteCampaign(projectId: String, campaignId: String): Boolean = mutex.withLock {
        val current = campaignsState.value.toMutableMap()
        val existing = current[campaignId]
        if (existing != null && existing.projectId == projectId) {
            current.remove(campaignId)
            campaignsState.value = current
            true
        } else {
            false
        }
    }

    override suspend fun saveRecipients(recipients: List<CampaignRecipient>): List<CampaignRecipient> = mutex.withLock {
        val current = recipientsState.value.toMutableMap()
        for (r in recipients) {
            current[r.recipientId] = r
        }
        recipientsState.value = current
        recipients
    }

    override suspend fun getRecipients(projectId: String, campaignId: String): List<CampaignRecipient> = mutex.withLock {
        recipientsState.value.values.filter { it.projectId == projectId && it.campaignId == campaignId }
    }

    override suspend fun updateRecipient(recipient: CampaignRecipient): CampaignRecipient = mutex.withLock {
        val current = recipientsState.value.toMutableMap()
        current[recipient.recipientId] = recipient
        recipientsState.value = current
        recipient
    }

    override fun observeAnnouncements(projectId: String): Flow<List<Announcement>> {
        return announcementsState.map { map ->
            map.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
        }
    }

    override suspend fun getAnnouncements(projectId: String): List<Announcement> = mutex.withLock {
        announcementsState.value.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
    }

    override suspend fun getAnnouncementById(projectId: String, announcementId: String): Announcement? = mutex.withLock {
        val a = announcementsState.value[announcementId]
        if (a?.projectId == projectId) a else null
    }

    override suspend fun saveAnnouncement(announcement: Announcement): Announcement = mutex.withLock {
        val current = announcementsState.value.toMutableMap()
        current[announcement.announcementId] = announcement
        announcementsState.value = current
        announcement
    }

    override fun observeBroadcasts(projectId: String): Flow<List<BroadcastMessage>> {
        return broadcastsState.map { map ->
            map.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
        }
    }

    override suspend fun getBroadcasts(projectId: String): List<BroadcastMessage> = mutex.withLock {
        broadcastsState.value.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
    }

    override suspend fun getBroadcastById(projectId: String, broadcastId: String): BroadcastMessage? = mutex.withLock {
        val b = broadcastsState.value[broadcastId]
        if (b?.projectId == projectId) b else null
    }

    override suspend fun saveBroadcast(broadcast: BroadcastMessage): BroadcastMessage = mutex.withLock {
        val current = broadcastsState.value.toMutableMap()
        current[broadcast.broadcastId] = broadcast
        broadcastsState.value = current
        broadcast
    }

    override suspend fun recordActivity(event: CampaignActivityEvent): CampaignActivityEvent = mutex.withLock {
        val current = activityEventsState.value.toMutableList()
        current.add(event)
        activityEventsState.value = current
        event
    }

    override suspend fun getActivityEvents(projectId: String, campaignId: String): List<CampaignActivityEvent> = mutex.withLock {
        activityEventsState.value.filter { it.projectId == projectId && it.campaignId == campaignId }
            .sortedBy { it.timestamp }
    }

    override fun observeActivityEvents(projectId: String): Flow<List<CampaignActivityEvent>> {
        return activityEventsState.map { list ->
            list.filter { it.projectId == projectId }.sortedByDescending { it.timestamp }
        }
    }

    override suspend fun getCandidateRecipients(projectId: String): List<CampaignAudienceResolver.CandidateRecipient> {
        // Read-only synthetic candidate resolution matching existing tenant/project users
        return listOf(
            CampaignAudienceResolver.CandidateRecipient(
                projectId = projectId,
                recipientType = "CUSTOMER",
                recipientEntityId = "cus-001",
                userId = "user-cus-001",
                role = "CUSTOMER",
                isActive = true
            ),
            CampaignAudienceResolver.CandidateRecipient(
                projectId = projectId,
                recipientType = "CUSTOMER",
                recipientEntityId = "cus-002",
                userId = "user-cus-002",
                role = "CUSTOMER",
                isActive = true
            ),
            CampaignAudienceResolver.CandidateRecipient(
                projectId = projectId,
                recipientType = "VENDOR",
                recipientEntityId = "ven-001",
                userId = "user-ven-001",
                role = "VENDOR",
                isActive = true
            ),
            CampaignAudienceResolver.CandidateRecipient(
                projectId = projectId,
                recipientType = "VENDOR",
                recipientEntityId = "ven-002",
                userId = "user-ven-002",
                role = "VENDOR",
                isActive = true
            ),
            CampaignAudienceResolver.CandidateRecipient(
                projectId = projectId,
                recipientType = "STAFF",
                recipientEntityId = "user-staff-01",
                userId = "user-staff-01",
                role = "STAFF",
                departmentId = "dept-prod",
                teamId = "team-press-01",
                isActive = true
            ),
            CampaignAudienceResolver.CandidateRecipient(
                projectId = projectId,
                recipientType = "STAFF",
                recipientEntityId = "user-qc-01",
                userId = "user-qc-01",
                role = "QC_INSPECTOR",
                departmentId = "dept-qc",
                teamId = "team-qc-01",
                isActive = true
            ),
            CampaignAudienceResolver.CandidateRecipient(
                projectId = projectId,
                recipientType = "USER",
                recipientEntityId = "user-admin-01",
                userId = "user-admin-01",
                role = "ADMIN",
                isActive = true
            )
        )
    }
}
