package com.sucharu.sucharupro.domain.model.productionplanning

import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import java.math.BigDecimal

// ============================================================
// ENUMS & CONSTANTS
// ============================================================

enum class PlanningStatus {
    DRAFT,
    ANALYZING,
    READY,
    BLOCKED,
    NEEDS_INFORMATION,
    REQUIRES_REVIEW,
    SUPERSEDED,
    CANCELLED,
    HANDED_OFF
}

enum class FeasibilityStatus {
    FEASIBLE,
    AT_RISK,
    NOT_FEASIBLE,
    UNKNOWN
}

enum class MachineCompatibilityStatus {
    COMPATIBLE,
    CONDITIONALLY_COMPATIBLE,
    INCOMPATIBLE,
    UNKNOWN
}

enum class DiagnosticSeverity {
    CRITICAL_BLOCKING,
    WARNING,
    INFO
}

enum class ProductionPlanningEventType {
    PLANNING_CREATED,
    ANALYSIS_COMPLETED,
    STATUS_CHANGED,
    DIAGNOSTIC_RECORDED,
    PLANNING_SUPERSEDED,
    PLANNING_HANDED_OFF,
    RECONCILIATION_PERFORMED
}

// ============================================================
// DOMAIN MODELS
// ============================================================

/**
 * Normalized physical printing job specification for manufacturing.
 */
data class ProductionJobSpecification(
    val specId: String,
    val jobTitle: String,
    val productType: String,
    val orderedQuantity: Long,
    val plannedQuantity: Long,
    val finishedWidthMm: BigDecimal,
    val finishedHeightMm: BigDecimal,
    val substrateType: String,
    val substrateGsm: Int,
    val substrateBrand: String? = null,
    val parentSheetWidthMm: BigDecimal,
    val parentSheetHeightMm: BigDecimal,
    val pressSheetWidthMm: BigDecimal,
    val pressSheetHeightMm: BigDecimal,
    val printingMethod: String,                // OFFSET | DIGITAL | SCREEN | FLEXO
    val colorsFront: Int,
    val colorsBack: Int,
    val coatingFront: String = "NONE",
    val coatingBack: String = "NONE",
    val impositionUps: Int,
    val sheetsPerItem: Int = 1,
    val itemsPerSheet: Int = 1,
    val lamination: String = "NONE",          // GLOSS | MATTE | SOFT_TOUCH | NONE
    val bindingMethod: String = "NONE",       // SADDLE_STITCH | PERFECT_BIND | WIRE_O | NONE
    val foldingType: String = "NONE",          // HALF_FOLD | TRI_FOLD | GATE_FOLD | NONE
    val cuttingRequired: Boolean = true,
    val dieCuttingRequired: Boolean = false,
    val packagingMethod: String = "CARTON_BOX",
    val artworkUrl: String? = null,
    val specialInstructions: String? = null,
    val specFingerprint: String
)

/**
 * Material / consumable production requirement estimate (non-deducting).
 */
data class ProductionPlanningRequirement(
    val requirementId: String,
    val planningId: String,
    val category: String,                      // SUBSTRATE | INK | PLATE | COATING | PACKAGING
    val itemCode: String,
    val description: String,
    val requiredQuantity: BigDecimal,
    val makeReadyQuantity: BigDecimal,
    val wasteQuantity: BigDecimal,
    val totalPlannedQuantity: BigDecimal,
    val unitOfMeasure: String,
    val estimatedAvailable: Boolean = true,
    val notes: String? = null
)

/**
 * Proposed sequential manufacturing operation routing.
 */
data class ProductionPlanningOperation(
    val operationId: String,
    val planningId: String,
    val sequenceNumber: Int,
    val stageType: ProductionStageType,
    val operationCode: String,
    val operationName: String,
    val targetWorkCenter: String,
    val estimatedSetupMinutes: Int = 0,
    val estimatedRunMinutes: Int = 0,
    val isMandatory: Boolean = true,
    val isQcCheckpoint: Boolean = false,
    val dependencies: List<String> = emptyList(),
    val notes: String? = null
)

/**
 * Machine compatibility evaluation result.
 */
data class MachineCompatibilityResult(
    val machineId: String,
    val machineName: String,
    val status: MachineCompatibilityStatus,
    val formatMatch: Boolean,
    val substrateMatch: Boolean,
    val colorMatch: Boolean,
    val notes: String? = null
)

