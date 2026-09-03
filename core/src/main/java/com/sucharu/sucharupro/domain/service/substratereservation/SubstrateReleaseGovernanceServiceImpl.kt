package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.domain.model.substratereservation.*
import com.sucharu.sucharupro.domain.repository.substratereservation.SubstrateReleaseGovernanceRepository
import com.sucharu.sucharupro.domain.repository.substratereservation.SubstrateReservationRepository
import java.util.UUID

/**
 * Implementation of SubstrateReleaseGovernanceService.
 * Module 19 Step 05.
 *
 * Enforces Segregation of Duties: EVALUATE -> APPROVE -> EXECUTE.
 * Reuses canonical Module 19 Step 02 reservation release path without creating shadow inventory.
 */
class SubstrateReleaseGovernanceServiceImpl(
    private val repository: SubstrateReleaseGovernanceRepository,
    private val reservationRepository: SubstrateReservationRepository? = null,
    private val reservationService: SubstrateReservationService? = null
) : SubstrateReleaseGovernanceService {

    override suspend fun evaluateCancellation(
        tenantId: String,
        input: SubstrateReleaseGovernanceEngine.EvaluationInput
    ): SubstrateReleaseGovernanceRecord {
        require(tenantId.isNotBlank()) { "Tenant ID cannot be blank." }

        // Deterministic engine evaluation
        val record = SubstrateReleaseGovernanceEngine.evaluate(input)

        // Idempotency: return existing active evaluation if fingerprint matches
        val existing = repository.findGovernanceRecordByFingerprint(tenantId, record.deduplicationFingerprint)
        if (existing != null && !existing.executionStatus.isTerminal) {
            return existing
        }

        val saved = repository.saveGovernanceRecord(record)

        repository.saveAuditEvent(
            SubstrateReleaseGovernanceAuditEvent(
                eventId = "AUD-${UUID.randomUUID().toString().take(12).uppercase()}",
                governanceId = saved.governanceId,
                tenantId = tenantId,
                action = "EVALUATE_CANCELLATION",
                previousStatus = null,
                newStatus = saved.executionStatus,
                actor = input.evaluator,
                reason = "Cancellation governance evaluated: decision=${saved.decision}, releasable=${saved.releasableSheets} sheets",
                timestamp = System.currentTimeMillis()
            )
        )

        return saved
    }

    override suspend fun evaluateRevision(
        tenantId: String,
        input: SubstrateReleaseGovernanceEngine.EvaluationInput
    ): SubstrateReleaseGovernanceRecord {
        require(tenantId.isNotBlank()) { "Tenant ID cannot be blank." }

        val record = SubstrateReleaseGovernanceEngine.evaluate(input)

        val existing = repository.findGovernanceRecordByFingerprint(tenantId, record.deduplicationFingerprint)
        if (existing != null && !existing.executionStatus.isTerminal) {
            return existing
        }

        val saved = repository.saveGovernanceRecord(record)

        repository.saveAuditEvent(
            SubstrateReleaseGovernanceAuditEvent(
                eventId = "AUD-${UUID.randomUUID().toString().take(12).uppercase()}",
                governanceId = saved.governanceId,
                tenantId = tenantId,
                action = "EVALUATE_REVISION",
                previousStatus = null,
                newStatus = saved.executionStatus,
                actor = input.evaluator,
                reason = "Revision governance evaluated: decision=${saved.decision}, releasable=${saved.releasableSheets}, additional=${saved.additionalRequiredSheets}",
                timestamp = System.currentTimeMillis()
            )
        )

        return saved
    }

    override suspend fun approveRelease(
        tenantId: String,
        governanceId: String,
        actor: String,
        notes: String?
    ): SubstrateReleaseGovernanceRecord {
        val existing = repository.getGovernanceRecordById(tenantId, governanceId)
            ?: throw IllegalArgumentException("Governance record $governanceId not found in tenant $tenantId.")

        check(existing.executionStatus == GovernanceExecutionStatus.EVALUATED) {
            "Only cases in EVALUATED status can be approved. Current: ${existing.executionStatus}."
        }

        check(
            existing.decision == ReleaseGovernanceDecision.RELEASE_ELIGIBLE ||
                existing.decision == ReleaseGovernanceDecision.PARTIAL_RELEASE_ELIGIBLE
        ) {
            "Cannot approve release for decision ${existing.decision}. Blocking reason: ${existing.blockingReason}."
        }

        val now = System.currentTimeMillis()
        val approved = existing.copy(
            executionStatus = GovernanceExecutionStatus.APPROVED,
            approvedBy = actor,
            approvedAt = now,
            notes = if (notes.isNullOrBlank()) existing.notes else "${existing.notes ?: ""}; Approval notes: $notes".trimStart(';', ' ')
        )

        val saved = repository.saveGovernanceRecord(approved)

        repository.saveAuditEvent(
            SubstrateReleaseGovernanceAuditEvent(
                eventId = "AUD-${UUID.randomUUID().toString().take(12).uppercase()}",
                governanceId = governanceId,
                tenantId = tenantId,
                action = "APPROVE_RELEASE",
                previousStatus = GovernanceExecutionStatus.EVALUATED,
                newStatus = GovernanceExecutionStatus.APPROVED,
                actor = actor,
                reason = notes ?: "Approved for substrate release (${saved.releasableSheets} sheets)",
                timestamp = now
            )
        )

        return saved
    }

    override suspend fun executeRelease(
        tenantId: String,
        governanceId: String,
        actor: String
    ): SubstrateReleaseGovernanceRecord {
        val existing = repository.getGovernanceRecordById(tenantId, governanceId)
            ?: throw IllegalArgumentException("Governance record $governanceId not found in tenant $tenantId.")

        check(existing.executionStatus == GovernanceExecutionStatus.APPROVED) {
            "Only APPROVED governance records can be executed. Current status: ${existing.executionStatus}."
        }

        val now = System.currentTimeMillis()

        // Interlock with canonical SubstrateReservationService / Repository (Step 02)
        if (existing.releasableSheets > 0L) {
            if (existing.releasableSheets >= existing.allocatedSheets) {
                // Full release: cancel the reservation
                reservationService?.releaseReservation(
                    tenantId = tenantId,
                    reservationId = existing.reservationId,
                    reason = "Governance Execution #${existing.governanceId}: ${existing.explanation}",
                    actor = actor
                )
            } else if (reservationRepository != null) {
                // Partial release: reduce reserved quantity in canonical reservation
                val res = reservationRepository.getReservationById(tenantId, existing.reservationId)
                if (res != null) {
                    val remaining = maxOf(0L, res.reservedSheets - existing.releasableSheets)
                    val updatedRes = res.copy(
                        reservedSheets = remaining,
                        updatedAt = now,
                        notes = "${res.notes ?: ""}; Partial release ${existing.releasableSheets} sheets executed under ${existing.governanceId} by $actor".trimStart(';', ' ')
                    )
                    reservationRepository.saveReservation(updatedRes)
                }
            }
        }

        val executed = existing.copy(
            executionStatus = GovernanceExecutionStatus.RELEASE_EXECUTED,
            executedBy = actor,
            executedAt = now
        )

        val saved = repository.saveGovernanceRecord(executed)

        repository.saveAuditEvent(
            SubstrateReleaseGovernanceAuditEvent(
                eventId = "AUD-${UUID.randomUUID().toString().take(12).uppercase()}",
                governanceId = governanceId,
                tenantId = tenantId,
                action = "EXECUTE_RELEASE",
                previousStatus = GovernanceExecutionStatus.APPROVED,
                newStatus = GovernanceExecutionStatus.RELEASE_EXECUTED,
                actor = actor,
                reason = "Executed substrate release (${saved.releasableSheets} sheets released, inventory restored)",
                timestamp = now
            )
        )

        return saved
    }

    override suspend fun rejectRelease(
        tenantId: String,
        governanceId: String,
        actor: String,
        reason: String
    ): SubstrateReleaseGovernanceRecord {
        val existing = repository.getGovernanceRecordById(tenantId, governanceId)
            ?: throw IllegalArgumentException("Governance record $governanceId not found in tenant $tenantId.")

        check(!existing.executionStatus.isTerminal) {
            "Cannot reject record in terminal status ${existing.executionStatus}."
        }

        val now = System.currentTimeMillis()
        val rejected = existing.copy(
            executionStatus = GovernanceExecutionStatus.REJECTED,
            notes = "${existing.notes ?: ""}; Rejected by $actor: $reason".trimStart(';', ' ')
        )

        val saved = repository.saveGovernanceRecord(rejected)

        repository.saveAuditEvent(
            SubstrateReleaseGovernanceAuditEvent(
                eventId = "AUD-${UUID.randomUUID().toString().take(12).uppercase()}",
                governanceId = governanceId,
                tenantId = tenantId,
                action = "REJECT_RELEASE",
                previousStatus = existing.executionStatus,
                newStatus = GovernanceExecutionStatus.REJECTED,
                actor = actor,
                reason = reason,
                timestamp = now
            )
        )

        return saved
    }

    override suspend fun getGovernanceRecord(tenantId: String, governanceId: String): SubstrateReleaseGovernanceRecord? {
        return repository.getGovernanceRecordById(tenantId, governanceId)
    }

    override suspend fun listGovernanceRecords(tenantId: String, limit: Int): List<SubstrateReleaseGovernanceRecord> {
        return repository.listGovernanceRecords(tenantId, limit)
    }

    override suspend fun exportHandoffContract(
        tenantId: String,
        governanceId: String
    ): Module19Step05SubstrateReleaseGovernanceHandoffContract {
        val record = repository.getGovernanceRecordById(tenantId, governanceId)
            ?: throw IllegalArgumentException("Governance record $governanceId not found in tenant $tenantId.")

        return Module19Step05SubstrateReleaseGovernanceHandoffContract(
            contractVersion = "5.0.0",
            governanceId = record.governanceId,
            tenantId = record.tenantId,
            reservationId = record.reservationId,
            orderId = record.orderId,
            orderItemId = record.orderItemId,
            executionJobId = record.executionJobId,
            triggerType = record.triggerType.name,
            sku = record.sku,
            materialName = record.materialName,
            warehouseId = record.warehouseId,
            allocatedSheets = record.allocatedSheets,
            consumedSheets = record.consumedSheets,
            committedSheets = record.committedSheets,
            releasableSheets = record.releasableSheets,
            retainedSheets = record.retainedSheets,
            additionalRequiredSheets = record.additionalRequiredSheets,
            decision = record.decision.name,
            executionStatus = record.executionStatus.name,
            blockingReason = record.blockingReason.name,
            explanation = record.explanation,
            deduplicationFingerprint = record.deduplicationFingerprint,
            masterIntegrityHash = record.masterIntegrityHash,
            evaluatedBy = record.evaluatedBy,
            evaluatedAt = record.evaluatedAt,
            approvedBy = record.approvedBy,
            executedBy = record.executedBy
        )
    }
}
