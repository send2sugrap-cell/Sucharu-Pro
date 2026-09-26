package com.sucharu.sucharupro.domain.model.erp

/**
 * Form 05 — Canonical ERP Workflow Orchestration Entity.
 *
 * Connects Customer Actions (View Product, Select Qty, Apply Offer, Order Now)
 * to canonical Module 00–24 ERP pipelines without duplicating business truth.
 */
data class ErpWorkflowOrchestration(
    val orchestrationId: String,
    val customerAction: CustomerActionType = CustomerActionType.ORDER_NOW,
    val customerId: String,
    val productId: String,
    val offerId: String? = null,
    val priceConfigId: String? = null,

    // ERP Pipeline Stage Trackers
    val workflowStatus: ErpWorkflowStatus = ErpWorkflowStatus.INITIATED,
    val quotationId: String? = null,
    val orderId: String? = null,
    val jobCardId: String? = null,
    val qcInspectionId: String? = null,
    val challanId: String? = null,
    val invoiceId: String? = null,

    val orderQuantity: Int,
    val orderAmount: Double,
    val currency: String = "BDT",
    val specialInstructions: String? = null,

    val actionLogs: List<ErpActionLog> = emptyList(),

    val createdAt: String,
    val updatedAt: String,
    val createdBy: String,
    val updatedBy: String? = null
) {
    init {
        require(orchestrationId.isNotBlank()) { "Orchestration ID cannot be blank." }
        require(customerId.isNotBlank()) { "Customer ID cannot be blank." }
        require(productId.isNotBlank()) { "Product ID cannot be blank." }
        require(orderQuantity > 0) { "Order quantity must be greater than zero." }
        require(orderAmount >= 0.0) { "Order amount cannot be negative." }
        require(createdAt.isNotBlank()) { "createdAt timestamp cannot be blank." }
        require(updatedAt.isNotBlank()) { "updatedAt timestamp cannot be blank." }
        require(createdBy.isNotBlank()) { "createdBy actor cannot be blank." }
    }
}
