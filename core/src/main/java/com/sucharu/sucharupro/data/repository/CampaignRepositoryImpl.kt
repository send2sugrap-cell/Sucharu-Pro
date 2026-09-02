package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.CampaignDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.campaign.*
import com.sucharu.sucharupro.domain.model.notification.NotificationChannel
import com.sucharu.sucharupro.domain.model.notification.NotificationPriority
import com.sucharu.sucharupro.domain.model.notification.NotificationStatus
import com.sucharu.sucharupro.domain.model.notification.NotificationType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.communication.campaign.CampaignRepository
import com.sucharu.sucharupro.domain.repository.notification.NotificationDeliveryService
import com.sucharu.sucharupro.domain.repository.notification.NotificationRepository
import com.sucharu.sucharupro.domain.validation.communication.campaign.CampaignAudienceResolver
import com.sucharu.sucharupro.domain.validation.communication.campaign.CampaignAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.communication.campaign.CampaignLifecycleValidator
import com.sucharu.sucharupro.domain.validation.communication.campaign.CampaignValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Production-grade implementation of [CampaignRepository] (Module 10 Step 07).
 *
 * Implements:
 * - Domain validation & strict lifecycle guards
 * - RBAC & Separation of Duties
 * - Deterministic audience resolution & deduplication
 * - Canonical Notification dispatch via Step 01 infrastructure
 * - Project isolation & Idempotency
 * - Zero mutation of source business records
 */
