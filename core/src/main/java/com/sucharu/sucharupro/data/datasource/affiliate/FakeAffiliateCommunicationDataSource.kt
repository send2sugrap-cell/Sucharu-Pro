package com.sucharu.sucharupro.data.datasource.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe In-Memory Fake Data Source for Affiliate Communication, Notification & Governance.
 * Used in unit tests and offline development. Module 20 Step 04.
 */
class FakeAffiliateCommunicationDataSource : AffiliateCommunicationDataSource {

    private val communications = ConcurrentHashMap<String, AffiliateCommunicationRecord>()
    private val communicationsByIdempotencyKey = ConcurrentHashMap<String, String>() // idempotencyKey -> communicationId
    private val preferences = ConcurrentHashMap<String, AffiliateNotificationPreference>()
    private val auditRecords = ConcurrentHashMap<String, MutableList<AffiliateCommunicationAuditRecord>>()

    private fun commKey(tenantId: String, communicationId: String) = "$tenantId:$communicationId"
    private fun prefKey(tenantId: String, affiliateId: String, type: AffiliateCommunicationType) =
        "$tenantId:$affiliateId:${type.name}"
    private fun auditKey(tenantId: String, affiliateId: String) = "$tenantId:$affiliateId"

    // ─────────────────────────────────────────────────────────────────
    // Communications
    // ─────────────────────────────────────────────────────────────────

    override suspend fun findCommunicationById(
        tenantId: String,
        communicationId: String
    ): AffiliateCommunicationRecord? {
        val record = communications[commKey(tenantId, communicationId)]
        return if (record != null && record.tenantId == tenantId) record else null
    }

    override suspend fun findCommunicationByIdempotencyKey(
        tenantId: String,
        idempotencyKey: String
    ): AffiliateCommunicationRecord? {
        val scopedKey = "$tenantId:$idempotencyKey"
        val communicationId = communicationsByIdempotencyKey[scopedKey] ?: return null
        return findCommunicationById(tenantId, communicationId)
    }

    override suspend fun saveCommunication(record: AffiliateCommunicationRecord): AffiliateCommunicationRecord {
        val key = commKey(record.tenantId, record.communicationId)
        communications[key] = record
        val scopedIdempotencyKey = "${record.tenantId}:${record.idempotencyKey}"
        communicationsByIdempotencyKey[scopedIdempotencyKey] = record.communicationId
        return record
    }

