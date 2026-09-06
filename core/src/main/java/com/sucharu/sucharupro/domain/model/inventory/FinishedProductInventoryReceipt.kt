package com.sucharu.sucharupro.domain.model.inventory

import java.math.BigDecimal

/**
 * Domain entity representing a Finished Product Inventory Receipt resulting from
 * completed production and certified Final QC release (Phase 06 Step 01).
 */
data class FinishedProductInventoryReceipt(
    val receiptId: String,
    val projectId: String,
    val executionJobId: String,
    val orderId: String,
    val productId: String,
    val warehouseId: String,
    val binId: String? = null,
    val receivedQuantity: BigDecimal,
    val unit: InventoryUnit = InventoryUnit.PCS,
    val unitCost: BigDecimal = BigDecimal.ZERO,
    val qcInspectionId: String? = null,
    val releaseId: String? = null,
    val status: String = "COMPLETED",
    val receivedBy: String,
    val receivedAt: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val version: Long = 1L
) {
    init {
        require(receiptId.isNotBlank()) { "Receipt ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(executionJobId.isNotBlank()) { "Execution Job ID cannot be blank." }
        require(orderId.isNotBlank()) { "Order ID cannot be blank." }
        require(productId.isNotBlank()) { "Product ID cannot be blank." }
        require(warehouseId.isNotBlank()) { "Warehouse ID cannot be blank." }
        require(receivedQuantity > BigDecimal.ZERO) { "Received quantity must be positive." }
        require(receivedBy.isNotBlank()) { "Received by actor cannot be blank." }
    }
}

/**
 * Evaluation result for finished goods inventory receiving eligibility.
 */
data class ProductionInventoryEligibility(
    val executionJobId: String,
    val orderId: String,
    val isEligible: Boolean,
    val eligibleQuantity: BigDecimal,
    val qcInspectionId: String? = null,
    val releaseId: String? = null,
    val refusalReasons: List<String> = emptyList(),
    val evaluatedAt: Long = System.currentTimeMillis()
)
