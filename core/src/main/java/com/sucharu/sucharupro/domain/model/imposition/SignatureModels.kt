package com.sucharu.sucharupro.domain.model.imposition

import com.sucharu.sucharupro.domain.model.printingcalculator.*
import java.math.BigDecimal

/**
 * Binding methods for multi-page publication products.
 * Module 18 Step 04.
 */
enum class BindingMethod {
    SADDLE_STITCH,       // Creep compensation critical; nested folded signatures
    PERFECT_BOUND,       // Flat spine stacked signatures with grind-off allowance
    SECTION_SEWN,        // Stacked and sewn signatures for hardbound / casebound books
    SPIRAL_WIRE_O,       // Mechanical punching with spine punch margin
    FOLDED_LEAFLET       // Single signature folded product (brochure / pamphlet)
}

/**
 * Press feeding and sheet turning methods for 2-sided imposition.
 * Module 18 Step 04.
 */
enum class SheetTurningMethod {
    SHEETWISE,           // Front and back use different plates; sheet turned left-to-right (gripper constant, side guide changes)
    WORK_AND_TURN,       // Front and back on same plate; sheet turned left-to-right (gripper constant, side guide changes)
    WORK_AND_TUMBLE,     // Front and back on same plate; sheet turned end-over-end (gripper flips, side guide constant)
    PERFECTING           // Both sides printed in single pass on perfecting press
}

/**
 * Common standard folding sequence schemes.
 * Module 18 Step 04.
 */
enum class FoldingScheme {
    HALF_FOLD,                 // 4pp single fold (1 sheet fold)
    LETTER_FOLD,               // 6pp tri-fold
    Z_FOLD,                    // 6pp accordion fold
    FRENCH_RIGHT_ANGLE_4PP,    // 4pp broadsheet right-angle fold
    RIGHT_ANGLE_8PP,           // 8pp standard booklet signature (2 folds, 1 parallel + 1 right-angle)
    RIGHT_ANGLE_16PP,          // 16pp standard book signature (3 right-angle folds)
    DOUBLE_RIGHT_ANGLE_32PP    // 32pp publication signature (4 right-angle folds)
}

/**
 * Form sides for signature plate rendering.
 * Module 18 Step 04.
 */
enum class SignatureFormSide {
    FRONT_SIDE_OUTER,          // Outer form (e.g. Pages 1, 16, 4, 13)
    BACK_SIDE_INNER,           // Inner form (e.g. Pages 2, 15, 3, 14)
    WORK_AND_TURN_COMBINED     // Combined single-plate form containing both front and back
}

/**
 * Head orientation of a page on the press sheet for head-to-head or head-to-foot imposition.
 * Module 18 Step 04.
 */
enum class PageHeadOrientation {
    HEAD_UP_0,                 // Head at top (0°)
    HEAD_DOWN_180,             // Head at bottom (180° - upside down for head-to-head)
    HEAD_LEFT_270,             // Head to the left (270°)
    HEAD_RIGHT_90              // Head to the right (90°)
}

/**
 * Lifecycle status of a Signature Imposition Specification.
 * Module 18 Step 04.
 */
enum class SignatureStatus {
    DRAFT,
    OPTIMIZED,
    APPROVED,
    APPLIED_TO_PLANNING,
    SUPERSEDED,
    CANCELLED
}

/**
 * Gutter and margin specifications tailored for multi-page signature binding and folding.
 * Module 18 Step 04.
 */
data class SignatureGutterSpec(
    val spineGutterMm: BigDecimal = BigDecimal("4.0000"),   // Spine milling/folding allowance
    val headGutterMm: BigDecimal = BigDecimal("6.0000"),    // Head-to-head trim gap
    val footGutterMm: BigDecimal = BigDecimal("6.0000"),    // Foot trim gap
    val faceTrimMm: BigDecimal = BigDecimal("4.0000"),      // Face / outside trim
    val bleedMm: BigDecimal = BigDecimal("3.0000")          // Prepress bleed margin
)

/**
 * Summary of creep / shingling compensation applied across signatures.
 * Module 18 Step 04.
 */
data class CreepCompensationSummary(
    val isEnabled: Boolean = true,
    val paperCaliperMm: BigDecimal = BigDecimal("0.1200"),
    val totalCreepMm: BigDecimal = BigDecimal.ZERO,
    val creepPerSheetMm: BigDecimal = BigDecimal.ZERO,
    val innermostPageShiftMm: BigDecimal = BigDecimal.ZERO
)

