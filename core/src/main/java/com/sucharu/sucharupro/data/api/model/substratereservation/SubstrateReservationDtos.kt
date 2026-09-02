package com.sucharu.sucharupro.data.api.model.substratereservation

import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperMaterialSpecification
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.model.substratereservation.*
import java.math.BigDecimal

data class ResolveSubstrateRequirementRequestDto(
    val orderId: String,
    val orderItemId: String,
    val calculationId: String? = null,
    val materialCode: String? = null,
    val materialName: String,
    val stockType: String = "ART_PAPER",
    val gsm: BigDecimal = BigDecimal("120.0000"),
    val sheetWidthMm: BigDecimal = BigDecimal("635.0000"),
    val sheetHeightMm: BigDecimal = BigDecimal("914.4000"),
    val productiveSheetsRequired: Long,
    val wasteSheetsRequired: Long = 0L,
    val grainDirection: String = "LONG_GRAIN",
    val warehouseId: String? = "WH-MAIN-01",
    val warehouseName: String? = "Central Paper Warehouse"
)

data class CreateSubstrateReservationRequestDto(
    val orderId: String,
    val orderItemId: String,
    val executionJobId: String? = null,
    val workOrderId: String? = null,
    val productId: String,
    val sku: String,
    val productName: String,
    val warehouseId: String = "WH-MAIN-01",
    val locationId: String? = null,
    val stockType: String = "ART_PAPER",
    val gsm: BigDecimal = BigDecimal("120.0000"),
    val sheetWidthMm: BigDecimal = BigDecimal("635.0000"),
    val sheetHeightMm: BigDecimal = BigDecimal("914.4000"),
    val totalSheetsRequired: Long,
    val isHardAllocation: Boolean = false,
    val notes: String? = null
)

data class CreateSoftReservationRequestDto(
    val orderId: String,
    val orderItemId: String,
    val productId: String,
    val sku: String,
    val productName: String,
    val warehouseId: String = "WH-MAIN-01",
    val locationId: String? = null,
    val stockType: String = "ART_PAPER",
    val gsm: BigDecimal = BigDecimal("120.0000"),
    val sheetWidthMm: BigDecimal = BigDecimal("635.0000"),
    val sheetHeightMm: BigDecimal = BigDecimal("914.4000"),
    val totalSheetsRequired: Long,
    val softHoldDurationMinutes: Long = 120L,
    val notes: String? = null
)

data class CreateHardReservationRequestDto(
    val orderId: String,
    val orderItemId: String,
    val executionJobId: String,
    val workOrderId: String? = null,
    val productId: String,
    val sku: String,
    val productName: String,
    val warehouseId: String = "WH-MAIN-01",
    val locationId: String? = null,
    val batchNumber: String? = null,
    val stockType: String = "ART_PAPER",
    val gsm: BigDecimal = BigDecimal("120.0000"),
    val sheetWidthMm: BigDecimal = BigDecimal("635.0000"),
    val sheetHeightMm: BigDecimal = BigDecimal("914.4000"),
    val totalSheetsRequired: Long,
    val notes: String? = null
)

data class PromoteSoftReservationRequestDto(
    val executionJobId: String,
    val workOrderId: String? = null,
    val allocatedWarehouseId: String? = null,
    val allocatedLocationId: String? = null,
    val allocatedBatchNumber: String? = null
)

data class AllocateReservationSourceRequestDto(
    val sources: List<SubstrateAllocationSourceInputDto>
)

data class SubstrateAllocationSourceInputDto(
    val warehouseId: String,
    val locationId: String? = null,
    val batchNumber: String? = null,
    val allocatedSheets: Long,
    val gsm: BigDecimal = BigDecimal("120.0000"),
    val sheetWidthMm: BigDecimal = BigDecimal("635.0000"),
    val sheetHeightMm: BigDecimal = BigDecimal("914.4000")
)

