package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.*

/**
 * Domain Service Interface for Affiliate Communication, Notification & Lifecycle Governance.
 * Module 20 Step 04.
 */
interface AffiliateCommunicationService {

    // ─────────────────────────────────────────────────────────────────
    // Communication Creation (triggered by domain events)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Creates and delivers a new affiliate communication, respecting the affiliate's
     * notification preferences and mandatory-type delivery rules.
     * Idempotent: repeated calls with the same idempotencyKey return the existing record.
     */
    suspend fun createCommunication(
        tenantId: String,
        affiliateId: String,
        recipientUserId: String,
        communicationType: AffiliateCommunicationType,
        title: String,
        message: String,
        referenceType: String? = null,
        referenceId: String? = null,
        actorUserId: String,
        actorRole: String,
        actorType: AffiliateActorType = AffiliateActorType.SYSTEM,
        idempotencyKey: String? = null,
        correlationId: String
    ): AffiliateCommunicationRecord

    // ─────────────────────────────────────────────────────────────────
    // Query
    // ─────────────────────────────────────────────────────────────────

    /**
     * Lists all communications for an affiliate, ordered by creation time (newest first).
     * Optionally filtered by status or type.
     */
    suspend fun listCommunications(
        tenantId: String,
        affiliateId: String,
        status: AffiliateCommunicationStatus? = null,
        communicationType: AffiliateCommunicationType? = null
    ): List<AffiliateCommunicationRecord>

    /**
     * Returns a communication by its ID.
     */
    suspend fun findCommunicationById(
        tenantId: String,
        communicationId: String
    ): AffiliateCommunicationRecord?

    /**
     * Returns the count of unread communications for an affiliate.
     */
    suspend fun getUnreadCount(tenantId: String, affiliateId: String): AffiliateUnreadSummary

    // ─────────────────────────────────────────────────────────────────
    // Lifecycle Mutations
    // ─────────────────────────────────────────────────────────────────

    /**
     * Marks a single communication as READ.
     * Only the owning affiliate user or an admin may perform this action.
     */
    suspend fun markRead(
        tenantId: String,
        communicationId: String,
        actorUserId: String,
        actorRole: String
    ): AffiliateCommunicationRecord

    /**
     * Marks all communications for an affiliate as READ.
     * Returns the number of records marked.
     */
    suspend fun markAllRead(
        tenantId: String,
        affiliateId: String,
        actorUserId: String,
        actorRole: String
    ): Int

    // ─────────────────────────────────────────────────────────────────
    // Preferences
    // ─────────────────────────────────────────────────────────────────

    /**
     * Returns all notification preferences for an affiliate.
     * Returns defaults for any types not explicitly configured.
     */
    suspend fun getPreferences(
        tenantId: String,
        affiliateId: String
    ): List<AffiliateNotificationPreference>

    /**
     * Creates or updates the notification preference for a specific communication type.
     * Mandatory types enforce in-app delivery regardless of preference.
     */
    suspend fun upsertPreference(
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
    ): AffiliateNotificationPreference

    // ─────────────────────────────────────────────────────────────────
    // Governance
    // ─────────────────────────────────────────────────────────────────

    /**
     * Returns governance summary for all affiliate communications in a tenant.
     */
    suspend fun getGovernanceSummary(tenantId: String): AffiliateNotificationGovernanceSummary

    // ─────────────────────────────────────────────────────────────────
    // Audit
    // ─────────────────────────────────────────────────────────────────

    /**
     * Returns the full SHA-256 chain audit trail for a specific affiliate's communications.
     */
    suspend fun listAuditRecords(
        tenantId: String,
        affiliateId: String
    ): List<AffiliateCommunicationAuditRecord>

    // ─────────────────────────────────────────────────────────────────
    // AI Handoff
    // ─────────────────────────────────────────────────────────────────

    /**
     * Synthesizes and returns an immutable, integrity-sealed AI Governance Handoff Contract.
     */
    suspend fun getHandoffContract(
        tenantId: String,
        affiliateId: String,
        userId: String
    ): Module20Step04AffiliateCommunicationHandoffContract
}
