package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.domain.model.substratereservation.*

/**
 * Service interface for Enterprise Reservation Audit, Reconciliation, and AI Handoff.
 * Module 19 Step 06.
 */
interface SubstrateEnterpriseAuditService {

    suspend fun recordAuditEvent(
        tenantId: String,
        reservationId: String,
        reservationVersion: Long = 1L,
        jobId: String? = null,
        orderId: String,
        orderItemId: String,
        substrateRequirementId: String? = null,
        batchLotId: String? = null,
        warehouseId: String? = null,
        eventType: ReservationAuditEventType,
        previousState: String? = null,
        newState: String,
        actorType: AuditActorType,
        actorId: String,
        role: String,
        permissionContext: String,
        reason: String,
        correlationId: String,
        traceId: String? = null,
        idempotencyKey: String? = null,
        sourceOperation: String
    ): SubstrateEnterpriseAuditRecord

    suspend fun getAuditHistory(tenantId: String, reservationId: String): List<SubstrateEnterpriseAuditRecord>

    suspend fun listAuditEvents(
        tenantId: String,
        orderId: String? = null,
        jobId: String? = null,
        eventType: ReservationAuditEventType? = null,
        limit: Int = 100
    ): List<SubstrateEnterpriseAuditRecord>

    suspend fun reconcileReservation(
        tenantId: String,
        reservationId: String,
        actor: String,
        notes: String? = null
    ): SubstrateReservationReconciliation

    suspend fun getReconciliation(tenantId: String, reconciliationId: String): SubstrateReservationReconciliation?

    suspend fun getLatestReconciliation(tenantId: String, reservationId: String): SubstrateReservationReconciliation?

    suspend fun verifyReservationIntegrity(
        tenantId: String,
        reservationId: String,
        actor: String
    ): SubstrateIntegrityVerificationResult

    suspend fun generateAiHandoffContract(
        tenantId: String,
        reservationId: String,
        actor: String
    ): Module19Step06EnterpriseReservationHandoffContract

    suspend fun getGovernanceSummary(tenantId: String): EnterpriseReservationGovernanceSummary
}
