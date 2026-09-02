package com.sucharu.sucharupro.domain.model.imposition

import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import java.math.BigDecimal
import java.time.Instant

/**
 * Lifecycle status of a CTP Output / Prepress Package Specification.
 * Module 18 Step 05.
 */
enum class CtpOutputStatus {
    DRAFT,
    GENERATED,
    APPROVED,
    REJECTED,
    EXPORTED_TO_RIP,
    CANCELLED
}

/**
 * Plate side classification for press mounting and exposure.
 * Module 18 Step 05.
 */
enum class PlateSide {
    FRONT,
    BACK,
    WORK_AND_TURN_COMBINED
}

/**
 * Color separation channels for offset CTP plate production.
 * Module 18 Step 05.
 */
enum class PlateColorSeparation {
    CYAN,
    MAGENTA,
    YELLOW,
    BLACK,
    SPOT_PANTONE,
    VARNISH_COATING,
    DIE_CUT_GUIDE
}

/**
 * CTP output laser exposure resolution.
 * Module 18 Step 05.
 */
enum class OutputResolutionDpi(val dpi: Int) {
    DPI_1200(1200),
    DPI_2400(2400),
    DPI_2540(2540),
    DPI_4000(4000)
}

/**
 * Screening methodology for halftone dot generation.
 * Module 18 Step 05.
 */
enum class ScreeningMethod {
    AM_CONVENTIONAL,       // Amplitude Modulation (e.g. 150 / 175 LPI)
    FM_STOCHASTIC,         // Frequency Modulation (e.g. 20 / 25 micron)
    HYBRID_XM              // Cross-Modulation hybrid screening
}

/**
 * Types of prepress marks placed on the press sheet / plate slug margin.
 * Module 18 Step 05.
 */
enum class PrepressMarkType {
    REGISTRATION_TARGET,       // Crosshair / target for 4-color register
    CROP_CORNER_MARK,          // Page trim boundary corner lines
    BLEED_LINE_MARK,           // Outer bleed limit indicator
    COLOR_CALIBRATION_BAR,     // CMYK / Spot density control step wedge
    FOLD_LINE_MARK,            // Dashed indicator for folding knife / buckle
    SPINE_CENTER_MARK,         // Centerline alignment mark for book spine
    PLATE_IDENTIFIER_SLUG,     // Text slug (Job ID, Color, Side, Screen, Date)
    GRIPPER_MARGIN_INDICATOR,  // Warning zone for press gripper clamp
    DENSITY_STEP_WEDGE         // Graduated halftone step wedge (10% - 100%)
}

/**
 * Individual prepress mark placement on the plate canvas.
 * Module 18 Step 05.
 */
data class PrepressMarkPlacement(
    val markId: String,
    val markType: PrepressMarkType,
    val plateSide: PlateSide,
    val xPositionMm: BigDecimal,
    val yPositionMm: BigDecimal,
    val widthMm: BigDecimal,
    val heightMm: BigDecimal,
    val rotationDegrees: BigDecimal = BigDecimal.ZERO,
    val labelText: String? = null,
    val targetColorSeparation: PlateColorSeparation? = null // null means mark applies to all plates (Registration Black)
)

/**
 * Prepress mark configuration policy.
 * Module 18 Step 05.
 */
data class PrepressMarkPolicy(
    val includeRegistrationMarks: Boolean = true,
    val includeCropMarks: Boolean = true,
    val includeBleedMarks: Boolean = true,
    val includeColorBars: Boolean = true,
    val includeFoldMarks: Boolean = true,
    val includePlateSlugs: Boolean = true,
    val markOffsetMm: BigDecimal = BigDecimal("5.0000"),
    val markLengthMm: BigDecimal = BigDecimal("8.0000"),
    val markStrokeWidthMm: BigDecimal = BigDecimal("0.1000")
)

/**
 * Press plate dimension and clamp margin specifications.
 * Module 18 Step 05.
 */
data class PlateDimensionSpec(
    val plateWidthMm: BigDecimal,
    val plateHeightMm: BigDecimal,
    val plateThicknessMm: BigDecimal = BigDecimal("0.3000"),
    val gripperMarginMm: BigDecimal = BigDecimal("45.0000"), // Press gripper clamp margin at bottom/lead edge
    val tailMarginMm: BigDecimal = BigDecimal("25.0000"),    // Press tail clamp margin
    val sideGuideMarginLeftMm: BigDecimal = BigDecimal("15.0000"),
    val sideGuideMarginRightMm: BigDecimal = BigDecimal("15.0000")
)

