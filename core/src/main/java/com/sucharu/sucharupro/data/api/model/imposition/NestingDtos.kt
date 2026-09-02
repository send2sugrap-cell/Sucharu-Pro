package com.sucharu.sucharupro.data.api.model.imposition

import com.sucharu.sucharupro.domain.model.imposition.*
import java.math.BigDecimal

/**
 * Candidate item DTO for dynamic nesting optimization request.
 */
data class NestingCandidateItemDto(
    val jobId: String,
    val orderId: String,
    val orderItemId: String,
    val productName: String,
    val finishedWidthMm: BigDecimal,
    val finishedHeightMm: BigDecimal,
    val requiredQuantity: Long,
    val paperStockType: String,
    val gsm: BigDecimal,
    val colorMode: String = "CMYK_FOUR_COLOR",
    val printingSideOption: String = "SINGLE_SIDED",
    val allowRotation: Boolean = true,
    val priorityScore: Int = 100
)

/**
 * Request DTO for Dynamic 2D Nesting Optimization.
 */
data class OptimizeNestingRequestDto(
    val name: String,
    val candidates: List<NestingCandidateItemDto>,
    val parentSheetWidthMm: BigDecimal = BigDecimal("635.0000"),
    val parentSheetHeightMm: BigDecimal = BigDecimal("914.4000"),
    val marginTopMm: BigDecimal = BigDecimal("10.0000"),
    val marginBottomMm: BigDecimal = BigDecimal("10.0000"),
    val marginLeftMm: BigDecimal = BigDecimal("10.0000"),
    val marginRightMm: BigDecimal = BigDecimal("10.0000"),
    val bleedMm: BigDecimal = BigDecimal("3.0000"),
    val horizontalGutterMm: BigDecimal = BigDecimal("4.0000"),
    val verticalGutterMm: BigDecimal = BigDecimal("4.0000"),
    val orientationPolicy: String = "ALLOW_ROTATION",
    val placementStrategy: String = "BOTTOM_LEFT_FILL",
    val minOffcutDimensionMm: BigDecimal = BigDecimal("100.0000"),
    val saveSpecification: Boolean = true
)

/**
 * Placement line DTO in dynamic nesting response.
 */
data class NestingItemPlacementDto(
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
    val orientation: String,
    val occupiedAreaMm2: BigDecimal
)

/**
 * Offcut remnant DTO in dynamic nesting response.
 */
data class NestingOffcutRemnantDto(
    val offcutId: String,
    val xMm: BigDecimal,
    val yMm: BigDecimal,
    val widthMm: BigDecimal,
    val heightMm: BigDecimal,
    val areaMm2: BigDecimal,
    val isRecoverable: Boolean
)

/**
 * Job allocation summary DTO in dynamic nesting response.
 */