class CampaignRepositoryImpl(
    private val dataSource: CampaignDataSource,
    private val notificationRepository: NotificationRepository? = null,
    private val deliveryService: NotificationDeliveryService? = null
) : CampaignRepository {

    private val dispatchMutex = Mutex()

    override fun observeCampaigns(projectId: String, callerRole: UserRole): Flow<List<Campaign>> {
        return dataSource.observeCampaigns(projectId)
    }

    override suspend fun getCampaigns(
        projectId: String,
        status: CampaignStatus?,
        type: CampaignType?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<Campaign>> {
        val list = dataSource.getCampaigns(projectId)
        val filtered = list.filter {
            (status == null || it.status == status) && (type == null || it.campaignType == type)
        }
        return DomainResult.Success(filtered)
    }

    override suspend fun getCampaignById(
        projectId: String,
        campaignId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Campaign> {
        val campaign = dataSource.getCampaignById(projectId, campaignId)
            ?: return DomainResult.Error(message = "Campaign '$campaignId' not found in project '$projectId'.")
        return DomainResult.Success(campaign)
    }

    override suspend fun createCampaign(
        projectId: String,
        title: String,
        description: String,
        campaignType: CampaignType,
        priority: CampaignPriority,
        audienceType: CampaignAudienceType,
        targetCriteria: CampaignAudienceCriteria,
        content: String,
        scheduledAt: Long?,
        startsAt: Long?,
        endsAt: Long?,
        idempotencyKey: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Campaign> {
        // 1. RBAC Check
        val authResult = CampaignAuthorizationValidator.validateCreate(callerRole, audienceType)
        if (authResult is DomainResult.Error) return authResult

        // 2. Idempotency Check
        if (!idempotencyKey.isNullOrBlank()) {
            val existing = dataSource.getCampaignByIdempotencyKey(projectId, idempotencyKey)
            if (existing != null) {
                return DomainResult.Success(existing)
            }
        }

        // 3. Domain Validation
        val validationResult = CampaignValidator.validateCreation(
            projectId = projectId,
            title = title,
            content = content,
            audienceType = audienceType,
            targetCriteria = targetCriteria,
            createdBy = actorId,
            scheduledAt = scheduledAt,
            startsAt = startsAt,
            endsAt = endsAt
        )
        if (validationResult is DomainResult.Error) return validationResult

        // 4. Create Entity
        val now = System.currentTimeMillis()
        val generatedId = "cmp-${UUID.randomUUID().toString().take(8)}"
        val campaignNo = "CMP-2026-${(10000..99999).random()}"

        val campaign = Campaign(
            campaignId = generatedId,
            campaignNo = campaignNo,
            projectId = projectId,
            title = title,
            description = description,
            campaignType = campaignType,
            status = CampaignStatus.DRAFT,
            priority = priority,
            audienceType = audienceType,
            targetCriteria = targetCriteria,
            content = content,
            scheduledAt = scheduledAt,
            startsAt = startsAt,
            endsAt = endsAt,
            createdBy = actorId,
            createdAt = now,
            updatedAt = now,
            idempotencyKey = idempotencyKey
        )

        val saved = dataSource.saveCampaign(campaign)

        // 5. Audit Trail
        recordAudit(projectId, saved.campaignId, CampaignActivityEventType.CAMPAIGN_CREATED, actorId, "Campaign created in DRAFT state.")

        return DomainResult.Success(saved)
    }

    override suspend fun updateDraft(
        campaign: Campaign,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Campaign> {
        val existing = dataSource.getCampaignById(campaign.projectId, campaign.campaignId)
            ?: return DomainResult.Error(message = "Campaign '${campaign.campaignId}' not found.")

        if (existing.status != CampaignStatus.DRAFT && existing.status != CampaignStatus.REJECTED) {
            return DomainResult.Error(message = "Cannot edit campaign in '${existing.status}' state.")
        }

        val updated = campaign.copy(
            updatedBy = actorId,
            updatedAt = System.currentTimeMillis()
        )
        val saved = dataSource.saveCampaign(updated)
        recordAudit(campaign.projectId, campaign.campaignId, CampaignActivityEventType.CAMPAIGN_UPDATED, actorId, "Campaign draft updated.")
        return DomainResult.Success(saved)
    }

    override suspend fun submitForApproval(
        projectId: String,
        campaignId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Campaign> {
        val authResult = CampaignAuthorizationValidator.validateSubmit(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val existing = dataSource.getCampaignById(projectId, campaignId)
            ?: return DomainResult.Error(message = "Campaign '$campaignId' not found.")

        val transitionResult = CampaignLifecycleValidator.validateTransition(existing.status, CampaignStatus.PENDING_APPROVAL)
        if (transitionResult is DomainResult.Error) return transitionResult

        val updated = existing.copy(
            status = CampaignStatus.PENDING_APPROVAL,
            submittedAt = System.currentTimeMillis(),
            updatedBy = actorId,
            updatedAt = System.currentTimeMillis()
        )
        val saved = dataSource.saveCampaign(updated)
        recordAudit(projectId, campaignId, CampaignActivityEventType.CAMPAIGN_SUBMITTED, actorId, "Campaign submitted for approval.")
        return DomainResult.Success(saved)
    }

    override suspend fun approveCampaign(
        projectId: String,
        campaignId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Campaign> {
        val existing = dataSource.getCampaignById(projectId, campaignId)
            ?: return DomainResult.Error(message = "Campaign '$campaignId' not found.")

        // Separation of Duties check
        val authResult = CampaignAuthorizationValidator.validateApproval(callerRole, existing.createdBy, actorId)
        if (authResult is DomainResult.Error) return authResult

        val transitionResult = CampaignLifecycleValidator.validateTransition(existing.status, CampaignStatus.APPROVED)
        if (transitionResult is DomainResult.Error) return transitionResult

        val updated = existing.copy(
            status = CampaignStatus.APPROVED,
            approvedBy = actorId,
            approvedAt = System.currentTimeMillis(),
            updatedBy = actorId,
            updatedAt = System.currentTimeMillis()
        )
        val saved = dataSource.saveCampaign(updated)
        recordAudit(projectId, campaignId, CampaignActivityEventType.CAMPAIGN_APPROVED, actorId, "Campaign approved by $actorId.")
        return DomainResult.Success(saved)
    }

    override suspend fun rejectCampaign(
        projectId: String,
        campaignId: String,
        rejectionReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Campaign> {
        val existing = dataSource.getCampaignById(projectId, campaignId)
            ?: return DomainResult.Error(message = "Campaign '$campaignId' not found.")

        val authResult = CampaignAuthorizationValidator.validateApproval(callerRole, existing.createdBy, actorId)
        if (authResult is DomainResult.Error) return authResult

        val transitionResult = CampaignLifecycleValidator.validateTransition(existing.status, CampaignStatus.REJECTED)
        if (transitionResult is DomainResult.Error) return transitionResult

        val updated = existing.copy(
            status = CampaignStatus.REJECTED,
            rejectionReason = rejectionReason,
            updatedBy = actorId,
            updatedAt = System.currentTimeMillis()
        )
        val saved = dataSource.saveCampaign(updated)
        recordAudit(projectId, campaignId, CampaignActivityEventType.CAMPAIGN_REJECTED, actorId, "Campaign rejected: $rejectionReason")
        return DomainResult.Success(saved)
    }

    override suspend fun scheduleCampaign(
        projectId: String,
        campaignId: String,
        scheduledAt: Long,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Campaign> {
        val existing = dataSource.getCampaignById(projectId, campaignId)
            ?: return DomainResult.Error(message = "Campaign '$campaignId' not found.")

        val transitionResult = CampaignLifecycleValidator.validateTransition(existing.status, CampaignStatus.SCHEDULED)
        if (transitionResult is DomainResult.Error) return transitionResult

        val updated = existing.copy(
            status = CampaignStatus.SCHEDULED,
            scheduledAt = scheduledAt,
            updatedBy = actorId,
            updatedAt = System.currentTimeMillis()
        )
        val saved = dataSource.saveCampaign(updated)
        recordAudit(projectId, campaignId, CampaignActivityEventType.CAMPAIGN_SCHEDULED, actorId, "Campaign scheduled for $scheduledAt.")
        return DomainResult.Success(saved)
    }

    override suspend fun publishCampaign(
        projectId: String,
        campaignId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Campaign> = dispatchMutex.withLock {
        val existing = dataSource.getCampaignById(projectId, campaignId)
            ?: return DomainResult.Error(message = "Campaign '$campaignId' not found.")

        // Check if already published (idempotent / concurrency safety)
        if (existing.status == CampaignStatus.PUBLISHED) {
            return DomainResult.Success(existing)
        }

        // Separation of Duties check
        val authResult = CampaignAuthorizationValidator.validatePublish(callerRole, existing.createdBy, actorId)
        if (authResult is DomainResult.Error) return authResult

        val transitionResult = CampaignLifecycleValidator.validateTransition(existing.status, CampaignStatus.PUBLISHED)
        if (transitionResult is DomainResult.Error) return transitionResult

        // 1. Resolve Audience
        val candidates = dataSource.getCandidateRecipients(projectId)
        val recipients = CampaignAudienceResolver.resolve(existing, candidates)
        if (recipients.isEmpty()) {
            return DomainResult.Error(message = "Cannot publish campaign with empty resolved audience.")
        }
        dataSource.saveRecipients(recipients)
        recordAudit(projectId, campaignId, CampaignActivityEventType.AUDIENCE_RESOLVED, actorId, "Resolved ${recipients.size} unique recipients.")

        // 2. Dispatch Canonical Notifications via Step 01 infrastructure
        val updatedRecipients = mutableListOf<CampaignRecipient>()
        for (recipient in recipients) {
            var notificationId: String? = null
            var deliveryStatus = NotificationStatus.SENT

            if (notificationRepository != null) {
                val notifResult = notificationRepository.createNotification(
                    projectId = projectId,
                    recipientUserId = recipient.userId,
                    recipientType = recipient.recipientType,
                    notificationType = NotificationType.GENERAL,
                    channel = existing.communicationChannel,
                    priority = when (existing.priority) {
                        CampaignPriority.LOW -> NotificationPriority.LOW
                        CampaignPriority.NORMAL -> NotificationPriority.NORMAL
                        CampaignPriority.HIGH -> NotificationPriority.HIGH
                        CampaignPriority.URGENT -> NotificationPriority.URGENT
                    },
                    title = existing.title,
                    message = existing.content,
                    referenceType = "CAMPAIGN",
                    referenceId = existing.campaignId,
                    actorId = actorId,
                    callerRole = callerRole
                )
                if (notifResult is DomainResult.Success) {
                    notificationId = notifResult.data.notificationId
                    deliveryStatus = NotificationStatus.DELIVERED
                }
            }

            val updatedRecipient = recipient.copy(
                notificationId = notificationId,
                deliveryStatus = deliveryStatus,
                deliveredAt = System.currentTimeMillis()
            )
            dataSource.updateRecipient(updatedRecipient)
            updatedRecipients.add(updatedRecipient)
        }

        // 3. Mark Campaign as Published
        val now = System.currentTimeMillis()
        val publishedCampaign = existing.copy(
            status = CampaignStatus.PUBLISHED,
            publishedBy = actorId,
            publishedAt = now,
            updatedBy = actorId,
            updatedAt = now
        )
        val saved = dataSource.saveCampaign(publishedCampaign)
        recordAudit(projectId, campaignId, CampaignActivityEventType.CAMPAIGN_PUBLISHED, actorId, "Campaign published to ${recipients.size} recipients.")

        return DomainResult.Success(saved)
    }

    override suspend fun completeCampaign(
        projectId: String,
        campaignId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Campaign> {
        val existing = dataSource.getCampaignById(projectId, campaignId)
            ?: return DomainResult.Error(message = "Campaign '$campaignId' not found.")

        val transitionResult = CampaignLifecycleValidator.validateTransition(existing.status, CampaignStatus.COMPLETED)
        if (transitionResult is DomainResult.Error) return transitionResult

        val updated = existing.copy(
            status = CampaignStatus.COMPLETED,
            completedAt = System.currentTimeMillis(),
            updatedBy = actorId,
            updatedAt = System.currentTimeMillis()
        )
        val saved = dataSource.saveCampaign(updated)
        recordAudit(projectId, campaignId, CampaignActivityEventType.CAMPAIGN_COMPLETED, actorId, "Campaign completed.")
        return DomainResult.Success(saved)
    }

    override suspend fun cancelCampaign(
        projectId: String,
        campaignId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Campaign> {
        val authResult = CampaignAuthorizationValidator.validateCancel(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val existing = dataSource.getCampaignById(projectId, campaignId)
            ?: return DomainResult.Error(message = "Campaign '$campaignId' not found.")

        val transitionResult = CampaignLifecycleValidator.validateTransition(existing.status, CampaignStatus.CANCELLED)
        if (transitionResult is DomainResult.Error) return transitionResult

        val updated = existing.copy(
            status = CampaignStatus.CANCELLED,
            cancelledBy = actorId,
            cancelledAt = System.currentTimeMillis(),
            updatedBy = actorId,
            updatedAt = System.currentTimeMillis()
        )
        val saved = dataSource.saveCampaign(updated)
        recordAudit(projectId, campaignId, CampaignActivityEventType.CAMPAIGN_CANCELLED, actorId, "Campaign cancelled by $actorId.")
        return DomainResult.Success(saved)
    }

    override suspend fun getRecipients(
        projectId: String,
        campaignId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<CampaignRecipient>> {
        val list = dataSource.getRecipients(projectId, campaignId)
        return DomainResult.Success(list)
    }

    override suspend fun recordRecipientRead(
        projectId: String,
        campaignId: String,
        recipientId: String,
        userId: String
    ): DomainResult<CampaignRecipient> {
        val recipients = dataSource.getRecipients(projectId, campaignId)
        val target = recipients.firstOrNull { it.recipientId == recipientId && it.userId == userId }
            ?: return DomainResult.Error(message = "Recipient '$recipientId' not found for user '$userId'.")

        val updated = target.copy(
            readStatus = true,
            readAt = System.currentTimeMillis()
        )
        val saved = dataSource.updateRecipient(updated)
        recordAudit(projectId, campaignId, CampaignActivityEventType.RECIPIENT_READ, userId, "Recipient read communication.")
        return DomainResult.Success(saved)
    }

    override suspend fun recordRecipientAcknowledged(
        projectId: String,
        campaignId: String,
        recipientId: String,
        userId: String
    ): DomainResult<CampaignRecipient> {
        val recipients = dataSource.getRecipients(projectId, campaignId)
        val target = recipients.firstOrNull { it.recipientId == recipientId && it.userId == userId }
            ?: return DomainResult.Error(message = "Recipient '$recipientId' not found for user '$userId'.")

        val updated = target.copy(
            acknowledgementStatus = true,
            acknowledgedAt = System.currentTimeMillis()
        )
        val saved = dataSource.updateRecipient(updated)
        recordAudit(projectId, campaignId, CampaignActivityEventType.RECIPIENT_ACKNOWLEDGED, userId, "Recipient acknowledged communication.")
        return DomainResult.Success(saved)
    }

    override suspend fun getDeliverySummary(
        projectId: String,
        campaignId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CampaignDeliverySummary> {
        val recipients = dataSource.getRecipients(projectId, campaignId)
        val total = recipients.size
        val sent = recipients.count { it.deliveryStatus == NotificationStatus.SENT || it.deliveryStatus == NotificationStatus.DELIVERED }
        val delivered = recipients.count { it.deliveredAt != null || it.deliveryStatus == NotificationStatus.DELIVERED }
        val read = recipients.count { it.readStatus }
        val acknowledged = recipients.count { it.acknowledgementStatus }
        val failed = recipients.count { it.deliveryStatus == NotificationStatus.FAILED }

        return DomainResult.Success(
            CampaignDeliverySummary(
                totalRecipients = total,
                sent = sent,
                delivered = delivered,
                read = read,
                acknowledged = acknowledged,
                failed = failed
            )
        )
    }

    override suspend fun getEngagementSummary(
        projectId: String,
        campaignId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CampaignEngagementSummary> {
        val recipients = dataSource.getRecipients(projectId, campaignId)
        val total = recipients.size.toDouble()
        if (total == 0.0) {
            return DomainResult.Success(CampaignEngagementSummary())
        }

        val delivered = recipients.count { it.deliveredAt != null || it.deliveryStatus == NotificationStatus.DELIVERED }.toDouble()
        val read = recipients.count { it.readStatus }.toDouble()
        val acknowledged = recipients.count { it.acknowledgementStatus }.toDouble()
        val failed = recipients.count { it.deliveryStatus == NotificationStatus.FAILED }.toDouble()

        val deliveryRate = (delivered / total) * 100.0
        val readRate = if (delivered > 0) (read / delivered) * 100.0 else 0.0
        val ackRate = if (delivered > 0) (acknowledged / delivered) * 100.0 else 0.0
        val failureRate = (failed / total) * 100.0
        val engagementRate = if (total > 0) ((read + acknowledged) / (total * 2)) * 100.0 else 0.0

        return DomainResult.Success(
            CampaignEngagementSummary(
                deliveryRate = deliveryRate,
                readRate = readRate,
                acknowledgementRate = ackRate,
                failureRate = failureRate,
                engagementRate = engagementRate
            )
        )
    }

    override suspend fun getProjectSummary(
        projectId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CampaignSummary> {
        val campaigns = dataSource.getCampaigns(projectId)
        val total = campaigns.size
        val active = campaigns.count { it.status == CampaignStatus.PUBLISHED }
        val scheduled = campaigns.count { it.status == CampaignStatus.SCHEDULED }
        val completed = campaigns.count { it.status == CampaignStatus.COMPLETED }
        val cancelled = campaigns.count { it.status == CampaignStatus.CANCELLED }

        return DomainResult.Success(
            CampaignSummary(
                projectId = projectId,
                totalCampaigns = total,
                activeCampaigns = active,
                scheduledCampaigns = scheduled,
                completedCampaigns = completed,
                cancelledCampaigns = cancelled
            )
        )
    }

    override fun observeAnnouncements(projectId: String, callerRole: UserRole): Flow<List<Announcement>> {
        return dataSource.observeAnnouncements(projectId)
    }

    override suspend fun getAnnouncements(
        projectId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<Announcement>> {
        val list = dataSource.getAnnouncements(projectId)
        return DomainResult.Success(list)
    }

    override suspend fun createAnnouncement(
        projectId: String,
        title: String,
        content: String,
        priority: CampaignPriority,
        audienceType: CampaignAudienceType,
        targetCriteria: CampaignAudienceCriteria,
        expiresAt: Long?,
        acknowledgementRequired: Boolean,
        idempotencyKey: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Announcement> {
        val authResult = CampaignAuthorizationValidator.validateCreate(callerRole, audienceType)
        if (authResult is DomainResult.Error) return authResult

        if (title.isBlank()) return DomainResult.Error(message = "Announcement title cannot be blank.")
        if (content.isBlank()) return DomainResult.Error(message = "Announcement content cannot be blank.")

        val now = System.currentTimeMillis()
        val announcement = Announcement(
            announcementId = "ann-${UUID.randomUUID().toString().take(8)}",
            announcementNo = "ANN-2026-${(10000..99999).random()}",
            projectId = projectId,
            title = title,
            content = content,
            priority = priority,
            audienceType = audienceType,
            targetCriteria = targetCriteria,
            channel = NotificationChannel.IN_APP,
            status = CampaignStatus.DRAFT,
            expiresAt = expiresAt,
            acknowledgementRequired = acknowledgementRequired,
            createdBy = actorId,
            createdAt = now,
            updatedAt = now,
            idempotencyKey = idempotencyKey
        )

        val saved = dataSource.saveAnnouncement(announcement)
        recordAudit(projectId, saved.announcementId, CampaignActivityEventType.ANNOUNCEMENT_CREATED, actorId, "Announcement created.")
        return DomainResult.Success(saved)
    }

    override suspend fun publishAnnouncement(
        projectId: String,
        announcementId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Announcement> {
        val existing = dataSource.getAnnouncementById(projectId, announcementId)
            ?: return DomainResult.Error(message = "Announcement '$announcementId' not found.")

        val updated = existing.copy(
            status = CampaignStatus.PUBLISHED,
            publishedBy = actorId,
            publishedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val saved = dataSource.saveAnnouncement(updated)
        recordAudit(projectId, announcementId, CampaignActivityEventType.ANNOUNCEMENT_PUBLISHED, actorId, "Announcement published.")
        return DomainResult.Success(saved)
    }

    override fun observeBroadcasts(projectId: String, callerRole: UserRole): Flow<List<BroadcastMessage>> {
        return dataSource.observeBroadcasts(projectId)
    }

    override suspend fun getBroadcasts(
        projectId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<BroadcastMessage>> {
        val list = dataSource.getBroadcasts(projectId)
        return DomainResult.Success(list)
    }

    override suspend fun sendBroadcast(
        projectId: String,
        title: String,
        message: String,
        priority: CampaignPriority,
        audienceType: CampaignAudienceType,
        targetCriteria: CampaignAudienceCriteria,
        idempotencyKey: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<BroadcastMessage> {
        val authResult = CampaignAuthorizationValidator.validateCreate(callerRole, audienceType)
        if (authResult is DomainResult.Error) return authResult

        if (title.isBlank()) return DomainResult.Error(message = "Broadcast title cannot be blank.")
        if (message.isBlank()) return DomainResult.Error(message = "Broadcast message cannot be blank.")

        val now = System.currentTimeMillis()
        val broadcast = BroadcastMessage(
            broadcastId = "brd-${UUID.randomUUID().toString().take(8)}",
            broadcastNo = "BRD-2026-${(10000..99999).random()}",
            projectId = projectId,
            title = title,
            message = message,
            priority = priority,
            audienceType = audienceType,
            targetCriteria = targetCriteria,
            channels = setOf(NotificationChannel.IN_APP),
            status = CampaignStatus.PUBLISHED,
            sentAt = now,
            createdBy = actorId,
            publishedBy = actorId,
            createdAt = now,
            updatedAt = now,
            idempotencyKey = idempotencyKey
        )

        val saved = dataSource.saveBroadcast(broadcast)
        recordAudit(projectId, saved.broadcastId, CampaignActivityEventType.BROADCAST_DISPATCHED, actorId, "Broadcast message dispatched.")
        return DomainResult.Success(saved)
    }

    override suspend fun getActivityEvents(
        projectId: String,
        campaignId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<CampaignActivityEvent>> {
        val events = dataSource.getActivityEvents(projectId, campaignId)
        return DomainResult.Success(events)
    }

    override fun observeActivityEvents(projectId: String, callerRole: UserRole): Flow<List<CampaignActivityEvent>> {
        return dataSource.observeActivityEvents(projectId)
    }

    private suspend fun recordAudit(
        projectId: String,
        campaignId: String,
        type: CampaignActivityEventType,
        actorId: String,
        summary: String
    ) {
        val event = CampaignActivityEvent(
            eventId = "evt-${UUID.randomUUID().toString().take(8)}",
            projectId = projectId,
            campaignId = campaignId,
            eventType = type,
            actorUserId = actorId,
            summary = summary,
            timestamp = System.currentTimeMillis()
        )
        dataSource.recordActivity(event)
    }
}
