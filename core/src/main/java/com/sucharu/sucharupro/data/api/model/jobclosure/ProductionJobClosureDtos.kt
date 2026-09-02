package com.sucharu.sucharupro.data.api.model.jobclosure

import com.sucharu.sucharupro.domain.model.jobclosure.*
import java.math.BigDecimal

data class ProductionJobProvenanceNodeDto(
    val stepNumber: Int,
    val stepName: String,
    val canonicalEntityName: String,
    val canonicalEntityId: String,
    val completionStatus: String,
    val verifiedAt: Long,
    val verifiedBy: String,
    val integrityHash: String? = null
)

data class ProductionJobProvenanceGraphDto(
    val executionJobId: String,
    val orderId: String,
    val nodes: List<ProductionJobProvenanceNodeDto>,
    val isChainUnbroken: Boolean,
    val masterProvenanceFingerprint: String
)

data class JobClosureReadinessAuditDto(
    val executionJobId: String,
    val isQuoteAndCommitmentVerified: Boolean,
    val isProductionPlanningComplete: Boolean,
    val areAllWorkOrdersCompleted: Boolean,
    val isSchedulingDispatched: Boolean,
    val isShopFloorTrackingRecorded: Boolean,
    val isFinalQcReleased: Boolean,
    val isActualJobCostingReconciled: Boolean,
    val isMultiTenantBoundaryValid: Boolean,
    val isReadyForClosure: Boolean,
    val auditDiscrepancies: List<String>,
    val auditedAt: Long,
    val auditedBy: String
)

data class ManufacturingPerformanceScorecardDto(
    val executionJobId: String,
    val orderId: String,
    val onTimeInFullPercentage: BigDecimal,
    val rightFirstTimePercentage: BigDecimal,
    val costAdherenceIndex: BigDecimal,
    val machineEfficiencyIndex: BigDecimal,
    val qualityYieldPercentage: BigDecimal,
    val overallManufacturingIndex: BigDecimal,
    val performanceGrade: String,
    val calculatedAt: Long
)

data class ProductionPostMortemSummaryDto(
    val executionJobId: String,
    val primaryDowntimeDrivers: List<String>,
    val scrapAndDefectTakeaways: List<String>,
    val costVarianceTakeaways: List<String>,
    val operationalRecommendations: List<String>,
    val generatedAt: Long
)

data class MasterProductionClosureCertificateDto(
    val certificateId: String,
    val executionJobId: String,
    val orderId: String,
    val masterSealHash: String,
    val totalGoodUnitsReleased: BigDecimal,
    val grandTotalActualCost: BigDecimal,
    val totalCostVariance: BigDecimal,
    val overallCostClassification: String,
    val overallManufacturingScore: BigDecimal,
    val sealedAt: Long,
    val sealedBy: String
)

data class CloseAndSealJobRequestDto(
    val orderId: String,
    val orderQuantity: BigDecimal,
    val goodUnitsReleased: BigDecimal,
    val estimatedTotalCost: BigDecimal,
    val actualTotalCost: BigDecimal,
    val totalCostVariance: BigDecimal,
    val reworkOrScrapUnits: BigDecimal = BigDecimal.ZERO,
    val machineEfficiency: BigDecimal = BigDecimal("85.0000"),
    val onTime: Boolean = true,
    val primaryDowntimeDrivers: List<String> = emptyList(),
    val scrapAndDefectTakeaways: List<String> = emptyList(),
    val costVarianceTakeaways: List<String> = emptyList(),
    val operationalRecommendations: List<String> = emptyList()
)

data class ProductionJobClosureResponseDto(
    val closureId: String,
    val executionJobId: String,
    val orderId: String,
    val closureStatus: String,
    val readinessAudit: JobClosureReadinessAuditDto,
    val scorecard: ManufacturingPerformanceScorecardDto,
    val provenanceGraph: ProductionJobProvenanceGraphDto,
    val postMortemSummary: ProductionPostMortemSummaryDto,
    val masterCertificate: MasterProductionClosureCertificateDto,
    val closedAt: Long,
    val closedBy: String
)

data class Module17Step10JobClosureGovernanceHandoffContractDto(
    val contractVersion: String,
    val executionJobId: String,
    val orderId: String,
    val closureStatus: String,
    val isReadyForClosure: Boolean,
    val isProvenanceChainUnbroken: Boolean,
    val overallManufacturingIndex: BigDecimal,
    val performanceGrade: String,
    val onTimeInFullPercentage: BigDecimal,
    val rightFirstTimePercentage: BigDecimal,
    val totalGoodUnitsReleased: BigDecimal,
    val grandTotalActualCost: BigDecimal,
    val totalCostVariance: BigDecimal,
    val masterClosureSealHash: String,
    val crossModuleInventoryConfirmed: Boolean,
    val crossModuleDeliveryConfirmed: Boolean,
    val crossModuleFinanceConfirmed: Boolean,
    val crossModuleProfitabilityLocked: Boolean,
    val exportedAt: Long
)

