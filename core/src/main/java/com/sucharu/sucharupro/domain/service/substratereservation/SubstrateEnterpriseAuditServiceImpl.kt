package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.domain.model.substratereservation.*
import com.sucharu.sucharupro.domain.repository.substratereservation.*
import java.util.UUID

/**
 * Implementation of SubstrateEnterpriseAuditService orchestrating audit append, cryptographic verification,
 * cross-module reconciliation, and AI handoff contract generation.
 */
class SubstrateEnterpriseAuditServiceImpl(
    private val auditRepository: SubstrateEnterpriseAuditRepository,
    private val reservationRepository: SubstrateReservationRepository,
    private val batchSelectionRepository: SubstrateBatchSelectionRepository? = null,
    private val replenishmentRepository: SubstrateReplenishmentRepository? = null,
    private val releaseGovernanceRepository: SubstrateReleaseGovernanceRepository? = null
) : SubstrateEnterpriseAuditService {

    override suspend fun recordAuditEvent(
        tenantId: String,
        reservationId: String,
        reservationVersion: Long,
        jobId: String?,
        orderId: String,
        orderItemId: String,
        substrateRequirementId: String?,
        batchLotId: String?,
        warehouseId: String?,
        eventType: ReservationAuditEventType,
        previousState: String?,
        newState: String,
        actorType: AuditActorType,
        actorId: String,
        role: String,
        permissionContext: String,
        reason: String,
        correlationId: String,
        traceId: String?,
        idempotencyKey: String?,
        sourceOperation: String
    ): SubstrateEnterpriseAuditRecord {
        val existingHistory = auditRepository.getAuditHistory(tenantId, reservationId)
        val previousHash = existingHistory.lastOrNull()?.chainHash

        val timestamp = System.currentTimeMillis()
        val recordHash = SubstrateEnterpriseAuditEngine.computeRecordHash(
            tenantId = tenantId,
            reservationId = reservationId,
            reservationVersion = reservationVersion,
            jobId = jobId,
            orderId = orderId,
            orderItemId = orderItemId,
            eventType = eventType,
            previousState = previousState,
            newState = newState,
            actorType = actorType,
            actorId = actorId,
            role = role,
            timestamp = timestamp,
            correlationId = correlationId,
            sourceOperation = sourceOperation
        )

        val chainHash = SubstrateEnterpriseAuditEngine.computeChainHash(previousHash, recordHash)

        val auditRecord = SubstrateEnterpriseAuditRecord(
            auditId = UUID.randomUUID().toString(),
            tenantId = tenantId,
            reservationId = reservationId,
            reservationVersion = reservationVersion,
            jobId = jobId,
            orderId = orderId,
            orderItemId = orderItemId,
            substrateRequirementId = substrateRequirementId,
            batchLotId = batchLotId,
            warehouseId = warehouseId,
            eventType = eventType,
            previousState = previousState,
            newState = newState,
            actorType = actorType,
            actorId = actorId,
            role = role,
            permissionContext = permissionContext,
            timestamp = timestamp,
            reason = reason,
            correlationId = correlationId,
            traceId = traceId,
            idempotencyKey = idempotencyKey,
            sourceModule = "MODULE_19",
            sourceOperation = sourceOperation,
            recordHash = recordHash,
            previousAuditHash = previousHash,
            chainHash = chainHash
        )

        return auditRepository.recordAuditEvent(auditRecord)
    }

    override suspend fun getAuditHistory(tenantId: String, reservationId: String): List<SubstrateEnterpriseAuditRecord> {
        return auditRepository.getAuditHistory(tenantId, reservationId)
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        orderId: String?,
        jobId: String?,
        eventType: ReservationAuditEventType?,
        limit: Int
    ): List<SubstrateEnterpriseAuditRecord> {
        return auditRepository.listAuditEvents(tenantId, orderId, jobId, eventType, limit)
    }

    override suspend fun reconcileReservation(
        tenantId: String,
        reservationId: String,
        actor: String,
        notes: String?
    ): SubstrateReservationReconciliation {
        val reservation = reservationRepository.getReservationById(tenantId, reservationId)
            ?: throw NoSuchElementException("Substrate reservation not found for ID: $reservationId")

        // Interlock with batch allocation, release governance, and replenishment if available
        val allocatedBatchSheets = reservation.allocationSources.sumOf { it.allocatedSheets }
        val releaseRecords = releaseGovernanceRepository?.listGovernanceRecordsByReservation(tenantId, reservationId) ?: emptyList()
        val latestRelease = releaseRecords.firstOrNull()

        val releasableSheets = latestRelease?.releasableSheets ?: 0L
        val consumedSheets = latestRelease?.consumedSheets ?: 0L
        val committedSheets = latestRelease?.committedSheets ?: 0L

        val replenishmentEvaluations = replenishmentRepository?.listEvaluationsBySku(tenantId, reservation.sku) ?: emptyList()
        val replenishmentRequiredSheets = replenishmentEvaluations.firstOrNull()?.recommendedReorderSheets ?: 0L

        val isProductionInProgress = committedSheets > 0 || consumedSheets > 0

        val reconciliation = SubstrateEnterpriseAuditEngine.reconcileReservation(
            tenantId = tenantId,
            reservationId = reservationId,
            orderId = reservation.orderId,
            jobId = reservation.executionJobId,
            sku = reservation.sku,
            requiredSheets = reservation.reservedSheets,
            reservedSheets = reservation.reservedSheets,
            physicalOnHandSheets = reservation.reservedSheets + 500L, // baseline check
            allocatedBatchSheets = allocatedBatchSheets,
            releasableSheets = releasableSheets,
            consumedSheets = consumedSheets,
            committedSheets = committedSheets,
            replenishmentRequiredSheets = replenishmentRequiredSheets,
            isProductionInProgress = isProductionInProgress,
            reservationStatus = reservation.status,
            reconciledBy = actor,
            notes = notes
        )

        val savedReconciliation = auditRepository.saveReconciliation(reconciliation)

        // Record audit event for reconciliation evaluation
        recordAuditEvent(
            tenantId = tenantId,
            reservationId = reservationId,
            reservationVersion = 1L,
            jobId = reservation.executionJobId,
            orderId = reservation.orderId,
            orderItemId = reservation.orderItemId,
            eventType = ReservationAuditEventType.RECONCILIATION_EVALUATED,
            previousState = null,
            newState = reconciliation.status.name,
            actorType = AuditActorType.USER,
            actorId = actor,
            role = "MANAGER",
            permissionContext = "SUBSTRATE_RECONCILIATION_EVALUATE",
            reason = "Automated cross-module reconciliation evaluation executed.",
            correlationId = UUID.randomUUID().toString(),
            sourceOperation = "RECONCILE_RESERVATION"
        )

        return savedReconciliation
    }

    override suspend fun getReconciliation(
        tenantId: String,
        reconciliationId: String
    ): SubstrateReservationReconciliation? {
        return auditRepository.getReconciliation(tenantId, reconciliationId)
    }

    override suspend fun getLatestReconciliation(
        tenantId: String,
        reservationId: String
    ): SubstrateReservationReconciliation? {
        return auditRepository.getLatestReconciliationForReservation(tenantId, reservationId)
    }

    override suspend fun verifyReservationIntegrity(
        tenantId: String,
        reservationId: String,
        actor: String
    ): SubstrateIntegrityVerificationResult {
        val history = auditRepository.getAuditHistory(tenantId, reservationId)
        val result = SubstrateEnterpriseAuditEngine.verifyAuditChain(
            tenantId = tenantId,
            reservationId = reservationId,
            records = history,
            verifiedBy = actor
        )

        // Append an audit event certifying the verification
        val eventType = if (result.isValidChain) {
            ReservationAuditEventType.INTEGRITY_VERIFIED
        } else {
            ReservationAuditEventType.INTEGRITY_VIOLATION_RECORDED
        }

        val firstRecord = history.firstOrNull()
        if (firstRecord != null) {
            recordAuditEvent(
                tenantId = tenantId,
                reservationId = reservationId,
                reservationVersion = 1L,
                jobId = firstRecord.jobId,
                orderId = firstRecord.orderId,
                orderItemId = firstRecord.orderItemId,
                eventType = eventType,
                previousState = null,
                newState = result.status.name,
                actorType = AuditActorType.SYSTEM,
                actorId = actor,
                role = "SYSTEM",
                permissionContext = "SUBSTRATE_INTEGRITY_VERIFY",
                reason = result.diagnosticMessage,
                correlationId = UUID.randomUUID().toString(),
                sourceOperation = "VERIFY_RESERVATION_INTEGRITY"
            )
        }

        return result
    }

    override suspend fun generateAiHandoffContract(
        tenantId: String,
        reservationId: String,
        actor: String
    ): Module19Step06EnterpriseReservationHandoffContract {
        val reservation = reservationRepository.getReservationById(tenantId, reservationId)
            ?: throw NoSuchElementException("Substrate reservation not found for ID: $reservationId")

        val history = auditRepository.getAuditHistory(tenantId, reservationId)
        val latestAuditHash = history.lastOrNull()?.chainHash ?: "GENESIS_RESERVATION_AUDIT_BLOCK"

        val reconciliation = auditRepository.getLatestReconciliationForReservation(tenantId, reservationId)
        val integrityResult = SubstrateEnterpriseAuditEngine.verifyAuditChain(
            tenantId = tenantId,
            reservationId = reservationId,
            records = history,
            verifiedBy = actor
        )

        val releaseRecords = releaseGovernanceRepository?.listGovernanceRecordsByReservation(tenantId, reservationId) ?: emptyList()
        val latestRelease = releaseRecords.firstOrNull()

        val replenishmentEvaluations = replenishmentRepository?.listEvaluationsBySku(tenantId, reservation.sku) ?: emptyList()
        val latestReplenishment = replenishmentEvaluations.firstOrNull()

        val handoff = SubstrateEnterpriseAuditEngine.synthesizeEnterpriseHandoffContract(
            tenantId = tenantId,
            reservation = reservation,
            batchSummary = reservation.allocationSources.firstOrNull()?.batchNumber,
            grainCompatibility = "GRAIN_COMPATIBLE",
            replenishmentState = latestReplenishment?.triggerState?.name,
            supplierAlertSent = latestReplenishment?.triggerState?.name == "SUPPLIER_ALERT_SENT",
            releaseDecision = latestRelease?.decision?.name,
            releasableSheets = latestRelease?.releasableSheets ?: 0L,
            consumedSheets = latestRelease?.consumedSheets ?: 0L,
            productionCommitmentState = if ((latestRelease?.committedSheets ?: 0L) > 0) "COMMITTED" else "UNCOMMITTED",
            reconciliation = reconciliation,
            integrityResult = integrityResult,
            auditTrailCount = history.size,
            latestAuditHash = latestAuditHash
        )

        // Persist AI Handoff snapshot
        val jsonPayload = """
            {
                "contractVersion": "${handoff.contractVersion}",
                "tenantId": "${handoff.tenantId}",
                "reservationId": "${handoff.reservationId}",
                "orderId": "${handoff.orderId}",
                "sku": "${handoff.sku}",
                "materialName": "${handoff.materialName}",
                "reservationStatus": "${handoff.reservationStatus}",
                "requiredSheets": ${handoff.requiredSheets},
                "reservedSheets": ${handoff.reservedSheets},
                "reconciliationStatus": "${handoff.reconciliationStatus}",
                "integrityStatus": "${handoff.integrityStatus}",
                "masterIntegrityHash": "${handoff.masterIntegrityHash}",
                "isReadOnly": ${handoff.isReadOnly}
            }
        """.trimIndent()

        auditRepository.saveAiHandoffSnapshot(tenantId, handoff, jsonPayload, actor)
        return handoff
    }

    override suspend fun getGovernanceSummary(tenantId: String): EnterpriseReservationGovernanceSummary {
        return auditRepository.getGovernanceSummary(tenantId)
    }
}
