package com.sucharu.sucharupro.data.api.model.imposition

import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import java.math.BigDecimal

/**
 * Request DTO to compute an optimal single-job imposition layout.
 */
data class CalculateImpositionRequestDto(
    val jobId: String? = null,
    val orderId: String,
    val orderItemId: String,
    val calculationId: String? = null,
    val productName: String,
    val finishedItemWidth: BigDecimal,
    val finishedItemHeight: BigDecimal,
    val finishedItemUnit: String = "MILLIMETERS",
    val parentSheetWidth: BigDecimal,
    val parentSheetHeight: BigDecimal,
    val parentSheetUnit: String = "MILLIMETERS",
    val marginTopMm: BigDecimal = BigDecimal("10.0000"),
    val marginBottomMm: BigDecimal = BigDecimal("10.0000"),
    val marginLeftMm: BigDecimal = BigDecimal("10.0000"),
    val marginRightMm: BigDecimal = BigDecimal("10.0000"),
    val bleedMm: BigDecimal = BigDecimal("3.0000"),
    val horizontalGutterMm: BigDecimal = BigDecimal("4.0000"),
    val verticalGutterMm: BigDecimal = BigDecimal("4.0000"),
    val orientationPolicy: String = "AUTO_OPTIMAL",
    val requiredQuantity: Long,
    val notes: String? = null,
    val saveSpecification: Boolean = true
)

/**
 * Candidate breakdown DTO.
 */
data class ImpositionCandidateDto(
    val orientation: String,
    val columns: Int,
    val rows: Int,
    val copiesPerSheet: Int,
    val usableWidthMm: BigDecimal,
    val usableHeightMm: BigDecimal,
    val occupiedAreaMm2: BigDecimal,
    val usableAreaMm2: BigDecimal,
    val wasteAreaMm2: BigDecimal,
    val yieldPercentage: BigDecimal,
    val isFeasible: Boolean
)

/**
 * Authoritative response DTO for an Imposition Specification.
 */
data class ImpositionSpecificationResponseDto(
    val impositionId: String,
    val tenantId: String,
    val jobId: String?,
    val orderId: String,
    val orderItemId: String,
    val calculationId: String?,
    val productName: String,
    val finishedItemWidthMm: BigDecimal,
    val finishedItemHeightMm: BigDecimal,
    val parentSheetWidthMm: BigDecimal,
    val parentSheetHeightMm: BigDecimal,
    val usableWidthMm: BigDecimal,
    val usableHeightMm: BigDecimal,
    val marginTopMm: BigDecimal,
    val marginBottomMm: BigDecimal,
    val marginLeftMm: BigDecimal,
    val marginRightMm: BigDecimal,
    val bleedMm: BigDecimal,
    val horizontalGutterMm: BigDecimal,
    val verticalGutterMm: BigDecimal,
    val orientationPolicy: String,
    val selectedOrientation: String,
    val columns: Int,
    val rows: Int,
    val copiesPerSheet: Int,
    val requiredQuantity: Long,
    val requiredSheets: Long,
    val totalProducedCapacity: Long,
    val overageQuantity: Long,
    val occupiedAreaMm2: BigDecimal,
    val usableAreaMm2: BigDecimal,
    val wasteAreaMm2: BigDecimal,
    val yieldPercentage: BigDecimal,
    val version: Int,
    val status: String,
    val integrityHash: String,
    val notes: String?,
    val candidates: List<ImpositionCandidateDto> = emptyList(),
    val createdAt: Long,
    val createdBy: String
)

/**
 * Status update request DTO.
 */
data class UpdateImpositionStatusRequestDto(
    val status: String,
    val notes: String? = null
)

fun ImpositionSpecification.toDto(): ImpositionSpecificationResponseDto {
    return ImpositionSpecificationResponseDto(
        impositionId = this.impositionId,
        tenantId = this.tenantId,
        jobId = this.jobId,
        orderId = this.orderId,
        orderItemId = this.orderItemId,
        calculationId = this.calculationId,
        productName = this.productName,
        finishedItemWidthMm = this.finishedItemDimension.width,
        finishedItemHeightMm = this.finishedItemDimension.height,
        parentSheetWidthMm = this.parentSheetDimension.width,
        parentSheetHeightMm = this.parentSheetDimension.height,
        usableWidthMm = this.usableWidthMm,
        usableHeightMm = this.usableHeightMm,
        marginTopMm = this.marginSpec.topMm,
        marginBottomMm = this.marginSpec.bottomMm,
        marginLeftMm = this.marginSpec.leftMm,
        marginRightMm = this.marginSpec.rightMm,
        bleedMm = this.spacingSpec.bleedMm,
        horizontalGutterMm = this.spacingSpec.horizontalGutterMm,
        verticalGutterMm = this.spacingSpec.verticalGutterMm,
        orientationPolicy = this.orientationPolicy.name,
        selectedOrientation = this.selectedOrientation.name,
        columns = this.columns,
        rows = this.rows,
        copiesPerSheet = this.copiesPerSheet,
        requiredQuantity = this.requiredQuantity,
        requiredSheets = this.requiredSheets,
        totalProducedCapacity = this.totalProducedCapacity,
        overageQuantity = this.overageQuantity,
        occupiedAreaMm2 = this.occupiedAreaMm2,
        usableAreaMm2 = this.usableAreaMm2,
        wasteAreaMm2 = this.wasteAreaMm2,
        yieldPercentage = this.yieldPercentage,
        version = this.version,
        status = this.status.name,
        integrityHash = this.integrityHash,
        notes = this.notes,
        candidates = this.candidateBreakdown.map {
            ImpositionCandidateDto(
                orientation = it.orientation.name,
                columns = it.columns,
                rows = it.rows,
                copiesPerSheet = it.copiesPerSheet,
                usableWidthMm = it.usableWidthMm,
                usableHeightMm = it.usableHeightMm,
                occupiedAreaMm2 = it.occupiedAreaMm2,
                usableAreaMm2 = it.usableAreaMm2,
                wasteAreaMm2 = it.wasteAreaMm2,
                yieldPercentage = it.yieldPercentage,
                isFeasible = it.isFeasible
            )
        },
        createdAt = this.createdAt,
        createdBy = this.createdBy
    )
}
