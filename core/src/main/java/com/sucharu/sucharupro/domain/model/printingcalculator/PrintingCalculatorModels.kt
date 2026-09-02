package com.sucharu.sucharupro.domain.model.printingcalculator

import com.sucharu.sucharupro.domain.model.product.ProductType
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Canonical Printing Process Type.
 * Module 17 Step 01.
 */
enum class PrintingProcessType(val displayName: String, val allowsPlates: Boolean) {
    OFFSET(displayName = "Offset Lithography", allowsPlates = true),
    DIGITAL(displayName = "Digital Press", allowsPlates = false),
    LARGE_FORMAT(displayName = "Large Format Inkjet", allowsPlates = false),
    SCREEN(displayName = "Screen Printing", allowsPlates = false),
    FLEXOGRAPHIC(displayName = "Flexographic", allowsPlates = true),
    OTHER(displayName = "Other Printing Process", allowsPlates = false)
}

/**
 * Printing Sides Option.
 */
enum class PrintingSideOption(val sideCount: Int, val isDoubleSided: Boolean) {
    SINGLE_SIDED(sideCount = 1, isDoubleSided = false),
    DOUBLE_SIDED_SAME(sideCount = 2, isDoubleSided = true),
    DOUBLE_SIDED_DIFFERENT(sideCount = 2, isDoubleSided = true)
}

/**
 * Color Configuration Mode.
 */
enum class ColorMode(val defaultPlatesPerSide: Int) {
    MONOCHROME(defaultPlatesPerSide = 1),
    TWO_COLOR(defaultPlatesPerSide = 2),
    CMYK_FOUR_COLOR(defaultPlatesPerSide = 4),
    CMYK_PLUS_SPOT(defaultPlatesPerSide = 5),
    SPOT_ONLY(defaultPlatesPerSide = 1),
    MULTI_PROCESS(defaultPlatesPerSide = 4)
}

/**
 * Measurement Units for physical dimensions.
 */
enum class MeasurementUnit(val toMmFactor: BigDecimal) {
    MILLIMETERS(BigDecimal("1.0000")),
    CENTIMETERS(BigDecimal("10.0000")),
    INCHES(BigDecimal("25.4000")),
    FEET(BigDecimal("304.8000")),
    POINTS(BigDecimal("0.3528")) // 1 pt = 1/72 inch
}

/**
 * Paper/Material Weight Unit.
 */
enum class PaperWeightUnit {
    GSM,
    PT,
    LB,
    KG_PER_REAM
}

/**
 * Quantity Unit for ordering and batching.
 */
enum class QuantityUnit {
    PIECES,
    SHEETS,
    SETS,
    COPIES,
    PACKS,
    REAMS,
    BOOKS
}

/**
 * Paper / Substrate Stock Category.
 */
enum class PaperStockType(val displayName: String) {
    ART_PAPER("Art Paper (Gloss / Matt)"),
    ART_CARD("Art Card"),
    OFFSET_PAPER("Offset Paper (White / Cream)"),
    KRAFT_PAPER("Kraft Paper / Board"),
    BOX_BOARD("Box Board (Grey Back / White Back)"),
    STICKER_PAPER("Sticker Paper / Self-Adhesive"),
    DUPLEX_BOARD("Duplex Board"),
    METALLIC_PAPER("Metallic / Foil Board"),
    SPECIALTY_PAPER("Specialty Textured Paper"),
    SYNTHETIC_PAPER("Synthetic / Yupo Paper"),
    PVC("PVC / Vinyl Sheet"),
    OTHER("Other Substrate")
}

/**
 * Canonical Post-Press & Finishing Operations.
 */
