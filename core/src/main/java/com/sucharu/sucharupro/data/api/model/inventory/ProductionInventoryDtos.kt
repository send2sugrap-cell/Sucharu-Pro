package com.sucharu.sucharupro.data.api.model.inventory

import com.sucharu.sucharupro.domain.model.inventory.FinishedProductInventoryReceipt
import com.sucharu.sucharupro.domain.model.inventory.ProductionInventoryEligibility
import java.math.BigDecimal

data class ReceiveFinishedGoodsRequestDto(
    val warehouseId: String,
    val binId: String? = null,
    val overrideQuantity: BigDecimal? = null,
    val notes: String? = null
)

data class FinishedGoodsReceiptResponseDto(
    val receiptId: String,
    val projectId: String,
    val executionJobId: String,
    val orderId: String,
    val productId: String,
    val warehouseId: String,
    val binId: String? = null,
    val receivedQuantity: BigDecimal,
    val unit: String,
    val qcInspectionId: String? = null,
    val releaseId: String? = null,
    val status: String,
    val receivedBy: String,
    val receivedAt: Long,
    val notes: String? = null
)

data class FinishedGoodsEligibilityResponseDto(
    val executionJobId: String,
    val orderId: String,
    val isEligible: Boolean,
    val eligibleQuantity: BigDecimal,
    val qcInspectionId: String? = null,
    val releaseId: String? = null,
    val refusalReasons: List<String> = emptyList(),
    val evaluatedAt: Long
)

fun FinishedProductInventoryReceipt.toDto() = FinishedGoodsReceiptResponseDto(
    receiptId = receiptId,
    projectId = projectId,
    executionJobId = executionJobId,
    orderId = orderId,
    productId = productId,
    warehouseId = warehouseId,
    binId = binId,
    receivedQuantity = receivedQuantity,
    unit = unit.name,
    qcInspectionId = qcInspectionId,
    releaseId = releaseId,
    status = status,
    receivedBy = receivedBy,
    receivedAt = receivedAt,
    notes = notes
)

fun ProductionInventoryEligibility.toDto() = FinishedGoodsEligibilityResponseDto(
    executionJobId = executionJobId,
    orderId = orderId,
    isEligible = isEligible,
    eligibleQuantity = eligibleQuantity,
    qcInspectionId = qcInspectionId,
    releaseId = releaseId,
    refusalReasons = refusalReasons,
    evaluatedAt = evaluatedAt
)
