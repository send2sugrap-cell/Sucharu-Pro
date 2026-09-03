package com.sucharu.sucharupro.data.datasource.substratereservation

import com.sucharu.sucharupro.domain.model.substratereservation.*

/**
 * Data source interface for Enterprise Reservation Audit and AI Handoff.
 */
interface SubstrateEnterpriseAuditDataSource {

    suspend fun insertAuditEvent(record: SubstrateEnterpriseAuditRecord): SubstrateEnterpriseAuditRecord

    suspend fun findAuditEventsByReservation(tenantId: String, reservationId: String): List<SubstrateEnterpriseAuditRecord>

    suspend fun findAuditEvents(
        tenantId: String,
        orderId: String? = null,
        jobId: String? = null,
        eventType: ReservationAuditEventType? = null,
        limit: Int = 100
    ): List<SubstrateEnterpriseAuditRecord>

    suspend fun insertReconciliation(reconciliation: SubstrateReservationReconciliation): SubstrateReservationReconciliation

    suspend fun findReconciliationById(tenantId: String, reconciliationId: String): SubstrateReservationReconciliation?

    suspend fun findLatestReconciliationByReservation(tenantId: String, reservationId: String): SubstrateReservationReconciliation?

    suspend fun insertAiHandoffSnapshot(
        tenantId: String,
        handoff: Module19Step06EnterpriseReservationHandoffContract,
        payloadJson: String,
        generatedBy: String
    )

    suspend fun findLatestAiHandoffPayload(tenantId: String, reservationId: String): String?

    suspend fun computeGovernanceSummary(tenantId: String): EnterpriseReservationGovernanceSummary
}
