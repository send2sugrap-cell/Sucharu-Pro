package com.sucharu.sucharupro.domain.model.finalqc

import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import java.math.BigDecimal

/**
 * Inspection Status Lifecycle for Step 08 Final Quality Control.
 */
enum class FinalQcInspectionStatus {
    PENDING_INSPECTION,
    IN_PROGRESS,
    ACCEPTED,
    CONDITIONALLY_ACCEPTED,
    REJECTED,
    REWORK_REQUIRED
}

/**
 * Standard AQL & Sampling Plan types.
 */
enum class InspectionSamplePlanType {
    FULL_100_PERCENT,
    AQL_LEVEL_II_NORMAL,
    AQL_LEVEL_I_REDUCED,
    RANDOM_SAMPLE
}

/**
 * Print & Manufacturing Defect Classification.
 */
enum class DefectClassificationType {
    PRINTING_DEFECT,
    COLOR_MISMATCH,
    REGISTRATION_ERROR,
    FINISHING_DEFECT,
    DIMENSIONAL_DEFECT,
    SUBSTRATE_DAMAGE,
    PACKAGING_DEFECT,
    COUNT_SHORTAGE
}

/**
 * Severity level of identified defects.
 */
enum class DefectSeverity {
    CRITICAL,
    MAJOR,
    MINOR
}

/**
 * Disposition of defective units under containment.
 */
enum class ContainmentDisposition {
    QUARANTINED,
    SCRAPPED,
    REWORK_ROUTED,
    CONCESSION_RELEASED
}

/**
 * Finished Goods Packaging Medium.
 */
enum class PackagingType {
    CORRUGATED_BOX,
    KRAFT_BUNDLE,
    WOODEN_PALLET,
    SHRINK_WRAP_BUNDLE,
    ENVELOPE_PACK
}

/**
 * Authoritative Finished Goods Release Status.
 */
enum class FinishedGoodsReleaseStatus {
    DRAFT,
    RELEASE_APPROVED,
    DISPATCHED_TO_WAREHOUSE,
    RELEASED_FOR_DELIVERY,
    BLOCKED_ON_HOLD
}

/**
 * Event Types for Step 08 Audit Trail.
 */
enum class FinalQcEventType {
    INSPECTION_STARTED,
    INSPECTION_COMPLETED,
    DEFECT_QUARANTINED,
    PACKAGING_COMPLETED,
    RELEASE_CERTIFIED
}

/**
 * Specific Quality Checklist item verified by inspector.
 */
data class QcChecklistItem(
    val checkCode: String,
    val checkTitle: String,
    val isPassed: Boolean,
    val measuredValue: String? = null,
    val toleranceLimit: String? = null,
    val remarks: String? = null
)

/**
 * Final QC Inspection Record.
 */
