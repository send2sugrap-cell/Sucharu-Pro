package com.sucharu.sucharupro.data.api.model.substratereservation

import com.sucharu.sucharupro.domain.model.substratereservation.*
import java.math.BigDecimal

data class EvaluateSubstrateReplenishmentRequestDto(
    val productId: String,
    val sku: String,
    val materialName: String,
    val stockType: String = "ART_PAPER",
    val gsm: BigDecimal,
    val sheetWidthMm: BigDecimal,
    val sheetHeightMm: BigDecimal,
    val warehouseId: String,
    val warehouseName: String,
    val onHandPhysicalSheets: Long,
    val activeReservedSheets: Long,
    val pendingInboundSheets: Long = 0L,
    val plannedDemandSheets: Long = 0L,
    val policyType: String = "DEMAND_AWARE",
    val minimumStockSheets: Long = 2000L,
    val safetyStockSheets: Long = 4000L,
    val reorderPointSheets: Long = 10000L,
    val targetStockSheets: Long = 30000L,
    val minimumOrderQuantitySheets: Long = 5000L,
    val standardPackReamSize: Int = 500,
    val leadTimeDays: Int = 5,
    val policyVersion: String = "1.0.0",
    val notes: String? = null
)

data class TriggerSupplierAlertRequestDto(
    val vendorId: String? = null
)

data class UpdateReplenishmentStatusRequestDto(
    val newState: String,
    val reason: String
)

data class SupplierReorderCandidateDto(
    val candidateId: String,
    val vendorId: String,
    val vendorCode: String,
    val vendorName: String,
    val rank: Int,
    val suitabilityScore: BigDecimal,
    val estimatedLeadTimeDays: Int,
    val quotedCostPerSheet: BigDecimal,
    val minimumOrderQuantitySheets: Long,
    val standardPackSize: Int,
    val primaryContactEmail: String?,
    val primaryContactPhone: String?,
    val isApprovedSupplier: Boolean,
    val selectionRationale: String
)

data class SubstrateReplenishmentResponseDto(
    val evaluationId: String,
    val tenantId: String,
    val productId: String,
    val sku: String,
    val materialName: String,
    val stockType: String,
    val gsm: BigDecimal,
    val sheetWidthMm: BigDecimal,
    val sheetHeightMm: BigDecimal,
    val warehouseId: String,
    val warehouseName: String,
    val onHandPhysicalSheets: Long,
    val activeReservedSheets: Long,
    val availableSheets: Long,
    val pendingInboundSheets: Long,
    val plannedDemandSheets: Long,
    val netProjectedAvailabilitySheets: Long,
    val safetyStockSheets: Long,
    val reorderPointSheets: Long,
    val targetStockSheets: Long,
    val isReorderRequired: Boolean,
    val projectedShortfallSheets: Long,
    val recommendedReorderSheets: Long,
    val recommendedReorderReams: BigDecimal,
    val triggerState: String,
    val priority: String,
    val primaryReason: String,
    val policyId: String,
    val policyVersion: String,
    val recommendedSuppliers: List<SupplierReorderCandidateDto>,
    val primaryVendorId: String?,
    val primaryVendorName: String?,
    val deduplicationFingerprint: String,
    val masterIntegrityHash: String,
    val evaluatedBy: String,
    val evaluatedAt: Long,
    val notes: String?
)

data class SupplierReorderAlertResponseDto(
    val alertId: String,
    val evaluationId: String,
    val tenantId: String,
    val vendorId: String,
    val vendorCode: String,
    val vendorName: String,
    val sku: String,
    val materialName: String,
    val requestedSheets: Long,
    val requestedReams: BigDecimal,
    val targetDeliveryTimestamp: Long?,
    val priority: String,
    val status: String,
    val alertPayloadJson: String?,
    val dispatchedBy: String,
    val dispatchedAt: Long,
    val acknowledgedAt: Long?,
    val purchaseRequisitionId: String?
)

fun SupplierReorderCandidate.toDto(): SupplierReorderCandidateDto = SupplierReorderCandidateDto(
    candidateId = candidateId,
    vendorId = vendorId,
    vendorCode = vendorCode,
    vendorName = vendorName,
    rank = rank,
    suitabilityScore = suitabilityScore,
    estimatedLeadTimeDays = estimatedLeadTimeDays,
    quotedCostPerSheet = quotedCostPerSheet,
    minimumOrderQuantitySheets = minimumOrderQuantitySheets,
    standardPackSize = standardPackSize,
    primaryContactEmail = primaryContactEmail,
    primaryContactPhone = primaryContactPhone,
    isApprovedSupplier = isApprovedSupplier,
    selectionRationale = selectionRationale
)

fun SubstrateReplenishmentEvaluation.toDto(): SubstrateReplenishmentResponseDto = SubstrateReplenishmentResponseDto(
    evaluationId = evaluationId,
    tenantId = tenantId,
    productId = productId,
    sku = sku,
    materialName = materialName,
    stockType = stockType.name,
    gsm = gsm,
    sheetWidthMm = sheetDimension.width,
    sheetHeightMm = sheetDimension.height,
    warehouseId = warehouseId,
    warehouseName = warehouseName,
    onHandPhysicalSheets = onHandPhysicalSheets,
    activeReservedSheets = activeReservedSheets,
    availableSheets = availableSheets,
    pendingInboundSheets = pendingInboundSheets,
    plannedDemandSheets = plannedDemandSheets,
    netProjectedAvailabilitySheets = netProjectedAvailabilitySheets,
    safetyStockSheets = safetyStockSheets,
    reorderPointSheets = reorderPointSheets,
    targetStockSheets = targetStockSheets,
    isReorderRequired = isReorderRequired,
    projectedShortfallSheets = projectedShortfallSheets,
    recommendedReorderSheets = recommendedReorderSheets,
    recommendedReorderReams = recommendedReorderReams,
    triggerState = triggerState.name,
    priority = priority.name,
    primaryReason = primaryReason.name,
    policyId = policyId,
    policyVersion = policyVersion,
    recommendedSuppliers = recommendedSuppliers.map { it.toDto() },
    primaryVendorId = primaryVendorId,
    primaryVendorName = primaryVendorName,
    deduplicationFingerprint = deduplicationFingerprint,
    masterIntegrityHash = masterIntegrityHash,
    evaluatedBy = evaluatedBy,
    evaluatedAt = evaluatedAt,
    notes = notes
)

fun SupplierReorderAlert.toDto(): SupplierReorderAlertResponseDto = SupplierReorderAlertResponseDto(
    alertId = alertId,
    evaluationId = evaluationId,
    tenantId = tenantId,
    vendorId = vendorId,
    vendorCode = vendorCode,
    vendorName = vendorName,
    sku = sku,
    materialName = materialName,
    requestedSheets = requestedSheets,
    requestedReams = requestedReams,
    targetDeliveryTimestamp = targetDeliveryTimestamp,
    priority = priority.name,
    status = status.name,
    alertPayloadJson = alertPayloadJson,
    dispatchedBy = dispatchedBy,
    dispatchedAt = dispatchedAt,
    acknowledgedAt = acknowledgedAt,
    purchaseRequisitionId = purchaseRequisitionId
)
