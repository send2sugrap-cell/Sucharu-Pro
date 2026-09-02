package com.sucharu.sucharupro.data.api.model.imposition

import com.sucharu.sucharupro.domain.model.imposition.*
import java.math.BigDecimal

/**
 * Request DTO to generate a CTP Output Package from an approved Signature Imposition.
 * Module 18 Step 05.
 */
data class GenerateCtpFromSignatureRequestDto(
    val signatureImpositionId: String,
    val plateWidthMm: BigDecimal? = null,
    val plateHeightMm: BigDecimal? = null,
    val plateThicknessMm: BigDecimal = BigDecimal("0.3000"),
    val gripperMarginMm: BigDecimal = BigDecimal("45.0000"),
    val tailMarginMm: BigDecimal = BigDecimal("25.0000"),
    val sideGuideMarginLeftMm: BigDecimal = BigDecimal("30.0000"),
    val sideGuideMarginRightMm: BigDecimal = BigDecimal("30.0000"),
    val resolutionDpi: Int = 2540,
    val screeningMethod: String = "AM_CONVENTIONAL",
    val screenRulingLpi: BigDecimal = BigDecimal("175.0000"),
    val includeRegistrationMarks: Boolean = true,
    val includeCropMarks: Boolean = true,
    val includeBleedMarks: Boolean = true,
    val includeColorBars: Boolean = true,
    val includePlateSlugs: Boolean = true,
    val colorSeparations: List<String> = listOf("CYAN", "MAGENTA", "YELLOW", "BLACK"),
    val spotColorNames: List<String> = emptyList()
)

/**
 * Request DTO to generate a CTP Output Package from an approved Single-Job Imposition.
 * Module 18 Step 05.
 */
data class GenerateCtpFromSingleJobRequestDto(
    val impositionId: String,
    val plateWidthMm: BigDecimal? = null,
    val plateHeightMm: BigDecimal? = null,
    val plateThicknessMm: BigDecimal = BigDecimal("0.3000"),
    val gripperMarginMm: BigDecimal = BigDecimal("45.0000"),
    val tailMarginMm: BigDecimal = BigDecimal("25.0000"),
    val sideGuideMarginLeftMm: BigDecimal = BigDecimal("30.0000"),
    val sideGuideMarginRightMm: BigDecimal = BigDecimal("30.0000"),
    val resolutionDpi: Int = 2540,
    val screeningMethod: String = "AM_CONVENTIONAL",
    val screenRulingLpi: BigDecimal = BigDecimal("175.0000"),
    val includeRegistrationMarks: Boolean = true,
    val includeCropMarks: Boolean = true,
    val includeBleedMarks: Boolean = true,
    val includeColorBars: Boolean = true,
    val includePlateSlugs: Boolean = true,
    val colorSeparations: List<String> = listOf("CYAN", "MAGENTA", "YELLOW", "BLACK")
)

/**
 * Prepress mark placement DTO.
 * Module 18 Step 05.
 */
data class PrepressMarkPlacementDto(
    val markId: String,
    val markType: String,
    val plateSide: String,
    val xPositionMm: BigDecimal,
    val yPositionMm: BigDecimal,
    val widthMm: BigDecimal,
    val heightMm: BigDecimal,
    val rotationDegrees: BigDecimal,
    val labelText: String?,
    val targetColorSeparation: String?
)

/**
 * Plate specification DTO.
 * Module 18 Step 05.
 */
data class PlateSpecificationDto(
    val plateId: String,
    val plateName: String,
    val formReferenceId: String,
    val signatureNumber: Int,
    val plateSide: String,
    val colorSeparation: String,
    val spotColorName: String?,
    val plateWidthMm: BigDecimal,
    val plateHeightMm: BigDecimal,
    val plateThicknessMm: BigDecimal,
    val resolutionDpi: Int,
    val screeningMethod: String,
    val screenRulingLpi: BigDecimal,
    val screenAngleDegrees: BigDecimal,
    val dotShape: String,
    val sheetOffsetXMm: BigDecimal,
    val sheetOffsetYMm: BigDecimal,
    val plateAreaMm2: BigDecimal,
    val plateIntegrityHash: String,
    val marksCount: Int
)

/**
 * CTP Output Package DTO.
 * Module 18 Step 05.
 */
data class CtpOutputPackageDto(
    val packageId: String,
    val packageVersion: Int,
    val sourceImpositionType: String,
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
    val plates: List<PlateSpecificationDto>,
    val marks: List<PrepressMarkPlacementDto>,
    val ripInstructions: String,
    val validationSummary: String,
    val integrityHash: String
)