data class ProductionFinalQcInspection(
    val inspectionId: String,
    val tenantId: String,
    val executionJobId: String,
    val orderId: String,
    val samplePlanType: InspectionSamplePlanType,
    val totalLotQuantity: BigDecimal,
    val sampleSize: BigDecimal,
    val acceptedQuantity: BigDecimal,
    val rejectedQuantity: BigDecimal,
    val reworkQuantity: BigDecimal,
    val status: FinalQcInspectionStatus,
    val checklist: List<QcChecklistItem> = emptyList(),
    val inspectorId: String,
    val inspectorName: String,
    val inspectionNotes: String? = null,
    val inspectedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

/**
 * Defect Containment & Quarantine Record.
 */
data class ProductionDefectContainmentRecord(
    val containmentId: String,
    val tenantId: String,
    val executionJobId: String,
    val inspectionId: String,
    val rootCauseStage: ProductionStageType,
    val defectType: DefectClassificationType,
    val severity: DefectSeverity,
    val defectQuantity: BigDecimal,
    val disposition: ContainmentDisposition,
    val quarantineLocation: String,
    val reworkWorkOrderId: String? = null,
    val rootCauseDetails: String,
    val loggedBy: String,
    val loggedAt: Long = System.currentTimeMillis()
)

/**
 * Packaging Orchestration Record.
 */
data class ProductionPackagingRecord(
    val packagingId: String,
    val tenantId: String,
    val executionJobId: String,
    val inspectionId: String,
    val packagingType: PackagingType,
    val unitsPerPackage: BigDecimal,
    val totalPackageCount: Int,
    val totalPackagedQuantity: BigDecimal,
    val palletIdentifier: String? = null,
    val cartonNumbersRange: String? = null,
    val grossWeightKg: BigDecimal? = null,
    val packagingSlipBarcode: String,
    val packagedBy: String,
    val packagedAt: Long = System.currentTimeMillis(),
    val notes: String? = null
)

/**
 * Authoritative Finished Goods Warehouse/Delivery Release Record.
 */
data class FinishedGoodsReleaseRecord(
    val releaseId: String,
    val tenantId: String,
    val executionJobId: String,
    val orderId: String,
    val inspectionId: String,
    val packagingId: String,
    val releasedQuantity: BigDecimal,
    val destination: String, // "WAREHOUSE_FINISHED_GOODS" or "DISPATCH_DOCK"
    val status: FinishedGoodsReleaseStatus,
    val authorizedBy: String,
    val authorizedAt: Long = System.currentTimeMillis(),
    val integrityHash: String, // SHA-256 Release Certificate
    val notes: String? = null
)

/**
 * Analytical Quality & Release Summary.
 */
data class FinalQcPackagingVarianceSummary(
    val executionJobId: String,
    val tenantId: String,
    val totalManufacturedOutput: BigDecimal,
    val sampleInspectedQuantity: BigDecimal,
    val totalAcceptedGoodQuantity: BigDecimal,
    val totalRejectedQuantity: BigDecimal,
    val totalReworkQuantity: BigDecimal,
    val overallQualityYieldPercentage: BigDecimal,
    val defectRatePercentage: BigDecimal,
    val totalPackagedQuantity: BigDecimal,
    val packagingBalanceVariance: BigDecimal,
    val isReadyForFullRelease: Boolean,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Append-only Event for Step 08.
 */
data class FinalQcPackagingEvent(
    val eventId: String,
    val tenantId: String,
    val executionJobId: String,
    val eventType: FinalQcEventType,
    val actor: String,
    val payload: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 8-Way Multi-Tier Multi-Tenant Quality Reconciliation Result.
 */
data class FinalQcPackagingReconciliationResult(
    val executionJobId: String,
    val tenantId: String,
    val outputMatchedInspectionLot: Boolean,
    val samplePlanConsistent: Boolean,
    val defectAccountingBalanced: Boolean,
    val zeroUncontainedCriticalDefects: Boolean,
    val packagingQuantityMatchesAccepted: Boolean,
    val releaseCertificateHashValid: Boolean,
    val multiTenantIsolationVerified: Boolean,
    val isFullyReconciled: Boolean,
    val discrepancies: List<String> = emptyList(),
    val reconciledAt: Long = System.currentTimeMillis()
)

/**
 * Canonical AI Handoff Contract for Step 08.
 */
data class Module17Step08FinalQcPackagingHandoffContract(
    val contractVersion: String = "1.0.0",
    val tenantId: String,
    val executionJobId: String,
    val orderId: String,
    val finalInspectionStatus: FinalQcInspectionStatus,
    val totalGoodQuantityAccepted: BigDecimal,
    val totalDefectQuantity: BigDecimal,
    val qualityYieldPercentage: BigDecimal,
    val defectRatePercentage: BigDecimal,
    val totalPackagedCartons: Int,
    val totalPackagedQuantity: BigDecimal,
    val packagingSlipBarcode: String,
    val releaseStatus: FinishedGoodsReleaseStatus,
    val releaseCertificateHash: String,
    val isFullyReconciled: Boolean,
    val inspectionSummary: List<String>,
    val defectContainmentSummary: List<String>,
    val exportedAt: Long = System.currentTimeMillis()
)