data class SubstrateAllocationSourceDto(
    val allocationId: String,
    val reservationId: String,
    val tenantId: String,
    val warehouseId: String,
    val locationId: String?,
    val batchNumber: String?,
    val allocatedSheets: Long,
    val allocatedReams: BigDecimal,
    val allocatedWeightKg: BigDecimal,
    val allocatedAt: Long,
    val allocatedBy: String
)

data class SubstrateSkuResolutionResponseDto(
    val resolutionId: String,
    val tenantId: String,
    val requirement: SubstrateRequirementDto,
    val matchedProductId: String?,
    val matchedSku: String?,
    val matchedProductName: String?,
    val warehouseId: String?,
    val warehouseName: String?,
    val confidence: String,
    val onHandPhysicalSheets: Long,
    val currentlyReservedSheets: Long,
    val availableReservableSheets: Long,
    val isSufficientStockAvailable: Boolean,
    val missingDeficitSheets: Long,
    val diagnosticReason: String?
)

data class SubstrateRequirementDto(
    val requirementId: String,
    val tenantId: String,
    val orderId: String,
    val orderItemId: String,
    val calculationId: String?,
    val stockType: String,
    val requestedMaterialCode: String?,
    val requestedMaterialName: String,
    val gsm: BigDecimal,
    val sheetWidthMm: BigDecimal,
    val sheetHeightMm: BigDecimal,
    val productiveSheetsRequired: Long,
    val wasteSheetsRequired: Long,
    val totalSheetsRequired: Long,
    val totalReamsRequired: BigDecimal,
    val totalWeightKg: BigDecimal,
    val grainDirection: String
)

data class SubstrateReservationResponseDto(
    val reservationId: String,
    val tenantId: String,
    val orderId: String,
    val orderItemId: String,
    val executionJobId: String?,
    val workOrderId: String?,
    val productId: String,
    val sku: String,
    val productName: String,
    val warehouseId: String,
    val locationId: String?,
    val stockType: String,
    val gsm: BigDecimal,
    val sheetWidthMm: BigDecimal,
    val sheetHeightMm: BigDecimal,
    val reservedSheets: Long,
    val reservedReams: BigDecimal,
    val reservedWeightKg: BigDecimal,
    val status: String,
    val mode: String,
    val idempotencyKey: String,
    val expiryTimestamp: Long?,
    val softHoldExpiresAt: Long?,
    val promotedAt: Long?,
    val promotedBy: String?,
    val reservedBy: String,
    val reservedAt: Long,
    val updatedAt: Long,
    val notes: String?,
    val allocationSources: List<SubstrateAllocationSourceDto> = emptyList()
)

data class SubstrateRealTimeAvailabilityResponseDto(
    val tenantId: String,
    val sku: String,
    val warehouseId: String?,
    val onHandPhysicalSheets: Long,
    val currentlyReservedSheets: Long,
    val availableReservableSheets: Long,
    val totalActiveHoldCount: Int
)

data class Module19Step02SubstrateReservationHandoffDto(
    val contractVersion: String,
    val tenantId: String,
    val reservationId: String,
    val orderId: String,
    val orderItemId: String,
    val executionJobId: String?,
    val sku: String,
    val productName: String,
    val gsm: BigDecimal,
    val sheetWidthMm: BigDecimal,
    val sheetHeightMm: BigDecimal,
    val reservedSheets: Long,
    val reservedReams: BigDecimal,
    val reservedWeightKg: BigDecimal,
    val mode: String,
    val status: String,
    val isHardAllocated: Boolean,
    val softHoldExpiresAt: Long?,
    val promotedAt: Long?,
    val promotedBy: String?,
    val allocationSourcesCount: Int,
    val reservedBy: String,
    val timestamp: Long
)

