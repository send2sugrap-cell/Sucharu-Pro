package com.sucharu.sucharupro.domain.model.substratereservation

import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import java.math.BigDecimal

/**
 * Standard Paper Grain Direction.
 * Module 19 Step 03.
 */
enum class PaperGrainDirection(val label: String) {
    LONG_GRAIN("Long Grain (Grain Parallel to Long Edge)"),
    SHORT_GRAIN("Short Grain (Grain Parallel to Short Edge)"),
    UNKNOWN("Unknown / Undefined Grain"),
    NOT_APPLICABLE("Not Applicable");

    companion object {
        fun fromString(value: String?): PaperGrainDirection {
            return when (value?.trim()?.uppercase()) {
                "LONG_GRAIN", "LONG", "LG" -> LONG_GRAIN
                "SHORT_GRAIN", "SHORT", "SG" -> SHORT_GRAIN
                "NOT_APPLICABLE", "NA" -> NOT_APPLICABLE
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Compatibility classification between required grain and candidate sheet grain.
 */
enum class GrainCompatibility(val label: String, val scoreWeight: Int) {
    EXACT_MATCH("Exact Grain Alignment", 100),
    ROTATED_COMPATIBLE("Compatible Through 90° Sheet Rotation", 80),
    UNKNOWN_GRAIN("Unknown Grain Direction (Verify Prior to Run)", 40),
    INCOMPATIBLE("Incompatible Grain Direction (Folding/Creasing Risk)", 0)
}

/**
 * Physical sheet dimension matching classification.
 */
enum class DimensionMatch(val label: String, val scoreWeight: Int) {
    EXACT_MATCH("Exact Sheet Dimensions", 100),
    ROTATED_MATCH("Exact Dimensions with 90° Press Rotation", 85),
    OVERSIZED_CUTTABLE("Oversized Sheet (Requires Pre-Cut)", 60),
    UNDERSIZED_MISMATCH("Undersized Sheet (Physically Incompatible)", 0),
    INVALID_DIMENSION("Invalid Dimension Specification", 0)
}

/**
 * Batch/Lot Selection Heuristic Policy.
 */
enum class BatchSelectionPolicy(val label: String) {
    FIFO("First-In, First-Out (Oldest Stock First)"),
    FEFO("First-Expired, First-Out (Earliest Expiry First)"),
    MINIMAL_WASTE("Exact Quantity & Dimension Fit (Minimal Remnant)"),
    SINGLE_LOT_ONLY("Strict Single Lot Fulfillment")
}

/**
 * Overall Batch/Lot Selection Status.
 */
enum class BatchLotSelectionStatus(val label: String) {
    FULLY_SATISFIED("Requirement Fully Satisfied by Selected Batches"),
    PARTIALLY_SATISFIED("Partially Satisfied (Shortage Detected)"),
    INSUFFICIENT_STOCK("Insufficient Total Usable Stock in Candidate Batches"),
    NO_COMPATIBLE_BATCH("No Candidate Batches Match Physical or Grain Specifications"),
    BLOCKED_BY_GRAIN("Candidate Batches Rejected Due to Incompatible Grain"),
    BLOCKED_BY_DIMENSION("Candidate Batches Rejected Due to Undersized Sheet Dimensions"),
    INVALID_REQUIREMENT("Invalid Requirement Specification");

    val isSuccess: Boolean get() = this == FULLY_SATISFIED
}

/**
 * Raw physical batch/lot candidate from canonical Module 06 inventory.
 */
data class BatchLotInventoryCandidate(
    val candidateId: String,
    val tenantId: String,
    val warehouseId: String,
    val warehouseName: String,
    val locationId: String? = null,
    val locationCode: String? = null,
    val productId: String,
    val sku: String,
    val productName: String,
    val batchNumber: String,
    val lotNumber: String,
    val supplierLotReference: String? = null,
    val stockType: PaperStockType,
    val gsm: BigDecimal,
    val sheetDimension: PrintingDimension,
    val grainDirection: PaperGrainDirection,
    val onHandPhysicalSheets: Long,
    val reservedSheets: Long = 0L,
    val hardAllocatedSheets: Long = 0L,
    val usableSheets: Long,
    val receivedTimestamp: Long = System.currentTimeMillis(),
    val expiryTimestamp: Long? = null,
    val qualityRating: BigDecimal = BigDecimal("1.0000"), // 0.0000 to 1.0000
    val status: String = "ACTIVE"
)

/**
 * Evaluated candidate with detailed match scoring and diagnostic reasons.
 */
data class EvaluatedBatchCandidate(
    val candidate: BatchLotInventoryCandidate,
    val dimensionMatch: DimensionMatch,
    val grainCompatibility: GrainCompatibility,
    val isRotated: Boolean,
    val gsmMatchScore: Int,
    val overallScore: BigDecimal, // 0.0000 to 100.0000
    val isEligible: Boolean,
    val allocatedSheetsFromThisBatch: Long = 0L,
    val allocatedReams: BigDecimal = BigDecimal.ZERO,
    val allocatedWeightKg: BigDecimal = BigDecimal.ZERO,
    val evaluationReasons: List<String> = emptyList(),
    val rejectionReasons: List<String> = emptyList()
)

/**
 * Individual batch/lot allocation within a selection decision.
 */
data class SelectedBatchAllocation(
    val allocationId: String,
    val selectionId: String,
    val tenantId: String,
    val warehouseId: String,
    val warehouseName: String,
    val locationId: String? = null,
    val batchNumber: String,
    val lotNumber: String,
    val sku: String,
    val allocatedSheets: Long,
    val allocatedReams: BigDecimal,
    val allocatedWeightKg: BigDecimal,
    val sheetDimension: PrintingDimension,
    val grainDirection: PaperGrainDirection,
    val isRotated: Boolean,
    val matchScore: BigDecimal
)

/**
 * Target Requirement Specification for Batch/Lot Selection.
 */
data class BatchLotSelectionSpecification(
    val selectionId: String,
    val tenantId: String,
    val orderId: String,
    val orderItemId: String,
    val executionJobId: String? = null,
    val workOrderId: String? = null,
    val reservationId: String? = null,
    val productId: String,
    val sku: String,
    val requestedMaterialName: String,
    val stockType: PaperStockType,
    val targetGsm: BigDecimal,
    val requiredSheetDimension: PrintingDimension,
    val requiredGrainDirection: PaperGrainDirection = PaperGrainDirection.LONG_GRAIN,
    val requiredSheets: Long,
    val allowSheetRotation: Boolean = true,
    val allowMultiBatchFulfillment: Boolean = true,
    val selectionPolicy: BatchSelectionPolicy = BatchSelectionPolicy.FIFO,
    val preferredWarehouseId: String? = null,
    val actor: String = "SYSTEM"
)

/**
 * Comprehensive Batch/Lot Selection Decision Result (Aggregate Root).
 */
data class BatchLotSelectionResult(
    val selectionId: String,
    val tenantId: String,
    val specification: BatchLotSelectionSpecification,
    val status: BatchLotSelectionStatus,
    val requiredSheets: Long,
    val allocatedSheets: Long,
    val deficitSheets: Long,
    val allocatedReams: BigDecimal,
    val allocatedWeightKg: BigDecimal,
    val isFullySatisfied: Boolean,
    val isMultiBatchFulfillment: Boolean,
    val selectedBatches: List<SelectedBatchAllocation>,
    val evaluatedCandidates: List<EvaluatedBatchCandidate>,
    val primarySelectedBatchNumber: String? = null,
    val primarySelectedLotNumber: String? = null,
    val primaryWarehouseId: String? = null,
    val overallCompatibilityScore: BigDecimal, // 0.0000 to 100.0000
    val selectionExplanation: String,
    val masterIntegrityHash: String,
    val selectedAt: Long = System.currentTimeMillis(),
    val selectedBy: String = "SYSTEM",
    val isConfirmedAndAllocated: Boolean = false,
    val confirmedAt: Long? = null,
    val confirmedBy: String? = null
)

/**
 * AI & Cross-Module Governance Handoff Contract for Module 19 Step 03.
 */
data class Module19Step03BatchSelectionHandoffContract(
    val contractVersion: String = "3.0.0",
    val tenantId: String,
    val selectionId: String,
    val orderId: String,
    val orderItemId: String,
    val executionJobId: String?,
    val reservationId: String?,
    val sku: String,
    val status: String,
    val requiredSheets: Long,
    val allocatedSheets: Long,
    val deficitSheets: Long,
    val allocatedReams: BigDecimal,
    val allocatedWeightKg: BigDecimal,
    val isFullySatisfied: Boolean,
    val isMultiBatchFulfillment: Boolean,
    val targetGsm: BigDecimal,
    val targetSheetWidthMm: BigDecimal,
    val targetSheetHeightMm: BigDecimal,
    val targetGrainDirection: String,
    val selectedBatchCount: Int,
    val selectedBatches: List<SelectedBatchSummaryDto>,
    val overallScore: BigDecimal,
    val selectionExplanation: String,
    val masterIntegrityHash: String,
    val selectedBy: String,
    val timestamp: Long
)

/**
 * Summary DTO of a selected batch in AI handoff.
 */
data class SelectedBatchSummaryDto(
    val batchNumber: String,
    val lotNumber: String,
    val warehouseId: String,
    val allocatedSheets: Long,
    val allocatedReams: BigDecimal,
    val allocatedWeightKg: BigDecimal,
    val grainDirection: String,
    val isRotated: Boolean,
    val matchScore: BigDecimal
)
