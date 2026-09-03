package com.sucharu.sucharupro.data.api.model.substratereservation

import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateReleaseGovernanceRecord

/**
 * Request DTO to evaluate substrate release on job/order cancellation.
 */
data class EvaluateCancellationGovernanceRequestDto(
    val reservationId: String,
    val orderId: String,
    val orderItemId: String,
    val executionJobId: String? = null,
    val upstreamEventId: String? = null,
    val sku: String,
    val materialName: String,
    val warehouseId: String,
    val allocatedSheets: Long,
    val consumedSheets: Long = 0L,
    val committedSheets: Long = 0L,
    val productionStatus: String? = null
)

/**
 * Request DTO to evaluate substrate release/re-reservation on job/order revision.
 */
data class EvaluateRevisionGovernanceRequestDto(
    val reservationId: String,
    val orderId: String,
    val orderItemId: String,
    val executionJobId: String? = null,
    val upstreamEventId: String? = null,
    val sku: String,
    val materialName: String,
    val warehouseId: String,
    val previousRequiredSheets: Long,
    val newRequiredSheets: Long,
    val allocatedSheets: Long,
    val consumedSheets: Long = 0L,
    val committedSheets: Long = 0L,
    val productionStatus: String? = null,
    val isSkuChanged: Boolean = false
)

/**
 * Request DTO to approve a release-eligible governance record.
 */
data class ApproveReleaseGovernanceRequestDto(
    val notes: String? = null
)

/**
 * Request DTO to reject release governance.
 */
data class RejectReleaseGovernanceRequestDto(
    val reason: String
)

/**
 * Response DTO representing an authoritative Substrate Release Governance Record.
 */
data class SubstrateReleaseGovernanceResponseDto(
    val governanceId: String,
    val tenantId: String,
    val reservationId: String,
    val orderId: String,
    val orderItemId: String,
    val executionJobId: String?,
    val triggerType: String,
    val upstreamEventId: String?,
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
    val decision: String,
    val executionStatus: String,
    val blockingReason: String,
    val explanation: String,
    val deduplicationFingerprint: String,
    val masterIntegrityHash: String,
    val evaluatedBy: String,
    val evaluatedAt: Long,
    val approvedBy: String?,
    val approvedAt: Long?,
    val executedBy: String?,
    val executedAt: Long?,
    val notes: String?
)

fun SubstrateReleaseGovernanceRecord.toDto(): SubstrateReleaseGovernanceResponseDto {
    return SubstrateReleaseGovernanceResponseDto(
        governanceId = governanceId,
        tenantId = tenantId,
        reservationId = reservationId,
        orderId = orderId,
        orderItemId = orderItemId,
        executionJobId = executionJobId,
        triggerType = triggerType.name,
        upstreamEventId = upstreamEventId,
        sku = sku,
        materialName = materialName,
        warehouseId = warehouseId,
        previousRequiredSheets = previousRequiredSheets,
        newRequiredSheets = newRequiredSheets,
        allocatedSheets = allocatedSheets,
        consumedSheets = consumedSheets,
        committedSheets = committedSheets,
        releasableSheets = releasableSheets,
        retainedSheets = retainedSheets,
        additionalRequiredSheets = additionalRequiredSheets,
        decision = decision.name,
        executionStatus = executionStatus.name,
        blockingReason = blockingReason.name,
        explanation = explanation,
        deduplicationFingerprint = deduplicationFingerprint,
        masterIntegrityHash = masterIntegrityHash,
        evaluatedBy = evaluatedBy,
        evaluatedAt = evaluatedAt,
        approvedBy = approvedBy,
        approvedAt = approvedAt,
        executedBy = executedBy,
        executedAt = executedAt,
        notes = notes
    )
}
