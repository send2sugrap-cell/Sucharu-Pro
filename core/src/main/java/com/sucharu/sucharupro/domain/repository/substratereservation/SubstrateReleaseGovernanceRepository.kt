package com.sucharu.sucharupro.domain.repository.substratereservation

import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateReleaseGovernanceAuditEvent
import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateReleaseGovernanceRecord

/**
 * Domain repository interface for Substrate Release & Revision Governance.
 * Module 19 Step 05.
 */
interface SubstrateReleaseGovernanceRepository {

    suspend fun saveGovernanceRecord(record: SubstrateReleaseGovernanceRecord): SubstrateReleaseGovernanceRecord

    suspend fun getGovernanceRecordById(tenantId: String, governanceId: String): SubstrateReleaseGovernanceRecord?

    suspend fun findGovernanceRecordByFingerprint(tenantId: String, fingerprint: String): SubstrateReleaseGovernanceRecord?

    suspend fun listGovernanceRecordsByReservation(tenantId: String, reservationId: String): List<SubstrateReleaseGovernanceRecord>

    suspend fun listGovernanceRecordsByOrder(tenantId: String, orderId: String): List<SubstrateReleaseGovernanceRecord>

    suspend fun listGovernanceRecords(tenantId: String, limit: Int = 50): List<SubstrateReleaseGovernanceRecord>

    suspend fun saveAuditEvent(event: SubstrateReleaseGovernanceAuditEvent): SubstrateReleaseGovernanceAuditEvent

    suspend fun listAuditEvents(tenantId: String, governanceId: String): List<SubstrateReleaseGovernanceAuditEvent>
}
