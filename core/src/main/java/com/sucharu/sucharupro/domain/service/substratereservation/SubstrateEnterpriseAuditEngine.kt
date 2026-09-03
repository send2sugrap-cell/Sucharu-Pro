package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.domain.model.substratereservation.*
import java.security.MessageDigest
import java.util.UUID

/**
 * Domain engine for Module 19 Step 06 Enterprise Reservation Audit, Reconciliation, and AI Handoff.
 */
object SubstrateEnterpriseAuditEngine {

    /**
     * Compute SHA-256 hash of core audit event record fields.
     */
    fun computeRecordHash(
        tenantId: String,
        reservationId: String,
        reservationVersion: Long,
        jobId: String?,
        orderId: String,
        orderItemId: String,
        eventType: ReservationAuditEventType,
        previousState: String?,
        newState: String,
        actorType: AuditActorType,
        actorId: String,
        role: String,
        timestamp: Long,
        correlationId: String,
        sourceOperation: String
    ): String {
        val payload = "$tenantId|$reservationId|$reservationVersion|${jobId ?: ""}|$orderId|$orderItemId|${eventType.name}|${previousState ?: ""}|$newState|${actorType.name}|$actorId|$role|$timestamp|$correlationId|$sourceOperation"
        return sha256(payload)
    }

    /**
     * Compute cryptographically chained hash connecting previous audit event to current record hash.
     */
    fun computeChainHash(previousAuditHash: String?, recordHash: String): String {
        val prev = previousAuditHash ?: "GENESIS_RESERVATION_AUDIT_BLOCK"
        return sha256("$prev|$recordHash")
    }

    /**
     * Verifies the cryptographic integrity of an ordered audit history chain.
     */
    fun verifyAuditChain(
        tenantId: String,
        reservationId: String,
        records: List<SubstrateEnterpriseAuditRecord>,
        verifiedBy: String
    ): SubstrateIntegrityVerificationResult {
        if (records.isEmpty()) {
            return SubstrateIntegrityVerificationResult(
                verificationId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                reservationId = reservationId,
                totalAuditRecords = 0,
                status = IntegrityVerificationStatus.INTACT,
                isValidChain = true,
                isMasterHashValid = true,
                tamperedRecordIds = emptyList(),
                verifiedBy = verifiedBy,
                diagnosticMessage = "Audit log is empty for reservation $reservationId. Genesis state intact."
            )
        }

        val sortedRecords = records.sortedBy { it.timestamp }
        val tamperedIds = mutableListOf<String>()
        var expectedPreviousHash: String? = null

        for ((index, record) in sortedRecords.withIndex()) {
            // Verify tenant ownership
            if (record.tenantId != tenantId) {
                tamperedIds.add(record.auditId)
                continue
            }

            // Verify record internal hash
            val recalculatedRecordHash = computeRecordHash(
                tenantId = record.tenantId,
                reservationId = record.reservationId,
                reservationVersion = record.reservationVersion,
                jobId = record.jobId,
                orderId = record.orderId,
                orderItemId = record.orderItemId,
                eventType = record.eventType,
                previousState = record.previousState,
                newState = record.newState,
                actorType = record.actorType,
                actorId = record.actorId,
                role = record.role,
                timestamp = record.timestamp,
                correlationId = record.correlationId,
                sourceOperation = record.sourceOperation
            )

            if (recalculatedRecordHash != record.recordHash) {
                tamperedIds.add(record.auditId)
                continue
            }

            // Verify chained hash
            if (index == 0) {
                if (record.previousAuditHash != null && record.previousAuditHash != "GENESIS_RESERVATION_AUDIT_BLOCK") {
                    tamperedIds.add(record.auditId)
                }
            } else {
                if (record.previousAuditHash != expectedPreviousHash) {
                    tamperedIds.add(record.auditId)
                }
            }

            val recalculatedChainHash = computeChainHash(record.previousAuditHash, record.recordHash)
            if (recalculatedChainHash != record.chainHash) {
                tamperedIds.add(record.auditId)
            }

            expectedPreviousHash = record.chainHash
        }

        val isValid = tamperedIds.isEmpty()
        val status = if (isValid) {
            IntegrityVerificationStatus.INTACT
        } else {
            IntegrityVerificationStatus.TAMPER_DETECTED
        }

        val message = if (isValid) {
            "Verified ${records.size} sequential audit records for reservation $reservationId. Cryptographic chain is INTACT."
        } else {
            "INTEGRITY VIOLATION detected in ${tamperedIds.size} audit record(s) for reservation $reservationId: $tamperedIds"
        }

        return SubstrateIntegrityVerificationResult(
            verificationId = UUID.randomUUID().toString(),
            tenantId = tenantId,
            reservationId = reservationId,
            totalAuditRecords = records.size,
            status = status,
            isValidChain = isValid,
            isMasterHashValid = isValid,
            tamperedRecordIds = tamperedIds,
            verifiedBy = verifiedBy,
            diagnosticMessage = message
        )
    }

