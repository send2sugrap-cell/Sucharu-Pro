package com.sucharu.sucharupro.data.api.model.imposition

import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import java.math.BigDecimal

/**
 * Request DTO for Multi-Page Signature Imposition Optimization.
 * Module 18 Step 04.
 */
data class OptimizeSignatureRequestDto(
    val name: String,
    val jobId: String,
    val orderId: String,
    val orderItemId: String,
    val productName: String,
    val totalPages: Int,
    val signaturePageCount: Int = 16,
    val bindingMethod: String = "SADDLE_STITCH",
    val sheetTurningMethod: String = "SHEETWISE",
    val foldingScheme: String = "RIGHT_ANGLE_16PP",
    val pageWidthMm: BigDecimal = BigDecimal("210.0000"),
    val pageHeightMm: BigDecimal = BigDecimal("297.0000"),
    val parentSheetWidthMm: BigDecimal = BigDecimal("635.0000"),
    val parentSheetHeightMm: BigDecimal = BigDecimal("914.4000"),
    val requiredQuantity: Long = 1000L,
    val paperStockType: String = "ART_PAPER",
    val gsm: BigDecimal = BigDecimal("150.0000"),
    val customCaliperMm: BigDecimal? = null,
    val marginTopMm: BigDecimal = BigDecimal("10.0000"),
    val marginBottomMm: BigDecimal = BigDecimal("10.0000"),
    val marginLeftMm: BigDecimal = BigDecimal("10.0000"),
    val marginRightMm: BigDecimal = BigDecimal("10.0000"),
    val spineGutterMm: BigDecimal = BigDecimal("6.0000"),
    val headGutterMm: BigDecimal = BigDecimal("10.0000"),
    val footGutterMm: BigDecimal = BigDecimal("8.0000"),
    val faceTrimMm: BigDecimal = BigDecimal("6.0000"),
    val bleedMm: BigDecimal = BigDecimal("3.0000"),
    val enableCreepCompensation: Boolean = true,
    val saveSpecification: Boolean = true
)

/**
 * Page placement DTO on a signature form.
 * Module 18 Step 04.
 */
data class SignaturePagePlacementDto(
    val placementId: String,
    val pageNumber: Int,
    val slotIndex: Int,
    val row: Int,
    val column: Int,
    val xMm: BigDecimal,
    val yMm: BigDecimal,
    val widthMm: BigDecimal,
    val heightMm: BigDecimal,
    val headOrientation: String,
    val creepShiftXMm: BigDecimal,
    val creepShiftYMm: BigDecimal,
    val isBlankPage: Boolean
)

/**
 * Signature form DTO (front, back, or combined work-and-turn plate).
 * Module 18 Step 04.
 */
data class SignatureFormDto(
    val formId: String,
    val signatureNumber: Int,
    val formSide: String,
    val pagesPerSide: Int,
    val columns: Int,
    val rows: Int,
    val pagePlacements: List<SignaturePagePlacementDto>,
    val formSheetWidthMm: BigDecimal,
    val formSheetHeightMm: BigDecimal,
    val occupiedAreaMm2: BigDecimal,
    val usableAreaMm2: BigDecimal,
    val yieldPercentage: BigDecimal
)

/**
 * Creep compensation summary DTO.
 * Module 18 Step 04.
 */
data class CreepCompensationSummaryDto(
    val isEnabled: Boolean,
    val paperCaliperMm: BigDecimal,
    val totalCreepMm: BigDecimal,
    val creepPerSheetMm: BigDecimal,
    val innermostPageShiftMm: BigDecimal
)

/**
 * Complete Response DTO for Signature Imposition Specification.
 * Module 18 Step 04.
 */
data class SignatureImpositionSpecificationResponseDto(
    val signatureImpositionId: String,
    val tenantId: String,
    val name: String,
    val jobId: String,
    val orderId: String,
    val orderItemId: String,
    val productName: String,
    val totalPages: Int,
    val paddedTotalPages: Int,
    val signaturePageCount: Int,
    val totalSignaturesCount: Int,
    val bindingMethod: String,
    val sheetTurningMethod: String,
    val foldingScheme: String,
    val paperStockType: String,
    val gsm: BigDecimal,
    val pageWidthMm: BigDecimal,
    val pageHeightMm: BigDecimal,
    val parentSheetWidthMm: BigDecimal,
    val parentSheetHeightMm: BigDecimal,
    val marginTopMm: BigDecimal,
    val marginBottomMm: BigDecimal,
    val marginLeftMm: BigDecimal,
    val marginRightMm: BigDecimal,
    val spineGutterMm: BigDecimal,
    val headGutterMm: BigDecimal,
    val footGutterMm: BigDecimal,
    val faceTrimMm: BigDecimal,
    val bleedMm: BigDecimal,
    val creepSummary: CreepCompensationSummaryDto,
    val signatureForms: List<SignatureFormDto>,
    val commonRequiredSheets: Long,
    val totalParentSheetsRequired: Long,
    val totalProducedCopies: Long,
    val overageCopies: Long,
    val totalSheetAreaMm2: BigDecimal,
    val usableAreaMm2: BigDecimal,
    val occupiedAreaMm2: BigDecimal,
    val wasteAreaMm2: BigDecimal,
    val sheetUtilizationPercentage: BigDecimal,
    val usableYieldPercentage: BigDecimal,
    val version: Int,
    val status: String,
    val integrityHash: String,
    val notes: String,
    val createdAt: Long,
    val createdBy: String
)

