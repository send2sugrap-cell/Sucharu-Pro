package com.sucharu.sucharupro.data.api.model.substratereservation

import com.sucharu.sucharupro.domain.model.substratereservation.*

data class SubstrateEnterpriseAuditRecordDto(
    val auditId: String,
    val tenantId: String,
    val reservationId: String,
    val reservationVersion: Long,
    val jobId: String?,
    val orderId: String,
    val orderItemId: String,
    val substrateRequirementId: String?,
    val batchLotId: String?,
    val warehouseId: String?,
    val eventType: String,
    val previousState: String?,
    val newState: String,
    val actorType: String,
    val actorId: String,
    val role: String,
    val permissionContext: String,
    val timestamp: Long,
    val reason: String,
    val correlationId: String,
    val traceId: String?,
    val idempotencyKey: String?,
    val sourceModule: String,
    val sourceOperation: String,
    val recordHash: String,
    val previousAuditHash: String?,
    val chainHash: String
)

data class SubstrateReconciliationDiscrepancyDto(
    val discrepancyId: String,
    val reconciliationId: String,
    val discrepancyType: String,
    val severity: String,
    val fieldOrContext: String,
    val expectedValue: String,
    val actualValue: String,
    val explanation: String,
    val resolutionRecommendation: String
)

data class SubstrateReservationReconciliationDto(
    val reconciliationId: String,
    val tenantId: String,
    val reservationId: String,
    val orderId: String,
    val jobId: String?,
    val sku: String,
    val requiredSheets: Long,
    val reservedSheets: Long,
    val physicalOnHandSheets: Long,
    val allocatedBatchSheets: Long,
    val releasableSheets: Long,
    val consumedSheets: Long,
    val committedSheets: Long,
    val replenishmentRequiredSheets: Long,
    val status: String,
    val discrepancies: List<SubstrateReconciliationDiscrepancyDto>,
    val reconciledBy: String,
    val reconciledAt: Long,
    val integrityHash: String,
    val notes: String?
)

data class SubstrateIntegrityVerificationResultDto(
    val verificationId: String,
    val tenantId: String,
    val reservationId: String,
    val totalAuditRecords: Int,
    val status: String,
    val isValidChain: Boolean,
    val isMasterHashValid: Boolean,
    val tamperedRecordIds: List<String>,
    val verifiedBy: String,
    val verifiedAt: Long,
    val diagnosticMessage: String
)

data class Module19Step06EnterpriseReservationHandoffContractDto(
    val contractVersion: String,
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
    val batchLotSelectionSummary: String?,
    val grainDirectionCompatibility: String?,
    val replenishmentTriggerState: String?,
    val supplierAlertDispatched: Boolean,
    val releaseGovernanceDecision: String?,
    val releasableSheets: Long,
    val consumedSheets: Long,
    val productionCommitmentState: String?,
    val reconciliationStatus: String,
    val activeDiscrepanciesCount: Int,
    val integrityStatus: String,
    val masterIntegrityHash: String,
    val isReadOnly: Boolean,
    val allowedActions: List<String>,
    val forbiddenActions: List<String>,
    val recommendedActions: List<String>,
    val auditTrailCount: Int,
    val latestAuditHash: String,
    val generatedAt: Long
)

