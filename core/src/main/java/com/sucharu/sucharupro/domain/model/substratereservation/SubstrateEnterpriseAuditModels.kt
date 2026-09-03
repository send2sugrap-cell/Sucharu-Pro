package com.sucharu.sucharupro.domain.model.substratereservation

/**
 * Enterprise Audit Event Types covering the entire Module 19 lifecycle (Steps 01–06).
 */
enum class ReservationAuditEventType {
    REQUIREMENT_RESOLVED,
    INVENTORY_INTERLOCKED,
    SOFT_RESERVED,
    HARD_ALLOCATED,
    BATCH_LOT_SELECTED,
    GRAIN_DIMENSION_VALIDATED,
    REPLENISHMENT_EVALUATED,
    SUPPLIER_ALERT_SENT,
    RESERVATION_REVISED,
    RELEASE_EVALUATED,
    RELEASE_APPROVED,
    RELEASE_EXECUTED,
    PRODUCTION_INTERLOCKED,
    MATERIAL_CONSUMED,
    RECONCILIATION_EVALUATED,
    INTEGRITY_VERIFIED,
    INTEGRITY_VIOLATION_RECORDED,
    GOVERNANCE_OVERRIDE_RECORDED,
    REVERSAL_RECORDED
}

/**
 * Originating actor classification for enterprise audit records.
 */
enum class AuditActorType {
    USER,
    SYSTEM,
    AI_AGENT
}

/**
 * Immutable, append-only enterprise audit log record with cryptographic chaining.
 */
data class SubstrateEnterpriseAuditRecord(
    val auditId: String,
    val tenantId: String,
    val reservationId: String,
    val reservationVersion: Long = 1L,
    val jobId: String? = null,
    val orderId: String,
    val orderItemId: String,
    val substrateRequirementId: String? = null,
    val batchLotId: String? = null,
    val warehouseId: String? = null,
    val eventType: ReservationAuditEventType,
    val previousState: String? = null,
    val newState: String,
    val actorType: AuditActorType,
    val actorId: String,
    val role: String,
    val permissionContext: String,
    val timestamp: Long = System.currentTimeMillis(),
    val reason: String,
    val correlationId: String,
    val traceId: String? = null,
    val idempotencyKey: String? = null,
    val sourceModule: String = "MODULE_19",
    val sourceOperation: String,
    val eventOutboxId: String? = null,
    val recordHash: String,
    val previousAuditHash: String? = null,
    val chainHash: String
)

/**
 * Types of discrepancies discovered during cross-module reconciliation.
 */
enum class ReconciliationDiscrepancyType {
    RESERVATION_MISMATCH,
    QUANTITY_MISMATCH,
    STALE_RESERVATION,
    RELEASED_BUT_RESERVED,
    CONSUMED_BUT_RESERVED,
    MISSING_INVENTORY_REFERENCE,
    INVALID_BATCH_REFERENCE,
    PRODUCTION_COMMITMENT_CONFLICT,
    REPLENISHMENT_INCONSISTENCY,
    CROSS_MODULE_REFERENCE_INCONSISTENCY
}

/**
 * Discrepancy severity classification.
 */
enum class ReconciliationDiscrepancySeverity {
    INFO,
    WARNING,
    CRITICAL
}

/**
 * Overall reconciliation health status.
 */
enum class ReconciliationStatus {
    HEALTHY,
    WARNING_DETECTED,
    DISCREPANCIES_DETECTED,
    RESOLVED
}

/**
 * Granular discrepancy item identified in a reconciliation run.
 */
data class SubstrateReconciliationDiscrepancy(
    val discrepancyId: String,
    val reconciliationId: String,
    val tenantId: String,
    val discrepancyType: ReconciliationDiscrepancyType,
    val severity: ReconciliationDiscrepancySeverity,
    val fieldOrContext: String,
    val expectedValue: String,
    val actualValue: String,
    val explanation: String,
    val resolutionRecommendation: String
)

/**
 * Cross-module reconciliation evaluation record.
 */
data class SubstrateReservationReconciliation(
    val reconciliationId: String,
    val tenantId: String,
    val reservationId: String,
    val orderId: String,
    val jobId: String? = null,
    val sku: String,
    val requiredSheets: Long,
    val reservedSheets: Long,
    val physicalOnHandSheets: Long = 0L,
    val allocatedBatchSheets: Long = 0L,
    val releasableSheets: Long = 0L,
    val consumedSheets: Long = 0L,
    val committedSheets: Long = 0L,
    val replenishmentRequiredSheets: Long = 0L,
    val status: ReconciliationStatus,
    val discrepancies: List<SubstrateReconciliationDiscrepancy> = emptyList(),
    val reconciledBy: String,
    val reconciledAt: Long = System.currentTimeMillis(),
    val integrityHash: String,
    val notes: String? = null
)

/**
 * Status of cryptographic audit chain integrity check.
 */
enum class IntegrityVerificationStatus {
    INTACT,
    TAMPER_DETECTED,
    CHAIN_BROKEN,
    HASH_MISMATCH
}

/**
 * Result of audit history cryptographic verification.
 */
data class SubstrateIntegrityVerificationResult(
    val verificationId: String,
    val tenantId: String,
    val reservationId: String,
    val totalAuditRecords: Int,
    val status: IntegrityVerificationStatus,
    val isValidChain: Boolean,
    val isMasterHashValid: Boolean,
    val tamperedRecordIds: List<String> = emptyList(),
    val verifiedBy: String,
    val verifiedAt: Long = System.currentTimeMillis(),
    val diagnosticMessage: String
)

/**
 * Authoritative cross-module Downstream AI Handoff Contract for Module 19 (v6.0.0 Synthesis).
 */
data class Module19Step06EnterpriseReservationHandoffContract(
    val contractVersion: String = "6.0.0",
    val tenantId: String,
    val reservationId: String,
    val orderId: String,
    val jobId: String?,
    val sku: String,
    val materialName: String,
    val warehouseId: String,
    val reservationStatus: String,
    val requiredSheets: Long,
    val reservedSheets: Long,
    val allocatedHardSheets: Long,
    val softReservedSheets: Long,
    val batchLotSelectionSummary: String? = null,
    val grainDirectionCompatibility: String? = null,
    val replenishmentTriggerState: String? = null,
    val supplierAlertDispatched: Boolean = false,
    val releaseGovernanceDecision: String? = null,
    val releasableSheets: Long = 0L,
    val consumedSheets: Long = 0L,
    val productionCommitmentState: String? = null,
    val reconciliationStatus: String,
    val activeDiscrepanciesCount: Int,
    val integrityStatus: String,
    val masterIntegrityHash: String,
    val isReadOnly: Boolean = true,
    val allowedActions: List<String>,
    val forbiddenActions: List<String>,
    val recommendedActions: List<String>,
    val auditTrailCount: Int,
    val latestAuditHash: String,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * High-level KPI summary for Enterprise Reservation Governance Command Center.
 */
data class EnterpriseReservationGovernanceSummary(
    val totalReservationsAudited: Long,
    val activeHardAllocations: Long,
    val activeSoftReservations: Long,
    val reconciledHealthyCount: Long,
    val discrepanciesDetectedCount: Long,
    val integrityVerifiedIntactCount: Long,
    val integrityViolationsCount: Long,
    val pendingReplenishmentAlertsCount: Long,
    val activeReleaseReviewsCount: Long
)
