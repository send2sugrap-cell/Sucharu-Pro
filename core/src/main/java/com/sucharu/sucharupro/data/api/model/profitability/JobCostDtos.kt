package com.sucharu.sucharupro.data.api.model.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

data class JobCostSnapshotDto(
    val snapshotId: String,
    val tenantId: String,
    val projectId: String,
    val jobId: String,
    val jobNumber: String? = null,
    val customerId: String? = null,
    val productId: String? = null,
    val jobQuantity: Int = 0,
    val calculationVersion: String = "JOB_COST_ENGINE_V1",
    val calculationTimestamp: Long,
    val currency: String = "BDT",
    val totalActualCost: BigDecimal,
    val totalDirectCost: BigDecimal,
    val totalIndirectCost: BigDecimal,
    val estimatedCost: BigDecimal? = null,
    val costVariance: BigDecimal? = null,
    val costVariancePercentage: BigDecimal? = null,
    val varianceClassification: String,
    val readinessStatus: String,
    val isReconciled: Boolean,
    val sourceCount: Int,
    val duplicateSourceCount: Int,
    val unresolvedSourceCount: Int,
    val costComponents: List<JobCostComponentDto> = emptyList(),
    val provenances: List<JobCostProvenanceDto> = emptyList(),
    val warnings: List<String> = emptyList(),
    val integrityHash: String,
    val generatedBy: String
)

data class JobCostComponentDto(
    val componentId: String,
    val componentType: String,
    val directness: String,
    val quantity: BigDecimal,
    val unitRate: BigDecimal,
    val originalAmount: BigDecimal,
    val attributedAmount: BigDecimal,
    val percentageOfTotalCost: BigDecimal,
    val currency: String,
    val attributionBasis: String,
    val sourceItemCount: Int,
    val calculationExplanation: String
)

data class JobCostProvenanceDto(
    val provenanceId: String,
    val sourceModule: String,
    val sourceEntityType: String,
    val sourceEntityId: String,
    val sourceTransactionId: String? = null,
    val sourceReference: String? = null,
    val costComponentType: String,
    val directness: String,
    val originalAmount: BigDecimal,
    val attributedAmount: BigDecimal,
    val currency: String,
    val attributionBasis: String,
    val calculationExplanation: String,
    val fingerprintHash: String
)

data class JobCostVarianceDto(
    val jobId: String,
    val actualCost: BigDecimal,
    val estimatedCost: BigDecimal? = null,
    val costVariance: BigDecimal? = null,
    val costVariancePercentage: BigDecimal? = null,
    val classification: String,
    val explanation: String
)

data class JobCostReconciliationEventDto(
    val reconciliationId: String,
    val snapshotId: String,
    val tenantId: String,
    val projectId: String,
    val jobId: String,
    val isReconciled: Boolean,
    val componentTotalCost: BigDecimal,
    val snapshotTotalCost: BigDecimal,
    val provenanceTotalCost: BigDecimal,
    val componentDifference: BigDecimal,
    val provenanceDifference: BigDecimal,
    val duplicateCount: Int,
    val missingSourceCount: Int,
    val discrepancies: List<String>,
    val checkedBy: String,
    val checkedAt: Long
)

data class CalculateJobCostRequestDto(
    val jobNumber: String? = null,
    val customerId: String? = null,
    val productId: String? = null,
    val jobQuantity: Int = 0,
    val customEstimatedCost: Double? = null,
    val idempotencyKey: String? = null
)

fun JobCostSnapshot.toDto(): JobCostSnapshotDto {
    return JobCostSnapshotDto(
        snapshotId = snapshotId,
        tenantId = tenantId,
        projectId = projectId,
        jobId = jobId,
        jobNumber = jobNumber,
        customerId = customerId,
        productId = productId,
        jobQuantity = jobQuantity,
        calculationVersion = calculationVersion,
        calculationTimestamp = calculationTimestamp,
        currency = currency,
        totalActualCost = totalActualCost,
        totalDirectCost = totalDirectCost,
        totalIndirectCost = totalIndirectCost,
        estimatedCost = estimatedCost,
        costVariance = costVariance,
        costVariancePercentage = costVariancePercentage,
        varianceClassification = varianceClassification.name,
        readinessStatus = readinessStatus.name,
        isReconciled = isReconciled,
        sourceCount = sourceCount,
        duplicateSourceCount = duplicateSourceCount,
        unresolvedSourceCount = unresolvedSourceCount,
        costComponents = costComponents.map { it.toDto() },
        provenances = provenances.map { it.toDto() },
        warnings = warnings,
        integrityHash = integrityHash,
        generatedBy = generatedBy
    )
}

fun JobCostComponent.toDto(): JobCostComponentDto {
    return JobCostComponentDto(
        componentId = componentId,
        componentType = componentType.name,
        directness = directness.name,
        quantity = quantity,
        unitRate = unitRate,
        originalAmount = originalAmount,
        attributedAmount = attributedAmount,
        percentageOfTotalCost = percentageOfTotalCost,
        currency = currency,
        attributionBasis = attributionBasis,
        sourceItemCount = sourceItemCount,
        calculationExplanation = calculationExplanation
    )
}

fun JobCostProvenance.toDto(): JobCostProvenanceDto {
    return JobCostProvenanceDto(
        provenanceId = provenanceId,
        sourceModule = sourceModule,
        sourceEntityType = sourceEntityType,
        sourceEntityId = sourceEntityId,
        sourceTransactionId = sourceTransactionId,
        sourceReference = sourceReference,
        costComponentType = costComponentType.name,
        directness = directness.name,
        originalAmount = originalAmount,
        attributedAmount = attributedAmount,
        currency = currency,
        attributionBasis = attributionBasis,
        calculationExplanation = calculationExplanation,
        fingerprintHash = fingerprintHash
    )
}

fun JobCostReconciliationEvent.toDto(): JobCostReconciliationEventDto {
    return JobCostReconciliationEventDto(
        reconciliationId = reconciliationId,
        snapshotId = snapshotId,
        tenantId = tenantId,
        projectId = projectId,
        jobId = jobId,
        isReconciled = isReconciled,
        componentTotalCost = componentTotalCost,
        snapshotTotalCost = snapshotTotalCost,
        provenanceTotalCost = provenanceTotalCost,
        componentDifference = componentDifference,
        provenanceDifference = provenanceDifference,
        duplicateCount = duplicateCount,
        missingSourceCount = missingSourceCount,
        discrepancies = discrepancies,
        checkedBy = checkedBy,
        checkedAt = checkedAt
    )
}
