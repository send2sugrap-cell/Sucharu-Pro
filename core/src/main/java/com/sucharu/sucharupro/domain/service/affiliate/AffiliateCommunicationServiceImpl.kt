package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.*
import com.sucharu.sucharupro.domain.repository.affiliate.AffiliateCommunicationRepository
import com.sucharu.sucharupro.domain.repository.affiliate.AffiliateRepository
import java.util.UUID

/**
 * Service Implementation for Affiliate Communication, Notification & Lifecycle Governance.
 *
 * Key Guarantees:
 * - Idempotency: repeated createCommunication calls with the same idempotencyKey return the stored record
 * - Mandatory Delivery: GOVERNANCE, SECURITY, and SYSTEM communications bypass channel preference filters
 * - Audit Chain: every state mutation (markRead, markAllRead, upsertPreference) records a SHA-256 chained audit entry
 * - Tenant Isolation: all queries are scoped by tenantId; cross-tenant reads throw ForbiddenException
 *
 * Module 20 Step 04.
 */
class AffiliateCommunicationServiceImpl(
    private val communicationRepository: AffiliateCommunicationRepository,
    private val affiliateRepository: AffiliateRepository
) : AffiliateCommunicationService {

    // ─────────────────────────────────────────────────────────────────
    // Communication Creation
    // ─────────────────────────────────────────────────────────────────

    override suspend fun createCommunication(
        tenantId: String,
        affiliateId: String,
        recipientUserId: String,
        communicationType: AffiliateCommunicationType,
        title: String,
        message: String,
        referenceType: String?,
        referenceId: String?,
        actorUserId: String,
        actorRole: String,
        actorType: AffiliateActorType,
        idempotencyKey: String?,
        correlationId: String
    ): AffiliateCommunicationRecord {
        // Resolve idempotency key
        val resolvedIdempotencyKey = idempotencyKey
            ?: AffiliateCommunicationPolicyEngine.generateIdempotencyKey(
                tenantId = tenantId,
                affiliateId = affiliateId,
                communicationType = communicationType,
                correlationId = correlationId
            )

        // Idempotency check — return existing if already created
        val existing = communicationRepository.findCommunicationByIdempotencyKey(tenantId, resolvedIdempotencyKey)
        if (existing != null) return existing

        // Verify affiliate exists and belongs to this tenant
        val affiliate = affiliateRepository.findById(tenantId, affiliateId)
            ?: throw NoSuchElementException("Affiliate '$affiliateId' not found in tenant '$tenantId'.")

        // Resolve notification preferences for channel determination
        val preference = communicationRepository.findPreference(tenantId, affiliateId, communicationType)
        val channels = AffiliateCommunicationPolicyEngine.resolveChannels(communicationType, preference)

        val communicationId = "COMM-${UUID.randomUUID().toString().take(12).uppercase()}"
        val now = System.currentTimeMillis()
        val channelsJson = "[${channels.joinToString(",") { "\"${it.name}\"" }}]"

        val record = AffiliateCommunicationRecord(
            communicationId = communicationId,
            tenantId = tenantId,
            affiliateId = affiliateId,
            recipientUserId = recipientUserId,
            communicationType = communicationType,
            subject = title.trim(),
            bodyPreview = message.take(250),
            channelsJson = channelsJson,
            status = AffiliateCommunicationStatus.DELIVERED,
            canonicalNotificationId = null, // Set after canonical notification is created (by BackendUseCases)
            referenceType = referenceType,
            referenceId = referenceId,
            idempotencyKey = resolvedIdempotencyKey,
            correlationId = correlationId,
            version = 1L,
            createdAt = now,
            deliveredAt = now
        )

        val saved = communicationRepository.saveCommunication(record)

        // Record audit entry for creation
        recordAudit(
            tenantId = tenantId,
            affiliateId = affiliateId,
            communicationId = communicationId,
            actorUserId = actorUserId,
            actorRole = actorRole,
            actorType = actorType,
            action = "COMMUNICATION_CREATED",
            previousStatus = null,
            newStatus = AffiliateCommunicationStatus.DELIVERED.name,
            reason = "Communication type: ${communicationType.name}",
            correlationId = correlationId
        )

        return saved
    }

    // ─────────────────────────────────────────────────────────────────
    // Query
    // ─────────────────────────────────────────────────────────────────

    override suspend fun listCommunications(
        tenantId: String,
        affiliateId: String,
        status: AffiliateCommunicationStatus?,
        communicationType: AffiliateCommunicationType?
    ): List<AffiliateCommunicationRecord> {
        return communicationRepository.listCommunications(tenantId, affiliateId, status, communicationType)
    }

    override suspend fun findCommunicationById(
        tenantId: String,
        communicationId: String
    ): AffiliateCommunicationRecord? {
        return communicationRepository.findCommunicationById(tenantId, communicationId)
    }

    override suspend fun getUnreadCount(
        tenantId: String,
        affiliateId: String
    ): AffiliateUnreadSummary {
        val total = communicationRepository.countUnread(tenantId, affiliateId)
        val byType = communicationRepository.countUnreadByType(tenantId, affiliateId)
        return AffiliateUnreadSummary(
            affiliateId = affiliateId,
            totalUnread = total,
            byType = byType
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // Lifecycle Mutations
    // ─────────────────────────────────────────────────────────────────

    override suspend fun markRead(
        tenantId: String,
        communicationId: String,
        actorUserId: String,
        actorRole: String
    ): AffiliateCommunicationRecord {
        val current = communicationRepository.findCommunicationById(tenantId, communicationId)
            ?: throw NoSuchElementException("Communication '$communicationId' not found in tenant '$tenantId'.")

        if (current.isRead) return current // Already read — idempotent

        val updated = communicationRepository.markRead(tenantId, communicationId)
            ?: throw NoSuchElementException("Communication '$communicationId' could not be marked as read.")

        val correlationId = "CORR-READ-${UUID.randomUUID().toString().take(8)}"
        recordAudit(
            tenantId = tenantId,
            affiliateId = current.affiliateId,
            communicationId = communicationId,
            actorUserId = actorUserId,
            actorRole = actorRole,
            actorType = AffiliateActorType.HUMAN,
            action = "COMMUNICATION_READ",
            previousStatus = current.status.name,
            newStatus = AffiliateCommunicationStatus.READ.name,
            reason = null,
            correlationId = correlationId
        )

        return updated
    }

    override suspend fun markAllRead(
        tenantId: String,
        affiliateId: String,
        actorUserId: String,
        actorRole: String
    ): Int {
        val count = communicationRepository.markAllRead(tenantId, affiliateId)

        if (count > 0) {
            val correlationId = "CORR-READALL-${UUID.randomUUID().toString().take(8)}"
            recordAudit(
                tenantId = tenantId,
                affiliateId = affiliateId,
                communicationId = "BATCH",
                actorUserId = actorUserId,
                actorRole = actorRole,
                actorType = AffiliateActorType.HUMAN,
                action = "COMMUNICATIONS_MARK_ALL_READ",
                previousStatus = null,
                newStatus = "READ (batch=$count)",
                reason = "Batch mark-all-read by $actorRole",
                correlationId = correlationId
            )
        }

        return count
    }

    // ─────────────────────────────────────────────────────────────────
    // Preferences
    // ─────────────────────────────────────────────────────────────────

    override suspend fun getPreferences(
        tenantId: String,
        affiliateId: String
    ): List<AffiliateNotificationPreference> {
        val stored = communicationRepository.listPreferences(tenantId, affiliateId)
        val storedTypes = stored.map { it.communicationType }.toSet()

        // Build defaults for any type not explicitly configured
        val defaults = AffiliateCommunicationType.entries
            .filter { it !in storedTypes }
            .map { type ->
                AffiliateNotificationPreference(
                    preferenceId = "DEFAULT-${type.name}",
                    tenantId = tenantId,
                    affiliateId = affiliateId,
                    userId = "",
                    communicationType = type,
                    inAppEnabled = true,
                    pushEnabled = !type.isMandatory,
                    emailEnabled = false,
                    smsEnabled = false
                )
            }

        return (stored + defaults).sortedBy { it.communicationType.name }
    }

    override suspend fun upsertPreference(
        tenantId: String,
        affiliateId: String,
        userId: String,
        communicationType: AffiliateCommunicationType,
        inAppEnabled: Boolean,
        pushEnabled: Boolean,
        emailEnabled: Boolean,
        smsEnabled: Boolean,
        actorUserId: String,
        actorRole: String,
        correlationId: String
    ): AffiliateNotificationPreference {
        // Enforce mandatory type rule: in-app cannot be disabled
        val effectiveInApp = if (communicationType.isMandatory) true else inAppEnabled

        val existing = communicationRepository.findPreference(tenantId, affiliateId, communicationType)
        val now = System.currentTimeMillis()

        val preference = if (existing != null) {
            existing.copy(
                inAppEnabled = effectiveInApp,
                pushEnabled = pushEnabled,
                emailEnabled = emailEnabled,
                smsEnabled = smsEnabled,
                updatedAt = now,
                version = existing.version + 1
            )
        } else {
            AffiliateNotificationPreference(
                preferenceId = "PREF-${UUID.randomUUID().toString().take(12).uppercase()}",
                tenantId = tenantId,
                affiliateId = affiliateId,
                userId = userId,
                communicationType = communicationType,
                inAppEnabled = effectiveInApp,
                pushEnabled = pushEnabled,
                emailEnabled = emailEnabled,
                smsEnabled = smsEnabled,
                version = 1L,
                createdAt = now,
                updatedAt = now
            )
        }

        val saved = communicationRepository.savePreference(preference)

        // Audit preference change
        recordAudit(
            tenantId = tenantId,
            affiliateId = affiliateId,
            communicationId = "PREFERENCE:${communicationType.name}",
            actorUserId = actorUserId,
            actorRole = actorRole,
            actorType = AffiliateActorType.HUMAN,
            action = "NOTIFICATION_PREFERENCE_UPDATED",
            previousStatus = existing?.let { "inApp=${it.inAppEnabled},push=${it.pushEnabled},email=${it.emailEnabled},sms=${it.smsEnabled}" },
            newStatus = "inApp=${effectiveInApp},push=${pushEnabled},email=${emailEnabled},sms=${smsEnabled}",
            reason = "Preference update for ${communicationType.name}",
            correlationId = correlationId
        )

        return saved
    }

    // ─────────────────────────────────────────────────────────────────
    // Governance
    // ─────────────────────────────────────────────────────────────────

    override suspend fun getGovernanceSummary(tenantId: String): AffiliateNotificationGovernanceSummary {
        return communicationRepository.getGovernanceSummary(tenantId)
    }

    // ─────────────────────────────────────────────────────────────────
    // Audit
    // ─────────────────────────────────────────────────────────────────

    override suspend fun listAuditRecords(
        tenantId: String,
        affiliateId: String
    ): List<AffiliateCommunicationAuditRecord> {
        return communicationRepository.listAuditRecords(tenantId, affiliateId)
    }

    // ─────────────────────────────────────────────────────────────────
    // AI Handoff
    // ─────────────────────────────────────────────────────────────────

    override suspend fun getHandoffContract(
        tenantId: String,
        affiliateId: String,
        userId: String
    ): Module20Step04AffiliateCommunicationHandoffContract {
        val communications = communicationRepository.listCommunications(tenantId, affiliateId)
        val preferences = communicationRepository.listPreferences(tenantId, affiliateId)
        return AffiliateCommunicationPolicyEngine.synthesizeHandoffContract(
            tenantId = tenantId,
            affiliateId = affiliateId,
            userId = userId,
            communications = communications,
            preferences = preferences
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // Internal: SHA-256 Audit Chain
    // ─────────────────────────────────────────────────────────────────

    private suspend fun recordAudit(
        tenantId: String,
        affiliateId: String,
        communicationId: String,
        actorUserId: String,
        actorRole: String,
        actorType: AffiliateActorType,
        action: String,
        previousStatus: String?,
        newStatus: String,
        reason: String?,
        correlationId: String
    ) {
        val auditId = "AUD-COMM-${UUID.randomUUID().toString().take(8).uppercase()}"
        val now = System.currentTimeMillis()

        val recordHash = AffiliateCommunicationPolicyEngine.computeAuditRecordHash(
            tenantId = tenantId,
            auditId = auditId,
            affiliateId = affiliateId,
            communicationId = communicationId,
            actorUserId = actorUserId,
            action = action,
            previousStatus = previousStatus,
            newStatus = newStatus,
            correlationId = correlationId,
            timestamp = now
        )

        val latestAudit = communicationRepository.getLatestAuditRecord(tenantId, affiliateId)
        val previousAuditHash = latestAudit?.recordHash
        val chainHash = AffiliateCommunicationPolicyEngine.computeAuditChainHash(latestAudit?.chainHash, recordHash)

        val auditRecord = AffiliateCommunicationAuditRecord(
            auditId = auditId,
            tenantId = tenantId,
            affiliateId = affiliateId,
            communicationId = communicationId,
            actorUserId = actorUserId,
            actorRole = actorRole,
            actorType = actorType,
            action = action,
            previousStatus = previousStatus,
            newStatus = newStatus,
            reason = reason,
            correlationId = correlationId,
            recordHash = recordHash,
            previousAuditHash = previousAuditHash,
            chainHash = chainHash,
            timestamp = now
        )

        communicationRepository.recordAudit(auditRecord)
    }
}
