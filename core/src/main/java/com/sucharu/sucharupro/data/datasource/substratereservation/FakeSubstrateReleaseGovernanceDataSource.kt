package com.sucharu.sucharupro.data.datasource.substratereservation

import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateReleaseGovernanceAuditEvent
import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateReleaseGovernanceRecord
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory test fake DataSource for Substrate Release & Revision Governance.
 * Module 19 Step 05.
 */
class FakeSubstrateReleaseGovernanceDataSource : SubstrateReleaseGovernanceDataSource {

    private val records = ConcurrentHashMap<String, SubstrateReleaseGovernanceRecord>()
    private val audits = ConcurrentHashMap<String, SubstrateReleaseGovernanceAuditEvent>()

    override suspend fun saveGovernanceRecord(record: SubstrateReleaseGovernanceRecord): SubstrateReleaseGovernanceRecord {
        records[record.governanceId] = record
        return record
    }

    override suspend fun getGovernanceRecordById(tenantId: String, governanceId: String): SubstrateReleaseGovernanceRecord? {
        val r = records[governanceId]
        return if (r != null && r.tenantId == tenantId) r else null
    }

    override suspend fun findGovernanceRecordByFingerprint(tenantId: String, fingerprint: String): SubstrateReleaseGovernanceRecord? {
        return records.values.firstOrNull { it.tenantId == tenantId && it.deduplicationFingerprint == fingerprint }
    }

    override suspend fun listGovernanceRecordsByReservation(tenantId: String, reservationId: String): List<SubstrateReleaseGovernanceRecord> {
        return records.values
            .filter { it.tenantId == tenantId && it.reservationId == reservationId }
            .sortedByDescending { it.evaluatedAt }
    }

    override suspend fun listGovernanceRecordsByOrder(tenantId: String, orderId: String): List<SubstrateReleaseGovernanceRecord> {
        return records.values
            .filter { it.tenantId == tenantId && it.orderId == orderId }
            .sortedByDescending { it.evaluatedAt }
    }

    override suspend fun listGovernanceRecords(tenantId: String, limit: Int): List<SubstrateReleaseGovernanceRecord> {
        return records.values
            .filter { it.tenantId == tenantId }
            .sortedByDescending { it.evaluatedAt }
            .take(limit)
    }

    override suspend fun saveAuditEvent(event: SubstrateReleaseGovernanceAuditEvent): SubstrateReleaseGovernanceAuditEvent {
        audits[event.eventId] = event
        return event
    }

    override suspend fun listAuditEvents(tenantId: String, governanceId: String): List<SubstrateReleaseGovernanceAuditEvent> {
        return audits.values
            .filter { it.tenantId == tenantId && it.governanceId == governanceId }
            .sortedBy { it.timestamp }
    }

    fun clear() {
        records.clear()
        audits.clear()
    }
}