data class NestingJobAllocationSummaryDto(
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
 * Response DTO for Dynamic Nesting Specification.
 */
data class DynamicNestingSpecificationResponseDto(
    val nestingId: String,
    val tenantId: String,
    val name: String,
    val paperStockType: String,
    val gsm: BigDecimal,
    val colorMode: String,
    val printingSideOption: String,
    val sheetWidthMm: BigDecimal,
    val sheetHeightMm: BigDecimal,
    val marginTopMm: BigDecimal,
    val marginBottomMm: BigDecimal,
    val marginLeftMm: BigDecimal,
    val marginRightMm: BigDecimal,
    val bleedMm: BigDecimal,
    val horizontalGutterMm: BigDecimal,
    val verticalGutterMm: BigDecimal,
    val orientationPolicy: String,
    val placementStrategy: String,
    val usableWidthMm: BigDecimal,
    val usableHeightMm: BigDecimal,
    val placements: List<NestingItemPlacementDto>,
    val offcutRemnants: List<NestingOffcutRemnantDto>,
    val jobSummaries: List<NestingJobAllocationSummaryDto>,
    val totalItemsPlaced: Int,
    val commonRequiredSheets: Long,
    val totalProducedItems: Long,
    val totalOverageItems: Long,
    val totalSheetAreaMm2: BigDecimal,
    val usableAreaMm2: BigDecimal,
    val occupiedAreaMm2: BigDecimal,
    val wasteAreaMm2: BigDecimal,
    val recoverableOffcutAreaMm2: BigDecimal,
    val sheetUtilizationPercentage: BigDecimal,
    val usableYieldPercentage: BigDecimal,
    val offcutRecoveryPercentage: BigDecimal,
    val version: Int,
    val status: String,
    val integrityHash: String,
    val notes: String?,
    val createdAt: Long,
    val createdBy: String
)

/**
 * Status update request DTO for Dynamic Nesting.
 */
data class UpdateNestingStatusRequestDto(
    val status: String,
    val notes: String? = null
)

/**
 * Handoff contract response DTO for Module 19.
 */
data class Module18Step03NestingHandoffContractDto(
    val contractVersion: String,
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
    val generatedAt: Long
)

// Extension mappers
fun DynamicNestingSpecification.toDto(): DynamicNestingSpecificationResponseDto {
    return DynamicNestingSpecificationResponseDto(
        nestingId = this.nestingId,
        tenantId = this.tenantId,
        name = this.name,
        paperStockType = this.paperStockType.name,
        gsm = this.gsm,
        colorMode = this.colorMode.name,
        printingSideOption = this.printingSideOption.name,
        sheetWidthMm = this.parentSheetDimension.width,
        sheetHeightMm = this.parentSheetDimension.height,
        marginTopMm = this.marginSpec.topMm,
        marginBottomMm = this.marginSpec.bottomMm,
        marginLeftMm = this.marginSpec.leftMm,
        marginRightMm = this.marginSpec.rightMm,
        bleedMm = this.spacingSpec.bleedMm,
        horizontalGutterMm = this.spacingSpec.horizontalGutterMm,
        verticalGutterMm = this.spacingSpec.verticalGutterMm,
        orientationPolicy = this.orientationPolicy.name,
        placementStrategy = this.placementStrategy.name,
        usableWidthMm = this.usableWidthMm,
        usableHeightMm = this.usableHeightMm,
        placements = this.placements.map {
            NestingItemPlacementDto(
                placementId = it.placementId,
                slotIndex = it.slotIndex,
                jobId = it.jobId,
                orderId = it.orderId,
                orderItemId = it.orderItemId,
                productName = it.productName,
                xMm = it.xMm,
                yMm = it.yMm,
                placedWidthMm = it.placedWidthMm,
                placedHeightMm = it.placedHeightMm,
                orientation = it.orientation.name,
                occupiedAreaMm2 = it.occupiedAreaMm2
            )
        },
        offcutRemnants = this.offcutRemnants.map {
            NestingOffcutRemnantDto(
                offcutId = it.offcutId,
                xMm = it.xMm,
                yMm = it.yMm,
                widthMm = it.widthMm,
                heightMm = it.heightMm,
                areaMm2 = it.areaMm2,
                isRecoverable = it.isRecoverable
            )
        },
        jobSummaries = this.jobSummaries.map {
            NestingJobAllocationSummaryDto(
                jobId = it.jobId,
                orderId = it.orderId,
                orderItemId = it.orderItemId,
                productName = it.productName,
                assignedCopiesOnSheet = it.assignedCopiesOnSheet,
                requiredQuantity = it.requiredQuantity,
                producedQuantity = it.producedQuantity,
                overageQuantity = it.overageQuantity,
                totalOccupiedAreaMm2 = it.totalOccupiedAreaMm2,
                relativeYieldPercentage = it.relativeYieldPercentage
            )
        },
        totalItemsPlaced = this.totalItemsPlaced,
        commonRequiredSheets = this.commonRequiredSheets,
        totalProducedItems = this.totalProducedItems,
        totalOverageItems = this.totalOverageItems,
        totalSheetAreaMm2 = this.totalSheetAreaMm2,
        usableAreaMm2 = this.usableAreaMm2,
        occupiedAreaMm2 = this.occupiedAreaMm2,
        wasteAreaMm2 = this.wasteAreaMm2,
        recoverableOffcutAreaMm2 = this.recoverableOffcutAreaMm2,
        sheetUtilizationPercentage = this.sheetUtilizationPercentage,
        usableYieldPercentage = this.usableYieldPercentage,
        offcutRecoveryPercentage = this.offcutRecoveryPercentage,
        version = this.version,
        status = this.status.name,
        integrityHash = this.integrityHash,
        notes = this.notes,
        createdAt = this.createdAt,
        createdBy = this.createdBy
    )
}

fun Module18Step03NestingHandoffContract.toDto(): Module18Step03NestingHandoffContractDto {
    return Module18Step03NestingHandoffContractDto(
        contractVersion = this.contractVersion,
        nestingId = this.nestingId,
        tenantId = this.tenantId,
        paperStockType = this.paperStockType,
        gsm = this.gsm,
        parentSheetWidthMm = this.parentSheetWidthMm,
        parentSheetHeightMm = this.parentSheetHeightMm,
        totalParentSheetsRequired = this.totalParentSheetsRequired,
        totalPlacedItems = this.totalPlacedItems,
        distinctJobIds = this.distinctJobIds,
        orderItemIds = this.orderItemIds,
        totalProducedItems = this.totalProducedItems,
        totalOverageItems = this.totalOverageItems,
        sheetUtilizationPercentage = this.sheetUtilizationPercentage,
        usableYieldPercentage = this.usableYieldPercentage,
        recoverableOffcutAreaMm2 = this.recoverableOffcutAreaMm2,
        integrityHash = this.integrityHash,
        generatedAt = this.generatedAt
    )
}