/**
 * Status mutation request DTO.
 * Module 18 Step 04.
 */
data class UpdateSignatureStatusRequestDto(
    val status: String,
    val notes: String? = null
)

/**
 * Handoff response DTO for Module 19 substrate reservation.
 * Module 18 Step 04.
 */
data class Module18Step04SignatureHandoffResponseDto(
    val contractVersion: String,
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
    val emittedAt: Long
)

// Extensions for Mapping Domain <-> DTO

fun SignaturePagePlacement.toDto(): SignaturePagePlacementDto = SignaturePagePlacementDto(
    placementId = placementId,
    pageNumber = pageNumber,
    slotIndex = slotIndex,
    row = row,
    column = column,
    xMm = xMm,
    yMm = yMm,
    widthMm = widthMm,
    heightMm = heightMm,
    headOrientation = headOrientation.name,
    creepShiftXMm = creepShiftXMm,
    creepShiftYMm = creepShiftYMm,
    isBlankPage = isBlankPage
)

fun SignatureForm.toDto(): SignatureFormDto = SignatureFormDto(
    formId = formId,
    signatureNumber = signatureNumber,
    formSide = formSide.name,
    pagesPerSide = pagesPerSide,
    columns = columns,
    rows = rows,
    pagePlacements = pagePlacements.map { it.toDto() },
    formSheetWidthMm = formSheetWidthMm,
    formSheetHeightMm = formSheetHeightMm,
    occupiedAreaMm2 = occupiedAreaMm2,
    usableAreaMm2 = usableAreaMm2,
    yieldPercentage = yieldPercentage
)

fun CreepCompensationSummary.toDto(): CreepCompensationSummaryDto = CreepCompensationSummaryDto(
    isEnabled = isEnabled,
    paperCaliperMm = paperCaliperMm,
    totalCreepMm = totalCreepMm,
    creepPerSheetMm = creepPerSheetMm,
    innermostPageShiftMm = innermostPageShiftMm
)

fun SignatureImpositionSpecification.toDto(): SignatureImpositionSpecificationResponseDto =
    SignatureImpositionSpecificationResponseDto(
        signatureImpositionId = signatureImpositionId,
        tenantId = tenantId,
        name = name,
        jobId = jobId,
        orderId = orderId,
        orderItemId = orderItemId,
        productName = productName,
        totalPages = totalPages,
        paddedTotalPages = paddedTotalPages,
        signaturePageCount = signaturePageCount,
        totalSignaturesCount = totalSignaturesCount,
        bindingMethod = bindingMethod.name,
        sheetTurningMethod = sheetTurningMethod.name,
        foldingScheme = foldingScheme.name,
        paperStockType = paperStockType.name,
        gsm = gsm,
        pageWidthMm = pageDimension.width,
        pageHeightMm = pageDimension.height,
        parentSheetWidthMm = parentSheetDimension.width,
        parentSheetHeightMm = parentSheetDimension.height,
        marginTopMm = marginSpec.topMm,
        marginBottomMm = marginSpec.bottomMm,
        marginLeftMm = marginSpec.leftMm,
        marginRightMm = marginSpec.rightMm,
        spineGutterMm = gutterSpec.spineGutterMm,
        headGutterMm = gutterSpec.headGutterMm,
        footGutterMm = gutterSpec.footGutterMm,
        faceTrimMm = gutterSpec.faceTrimMm,
        bleedMm = gutterSpec.bleedMm,
        creepSummary = creepSummary.toDto(),
        signatureForms = signatureForms.map { it.toDto() },
        commonRequiredSheets = commonRequiredSheets,
        totalParentSheetsRequired = totalParentSheetsRequired,
        totalProducedCopies = totalProducedCopies,
        overageCopies = overageCopies,
        totalSheetAreaMm2 = totalSheetAreaMm2,
        usableAreaMm2 = usableAreaMm2,
        occupiedAreaMm2 = occupiedAreaMm2,
        wasteAreaMm2 = wasteAreaMm2,
        sheetUtilizationPercentage = sheetUtilizationPercentage,
        usableYieldPercentage = usableYieldPercentage,
        version = version,
        status = status.name,
        integrityHash = integrityHash,
        notes = notes,
        createdAt = createdAt,
        createdBy = createdBy
    )

fun Module18Step04SignatureHandoffContract.toDto(): Module18Step04SignatureHandoffResponseDto =
    Module18Step04SignatureHandoffResponseDto(
        contractVersion = contractVersion,
        signatureImpositionId = signatureImpositionId,
        tenantId = tenantId,
        jobId = jobId,
        orderId = orderId,
        orderItemId = orderItemId,
        productName = productName,
        totalSignatures = totalSignatures,
        signaturePageCount = signaturePageCount,
        paperStockType = paperStockType,
        gsm = gsm,
        parentSheetWidthMm = parentSheetWidthMm,
        parentSheetHeightMm = parentSheetHeightMm,
        sheetsPerSignature = sheetsPerSignature,
        totalParentSheetsRequired = totalParentSheetsRequired,
        bindingMethod = bindingMethod,
        sheetTurningMethod = sheetTurningMethod,
        integrityHash = integrityHash,
        emittedAt = emittedAt
    )