fun SubstrateRequirement.toDto(): SubstrateRequirementDto = SubstrateRequirementDto(
    requirementId = requirementId,
    tenantId = tenantId,
    orderId = orderId,
    orderItemId = orderItemId,
    calculationId = calculationId,
    stockType = stockType.name,
    requestedMaterialCode = requestedMaterialCode,
    requestedMaterialName = requestedMaterialName,
    gsm = gsm,
    sheetWidthMm = sheetDimension.width,
    sheetHeightMm = sheetDimension.height,
    productiveSheetsRequired = productiveSheetsRequired,
    wasteSheetsRequired = wasteSheetsRequired,
    totalSheetsRequired = totalSheetsRequired,
    totalReamsRequired = totalReamsRequired,
    totalWeightKg = totalWeightKg,
    grainDirection = grainDirection
)

fun SubstrateSkuResolutionResult.toDto(): SubstrateSkuResolutionResponseDto = SubstrateSkuResolutionResponseDto(
    resolutionId = resolutionId,
    tenantId = tenantId,
    requirement = requirement.toDto(),
    matchedProductId = matchedProductId,
    matchedSku = matchedSku,
    matchedProductName = matchedProductName,
    warehouseId = warehouseId,
    warehouseName = warehouseName,
    confidence = confidence.name,
    onHandPhysicalSheets = onHandPhysicalSheets,
    currentlyReservedSheets = currentlyReservedSheets,
    availableReservableSheets = availableReservableSheets,
    isSufficientStockAvailable = isSufficientStockAvailable,
    missingDeficitSheets = missingDeficitSheets,
    diagnosticReason = diagnosticReason
)

fun SubstrateAllocationSource.toDto(): SubstrateAllocationSourceDto = SubstrateAllocationSourceDto(
    allocationId = allocationId,
    reservationId = reservationId,
    tenantId = tenantId,
    warehouseId = warehouseId,
    locationId = locationId,
    batchNumber = batchNumber,
    allocatedSheets = allocatedSheets,
    allocatedReams = allocatedReams,
    allocatedWeightKg = allocatedWeightKg,
    allocatedAt = allocatedAt,
    allocatedBy = allocatedBy
)

fun SubstrateReservation.toDto(): SubstrateReservationResponseDto = SubstrateReservationResponseDto(
    reservationId = reservationId,
    tenantId = tenantId,
    orderId = orderId,
    orderItemId = orderItemId,
    executionJobId = executionJobId,
    workOrderId = workOrderId,
    productId = productId,
    sku = sku,
    productName = productName,
    warehouseId = warehouseId,
    locationId = locationId,
    stockType = stockType.name,
    gsm = gsm,
    sheetWidthMm = sheetDimension.width,
    sheetHeightMm = sheetDimension.height,
    reservedSheets = reservedSheets,
    reservedReams = reservedReams,
    reservedWeightKg = reservedWeightKg,
    status = status.name,
    mode = mode.name,
    idempotencyKey = idempotencyKey,
    expiryTimestamp = expiryTimestamp,
    softHoldExpiresAt = softHoldExpiresAt,
    promotedAt = promotedAt,
    promotedBy = promotedBy,
    reservedBy = reservedBy,
    reservedAt = reservedAt,
    updatedAt = updatedAt,
    notes = notes,
    allocationSources = allocationSources.map { it.toDto() }
)

fun Module19Step02SubstrateReservationHandoffContract.toDto(): Module19Step02SubstrateReservationHandoffDto = Module19Step02SubstrateReservationHandoffDto(
    contractVersion = contractVersion,
    tenantId = tenantId,
    reservationId = reservationId,
    orderId = orderId,
    orderItemId = orderItemId,
    executionJobId = executionJobId,
    sku = sku,
    productName = productName,
    gsm = gsm,
    sheetWidthMm = sheetWidthMm,
    sheetHeightMm = sheetHeightMm,
    reservedSheets = reservedSheets,
    reservedReams = reservedReams,
    reservedWeightKg = reservedWeightKg,
    mode = mode,
    status = status,
    isHardAllocated = isHardAllocated,
    softHoldExpiresAt = softHoldExpiresAt,
    promotedAt = promotedAt,
    promotedBy = promotedBy,
    allocationSourcesCount = allocationSourcesCount,
    reservedBy = reservedBy,
    timestamp = timestamp
)