/**
 * Response DTO for CTP Output Specification.
 * Module 18 Step 05.
 */
data class CtpOutputSpecificationResponseDto(
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
    val status: String,
    val packageVersion: Int,
    val resolutionDpi: Int,
    val screeningMethod: String,
    val defaultScreenRulingLpi: BigDecimal,
    val plateWidthMm: BigDecimal,
    val plateHeightMm: BigDecimal,
    val gripperMarginMm: BigDecimal,
    val tailMarginMm: BigDecimal,
    val outputPackage: CtpOutputPackageDto,
    val integrityHash: String,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Status mutation request DTO.
 * Module 18 Step 05.
 */
data class UpdateCtpStatusRequestDto(
    val status: String,
    val reason: String? = null
)

/**
 * Extension mappers for CTP Domain Models to DTOs.
 */
fun PrepressMarkPlacement.toDto(): PrepressMarkPlacementDto = PrepressMarkPlacementDto(
    markId = markId,
    markType = markType.name,
    plateSide = plateSide.name,
    xPositionMm = xPositionMm,
    yPositionMm = yPositionMm,
    widthMm = widthMm,
    heightMm = heightMm,
    rotationDegrees = rotationDegrees,
    labelText = labelText,
    targetColorSeparation = targetColorSeparation?.name
)

fun PlateSpecification.toDto(): PlateSpecificationDto = PlateSpecificationDto(
    plateId = plateId,
    plateName = plateName,
    formReferenceId = formReferenceId,
    signatureNumber = signatureNumber,
    plateSide = plateSide.name,
    colorSeparation = colorSeparation.name,
    spotColorName = spotColorName,
    plateWidthMm = plateWidthMm,
    plateHeightMm = plateHeightMm,
    plateThicknessMm = plateThicknessMm,
    resolutionDpi = resolutionDpi.dpi,
    screeningMethod = screeningMethod.name,
    screenRulingLpi = screenRulingLpi,
    screenAngleDegrees = screenAngleDegrees,
    dotShape = dotShape,
    sheetOffsetXMm = sheetOffsetXMm,
    sheetOffsetYMm = sheetOffsetYMm,
    plateAreaMm2 = plateAreaMm2,
    plateIntegrityHash = plateIntegrityHash,
    marksCount = marks.size
)

fun CtpOutputPackage.toDto(): CtpOutputPackageDto = CtpOutputPackageDto(
    packageId = packageId,
    packageVersion = packageVersion,
    sourceImpositionType = sourceImpositionType,
    sourceImpositionId = sourceImpositionId,
    sourceIntegrityHash = sourceIntegrityHash,
    totalPlatesCount = totalPlatesCount,
    frontPlatesCount = frontPlatesCount,
    backPlatesCount = backPlatesCount,
    spotColorsCount = spotColorsCount,
    pressSheetWidthMm = pressSheetWidthMm,
    pressSheetHeightMm = pressSheetHeightMm,
    plateWidthMm = plateWidthMm,
    plateHeightMm = plateHeightMm,
    gripperMarginMm = gripperMarginMm,
    tailMarginMm = tailMarginMm,
    sideGuideMarginLeftMm = sideGuideMarginLeftMm,
    sideGuideMarginRightMm = sideGuideMarginRightMm,
    plates = plates.map { it.toDto() },
    marks = globalMarks.map { it.toDto() },
    ripInstructions = ripInstructions,
    validationSummary = validationSummary,
    integrityHash = integrityHash
)

fun CtpOutputSpecification.toDto(): CtpOutputSpecificationResponseDto = CtpOutputSpecificationResponseDto(
    ctpOutputId = ctpOutputId,
    tenantId = tenantId,
    name = name,
    jobId = jobId,
    orderId = orderId,
    orderItemId = orderItemId,
    productName = productName,
    sourceImpositionType = sourceImpositionType,
    sourceImpositionId = sourceImpositionId,
    sourceImpositionHash = sourceImpositionHash,
    status = status.name,
    packageVersion = packageVersion,
    resolutionDpi = resolutionDpi.dpi,
    screeningMethod = screeningMethod.name,
    defaultScreenRulingLpi = defaultScreenRulingLpi,
    plateWidthMm = plateDimensionSpec.plateWidthMm,
    plateHeightMm = plateDimensionSpec.plateHeightMm,
    gripperMarginMm = plateDimensionSpec.gripperMarginMm,
    tailMarginMm = plateDimensionSpec.tailMarginMm,
    outputPackage = outputPackage.toDto(),
    integrityHash = integrityHash,
    notes = notes,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString()
)
