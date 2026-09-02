package com.sucharu.sucharupro.domain.model.delivery.challan

/**
 * Aggregate root representing a Delivery Challan business document in Sucharu Pro (Module 08 Step 02).
 *
 * @param challanId Unique identifier for the challan document.
 * @param projectId Project boundary context.
 * @param challanNo Project-scoped unique human-readable Challan number.
 * @param deliveryOrderId Reference to the parent Delivery Order.
 * @param customerId Optional customer identifier reference.
 * @param sourceReferenceId Optional source reference ID (e.g., Sales Order or Production Job).
 * @param sourceReferenceType Optional source reference type.
 * @param challanType Type classification of the challan.
 * @param status Current lifecycle stage.
 * @param issueDate Date of document issue (epoch millis).
 * @param notes Optional operational remarks.
 * @param createdBy User ID who created the document.
 * @param createdAt Creation timestamp (epoch millis).
 * @param updatedAt Modification timestamp (epoch millis).
 */
data class DeliveryChallan(
    val challanId: String,
    val projectId: String,
    val challanNo: String,
    val deliveryOrderId: String,
    val customerId: String?,
    val sourceReferenceId: String?,
    val sourceReferenceType: String?,
    val challanType: DeliveryChallanType,
    val status: DeliveryChallanStatus,
    val issueDate: Long,
    val notes: String?,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    init {
        require(challanId.isNotBlank()) { "Challan ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(challanNo.isNotBlank()) { "Challan Number cannot be blank." }
        require(deliveryOrderId.isNotBlank()) { "Delivery Order ID cannot be blank." }
        require(createdBy.isNotBlank()) { "Created By cannot be blank." }
        require(issueDate > 0) { "Issue Date must be positive." }
        require(createdAt > 0) { "Created At timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated At cannot be before Created At." }
    }
}
