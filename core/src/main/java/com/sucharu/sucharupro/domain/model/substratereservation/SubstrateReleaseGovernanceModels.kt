package com.sucharu.sucharupro.domain.model.substratereservation

import java.math.BigDecimal

/**
 * Trigger event that initiates Substrate Release & Revision Governance.
 * Module 19 Step 05.
 */
enum class GovernanceTriggerType(val defaultLabel: String) {
    JOB_CANCELLATION("Production Job Cancellation"),
    ORDER_CANCELLATION("Customer Order Cancellation"),
    QUANTITY_REDUCTION("Job Quantity Reduced"),
    QUANTITY_INCREASE("Job Quantity Increased"),
    SPECIFICATION_REVISION("Substrate / Spec Revision"),
    SCHEDULE_CHANGE("Production Schedule / Allocation Change"),
    MANUAL_REVIEW("Manual Governance Review")
}

/**
 * Deterministic Governance Decision regarding material release.
 */
enum class ReleaseGovernanceDecision(val defaultLabel: String) {
    RELEASE_ELIGIBLE("Full Substrate Release Eligible"),
    PARTIAL_RELEASE_ELIGIBLE("Partial Substrate Release Eligible"),
    RELEASE_BLOCKED("Release Blocked by Production Authority"),
    NO_RELEASE_REQUIRED("No Stock Release Required"),
    REQUIRES_REVIEW("Ambiguous State - Requires Supervisory Review")
}

/**
 * Execution lifecycle status of a Governance Case.
 * Enforces Segregation of Duties: EVALUATED -> APPROVED -> RELEASE_EXECUTED.
 */
enum class GovernanceExecutionStatus(val defaultLabel: String) {
    EVALUATED("Evaluated & Pending Review"),
    APPROVED("Approved for Release"),
    RELEASE_EXECUTED("Release Executed & Inventory Restored"),
    REJECTED("Release Rejected"),
    SUPERSEDED("Superseded by Subsequent Revision");

    val isTerminal: Boolean get() = this == RELEASE_EXECUTED || this == REJECTED || this == SUPERSEDED
}

/**
 * Operational reason blocking release of reserved substrate.
 */
enum class ReleaseBlockingReason(val defaultLabel: String) {
    MATERIAL_ALREADY_CONSUMED("Substrate has already been physically consumed on floor"),
    PRODUCTION_IN_PROGRESS("Job is actively in progress; sheets cannot be safely pulled"),
    MATERIAL_COMMITTED_TO_FLOOR("Material is staged or operationally committed to work center"),
    JOB_COMPLETED("Job execution complete; no releasable reservation remains"),
    AMBIGUOUS_PRODUCTION_STATE("Production lifecycle in non-deterministic status"),
    NONE("No operational blockers detected")
}

/**
 * Authoritative Governance Decision Record for Substrate Release & Revision.
 * Immutable audit snapshot - NOT a second inventory ledger.
 */
data class SubstrateReleaseGovernanceRecord(
    val governanceId: String,
    val tenantId: String,
    val reservationId: String,
    val orderId: String,
    val orderItemId: String,
    val executionJobId: String? = null,
    val triggerType: GovernanceTriggerType,
    val upstreamEventId: String? = null,
    val sku: String,
    val materialName: String,
    val warehouseId: String,
    val previousRequiredSheets: Long,
    val newRequiredSheets: Long,
    val allocatedSheets: Long,
    val consumedSheets: Long,
    val committedSheets: Long,
    val releasableSheets: Long,
    val retainedSheets: Long,
    val additionalRequiredSheets: Long,
    val decision: ReleaseGovernanceDecision,
    val executionStatus: GovernanceExecutionStatus = GovernanceExecutionStatus.EVALUATED,
    val blockingReason: ReleaseBlockingReason = ReleaseBlockingReason.NONE,
    val explanation: String,
    val deduplicationFingerprint: String,
    val masterIntegrityHash: String,
    val evaluatedBy: String,
    val evaluatedAt: Long = System.currentTimeMillis(),
    val approvedBy: String? = null,
    val approvedAt: Long? = null,
    val executedBy: String? = null,
    val executedAt: Long? = null,
    val notes: String? = null
)

/**
 * Immutable Audit Event for Substrate Release Governance.
 */
data class SubstrateReleaseGovernanceAuditEvent(
    val eventId: String,
    val governanceId: String,
    val tenantId: String,
    val action: String,
    val previousStatus: GovernanceExecutionStatus?,
    val newStatus: GovernanceExecutionStatus,
    val actor: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Downstream AI & Cross-Module Governance Handoff Contract for Module 19 Step 05.
 */
data class Module19Step05SubstrateReleaseGovernanceHandoffContract(
    val contractVersion: String = "5.0.0",
    val governanceId: String,
    val tenantId: String,
    val reservationId: String,
    val orderId: String,
    val orderItemId: String,
    val executionJobId: String?,
    val triggerType: String,
    val sku: String,
    val materialName: String,
    val warehouseId: String,
    val allocatedSheets: Long,
    val consumedSheets: Long,
    val committedSheets: Long,
    val releasableSheets: Long,
    val retainedSheets: Long,
    val additionalRequiredSheets: Long,
    val decision: String,
    val executionStatus: String,
    val blockingReason: String,
    val explanation: String,
    val deduplicationFingerprint: String,
    val masterIntegrityHash: String,
    val evaluatedBy: String,
    val evaluatedAt: Long,
    val approvedBy: String?,
    val executedBy: String?
)