data class EnterpriseReservationGovernanceSummaryDto(
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

data class ReconcileReservationRequestDto(
    val reservationId: String,
    val notes: String? = null
)

data class VerifyIntegrityRequestDto(
    val reservationId: String
)

data class RecordAuditEventRequestDto(
    val reservationId: String,
    val orderId: String,
    val orderItemId: String,
    val jobId: String? = null,
    val eventType: String,
    val newState: String,
    val previousState: String? = null,
    val reason: String,
    val sourceOperation: String
)

fun SubstrateEnterpriseAuditRecord.toDto() = SubstrateEnterpriseAuditRecordDto(
    auditId = auditId,
    tenantId = tenantId,
    reservationId = reservationId,
    reservationVersion = reservationVersion,
    jobId = jobId,
    orderId = orderId,
    orderItemId = orderItemId,
    substrateRequirementId = substrateRequirementId,
    batchLotId = batchLotId,
    warehouseId = warehouseId,
    eventType = eventType.name,
    previousState = previousState,
    newState = newState,
    actorType = actorType.name,
    actorId = actorId,
    role = role,
    permissionContext = permissionContext,
    timestamp = timestamp,
    reason = reason,
    correlationId = correlationId,
    traceId = traceId,
    idempotencyKey = idempotencyKey,
    sourceModule = sourceModule,
    sourceOperation = sourceOperation,
    recordHash = recordHash,
    previousAuditHash = previousAuditHash,
    chainHash = chainHash
)

fun SubstrateReconciliationDiscrepancy.toDto() = SubstrateReconciliationDiscrepancyDto(
    discrepancyId = discrepancyId,
    reconciliationId = reconciliationId,
    discrepancyType = discrepancyType.name,
    severity = severity.name,
    fieldOrContext = fieldOrContext,
    expectedValue = expectedValue,
    actualValue = actualValue,
    explanation = explanation,
    resolutionRecommendation = resolutionRecommendation
)

fun SubstrateReservationReconciliation.toDto() = SubstrateReservationReconciliationDto(
    reconciliationId = reconciliationId,
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
    status = status.name,
    discrepancies = discrepancies.map { it.toDto() },
    reconciledBy = reconciledBy,
    reconciledAt = reconciledAt,
    integrityHash = integrityHash,
    notes = notes
)

fun SubstrateIntegrityVerificationResult.toDto() = SubstrateIntegrityVerificationResultDto(
    verificationId = verificationId,
    tenantId = tenantId,
    reservationId = reservationId,
    totalAuditRecords = totalAuditRecords,
    status = status.name,
    isValidChain = isValidChain,
    isMasterHashValid = isMasterHashValid,
    tamperedRecordIds = tamperedRecordIds,
    verifiedBy = verifiedBy,
    verifiedAt = verifiedAt,
    diagnosticMessage = diagnosticMessage
)

fun Module19Step06EnterpriseReservationHandoffContract.toDto() = Module19Step06EnterpriseReservationHandoffContractDto(
    contractVersion = contractVersion,
    tenantId = tenantId,
    reservationId = reservationId,
    orderId = orderId,
    jobId = jobId,
    sku = sku,
    materialName = materialName,
    warehouseId = warehouseId,
    reservationStatus = reservationStatus,
    requiredSheets = requiredSheets,
    reservedSheets = reservedSheets,
    allocatedHardSheets = allocatedHardSheets,
    softReservedSheets = softReservedSheets,
    batchLotSelectionSummary = batchLotSelectionSummary,
    grainDirectionCompatibility = grainDirectionCompatibility,
    replenishmentTriggerState = replenishmentTriggerState,
    supplierAlertDispatched = supplierAlertDispatched,
    releaseGovernanceDecision = releaseGovernanceDecision,
    releasableSheets = releasableSheets,
    consumedSheets = consumedSheets,
    productionCommitmentState = productionCommitmentState,
    reconciliationStatus = reconciliationStatus,
    activeDiscrepanciesCount = activeDiscrepanciesCount,
    integrityStatus = integrityStatus,
    masterIntegrityHash = masterIntegrityHash,
    isReadOnly = isReadOnly,
    allowedActions = allowedActions,
    forbiddenActions = forbiddenActions,
    recommendedActions = recommendedActions,
    auditTrailCount = auditTrailCount,
    latestAuditHash = latestAuditHash,
    generatedAt = generatedAt
)

fun EnterpriseReservationGovernanceSummary.toDto() = EnterpriseReservationGovernanceSummaryDto(
    totalReservationsAudited = totalReservationsAudited,
    activeHardAllocations = activeHardAllocations,
    activeSoftReservations = activeSoftReservations,
    reconciledHealthyCount = reconciledHealthyCount,
    discrepanciesDetectedCount = discrepanciesDetectedCount,
    integrityVerifiedIntactCount = integrityVerifiedIntactCount,
    integrityViolationsCount = integrityViolationsCount,
    pendingReplenishmentAlertsCount = pendingReplenishmentAlertsCount,
    activeReleaseReviewsCount = activeReleaseReviewsCount
)