fun JobClosureReadinessAudit.toDto() = JobClosureReadinessAuditDto(
    executionJobId = executionJobId,
    isQuoteAndCommitmentVerified = isQuoteAndCommitmentVerified,
    isProductionPlanningComplete = isProductionPlanningComplete,
    areAllWorkOrdersCompleted = areAllWorkOrdersCompleted,
    isSchedulingDispatched = isSchedulingDispatched,
    isShopFloorTrackingRecorded = isShopFloorTrackingRecorded,
    isFinalQcReleased = isFinalQcReleased,
    isActualJobCostingReconciled = isActualJobCostingReconciled,
    isMultiTenantBoundaryValid = isMultiTenantBoundaryValid,
    isReadyForClosure = isReadyForClosure,
    auditDiscrepancies = auditDiscrepancies,
    auditedAt = auditedAt,
    auditedBy = auditedBy
)

fun ManufacturingPerformanceScorecard.toDto() = ManufacturingPerformanceScorecardDto(
    executionJobId = executionJobId,
    orderId = orderId,
    onTimeInFullPercentage = onTimeInFullPercentage,
    rightFirstTimePercentage = rightFirstTimePercentage,
    costAdherenceIndex = costAdherenceIndex,
    machineEfficiencyIndex = machineEfficiencyIndex,
    qualityYieldPercentage = qualityYieldPercentage,
    overallManufacturingIndex = overallManufacturingIndex,
    performanceGrade = performanceGrade,
    calculatedAt = calculatedAt
)

fun ProductionJobProvenanceGraph.toDto() = ProductionJobProvenanceGraphDto(
    executionJobId = executionJobId,
    orderId = orderId,
    nodes = nodes.map {
        ProductionJobProvenanceNodeDto(
            stepNumber = it.stepNumber,
            stepName = it.stepName,
            canonicalEntityName = it.canonicalEntityName,
            canonicalEntityId = it.canonicalEntityId,
            completionStatus = it.completionStatus.name,
            verifiedAt = it.verifiedAt,
            verifiedBy = it.verifiedBy,
            integrityHash = it.integrityHash
        )
    },
    isChainUnbroken = isChainUnbroken,
    masterProvenanceFingerprint = masterProvenanceFingerprint
)

fun ProductionPostMortemSummary.toDto() = ProductionPostMortemSummaryDto(
    executionJobId = executionJobId,
    primaryDowntimeDrivers = primaryDowntimeDrivers,
    scrapAndDefectTakeaways = scrapAndDefectTakeaways,
    costVarianceTakeaways = costVarianceTakeaways,
    operationalRecommendations = operationalRecommendations,
    generatedAt = generatedAt
)

fun MasterProductionClosureCertificate.toDto() = MasterProductionClosureCertificateDto(
    certificateId = certificateId,
    executionJobId = executionJobId,
    orderId = orderId,
    masterSealHash = masterSealHash,
    totalGoodUnitsReleased = totalGoodUnitsReleased,
    grandTotalActualCost = grandTotalActualCost,
    totalCostVariance = totalCostVariance,
    overallCostClassification = overallCostClassification.name,
    overallManufacturingScore = overallManufacturingScore,
    sealedAt = sealedAt,
    sealedBy = sealedBy
)

fun ProductionJobClosureRecord.toDto() = ProductionJobClosureResponseDto(
    closureId = closureId,
    executionJobId = executionJobId,
    orderId = orderId,
    closureStatus = closureStatus.name,
    readinessAudit = readinessAudit.toDto(),
    scorecard = scorecard.toDto(),
    provenanceGraph = provenanceGraph.toDto(),
    postMortemSummary = postMortemSummary.toDto(),
    masterCertificate = masterCertificate.toDto(),
    closedAt = closedAt,
    closedBy = closedBy
)

fun Module17Step10JobClosureGovernanceHandoffContract.toDto() = Module17Step10JobClosureGovernanceHandoffContractDto(
    contractVersion = contractVersion,
    executionJobId = executionJobId,
    orderId = orderId,
    closureStatus = closureStatus.name,
    isReadyForClosure = isReadyForClosure,
    isProvenanceChainUnbroken = isProvenanceChainUnbroken,
    overallManufacturingIndex = overallManufacturingIndex,
    performanceGrade = performanceGrade,
    onTimeInFullPercentage = onTimeInFullPercentage,
    rightFirstTimePercentage = rightFirstTimePercentage,
    totalGoodUnitsReleased = totalGoodUnitsReleased,
    grandTotalActualCost = grandTotalActualCost,
    totalCostVariance = totalCostVariance,
    masterClosureSealHash = masterClosureSealHash,
    crossModuleInventoryConfirmed = crossModuleInventoryConfirmed,
    crossModuleDeliveryConfirmed = crossModuleDeliveryConfirmed,
    crossModuleFinanceConfirmed = crossModuleFinanceConfirmed,
    crossModuleProfitabilityLocked = crossModuleProfitabilityLocked,
    exportedAt = exportedAt
)