enum class FinishingOperationType(val displayName: String, val category: String) {
    CUTTING_TRIMMING("Cutting & Trimming", "Cutting"),
    GLOSS_LAMINATION("Gloss Lamination", "Coating"),
    MATTE_LAMINATION("Matte Lamination", "Coating"),
    SOFT_TOUCH_LAMINATION("Soft-Touch Velvet Lamination", "Coating"),
    THERMAL_LAMINATION("Thermal Lamination", "Coating"),
    FOLDING("Folding / Creasing", "Binding"),
    CREASING("Creasing / Scoring", "Binding"),
    PERFORATION("Perforation", "Binding"),
    DIE_CUTTING("Die Cutting / Punching", "Forming"),
    EMBOSSING("Embossing / Debossing", "Embellishment"),
    FOIL_STAMPING("Hot Foil Stamping", "Embellishment"),
    SPOT_UV("Spot UV Coating", "Embellishment"),
    SADDLE_STITCHING("Saddle Stitching", "Binding"),
    PERFECT_BINDING("Perfect Binding / Glue Bind", "Binding"),
    WIRE_O_BINDING("Wire-O / Spiral Binding", "Binding"),
    HARDCOVER_BINDING("Hardcover / Case Binding", "Binding"),
    PUNCHING_DRILLING("Hole Punching / Drilling", "Finishing"),
    NUMBERING("Sequential Numbering", "Finishing"),
    PASTING_GLUING("Pasting & Gluing", "Assembly"),
    CORNER_ROUNDING("Corner Rounding", "Finishing"),
    EYELETING("Eyelet / Grommet Fitting", "Finishing")
}

/**
 * Calculation Outcome Status.
 */
enum class CalculationStatus {
    SUCCESSFUL,
    PARTIAL_CALCULATION,
    INVALID_REQUEST,
    INSUFFICIENT_INPUT
}

/**
 * Estimate vs Actual Distinction Boundary.
 */
enum class EstimateActualClassification {
    ESTIMATED,
    ACTUAL_REFERENCE
}

/**
 * Diagnostic Severity Level.
 */
enum class DiagnosticSeverity {
    INFO,
    WARNING,
    ERROR
}

/**
 * Diagnostic Codes for structured explanations.
 */
enum class DiagnosticCode {
    MISSING_MATERIAL_PRICE,
    MISSING_MACHINE_RATE,
    MISSING_PLATE_RATE,
    INVALID_DIMENSION,
    INVALID_QUANTITY,
    INVALID_UNIT,
    INVALID_WASTE_PERCENTAGE,
    UNSUPPORTED_PRINT_PROCESS,
    UNSUPPORTED_FINISHING_OPERATION,
    INSUFFICIENT_INPUT,
    PARTIAL_CALCULATION,
    SHEET_SIZE_SMALLER_THAN_ITEM,
    EXCESSIVE_WASTE_PERCENTAGE,
    IMPOSITION_INEFFICIENT,
    MACHINE_DIMENSION_LIMIT_EXCEEDED
}

/**
 * Explicit Physical Dimension Representation.
 */
data class PrintingDimension(
    val width: BigDecimal,
    val height: BigDecimal,
    val unit: MeasurementUnit = MeasurementUnit.MILLIMETERS
) {
    init {
        require(width > BigDecimal.ZERO) { "Dimension width must be strictly positive: $width" }
        require(height > BigDecimal.ZERO) { "Dimension height must be strictly positive: $height" }
    }
}

/**
 * Explicit Quantity Specification.
 */
data class QuantitySpecification(
    val orderedQuantity: Long,
    val unit: QuantityUnit = QuantityUnit.PIECES,
    val normalizedQuantity: Long = orderedQuantity,
    val spoilageAllowanceQuantity: Long = 0L
) {
    val totalRequiredQuantity: Long get() = normalizedQuantity + spoilageAllowanceQuantity
}

/**
 * Paper / Substrate Input Specification.
 */
data class PaperMaterialSpecification(
    val materialId: String? = null,
    val materialCode: String? = null,
    val materialName: String,
    val stockType: PaperStockType = PaperStockType.ART_PAPER,
    val gsm: BigDecimal? = null,
    val thicknessPt: BigDecimal? = null,
    val sheetDimension: PrintingDimension? = null,
    val unitPricePerSheet: BigDecimal? = null,
    val unitPricePerKg: BigDecimal? = null,
    val unitPricePerReam: BigDecimal? = null,
    val sheetsPerReam: Int = 500,
    val currency: String = "BDT"
)