    /**
     * Deterministic multi-dimensional cross-module reconciliation.
     */
    fun reconcileReservation(
        tenantId: String,
        reservationId: String,
        orderId: String,
        jobId: String?,
        sku: String,
        requiredSheets: Long,
        reservedSheets: Long,
        physicalOnHandSheets: Long,
        allocatedBatchSheets: Long,
        releasableSheets: Long,
        consumedSheets: Long,
        committedSheets: Long,
        replenishmentRequiredSheets: Long,
        isProductionInProgress: Boolean,
        reservationStatus: SubstrateReservationStatus,
        reconciledBy: String,
        notes: String? = null
    ): SubstrateReservationReconciliation {
        val discrepancies = mutableListOf<SubstrateReconciliationDiscrepancy>()
        val reconId = UUID.randomUUID().toString()

        // Check 1: Quantity mismatch between required and reserved
        if (reservationStatus == SubstrateReservationStatus.ALLOCATED_HARD && reservedSheets < requiredSheets) {
            discrepancies.add(
                SubstrateReconciliationDiscrepancy(
                    discrepancyId = UUID.randomUUID().toString(),
                    reconciliationId = reconId,
                    tenantId = tenantId,
                    discrepancyType = ReconciliationDiscrepancyType.QUANTITY_MISMATCH,
                    severity = ReconciliationDiscrepancySeverity.CRITICAL,
                    fieldOrContext = "reservedSheets",
                    expectedValue = "$requiredSheets",
                    actualValue = "$reservedSheets",
                    explanation = "Hard allocated sheets ($reservedSheets) is less than required sheets ($requiredSheets).",
                    resolutionRecommendation = "Evaluate inventory availability and allocate additional lot batches or trigger replenishment."
                )
            )
        }

        // Check 2: Physical inventory deficit
        if (physicalOnHandSheets < reservedSheets && reservationStatus.isActiveHold) {
            discrepancies.add(
                SubstrateReconciliationDiscrepancy(
                    discrepancyId = UUID.randomUUID().toString(),
                    reconciliationId = reconId,
                    tenantId = tenantId,
                    discrepancyType = ReconciliationDiscrepancyType.MISSING_INVENTORY_REFERENCE,
                    severity = ReconciliationDiscrepancySeverity.CRITICAL,
                    fieldOrContext = "physicalOnHandSheets",
                    expectedValue = ">= $reservedSheets",
                    actualValue = "$physicalOnHandSheets",
                    explanation = "Physical on-hand inventory ($physicalOnHandSheets) is insufficient to cover reserved hold ($reservedSheets).",
                    resolutionRecommendation = "Perform stock count reconciliation or expedite pending supplier inbound PO."
                )
            )
        }

        // Check 3: Consumed but still marked reserved
        if (consumedSheets >= reservedSheets && reservedSheets > 0 && reservationStatus == SubstrateReservationStatus.ALLOCATED_HARD) {
            discrepancies.add(
                SubstrateReconciliationDiscrepancy(
                    discrepancyId = UUID.randomUUID().toString(),
                    reconciliationId = reconId,
                    tenantId = tenantId,
                    discrepancyType = ReconciliationDiscrepancyType.CONSUMED_BUT_RESERVED,
                    severity = ReconciliationDiscrepancySeverity.WARNING,
                    fieldOrContext = "reservationStatus",
                    expectedValue = "ISSUED_TO_FLOOR",
                    actualValue = reservationStatus.name,
                    explanation = "Material consumed ($consumedSheets) meets or exceeds reservation ($reservedSheets), but status is still ALLOCATED_HARD.",
                    resolutionRecommendation = "Synchronize execution actuals from Module 17 and transition reservation to ISSUED_TO_FLOOR."
                )
            )
        }

        // Check 4: Production commitment conflict
        if (isProductionInProgress && releasableSheets > 0 && committedSheets == 0L) {
            discrepancies.add(
                SubstrateReconciliationDiscrepancy(
                    discrepancyId = UUID.randomUUID().toString(),
                    reconciliationId = reconId,
                    tenantId = tenantId,
                    discrepancyType = ReconciliationDiscrepancyType.PRODUCTION_COMMITMENT_CONFLICT,
                    severity = ReconciliationDiscrepancySeverity.WARNING,
                    fieldOrContext = "committedSheets",
                    expectedValue = "> 0",
                    actualValue = "0",
                    explanation = "Production is actively in progress on shop floor, but committed sheets is recorded as 0.",
                    resolutionRecommendation = "Verify shop floor job card commitment with Module 17 execution queue."
                )
            )
        }

        // Check 5: Replenishment inconsistency
        val netStock = physicalOnHandSheets - reservedSheets
        if (netStock < 0 && replenishmentRequiredSheets == 0L && reservationStatus.isActiveHold) {
            discrepancies.add(
                SubstrateReconciliationDiscrepancy(
                    discrepancyId = UUID.randomUUID().toString(),
                    reconciliationId = reconId,
                    tenantId = tenantId,
                    discrepancyType = ReconciliationDiscrepancyType.REPLENISHMENT_INCONSISTENCY,
                    severity = ReconciliationDiscrepancySeverity.WARNING,
                    fieldOrContext = "replenishmentRequiredSheets",
                    expectedValue = "> 0",
                    actualValue = "0",
                    explanation = "Net available stock ($netStock) is negative, but replenishment evaluation has not requested reorder sheets.",
                    resolutionRecommendation = "Run Module 19 Step 04 Auto-Replenishment evaluation for SKU $sku."
                )
            )
        }

        val status = when {
            discrepancies.any { it.severity == ReconciliationDiscrepancySeverity.CRITICAL } -> ReconciliationStatus.DISCREPANCIES_DETECTED
            discrepancies.isNotEmpty() -> ReconciliationStatus.WARNING_DETECTED
            else -> ReconciliationStatus.HEALTHY
        }

        val integrityHash = sha256("$tenantId|$reservationId|$sku|$requiredSheets|$reservedSheets|$physicalOnHandSheets|$allocatedBatchSheets|$status|${discrepancies.size}")

        return SubstrateReservationReconciliation(
            reconciliationId = reconId,
            tenantId = tenantId,
            reservationId = reservationId,
            orderId = orderId,
            jobId = jobId,
            sku = sku,
            requiredSheets = requiredSheets,
            reservedSheets = reservedSheets,
            physicalOnHandSheets = physicalOnHandSheets,
            allocatedBatchSheets = allocatedBatchSheets,
            releasableSheets = releasableSheets,
            consumedSheets = consumedSheets,
            committedSheets = committedSheets,
            replenishmentRequiredSheets = replenishmentRequiredSheets,
            status = status,
            discrepancies = discrepancies,
            reconciledBy = reconciledBy,
            reconciledAt = System.currentTimeMillis(),
            integrityHash = integrityHash,
            notes = notes
        )
    }

