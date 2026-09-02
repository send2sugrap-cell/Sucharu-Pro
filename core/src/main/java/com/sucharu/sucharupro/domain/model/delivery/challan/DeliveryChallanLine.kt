package com.sucharu.sucharupro.domain.model.delivery.challan

/**
 * Item line within a Delivery Challan document (Module 08 Step 02).
 *
 * @param lineId Unique identifier for the challan line.
 * @param challanId Reference to parent Challan document.
 * @param projectId Project boundary context.
 * @param deliveryOrderLineId Reference to the parent DeliveryOrderLine.
 * @param productId Product reference.
 * @param quantity The authorized allocated quantity on this challan (must be > 0).
 * @param notes Optional item remarks.
 * @param batchId Optional batch reference.
 * @param lotId Optional lot reference.
 */
data class DeliveryChallanLine(
    val lineId: String,
    val challanId: String,
    val projectId: String,
    val deliveryOrderLineId: String,
    val productId: String,
    val quantity: Double,
    val notes: String? = null,
    val batchId: String? = null,
    val lotId: String? = null
) {
    init {
        require(lineId.isNotBlank()) { "Line ID cannot be blank." }
        require(challanId.isNotBlank()) { "Challan ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(deliveryOrderLineId.isNotBlank()) { "Delivery Order Line ID cannot be blank." }
        require(productId.isNotBlank()) { "Product ID cannot be blank." }
        require(quantity > 0) { "Quantity must be greater than zero." }
    }
}
