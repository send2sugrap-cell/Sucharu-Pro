package com.sucharu.sucharupro.domain.repository.substratereservation

import com.sucharu.sucharupro.domain.model.substratereservation.*

/**
 * Enterprise Audit, Reconciliation, and AI Handoff Repository for Module 19.
 */
interface SubstrateEnterpriseAuditRepository {

    suspend fun recordAuditEvent(record: SubstrateEnterpriseAuditRecord): SubstrateEnterpriseAuditRecord

    suspend fun getAuditHistory(tenantId: String, reservationId: String): List<SubstrateEnterpriseAuditRecord>

    suspend fun listAuditEvents(
        tenantId: String,
        orderId: String? = null,
        jobId: String? = null,
        eventType: ReservationAuditEventType? = null,
        limit: Int = 100
    ): List<SubstrateEnterpriseAuditRecord>

    suspend fun saveReconciliation(reconciliation: SubstrateReservationReconciliation): SubstrateReservationReconciliation

    suspend fun getReconciliation(tenantId: String, reconciliationId: String): SubstrateReservationReconciliation?

    suspend fun getLatestReconciliationForReservation(tenantId: String, reservationId: String): SubstrateReservationReconciliation?

    suspend fun saveAiHandoffSnapshot(
        tenantId: String,
        handoff: Module19Step06EnterpriseReservationHandoffContract,
        payloadJson: String,
        generatedBy: String
    )

    suspend fun getLatestAiHandoffSnapshot(tenantId: String, reservationId: String): String?

    suspend fun getGovernanceSummary(tenantId: String): EnterpriseReservationGovernanceSummary
}