    /**
     * Synthesize authoritative downstream AI Handoff Contract for Module 19 (v6.0.0).
     */
    fun synthesizeEnterpriseHandoffContract(
        tenantId: String,
        reservation: SubstrateReservation,
        batchSummary: String? = null,
        grainCompatibility: String? = null,
        replenishmentState: String? = null,
        supplierAlertSent: Boolean = false,
        releaseDecision: String? = null,
        releasableSheets: Long = 0L,
        consumedSheets: Long = 0L,
        productionCommitmentState: String? = null,
        reconciliation: SubstrateReservationReconciliation?,
        integrityResult: SubstrateIntegrityVerificationResult?,
        auditTrailCount: Int,
        latestAuditHash: String
    ): Module19Step06EnterpriseReservationHandoffContract {
        val reconStatus = reconciliation?.status?.name ?: "UNRECONCILED"
        val activeDiscrepanciesCount = reconciliation?.discrepancies?.size ?: 0
        val integrityStatus = integrityResult?.status?.name ?: "UNVERIFIED"

        val allowedActions = listOf(
            "INSPECT_RESERVATION_AUDIT_TRAIL",
            "EVALUATE_RECONCILIATION",
            "ANALYZE_REPLENISHMENT_OPTIONS",
            "RECOMMEND_OPTIMIZED_BATCH_LOT",
            "EXPLAIN_GOVERNANCE_DECISION"
        )

        val forbiddenActions = listOf(
            "MUTATE_RESERVATION_STATE",
            "EXECUTE_SUBSTRATE_RELEASE",
            "DISPATCH_SUPPLIER_REORDER_ALERT",
            "MUTATE_PHYSICAL_INVENTORY",
            "BYPASS_ROW_LEVEL_SECURITY",
            "REWRITE_AUDIT_HISTORY"
        )

        val recommendedActions = mutableListOf<String>()
        if (reconStatus == "DISCREPANCIES_DETECTED" || reconStatus == "WARNING_DETECTED") {
            recommendedActions.add("Human manager review required for active reconciliation discrepancies.")
        }
        if (integrityStatus == "TAMPER_DETECTED" || integrityStatus == "CHAIN_BROKEN") {
            recommendedActions.add("Security incident alert: Audit chain integrity violation detected.")
        }
        if (replenishmentState == "REORDER_TRIGGERED" && !supplierAlertSent) {
            recommendedActions.add("Evaluate and dispatch supplier replenishment alert.")
        }
        if (releaseDecision == "FULL_RELEASE_ELIGIBLE" || releaseDecision == "PARTIAL_RELEASE_ELIGIBLE") {
            recommendedActions.add("Approve and execute substrate release back to unallocated stock.")
        }
        if (recommendedActions.isEmpty()) {
            recommendedActions.add("All reservation parameters reconciled and operating normally.")
        }

        val masterHash = sha256(
            "$tenantId|${reservation.reservationId}|${reservation.sku}|${reservation.reservedSheets}|${reservation.reservedSheets}|" +
            "${reservation.status.name}|$reconStatus|$integrityStatus|$latestAuditHash"
        )

        return Module19Step06EnterpriseReservationHandoffContract(
            contractVersion = "6.0.0",
            tenantId = tenantId,
            reservationId = reservation.reservationId,
            orderId = reservation.orderId,
            jobId = reservation.executionJobId,
            sku = reservation.sku,
            materialName = reservation.productName,
            warehouseId = reservation.warehouseId,
            reservationStatus = reservation.status.name,
            requiredSheets = reservation.reservedSheets,
            reservedSheets = reservation.reservedSheets,
            allocatedHardSheets = if (reservation.mode == SubstrateReservationMode.HARD) reservation.reservedSheets else 0L,
            softReservedSheets = if (reservation.mode == SubstrateReservationMode.SOFT) reservation.reservedSheets else 0L,
            batchLotSelectionSummary = batchSummary,
            grainDirectionCompatibility = grainCompatibility,
            replenishmentTriggerState = replenishmentState,
            supplierAlertDispatched = supplierAlertSent,
            releaseGovernanceDecision = releaseDecision,
            releasableSheets = releasableSheets,
            consumedSheets = consumedSheets,
            productionCommitmentState = productionCommitmentState,
            reconciliationStatus = reconStatus,
            activeDiscrepanciesCount = activeDiscrepanciesCount,
            integrityStatus = integrityStatus,
            masterIntegrityHash = masterHash,
            isReadOnly = true,
            allowedActions = allowedActions,
            forbiddenActions = forbiddenActions,
            recommendedActions = recommendedActions,
            auditTrailCount = auditTrailCount,
            latestAuditHash = latestAuditHash,
            generatedAt = System.currentTimeMillis()
        )
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