/**
 * Color and Process Configuration Specification.
 */
data class ColorSpecification(
    val colorMode: ColorMode = ColorMode.CMYK_FOUR_COLOR,
    val frontColorsCount: Int = 4,
    val backColorsCount: Int = 0,
    val spotColorsCount: Int = 0,
    val varnishType: String? = null
) {
    val totalColorsCount: Int get() = frontColorsCount + backColorsCount + spotColorsCount
}

/**
 * Finishing Operation Specification.
 */
data class FinishingOperationSpecification(
    val operationType: FinishingOperationType,
    val description: String = operationType.displayName,
    val unitRate: BigDecimal? = null,
    val setupRate: BigDecimal? = null,
    val isOptional: Boolean = false
)

/**
 * Waste & Allowance Specification.
 */
data class WasteAllowanceSpecification(
    val setupSheets: Long = 0L,
    val runningWastePercentage: BigDecimal = BigDecimal.ZERO,
    val finishingWastePercentage: BigDecimal = BigDecimal.ZERO,
    val totalWasteSheets: Long = 0L
)

/**
 * Machine & Press Setup Specification.
 */
data class MachineSpecification(
    val machineId: String? = null,
    val machineName: String? = null,
    val processType: PrintingProcessType = PrintingProcessType.OFFSET,
    val maxSheetDimension: PrintingDimension? = null,
    val minSheetDimension: PrintingDimension? = null,
    val hourlyRate: BigDecimal? = null,
    val impressionsPerHour: Int? = null,
    val plateCostPerUnit: BigDecimal? = null
)

/**
 * Fully Normalized Specification.
 */
data class NormalizedPrintingSpecification(
    val jobTitle: String,
    val productType: ProductType = ProductType.PRINTING_JOB,
    val finishedDimension: PrintingDimension,
    val normalizedDimensionMm: PrintingDimension,
    val quantity: QuantitySpecification,
    val material: PaperMaterialSpecification,
    val processType: PrintingProcessType,
    val sides: PrintingSideOption,
    val color: ColorSpecification,
    val finishingOperations: List<FinishingOperationSpecification> = emptyList(),
    val waste: WasteAllowanceSpecification = WasteAllowanceSpecification(),
    val machine: MachineSpecification? = null,
    val currency: String = "BDT"
)

/**
 * Raw Calculation Request.
 */
data class PrintingCalculationRequest(
    val calculationId: String? = null,
    val tenantId: String,
    val projectId: String,
    val jobTitle: String = "Print Calculation Estimate",
    val productType: ProductType = ProductType.PRINTING_JOB,
    val quantity: Long,
    val quantityUnit: QuantityUnit = QuantityUnit.PIECES,
    val finishedWidth: BigDecimal,
    val finishedHeight: BigDecimal,
    val dimensionUnit: MeasurementUnit = MeasurementUnit.MILLIMETERS,
    val materialName: String,
    val stockType: PaperStockType = PaperStockType.ART_PAPER,
    val gsm: BigDecimal? = null,
    val sheetWidth: BigDecimal? = null,
    val sheetHeight: BigDecimal? = null,
    val sheetDimensionUnit: MeasurementUnit = MeasurementUnit.MILLIMETERS,
    val materialUnitPricePerSheet: BigDecimal? = null,
    val processType: PrintingProcessType = PrintingProcessType.OFFSET,
    val sides: PrintingSideOption = PrintingSideOption.SINGLE_SIDED,
    val colorMode: ColorMode = ColorMode.CMYK_FOUR_COLOR,
    val frontColorsCount: Int = 4,
    val backColorsCount: Int = 0,
    val spotColorsCount: Int = 0,
    val setupSheets: Long = 0L,
    val runningWastePercentage: BigDecimal = BigDecimal.ZERO,
    val finishingWastePercentage: BigDecimal = BigDecimal.ZERO,
    val finishingOperations: List<FinishingOperationSpecification> = emptyList(),
    val machine: MachineSpecification? = null,
    val currency: String = "BDT",
    val requestedBy: String = "SYSTEM",
    val requestedAt: Long = System.currentTimeMillis(),
    val idempotencyKey: String? = null
)