    override suspend fun listCommunications(
        tenantId: String,
        affiliateId: String,
        status: AffiliateCommunicationStatus?,
        communicationType: AffiliateCommunicationType?
    ): List<AffiliateCommunicationRecord> {
        return communications.values
            .filter { it.tenantId == tenantId && it.affiliateId == affiliateId }
            .filter { status == null || it.status == status }
            .filter { communicationType == null || it.communicationType == communicationType }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun countUnread(tenantId: String, affiliateId: String): Long {
        return communications.values
            .count { it.tenantId == tenantId && it.affiliateId == affiliateId && !it.isRead }
            .toLong()
    }

    override suspend fun countUnreadByType(
        tenantId: String,
        affiliateId: String
    ): Map<String, Long> {
        return communications.values
            .filter { it.tenantId == tenantId && it.affiliateId == affiliateId && !it.isRead }
            .groupBy { it.communicationType.name }
            .mapValues { (_, list) -> list.size.toLong() }
    }

    override suspend fun markRead(tenantId: String, communicationId: String): AffiliateCommunicationRecord? {
        val key = commKey(tenantId, communicationId)
        val current = communications[key] ?: return null
        if (current.tenantId != tenantId) return null
        val updated = current.copy(
            status = AffiliateCommunicationStatus.READ,
            readAt = System.currentTimeMillis(),
            version = current.version + 1
        )
        communications[key] = updated
        return updated
    }

    override suspend fun markAllRead(tenantId: String, affiliateId: String): Int {
        var count = 0
        val now = System.currentTimeMillis()
        communications.entries.forEach { (key, record) ->
            if (record.tenantId == tenantId && record.affiliateId == affiliateId && !record.isRead) {
                communications[key] = record.copy(
                    status = AffiliateCommunicationStatus.READ,
                    readAt = now,
                    version = record.version + 1
                )
                count++
            }
        }
        return count
    }

    // ─────────────────────────────────────────────────────────────────
    // Preferences
    // ─────────────────────────────────────────────────────────────────

    override suspend fun findPreference(
        tenantId: String,
        affiliateId: String,
        communicationType: AffiliateCommunicationType
    ): AffiliateNotificationPreference? {
        return preferences[prefKey(tenantId, affiliateId, communicationType)]
            ?.takeIf { it.tenantId == tenantId && it.affiliateId == affiliateId }
    }

    override suspend fun listPreferences(
        tenantId: String,
        affiliateId: String
    ): List<AffiliateNotificationPreference> {
        return preferences.values
            .filter { it.tenantId == tenantId && it.affiliateId == affiliateId }
            .sortedBy { it.communicationType.name }
    }

    override suspend fun savePreference(preference: AffiliateNotificationPreference): AffiliateNotificationPreference {
        preferences[prefKey(preference.tenantId, preference.affiliateId, preference.communicationType)] = preference
        return preference
    }

    // ─────────────────────────────────────────────────────────────────
    // Audit
    // ─────────────────────────────────────────────────────────────────

    override suspend fun saveAuditRecord(record: AffiliateCommunicationAuditRecord): AffiliateCommunicationAuditRecord {
        val list = auditRecords.computeIfAbsent(auditKey(record.tenantId, record.affiliateId)) { mutableListOf() }
        synchronized(list) { list.add(record) }
        return record
    }

    override suspend fun listAuditRecords(
        tenantId: String,
        affiliateId: String
    ): List<AffiliateCommunicationAuditRecord> {
        val list = auditRecords[auditKey(tenantId, affiliateId)] ?: return emptyList()
        return synchronized(list) { list.toList() }
    }

    override suspend fun getLatestAuditRecord(
        tenantId: String,
        affiliateId: String
    ): AffiliateCommunicationAuditRecord? {
        val list = auditRecords[auditKey(tenantId, affiliateId)] ?: return null
        return synchronized(list) { list.lastOrNull() }
    }

    // ─────────────────────────────────────────────────────────────────
    // Governance Summary
    // ─────────────────────────────────────────────────────────────────

    override suspend fun getGovernanceSummary(tenantId: String): AffiliateNotificationGovernanceSummary {
        val tenantComms = communications.values.filter { it.tenantId == tenantId }
        val delivered = tenantComms.count { it.isDelivered }.toLong()
        val read = tenantComms.count { it.isRead }.toLong()
        val failed = tenantComms.count { it.status == AffiliateCommunicationStatus.FAILED }.toLong()
        val cancelled = tenantComms.count { it.status == AffiliateCommunicationStatus.CANCELLED }.toLong()
        val pending = tenantComms.count { it.isPending }.toLong()
        val unread = tenantComms.count { it.isDelivered && !it.isRead }.toLong()
        val byType = tenantComms
            .groupBy { it.communicationType.name }
            .mapValues { (_, list) -> list.size.toLong() }

        return AffiliateNotificationGovernanceSummary(
            tenantId = tenantId,
            totalCommunications = tenantComms.size.toLong(),
            deliveredCount = delivered,
            readCount = read,
            failedCount = failed,
            cancelledCount = cancelled,
            pendingCount = pending,
            unreadCount = unread,
            byType = byType
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // Test Utilities
    // ─────────────────────────────────────────────────────────────────

    fun clear() {
        communications.clear()
        communicationsByIdempotencyKey.clear()
        preferences.clear()
        auditRecords.clear()
    }
}
