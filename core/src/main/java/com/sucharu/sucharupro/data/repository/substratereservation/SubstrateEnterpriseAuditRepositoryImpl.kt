package com.sucharu.sucharupro.data.repository.substratereservation

import com.sucharu.sucharupro.data.datasource.substratereservation.SubstrateEnterpriseAuditDataSource
import com.sucharu.sucharupro.domain.model.substratereservation.*
import com.sucharu.sucharupro.domain.repository.substratereservation.SubstrateEnterpriseAuditRepository

/**
 * Implementation of SubstrateEnterpriseAuditRepository delegating to SubstrateEnterpriseAuditDataSource.
 * Module 19 Step 06.
 */
class SubstrateEnterpriseAuditRepositoryImpl(
    private val dataSource: SubstrateEnterpriseAuditDataSource
) : SubstrateEnterpriseAuditRepository {

    override suspend fun recordAuditEvent(record: SubstrateEnterpriseAuditRecord): SubstrateEnterpriseAuditRecord {
        return dataSource.insertAuditEvent(record)
    }

    override suspend fun getAuditHistory(tenantId: String, reservationId: String): List<SubstrateEnterpriseAuditRecord> {
        return dataSource.findAuditEventsByReservation(tenantId, reservationId)
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        orderId: String?,
        jobId: String?,
        eventType: ReservationAuditEventType?,
        limit: Int
    ): List<SubstrateEnterpriseAuditRecord> {
        return dataSource.findAuditEvents(tenantId, orderId, jobId, eventType, limit)
    }

    override suspend fun saveReconciliation(
        reconciliation: SubstrateReservationReconciliation
    ): SubstrateReservationReconciliation {
        return dataSource.insertReconciliation(reconciliation)
    }

    override suspend fun getReconciliation(
        tenantId: String,
        reconciliationId: String
    ): SubstrateReservationReconciliation? {
        return dataSource.findReconciliationById(tenantId, reconciliationId)
    }

    override suspend fun getLatestReconciliationForReservation(
        tenantId: String,
        reservationId: String
    ): SubstrateReservationReconciliation? {
        return dataSource.findLatestReconciliationByReservation(tenantId, reservationId)
    }

    override suspend fun saveAiHandoffSnapshot(
        tenantId: String,
        handoff: Module19Step06EnterpriseReservationHandoffContract,
        payloadJson: String,
        generatedBy: String
    ) {
        dataSource.insertAiHandoffSnapshot(tenantId, handoff, payloadJson, generatedBy)
    }

    override suspend fun getLatestAiHandoffSnapshot(tenantId: String, reservationId: String): String? {
        return dataSource.findLatestAiHandoffPayload(tenantId, reservationId)
    }

    override suspend fun getGovernanceSummary(tenantId: String): EnterpriseReservationGovernanceSummary {
        return dataSource.computeGovernanceSummary(tenantId)
    }
}
