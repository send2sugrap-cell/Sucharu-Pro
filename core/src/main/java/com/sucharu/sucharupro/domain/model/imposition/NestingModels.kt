package com.sucharu.sucharupro.domain.model.imposition

import com.sucharu.sucharupro.domain.model.printingcalculator.ColorMode
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingSideOption
import java.math.BigDecimal

/**
 * Orientation policy for dynamic nesting placement exploration.
 * Module 18 Step 03.
 */
enum class NestingOrientationPolicy(val displayName: String) {
    ALLOW_ROTATION("Allow 0° and 90° Rotation (Optimal Area Packing)"),
    FORCE_STANDARD_0_DEG("Force Standard (0° Unrotated Only)"),
    FORCE_ROTATED_90_DEG("Force Rotated (90° Clockwise Only)"),
    PROHIBIT_ROTATION("Prohibit Rotation (Strict Grain Direction)")
}

/**
 * Heuristic strategy used for 2D bin-packing placement.
 */
enum class NestingPlacementStrategy(val displayName: String) {
    BOTTOM_LEFT_FILL("Bottom-Left Skyline / Shelf Fill"),
    BEST_AREA_FIT("Best Area Fit (Maximal Rectangles)"),
    GUILLOTINE_CUT_FIRST("Guillotine Cut Optimization (Straight Slits)")
}

/**
 * Lifecycle status of a Dynamic Nesting Specification.
 */
enum class NestingStatus {
    DRAFT,
    OPTIMIZED,
    APPLIED_TO_PLANNING,
    SUPERSEDED,
    CANCELLED
}

/**
 * Candidate item submitted to the Dynamic Nesting Pool.
 */
data class NestingCandidateItem(
    val jobId: String,
    val orderId: String,
    val orderItemId: String,
    val productName: String,
    val finishedDimension: PrintingDimension,
    val requiredQuantity: Long,
    val paperStockType: PaperStockType,
    val gsm: BigDecimal,
    val colorMode: ColorMode = ColorMode.CMYK_FOUR_COLOR,
    val printingSideOption: PrintingSideOption = PrintingSideOption.SINGLE_SIDED,
    val allowRotation: Boolean = true,
    val priorityScore: Int = 100
) {
    init {
        require(jobId.isNotBlank()) { "Job ID must not be blank." }
        require(orderId.isNotBlank()) { "Order ID must not be blank." }
        require(orderItemId.isNotBlank()) { "Order Item ID must not be blank." }
        require(requiredQuantity > 0L) { "Required quantity must be positive: $requiredQuantity" }
        require(gsm > BigDecimal.ZERO) { "GSM must be strictly positive: $gsm" }
    }
}

/**
 * Positioned individual cut piece placement on the press sheet canvas.
 */
data class NestingItemPlacement(
    val placementId: String,
    val slotIndex: Int,
    val jobId: String,
    val orderId: String,
    val orderItemId: String,
    val productName: String,
    val xMm: BigDecimal,
    val yMm: BigDecimal,
    val placedWidthMm: BigDecimal,
    val placedHeightMm: BigDecimal,
    val orientation: ImpositionLayoutOrientation,
    val occupiedAreaMm2: BigDecimal
) {
    init {
        require(xMm >= BigDecimal.ZERO) { "X position cannot be negative: $xMm" }
        require(yMm >= BigDecimal.ZERO) { "Y position cannot be negative: $yMm" }
        require(placedWidthMm > BigDecimal.ZERO) { "Placed width must be positive: $placedWidthMm" }
        require(placedHeightMm > BigDecimal.ZERO) { "Placed height must be positive: $placedHeightMm" }
    }
}

/**
 * Identified unallocated rectangular offcut remnant region on the press sheet.
 */
data class NestingOffcutRemnant(
    val offcutId: String,
    val xMm: BigDecimal,
    val yMm: BigDecimal,
    val widthMm: BigDecimal,
    val heightMm: BigDecimal,
    val areaMm2: BigDecimal,
    val isRecoverable: Boolean
) {
    init {
        require(xMm >= BigDecimal.ZERO) { "X position cannot be negative: $xMm" }
        require(yMm >= BigDecimal.ZERO) { "Y position cannot be negative: $yMm" }
        require(widthMm > BigDecimal.ZERO) { "Offcut width must be positive: $widthMm" }
        require(heightMm > BigDecimal.ZERO) { "Offcut height must be positive: $heightMm" }
    }
}

/**
 * Summary breakdown of allocated items per distinct job.
 */
data class NestingJobAllocationSummary(
    val jobId: String,
    val orderId: String,
    val orderItemId: String,
    val productName: String,
    val assignedCopiesOnSheet: Int,
    val requiredQuantity: Long,
    val producedQuantity: Long,
    val overageQuantity: Long,
    val totalOccupiedAreaMm2: BigDecimal,
    val relativeYieldPercentage: BigDecimal
)

/**
 * Authoritative Aggregate Root for Module 18 Step 03: Dynamic Nesting Specification.
 */
data class DynamicNestingSpecification(
    val nestingId: String,
    val tenantId: String,
    val name: String,
    val paperStockType: PaperStockType,
    val gsm: BigDecimal,
    val colorMode: ColorMode,
    val printingSideOption: PrintingSideOption,
    val parentSheetDimension: PrintingDimension,
    val marginSpec: ImpositionMarginSpec,
    val spacingSpec: ImpositionSpacingSpec,
    val orientationPolicy: NestingOrientationPolicy,
    val placementStrategy: NestingPlacementStrategy,
    
    // Canvas dimensions
    val usableWidthMm: BigDecimal,
    val usableHeightMm: BigDecimal,
    
    // Placed items & remnants
    val placements: List<NestingItemPlacement>,
    val offcutRemnants: List<NestingOffcutRemnant>,
    val jobSummaries: List<NestingJobAllocationSummary>,
    
    // Counts & Press metrics
    val totalItemsPlaced: Int,
    val commonRequiredSheets: Long,
    val totalProducedItems: Long,
    val totalOverageItems: Long,
    
    // Area & Efficiency Metrics
    val totalSheetAreaMm2: BigDecimal,
    val usableAreaMm2: BigDecimal,
    val occupiedAreaMm2: BigDecimal,
    val wasteAreaMm2: BigDecimal,
    val recoverableOffcutAreaMm2: BigDecimal,
    val sheetUtilizationPercentage: BigDecimal,
    val usableYieldPercentage: BigDecimal,
    val offcutRecoveryPercentage: BigDecimal,
    
    // Governance & Lifecycle
    val version: Int = 1,
    val status: NestingStatus = NestingStatus.OPTIMIZED,
    val integrityHash: String,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String
)

/**
 * Sealed downstream handoff contract for Module 19 (Substrate Stock Auto-Reservation).
 * Module 18 Step 03.
 */
data class Module18Step03NestingHandoffContract(
    val contractVersion: String = "1.0.0",
    val nestingId: String,
    val tenantId: String,
    val paperStockType: String,
    val gsm: BigDecimal,
    val parentSheetWidthMm: BigDecimal,
    val parentSheetHeightMm: BigDecimal,
    val totalParentSheetsRequired: Long,
    val totalPlacedItems: Int,
    val distinctJobIds: List<String>,
    val orderItemIds: List<String>,
    val totalProducedItems: Long,
    val totalOverageItems: Long,
    val sheetUtilizationPercentage: BigDecimal,
    val usableYieldPercentage: BigDecimal,
    val recoverableOffcutAreaMm2: BigDecimal,
    val integrityHash: String,
    val generatedAt: Long = System.currentTimeMillis()
)
