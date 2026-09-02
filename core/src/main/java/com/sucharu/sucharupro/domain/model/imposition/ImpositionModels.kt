package com.sucharu.sucharupro.domain.model.imposition

import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import java.math.BigDecimal

/**
 * Orientation policy for single-job imposition calculation.
 */
enum class ImpositionOrientationPolicy(val displayName: String) {
    AUTO_OPTIMAL("Automatic Optimal Selection (Standard or Rotated)"),
    FORCE_STANDARD_0_DEG("Force Standard (0° Unrotated)"),
    FORCE_ROTATED_90_DEG("Force Rotated (90° Clockwise)"),
    PROHIBIT_ROTATION("Prohibit Rotation (Strict Standard Only)")
}

/**
 * Chosen layout orientation for candidate imposition.
 */
enum class ImpositionLayoutOrientation(val rotationDegrees: Int, val displayName: String) {
    STANDARD(0, "Standard Parallel (0°)"),
    ROTATED(90, "Rotated (90°)")
}

/**
 * Lifecycle status of an Imposition Specification.
 */
enum class ImpositionStatus {
    DRAFT,
    OPTIMIZED,
    APPLIED_TO_PLANNING,
    SUPERSEDED,
    CANCELLED
}

/**
 * Outer margin constraints on the parent sheet (gripper margins, guide margins, machine edges).
 */
data class ImpositionMarginSpec(
    val topMm: BigDecimal = BigDecimal("10.0000"),
    val bottomMm: BigDecimal = BigDecimal("10.0000"),
    val leftMm: BigDecimal = BigDecimal("10.0000"),
    val rightMm: BigDecimal = BigDecimal("10.0000")
) {
    val totalHorizontalMarginMm: BigDecimal get() = leftMm.add(rightMm)
    val totalVerticalMarginMm: BigDecimal get() = topMm.add(bottomMm)
}

/**
 * Prepress spacing constraints between finished items on the sheet.
 */
data class ImpositionSpacingSpec(
    val bleedMm: BigDecimal = BigDecimal("3.0000"),
    val horizontalGutterMm: BigDecimal = BigDecimal("4.0000"),
    val verticalGutterMm: BigDecimal = BigDecimal("4.0000")
)

/**
 * Geometric layout candidate outcome evaluated by the engine.
 */
data class ImpositionCandidate(
    val orientation: ImpositionLayoutOrientation,
    val columns: Int,
    val rows: Int,
    val copiesPerSheet: Int,
    val usableWidthMm: BigDecimal,
    val usableHeightMm: BigDecimal,
    val itemEffectiveWidthMm: BigDecimal,
    val itemEffectiveHeightMm: BigDecimal,
    val occupiedAreaMm2: BigDecimal,
    val usableAreaMm2: BigDecimal,
    val wasteAreaMm2: BigDecimal,
    val yieldPercentage: BigDecimal,
    val isFeasible: Boolean
)

/**
 * Canonical Imposition Specification Aggregate Entity.
 * Authoritative record of Module 18 Step 01.
 */
data class ImpositionSpecification(
    val impositionId: String,
    val tenantId: String,
    val jobId: String?,
    val orderId: String,
    val orderItemId: String,
    val calculationId: String?,
    val productName: String,
    val finishedItemDimension: PrintingDimension,
    val parentSheetDimension: PrintingDimension,
    val marginSpec: ImpositionMarginSpec,
    val spacingSpec: ImpositionSpacingSpec,
    val orientationPolicy: ImpositionOrientationPolicy,
    val selectedOrientation: ImpositionLayoutOrientation,
    val columns: Int,
    val rows: Int,
    val copiesPerSheet: Int,
    val requiredQuantity: Long,
    val requiredSheets: Long,
    val totalProducedCapacity: Long,
    val overageQuantity: Long,
    val usableWidthMm: BigDecimal,
    val usableHeightMm: BigDecimal,
    val occupiedAreaMm2: BigDecimal,
    val usableAreaMm2: BigDecimal,
    val wasteAreaMm2: BigDecimal,
    val yieldPercentage: BigDecimal,
    val version: Int = 1,
    val status: ImpositionStatus = ImpositionStatus.OPTIMIZED,
    val integrityHash: String,
    val notes: String? = null,
    val candidateBreakdown: List<ImpositionCandidate> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String
)

/**
 * Structured downstream handoff contract for Module 19 (Substrate Stock Auto-Reservation)
 * and AI orchestration engines.
 */
data class Module18Step01ImpositionHandoffContract(
    val contractVersion: String = "1.0.0",
    val impositionId: String,
    val tenantId: String,
    val orderId: String,
    val orderItemId: String,
    val jobId: String?,
    val parentSheetWidthMm: BigDecimal,
    val parentSheetHeightMm: BigDecimal,
    val finishedItemWidthMm: BigDecimal,
    val finishedItemHeightMm: BigDecimal,
    val orientation: String,
    val columns: Int,
    val rows: Int,
    val copiesPerSheet: Int,
    val requiredProductiveQuantity: Long,
    val requiredProductiveSheets: Long,
    val totalProducedCapacity: Long,
    val layoutOverageItems: Long,
    val sheetYieldPercentage: BigDecimal,
    val wasteAreaPercentage: BigDecimal,
    val integrityHash: String,
    val generatedAt: Long = System.currentTimeMillis()
)
