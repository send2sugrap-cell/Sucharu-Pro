package com.sucharu.sucharupro.data.repository.affiliate

import com.sucharu.sucharupro.data.datasource.affiliate.AffiliateCommunicationDataSource
import com.sucharu.sucharupro.domain.model.affiliate.*
import com.sucharu.sucharupro.domain.repository.affiliate.AffiliateCommunicationRepository

/**
 * Repository Implementation for Affiliate Communication, Notification & Governance (Module 20 Step 04).
 * Delegates to the datasource; provides domain isolation.
 */
class AffiliateCommunicationRepositoryImpl(
    private val dataSource: AffiliateCommunicationDataSource
) : AffiliateCommunicationRepository {

    override suspend fun findCommunicationById(tenantId: String, communicationId: String): AffiliateCommunicationRecord? =
        dataSource.findCommunicationById(tenantId, communicationId)

    override suspend fun findCommunicationByIdempotencyKey(tenantId: String, idempotencyKey: String): AffiliateCommunicationRecord? =
        dataSource.findCommunicationByIdempotencyKey(tenantId, idempotencyKey)

    override suspend fun saveCommunication(record: AffiliateCommunicationRecord): AffiliateCommunicationRecord =
        dataSource.saveCommunication(record)

    override suspend fun listCommunications(
        tenantId: String,
        affiliateId: String,
        status: AffiliateCommunicationStatus?,
        communicationType: AffiliateCommunicationType?
    ): List<AffiliateCommunicationRecord> =
        dataSource.listCommunications(tenantId, affiliateId, status, communicationType)

    override suspend fun countUnread(tenantId: String, affiliateId: String): Long =
        dataSource.countUnread(tenantId, affiliateId)

    override suspend fun countUnreadByType(tenantId: String, affiliateId: String): Map<String, Long> =
        dataSource.countUnreadByType(tenantId, affiliateId)

    override suspend fun markRead(tenantId: String, communicationId: String): AffiliateCommunicationRecord? =
        dataSource.markRead(tenantId, communicationId)

    override suspend fun markAllRead(tenantId: String, affiliateId: String): Int =
        dataSource.markAllRead(tenantId, affiliateId)

    override suspend fun findPreference(
        tenantId: String,
        affiliateId: String,
        communicationType: AffiliateCommunicationType
    ): AffiliateNotificationPreference? =
        dataSource.findPreference(tenantId, affiliateId, communicationType)

    override suspend fun listPreferences(tenantId: String, affiliateId: String): List<AffiliateNotificationPreference> =
        dataSource.listPreferences(tenantId, affiliateId)

    override suspend fun savePreference(preference: AffiliateNotificationPreference): AffiliateNotificationPreference =
        dataSource.savePreference(preference)

    override suspend fun recordAudit(record: AffiliateCommunicationAuditRecord): AffiliateCommunicationAuditRecord =
        dataSource.saveAuditRecord(record)

    override suspend fun listAuditRecords(tenantId: String, affiliateId: String): List<AffiliateCommunicationAuditRecord> =
        dataSource.listAuditRecords(tenantId, affiliateId)

    override suspend fun getLatestAuditRecord(tenantId: String, affiliateId: String): AffiliateCommunicationAuditRecord? =
        dataSource.getLatestAuditRecord(tenantId, affiliateId)

    override suspend fun getGovernanceSummary(tenantId: String): AffiliateNotificationGovernanceSummary =
        dataSource.getGovernanceSummary(tenantId)
}
