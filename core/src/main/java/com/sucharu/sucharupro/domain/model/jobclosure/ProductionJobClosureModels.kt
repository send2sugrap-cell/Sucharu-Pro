package com.sucharu.sucharupro.domain.model.jobclosure

import com.sucharu.sucharupro.domain.model.jobcosting.VarianceClassification
import java.math.BigDecimal

enum class JobClosureStatus {
    OPEN,
    PRE_CLOSURE_AUDITED,
    CLOSED_PENDING_SEAL,
    GOVERNANCE_SEALED,
    ARCHIVED
}

enum class StepCompletionStatus {
    COMPLETED_AND_VERIFIED,
    PENDING,
    FAILED_OR_SKIPPED
}

data class ProductionJobProvenanceNode(
    val stepNumber: Int,
    val stepName: String,
    val canonicalEntityName: String,
    val canonicalEntityId: String,
    val completionStatus: StepCompletionStatus,
    val verifiedAt: Long,
    val verifiedBy: String,
    val integrityHash: String? = null
)

data class ProductionJobProvenanceGraph(
    val executionJobId: String,
    val tenantId: String,
    val orderId: String,
    val nodes: List<ProductionJobProvenanceNode> = emptyList(),
    val isChainUnbroken: Boolean = false,
    val masterProvenanceFingerprint: String = ""
)

data class JobClosureReadinessAudit(
    val executionJobId: String,
    val tenantId: String,
    val isQuoteAndCommitmentVerified: Boolean,
    val isProductionPlanningComplete: Boolean,
    val areAllWorkOrdersCompleted: Boolean,
    val isSchedulingDispatched: Boolean,
    val isShopFloorTrackingRecorded: Boolean,
    val isFinalQcReleased: Boolean,
    val isActualJobCostingReconciled: Boolean,
    val isMultiTenantBoundaryValid: Boolean,
    val isReadyForClosure: Boolean,
    val auditDiscrepancies: List<String> = emptyList(),
    val auditedAt: Long = System.currentTimeMillis(),
    val auditedBy: String = "closure-auditor"
)

data class ManufacturingPerformanceScorecard(
    val executionJobId: String,
    val tenantId: String,
    val orderId: String,
    val onTimeInFullPercentage: BigDecimal, // OTIF %
    val rightFirstTimePercentage: BigDecimal, // RFT %
    val costAdherenceIndex: BigDecimal, // Planned / Actual Cost * 100
    val machineEfficiencyIndex: BigDecimal, // Recorded / Rated Speed * 100
    val qualityYieldPercentage: BigDecimal, // Good Output / Total Material * 100
    val overallManufacturingIndex: BigDecimal, // Weighted composite 0 - 100
    val performanceGrade: String, // A+, A, B, C, D
    val calculatedAt: Long = System.currentTimeMillis()
)

data class ProductionPostMortemSummary(
    val executionJobId: String,
    val tenantId: String,
    val primaryDowntimeDrivers: List<String> = emptyList(),
    val scrapAndDefectTakeaways: List<String> = emptyList(),
    val costVarianceTakeaways: List<String> = emptyList(),
    val operationalRecommendations: List<String> = emptyList(),
    val generatedAt: Long = System.currentTimeMillis()
)

data class MasterProductionClosureCertificate(
    val certificateId: String,
    val tenantId: String,
    val executionJobId: String,
    val orderId: String,
    val masterSealHash: String,
    val totalGoodUnitsReleased: BigDecimal,
    val grandTotalActualCost: BigDecimal,
    val totalCostVariance: BigDecimal,
    val overallCostClassification: VarianceClassification,
    val overallManufacturingScore: BigDecimal,
    val sealedAt: Long = System.currentTimeMillis(),
    val sealedBy: String = "operations-director"
)

data class ProductionJobClosureRecord(
    val closureId: String,
    val tenantId: String,
    val executionJobId: String,
    val orderId: String,
    val closureStatus: JobClosureStatus,
    val readinessAudit: JobClosureReadinessAudit,
    val scorecard: ManufacturingPerformanceScorecard,
    val provenanceGraph: ProductionJobProvenanceGraph,
    val postMortemSummary: ProductionPostMortemSummary,
    val masterCertificate: MasterProductionClosureCertificate,
    val closedAt: Long = System.currentTimeMillis(),
    val closedBy: String = "plant-manager"
)

data class ProductionJobClosureEvent(
    val eventId: String,
    val tenantId: String,
    val executionJobId: String,
    val eventType: String,
    val actor: String,
    val payload: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class Module17Step10JobClosureGovernanceHandoffContract(
    val contractVersion: String = "1.0.0",
    val tenantId: String,
    val executionJobId: String,
    val orderId: String,
    val closureStatus: JobClosureStatus,
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
    val crossModuleInventoryConfirmed: Boolean = true,
    val crossModuleDeliveryConfirmed: Boolean = true,
    val crossModuleFinanceConfirmed: Boolean = true,
    val crossModuleProfitabilityLocked: Boolean = true,
    val exportedAt: Long = System.currentTimeMillis()
)
