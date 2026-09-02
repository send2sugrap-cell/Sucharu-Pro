package com.sucharu.sucharupro.data.api.model.imposition

import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.*
import java.math.BigDecimal

/**
 * DTO for candidate item in gang-run optimization request.
 */
data class GangRunCandidateItemDto(
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
    val targetDueDateEpochMs: Long? = null
)

/**
 * Request DTO for multi-job gang-run batching and optimization.
 */
data class OptimizeGangRunRequestDto(
    val batchName: String,
    val candidates: List<GangRunCandidateItemDto>,
    val parentSheetWidthMm: BigDecimal = BigDecimal("635.0000"),
    val parentSheetHeightMm: BigDecimal = BigDecimal("914.4000"),
    val marginTopMm: BigDecimal = BigDecimal("10.0000"),
    val marginBottomMm: BigDecimal = BigDecimal("10.0000"),
    val marginLeftMm: BigDecimal = BigDecimal("10.0000"),
    val marginRightMm: BigDecimal = BigDecimal("10.0000"),
    val bleedMm: BigDecimal = BigDecimal("3.0000"),
    val horizontalGutterMm: BigDecimal = BigDecimal("4.0000"),
    val verticalGutterMm: BigDecimal = BigDecimal("4.0000"),
    val policy: String = "STRICT_IDENTICAL_SUBSTRATE",
    val saveSpecification: Boolean = true
)

/**
 * Line item allocation DTO in gang-run response.
 */
data class GangRunItemAllocationDto(
    val jobId: String,
    val orderId: String,
    val orderItemId: String,
    val productName: String,
    val assignedSlots: Int,
    val orientation: String,
    val slotItemWidthMm: BigDecimal,
    val slotItemHeightMm: BigDecimal,
    val requiredQuantity: Long,
    val producedQuantity: Long,
    val overageQuantity: Long,
    val itemOccupiedAreaMm2: BigDecimal,
    val relativeYieldPercentage: BigDecimal
)

/**
 * Response DTO for Gang-Run Batch Specification.
 */
data class GangRunSpecificationResponseDto(
    val gangRunId: String,
    val tenantId: String,
    val batchName: String,
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
    val totalAvailableSlots: Int,
    val allocatedSlotsCount: Int,
    val commonRequiredSheets: Long,
    val totalProducedItems: Long,
    val totalOverageItems: Long,
    val usableAreaMm2: BigDecimal,
    val occupiedAreaMm2: BigDecimal,
    val wasteAreaMm2: BigDecimal,
    val sheetYieldPercentage: BigDecimal,
    val allocations: List<GangRunItemAllocationDto>,
    val version: Int,
    val status: String,
    val integrityHash: String,
    val notes: String?,
    val createdAt: Long,
    val createdBy: String
)

/**
 * Status update request DTO for Gang-Run.
 */
data class UpdateGangRunStatusRequestDto(
    val status: String,
    val notes: String? = null
)

/**
 * Handoff contract response DTO for Module 19.
 */
data class Module18Step02GangRunHandoffContractDto(
    val contractVersion: String,
    val gangRunId: String,
    val tenantId: String,
    val paperStockType: String,
    val gsm: BigDecimal,
    val parentSheetWidthMm: BigDecimal,
    val parentSheetHeightMm: BigDecimal,
    val totalParentSheetsRequired: Long,
    val totalAllocatedJobs: Int,
    val jobIds: List<String>,
    val orderItemIds: List<String>,
    val totalProducedItems: Long,
    val totalOverageItems: Long,
    val sheetYieldPercentage: BigDecimal,
    val integrityHash: String,
    val generatedAt: Long
)

// Extension mappers
fun GangRunSpecification.toDto(): GangRunSpecificationResponseDto {
    return GangRunSpecificationResponseDto(
        gangRunId = this.gangRunId,
        tenantId = this.tenantId,
        batchName = this.batchName,
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
        totalAvailableSlots = this.totalAvailableSlots,
        allocatedSlotsCount = this.allocatedSlotsCount,
        commonRequiredSheets = this.commonRequiredSheets,
        totalProducedItems = this.totalProducedItems,
        totalOverageItems = this.totalOverageItems,
        usableAreaMm2 = this.usableAreaMm2,
        occupiedAreaMm2 = this.occupiedAreaMm2,
        wasteAreaMm2 = this.wasteAreaMm2,
        sheetYieldPercentage = this.sheetYieldPercentage,
        allocations = this.allocations.map {
            GangRunItemAllocationDto(
                jobId = it.jobId,
                orderId = it.orderId,
                orderItemId = it.orderItemId,
                productName = it.productName,
                assignedSlots = it.assignedSlots,
                orientation = it.orientation.name,
                slotItemWidthMm = it.slotItemWidthMm,
                slotItemHeightMm = it.slotItemHeightMm,
                requiredQuantity = it.requiredQuantity,
                producedQuantity = it.producedQuantity,
                overageQuantity = it.overageQuantity,
                itemOccupiedAreaMm2 = it.itemOccupiedAreaMm2,
                relativeYieldPercentage = it.relativeYieldPercentage
            )
        },
        version = this.version,
        status = this.status.name,
        integrityHash = this.integrityHash,
        notes = this.notes,
        createdAt = this.createdAt,
        createdBy = this.createdBy
    )
}

fun Module18Step02GangRunHandoffContract.toDto(): Module18Step02GangRunHandoffContractDto {
    return Module18Step02GangRunHandoffContractDto(
        contractVersion = this.contractVersion,
        gangRunId = this.gangRunId,
        tenantId = this.tenantId,
        paperStockType = this.paperStockType,
        gsm = this.gsm,
        parentSheetWidthMm = this.parentSheetWidthMm,
        parentSheetHeightMm = this.parentSheetHeightMm,
        totalParentSheetsRequired = this.totalParentSheetsRequired,
        totalAllocatedJobs = this.totalAllocatedJobs,
        jobIds = this.jobIds,
        orderItemIds = this.orderItemIds,
        totalProducedItems = this.totalProducedItems,
        totalOverageItems = this.totalOverageItems,
        sheetYieldPercentage = this.sheetYieldPercentage,
        integrityHash = this.integrityHash,
        generatedAt = this.generatedAt
    )
}