/**
 * Material Requirement Output.
 */
data class MaterialRequirementResult(
    val finishedItemsPerSheet: Int,
    val cutDirection: String,
    val productiveSheetsRequired: Long,
    val wasteSheetsRequired: Long,
    val totalSheetsRequired: Long,
    val totalReamsRequired: BigDecimal,
    val totalWeightKg: BigDecimal?,
    val estimatedMaterialCost: BigDecimal?,
    val costStatus: CalculationStatus,
    val missingPriceReason: String? = null
)

/**
 * Printing Requirement Output.
 */
data class PrintingRequirementResult(
    val totalImpressions: Long,
    val totalPasses: Int,
    val plateCount: Int,
    val estimatedPrintingCost: BigDecimal?,
    val estimatedPlateCost: BigDecimal?,
    val costStatus: CalculationStatus,
    val missingRateReason: String? = null
)

/**
 * Finishing Requirement Output.
 */
data class FinishingRequirementResult(
    val operations: List<CalculationBreakdownItem>,
    val totalEstimatedFinishingCost: BigDecimal?,
    val costStatus: CalculationStatus
)

/**
 * Structured Calculation Breakdown Item.
 */
data class CalculationBreakdownItem(
    val componentCode: String,
    val description: String,
    val quantity: BigDecimal,
    val unit: String,
    val unitRate: BigDecimal?,
    val calculatedAmount: BigDecimal?,
    val classification: EstimateActualClassification = EstimateActualClassification.ESTIMATED,
    val formulaReference: String,
    val diagnosticCode: DiagnosticCode? = null
)

/**
 * Actionable Calculation Diagnostic.
 */
data class CalculationDiagnostic(
    val code: DiagnosticCode,
    val severity: DiagnosticSeverity,
    val message: String,
    val targetField: String? = null,
    val suggestedRemediation: String? = null
)

/**
 * Canonical Immutable Calculation Result / Snapshot.
 */
data class PrintingCalculationResult(
    val calculationId: String,
    val tenantId: String,
    val projectId: String,
    val requestFingerprint: String,
    val requestedAt: Long,
    val calculatedAt: Long,
    val status: CalculationStatus,
    val classification: EstimateActualClassification = EstimateActualClassification.ESTIMATED,
    val normalizedSpecification: NormalizedPrintingSpecification,
    val materialRequirement: MaterialRequirementResult,
    val printingRequirement: PrintingRequirementResult,
    val finishingRequirement: FinishingRequirementResult,
    val breakdownItems: List<CalculationBreakdownItem>,
    val totalEstimatedCost: BigDecimal?,
    val estimatedUnitCost: BigDecimal?,
    val currency: String = "BDT",
    val diagnostics: List<CalculationDiagnostic>,
    val integrityHash: String,
    val calculationVersion: String = "1.0.0"
)

/**
 * Downstream Verified Read-Only Handoff Contract for AI Agents & Quotation Engine.
 * Module 17 Step 01.
 */
data class Module17Step01PrintingCalculatorHandoffContract(
    val handoffId: String,
    val calculationId: String,
    val tenantId: String,
    val projectId: String,
    val generatedAt: Long,
    val contractVersion: String = "1.0.0",
    val requestFingerprint: String,
    val calculationStatus: CalculationStatus,
    val classification: EstimateActualClassification = EstimateActualClassification.ESTIMATED,
    val jobTitle: String,
    val orderedQuantity: Long,
    val finishedDimensionsMm: String,
    val substrateDetails: String,
    val totalSheetsRequired: Long,
    val totalImpressions: Long,
    val totalEstimatedCost: BigDecimal?,
    val estimatedUnitCost: BigDecimal?,
    val currency: String,
    val diagnosticsSummary: List<String>,
    val breakdownSummary: List<CalculationBreakdownItem>,
    val isReadOnly: Boolean = true,
    val handoffIntegrityHash: String
)