/**
 * Structured diagnostic issue or warning.
 */
data class PlanningDiagnostic(
    val diagnosticId: String,
    val planningId: String,
    val code: String,                          // e.g. MISSING_SUBSTRATE, DUE_DATE_AT_RISK
    val severity: DiagnosticSeverity,
    val category: String,                      // SPECIFICATION | MACHINE | SCHEDULE | COMMERCIAL
    val message: String,
    val isBlocking: Boolean,
    val recommendedAction: String? = null
)

/**
 * Append-only lifecycle audit event.
 */
data class ProductionPlanningEvent(
    val eventId: String,
    val planningId: String,
    val tenantId: String,
    val eventType: ProductionPlanningEventType,
    val fromStatus: PlanningStatus? = null,
    val toStatus: PlanningStatus? = null,
    val eventPayload: String? = null,
    val performedBy: String,
    val performedAt: Long = System.currentTimeMillis()
)

/**
 * Quantitative Manufacturing Readiness Evaluation.
 */
data class ManufacturingReadinessEvaluation(
    val overallScore: BigDecimal,              // 0.0000 to 100.0000
    val isManufacturingReady: Boolean,
    val feasibilityStatus: FeasibilityStatus,
    val commercialReadinessScore: BigDecimal,  // 0.0000 to 100.0000
    val specificationReadinessScore: BigDecimal,
    val materialReadinessScore: BigDecimal,
    val machineReadinessScore: BigDecimal,
    val scheduleReadinessScore: BigDecimal,
    val blockingIssuesCount: Int,
    val warningsCount: Int,
    val diagnostics: List<PlanningDiagnostic> = emptyList()
)

/**
 * Primary authoritative Production Planning Snapshot entity.
 */
data class ProductionPlanningSnapshot(
    val planningId: String,
    val tenantId: String,
    val projectId: String,
    val orderId: String,
    val orderNumber: String,
    val orderItemId: String,
    val commercialCommitmentId: String?,
    val quotationId: String?,
    val quotationVersionNumber: Int?,
    val customerId: String,
    val status: PlanningStatus,
    val version: Int = 1,
    val isCurrent: Boolean = true,
    val readinessScore: BigDecimal,
    val feasibilityStatus: FeasibilityStatus,
    val specification: ProductionJobSpecification,
    val requirements: List<ProductionPlanningRequirement> = emptyList(),
    val operations: List<ProductionPlanningOperation> = emptyList(),
    val diagnostics: List<PlanningDiagnostic> = emptyList(),
    val machineCompatibility: List<MachineCompatibilityResult> = emptyList(),
    val orderRequestedDate: Long? = null,
    val estimatedCompletionDate: Long? = null,
    val planningFingerprint: String,
    val integrityHash: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String
)

/**
 * Multi-tier reconciliation outcome across Order ↔ Commitment ↔ Quotation ↔ Plan.
 */
data class ProductionPlanningReconciliationResult(
    val planningId: String,
    val orderId: String,
    val quotationId: String?,
    val commercialCommitmentId: String?,
    val isFullyReconciled: Boolean,
    val customerMatch: Boolean,
    val quantityMatch: Boolean,
    val specFingerprintMatch: Boolean,
    val pricingBoundaryPreserved: Boolean,
    val tenantIsolationVerified: Boolean,
    val discrepancies: List<String> = emptyList(),
    val verifiedAt: Long = System.currentTimeMillis()
)

/**
 * Read-only AI Agent Handoff Contract for Module 17 Step 04.
 */
data class Module17Step04ProductionPlanningHandoffContract(
    val contractVersion: String = "1.0.0",
    val planningId: String,
    val tenantId: String,
    val projectId: String,
    val orderId: String,
    val orderNumber: String,
    val customerId: String,
    val planningStatus: String,
    val readinessScore: BigDecimal,
    val isManufacturingReady: Boolean,
    val feasibilityStatus: String,
    val jobTitle: String,
    val orderedQuantity: Long,
    val plannedQuantity: Long,
    val primaryWorkCenter: String,
    val totalEstimatedRunMinutes: Int,
    val operationsCount: Int,
    val blockingIssues: List<String>,
    val warnings: List<String>,
    val reconciliationStatus: String,
    val integrityHash: String,
    val generatedAt: Long = System.currentTimeMillis()
)
