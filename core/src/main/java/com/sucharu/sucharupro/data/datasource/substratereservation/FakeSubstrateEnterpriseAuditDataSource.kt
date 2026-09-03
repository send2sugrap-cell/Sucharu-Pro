package com.sucharu.sucharupro.data.datasource.substratereservation

import com.sucharu.sucharupro.domain.model.substratereservation.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe, multi-tenant in-memory test fake data source for SubstrateEnterpriseAudit.
 */
class FakeSubstrateEnterpriseAuditDataSource : SubstrateEnterpriseAuditDataSource {

    private val auditLogs = ConcurrentHashMap<String, MutableList<SubstrateEnterpriseAuditRecord>>()
    private val reconciliations = ConcurrentHashMap<String, MutableMap<String, SubstrateReservationReconciliation>>()
    private val handoffSnapshots = ConcurrentHashMap<String, MutableMap<String, String>>()

    override suspend fun insertAuditEvent(record: SubstrateEnterpriseAuditRecord): SubstrateEnterpriseAuditRecord {
        val tenantAudits = auditLogs.computeIfAbsent(record.tenantId) { mutableListOf() }
        synchronized(tenantAudits) {
            tenantAudits.add(record)
        }
        return record
    }

    override suspend fun findAuditEventsByReservation(
        tenantId: String,
        reservationId: String
    ): List<SubstrateEnterpriseAuditRecord> {
        val tenantAudits = auditLogs[tenantId] ?: return emptyList()
        synchronized(tenantAudits) {
            return tenantAudits.filter { it.reservationId == reservationId }.sortedBy { it.timestamp }
        }
    }

    override suspend fun findAuditEvents(
        tenantId: String,
        orderId: String?,
        jobId: String?,
        eventType: ReservationAuditEventType?,
        limit: Int
    ): List<SubstrateEnterpriseAuditRecord> {
        val tenantAudits = auditLogs[tenantId] ?: return emptyList()
        synchronized(tenantAudits) {
            return tenantAudits.asSequence()
                .filter { orderId == null || it.orderId == orderId }
                .filter { jobId == null || it.jobId == jobId }
                .filter { eventType == null || it.eventType == eventType }
                .sortedByDescending { it.timestamp }
                .take(limit)
                .toList()
        }
    }

    override suspend fun insertReconciliation(
        reconciliation: SubstrateReservationReconciliation
    ): SubstrateReservationReconciliation {
        val tenantRecons = reconciliations.computeIfAbsent(reconciliation.tenantId) { ConcurrentHashMap() }
        tenantRecons[reconciliation.reconciliationId] = reconciliation
        return reconciliation
    }

    override suspend fun findReconciliationById(
        tenantId: String,
        reconciliationId: String
    ): SubstrateReservationReconciliation? {
        return reconciliations[tenantId]?.get(reconciliationId)
    }

    override suspend fun findLatestReconciliationByReservation(
        tenantId: String,
        reservationId: String
    ): SubstrateReservationReconciliation? {
        val tenantRecons = reconciliations[tenantId] ?: return null
        return tenantRecons.values
            .filter { it.reservationId == reservationId }
            .maxByOrNull { it.reconciledAt }
    }

    override suspend fun insertAiHandoffSnapshot(
        tenantId: String,
        handoff: Module19Step06EnterpriseReservationHandoffContract,
        payloadJson: String,
        generatedBy: String
    ) {
        val tenantHandoffs = handoffSnapshots.computeIfAbsent(tenantId) { ConcurrentHashMap() }
        tenantHandoffs[handoff.reservationId] = payloadJson
    }

    override suspend fun findLatestAiHandoffPayload(
        tenantId: String,
        reservationId: String
    ): String? {
        return handoffSnapshots[tenantId]?.get(reservationId)
    }

    override suspend fun computeGovernanceSummary(tenantId: String): EnterpriseReservationGovernanceSummary {
        val tenantAudits = auditLogs[tenantId] ?: emptyList()
        val distinctReservations = tenantAudits.map { it.reservationId }.distinct()

        val tenantRecons = reconciliations[tenantId]?.values ?: emptyList()
        val healthyRecons = tenantRecons.count { it.status == ReconciliationStatus.HEALTHY }.toLong()
        val discrepancyRecons = tenantRecons.count { it.status == ReconciliationStatus.DISCREPANCIES_DETECTED || it.status == ReconciliationStatus.WARNING_DETECTED }.toLong()

        return EnterpriseReservationGovernanceSummary(
            totalReservationsAudited = distinctReservations.size.toLong(),
            activeHardAllocations = tenantAudits.count { it.eventType == ReservationAuditEventType.HARD_ALLOCATED }.toLong(),
            activeSoftReservations = tenantAudits.count { it.eventType == ReservationAuditEventType.SOFT_RESERVED }.toLong(),
            reconciledHealthyCount = healthyRecons,
            discrepanciesDetectedCount = discrepancyRecons,
            integrityVerifiedIntactCount = distinctReservations.size.toLong(),
            integrityViolationsCount = 0L,
            pendingReplenishmentAlertsCount = tenantAudits.count { it.eventType == ReservationAuditEventType.REPLENISHMENT_EVALUATED }.toLong(),
            activeReleaseReviewsCount = tenantAudits.count { it.eventType == ReservationAuditEventType.RELEASE_EVALUATED }.toLong()
        )
    }

    fun clear() {
        auditLogs.clear()
        reconciliations.clear()
        handoffSnapshots.clear()
    }
}
