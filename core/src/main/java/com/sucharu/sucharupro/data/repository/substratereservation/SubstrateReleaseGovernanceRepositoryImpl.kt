package com.sucharu.sucharupro.data.repository.substratereservation

import com.sucharu.sucharupro.data.datasource.substratereservation.SubstrateReleaseGovernanceDataSource
import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateReleaseGovernanceAuditEvent
import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateReleaseGovernanceRecord
import com.sucharu.sucharupro.domain.repository.substratereservation.SubstrateReleaseGovernanceRepository

/**
 * Implementation of SubstrateReleaseGovernanceRepository delegating to SubstrateReleaseGovernanceDataSource.
 * Module 19 Step 05.
 */
class SubstrateReleaseGovernanceRepositoryImpl(
    private val dataSource: SubstrateReleaseGovernanceDataSource
) : SubstrateReleaseGovernanceRepository {

    override suspend fun saveGovernanceRecord(record: SubstrateReleaseGovernanceRecord): SubstrateReleaseGovernanceRecord {
        return dataSource.saveGovernanceRecord(record)
    }

    override suspend fun getGovernanceRecordById(tenantId: String, governanceId: String): SubstrateReleaseGovernanceRecord? {
        return dataSource.getGovernanceRecordById(tenantId, governanceId)
    }

    override suspend fun findGovernanceRecordByFingerprint(tenantId: String, fingerprint: String): SubstrateReleaseGovernanceRecord? {
        return dataSource.findGovernanceRecordByFingerprint(tenantId, fingerprint)
    }

    override suspend fun listGovernanceRecordsByReservation(tenantId: String, reservationId: String): List<SubstrateReleaseGovernanceRecord> {
        return dataSource.listGovernanceRecordsByReservation(tenantId, reservationId)
    }

    override suspend fun listGovernanceRecordsByOrder(tenantId: String, orderId: String): List<SubstrateReleaseGovernanceRecord> {
        return dataSource.listGovernanceRecordsByOrder(tenantId, orderId)
    }

    override suspend fun listGovernanceRecords(tenantId: String, limit: Int): List<SubstrateReleaseGovernanceRecord> {
        return dataSource.listGovernanceRecords(tenantId, limit)
    }

    override suspend fun saveAuditEvent(event: SubstrateReleaseGovernanceAuditEvent): SubstrateReleaseGovernanceAuditEvent {
        return dataSource.saveAuditEvent(event)
    }

    override suspend fun listAuditEvents(tenantId: String, governanceId: String): List<SubstrateReleaseGovernanceAuditEvent> {
        return dataSource.listAuditEvents(tenantId, governanceId)
    }
}