/**
 * Exact placement coordinates, page number, orientation, and creep offset of a page on a signature form.
 * Module 18 Step 04.
 */
data class SignaturePagePlacement(
    val placementId: String,
    val pageNumber: Int,                     // 1-based publication page number (or 0 for blank pad)
    val slotIndex: Int,                      // Grid slot on the signature sheet
    val row: Int,
    val column: Int,
    val xMm: BigDecimal,                     // X position on press sheet
    val yMm: BigDecimal,                     // Y position on press sheet
    val widthMm: BigDecimal,                 // Trimmed page width
    val heightMm: BigDecimal,                // Trimmed page height
    val headOrientation: PageHeadOrientation,// 0°, 180°, etc.
    val creepShiftXMm: BigDecimal = BigDecimal.ZERO, // Progressive shingling offset X
    val creepShiftYMm: BigDecimal = BigDecimal.ZERO, // Progressive shingling offset Y
    val isBlankPage: Boolean = false         // True if inserted to complete signature page count
)

/**
 * A single printed form (front or back plate) within a publication signature.
 * Module 18 Step 04.
 */
data class SignatureForm(
    val formId: String,
    val signatureNumber: Int,                // 1-based signature index (e.g., Sig 1 of 4)
    val formSide: SignatureFormSide,
    val pagesPerSide: Int,                   // e.g. 8 pages per side on a 16pp signature
    val columns: Int,
    val rows: Int,
    val pagePlacements: List<SignaturePagePlacement>,
    val formSheetWidthMm: BigDecimal,
    val formSheetHeightMm: BigDecimal,
    val occupiedAreaMm2: BigDecimal,
    val usableAreaMm2: BigDecimal,
    val yieldPercentage: BigDecimal
)

/**
 * Authoritative Aggregate Root for Multi-Page Signature Imposition.
 * Module 18 Step 04.
 */
data class SignatureImpositionSpecification(
    val signatureImpositionId: String,
    val tenantId: String,
    val name: String,
    val jobId: String,
    val orderId: String,
    val orderItemId: String,
    val productName: String,
    val totalPages: Int,                     // Total publication pages
    val paddedTotalPages: Int,               // Total pages after signature padding
    val signaturePageCount: Int,             // e.g. 16 pages per signature
    val totalSignaturesCount: Int,           // e.g. 4 signatures for a 64pp book
    val bindingMethod: BindingMethod,
    val sheetTurningMethod: SheetTurningMethod,
    val foldingScheme: FoldingScheme,
    val paperStockType: PaperStockType,
    val gsm: BigDecimal,
    val pageDimension: PrintingDimension,    // Finished trimmed page size
    val parentSheetDimension: PrintingDimension, // Press sheet size
    val marginSpec: ImpositionMarginSpec,
    val gutterSpec: SignatureGutterSpec,
    val creepSummary: CreepCompensationSummary,
    val signatureForms: List<SignatureForm>,
    val commonRequiredSheets: Long,          // Press sheets run length per signature
    val totalParentSheetsRequired: Long,     // Total parent sheets across all signatures
    val totalProducedCopies: Long,
    val overageCopies: Long,
    val totalSheetAreaMm2: BigDecimal,
    val usableAreaMm2: BigDecimal,
    val occupiedAreaMm2: BigDecimal,
    val wasteAreaMm2: BigDecimal,
    val sheetUtilizationPercentage: BigDecimal,
    val usableYieldPercentage: BigDecimal,
    val version: Int = 1,
    val status: SignatureStatus = SignatureStatus.OPTIMIZED,
    val integrityHash: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String
)

/**
 * Downstream sealed handoff contract emitted for Module 19 Substrate Stock Reservation.
 * Module 18 Step 04.
 */
data class Module18Step04SignatureHandoffContract(
    val contractVersion: String = "1.0.0",
    val signatureImpositionId: String,
    val tenantId: String,
    val jobId: String,
    val orderId: String,
    val orderItemId: String,
    val productName: String,
    val totalSignatures: Int,
    val signaturePageCount: Int,
    val paperStockType: String,
    val gsm: BigDecimal,
    val parentSheetWidthMm: BigDecimal,
    val parentSheetHeightMm: BigDecimal,
    val sheetsPerSignature: Long,
    val totalParentSheetsRequired: Long,
    val bindingMethod: String,
    val sheetTurningMethod: String,
    val integrityHash: String,
    val emittedAt: Long = System.currentTimeMillis()
)