/**
 * Individual offset plate specification within a CTP output package.
 * Module 18 Step 05.
 */
data class PlateSpecification(
    val plateId: String,
    val plateName: String,
    val formReferenceId: String,
    val signatureNumber: Int = 1,
    val plateSide: PlateSide,
    val colorSeparation: PlateColorSeparation,
    val spotColorName: String? = null,
    val plateWidthMm: BigDecimal,
    val plateHeightMm: BigDecimal,
    val plateThicknessMm: BigDecimal = BigDecimal("0.3000"),
    val resolutionDpi: OutputResolutionDpi = OutputResolutionDpi.DPI_2540,
    val screeningMethod: ScreeningMethod = ScreeningMethod.AM_CONVENTIONAL,
    val screenRulingLpi: BigDecimal = BigDecimal("175.0000"),
    val screenAngleDegrees: BigDecimal,
    val dotShape: String = "EUCLIDEAN",
    val sheetOffsetXMm: BigDecimal,
    val sheetOffsetYMm: BigDecimal,
    val marks: List<PrepressMarkPlacement> = emptyList(),
    val plateAreaMm2: BigDecimal,
    val plateIntegrityHash: String
)

/**
 * Comprehensive CTP Prepress Output Package.
 * Module 18 Step 05.
 */
data class CtpOutputPackage(
    val packageId: String,
    val packageVersion: Int = 1,
    val sourceImpositionType: String, // "SINGLE_JOB", "GANG_RUN", "DYNAMIC_NESTING", "SIGNATURE_PUBLICATION"
    val sourceImpositionId: String,
    val sourceIntegrityHash: String,
    val totalPlatesCount: Int,
    val frontPlatesCount: Int,
    val backPlatesCount: Int,
    val spotColorsCount: Int,
    val pressSheetWidthMm: BigDecimal,
    val pressSheetHeightMm: BigDecimal,
    val plateWidthMm: BigDecimal,
    val plateHeightMm: BigDecimal,
    val gripperMarginMm: BigDecimal,
    val tailMarginMm: BigDecimal,
    val sideGuideMarginLeftMm: BigDecimal,
    val sideGuideMarginRightMm: BigDecimal,
    val plates: List<PlateSpecification>,
    val globalMarks: List<PrepressMarkPlacement>,
    val ripInstructions: String,
    val validationSummary: String,
    val integrityHash: String
)

/**
 * Root Aggregate for CTP Output Specification.
 * Module 18 Step 05.
 */
data class CtpOutputSpecification(
    val ctpOutputId: String,
    val tenantId: String,
    val name: String,
    val jobId: String,
    val orderId: String,
    val orderItemId: String,
    val productName: String,
    val sourceImpositionType: String,
    val sourceImpositionId: String,
    val sourceImpositionHash: String,
    val status: CtpOutputStatus = CtpOutputStatus.GENERATED,
    val packageVersion: Int = 1,
    val resolutionDpi: OutputResolutionDpi = OutputResolutionDpi.DPI_2540,
    val screeningMethod: ScreeningMethod = ScreeningMethod.AM_CONVENTIONAL,
    val defaultScreenRulingLpi: BigDecimal = BigDecimal("175.0000"),
    val markPolicy: PrepressMarkPolicy = PrepressMarkPolicy(),
    val plateDimensionSpec: PlateDimensionSpec,
    val outputPackage: CtpOutputPackage,
    val integrityHash: String,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

/**
 * Cryptographic Handoff Contract emitted for downstream Production RIP & AI Interlock (Step 06).
 * Module 18 Step 05.
 */
data class Module18Step05CtpHandoffContract(
    val contractVersion: String = "1.0.0",
    val tenantId: String,
    val ctpOutputId: String,
    val jobId: String,
    val orderId: String,
    val orderItemId: String,
    val sourceImpositionType: String,
    val sourceImpositionId: String,
    val sourceImpositionHash: String,
    val status: String,
    val packageVersion: Int,
    val totalPlatesCount: Int,
    val frontPlatesCount: Int,
    val backPlatesCount: Int,
    val resolutionDpi: Int,
    val screeningMethod: String,
    val defaultScreenRulingLpi: BigDecimal,
    val plateWidthMm: BigDecimal,
    val plateHeightMm: BigDecimal,
    val pressSheetWidthMm: BigDecimal,
    val pressSheetHeightMm: BigDecimal,
    val gripperMarginMm: BigDecimal,
    val tailMarginMm: BigDecimal,
    val ctpOutputIntegrityHash: String,
    val generatedTimestamp: String = Instant.now().toString()
)
