package com.sucharu.sucharupro.domain.repository.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.*

/**
 * Domain Repository Interface for Affiliate Communication, Notification & Lifecycle Governance.
 * Module 20 Step 04.
 */
interface AffiliateCommunicationRepository {

    // Communications
    suspend fun findCommunicationById(tenantId: String, communicationId: String): AffiliateCommunicationRecord?
    suspend fun findCommunicationByIdempotencyKey(tenantId: String, idempotencyKey: String): AffiliateCommunicationRecord?
    suspend fun saveCommunication(record: AffiliateCommunicationRecord): AffiliateCommunicationRecord
    suspend fun listCommunications(
        tenantId: String,
        affiliateId: String,
        status: AffiliateCommunicationStatus? = null,
        communicationType: AffiliateCommunicationType? = null
    ): List<AffiliateCommunicationRecord>
    suspend fun countUnread(tenantId: String, affiliateId: String): Long
    suspend fun countUnreadByType(tenantId: String, affiliateId: String): Map<String, Long>
    suspend fun markRead(tenantId: String, communicationId: String): AffiliateCommunicationRecord?
    suspend fun markAllRead(tenantId: String, affiliateId: String): Int

    // Preferences
    suspend fun findPreference(tenantId: String, affiliateId: String, communicationType: AffiliateCommunicationType): AffiliateNotificationPreference?
    suspend fun listPreferences(tenantId: String, affiliateId: String): List<AffiliateNotificationPreference>
    suspend fun savePreference(preference: AffiliateNotificationPreference): AffiliateNotificationPreference

    // Audit
    suspend fun recordAudit(record: AffiliateCommunicationAuditRecord): AffiliateCommunicationAuditRecord
    suspend fun listAuditRecords(tenantId: String, affiliateId: String): List<AffiliateCommunicationAuditRecord>
    suspend fun getLatestAuditRecord(tenantId: String, affiliateId: String): AffiliateCommunicationAuditRecord?

    // Governance
    suspend fun getGovernanceSummary(tenantId: String): AffiliateNotificationGovernanceSummary
}
