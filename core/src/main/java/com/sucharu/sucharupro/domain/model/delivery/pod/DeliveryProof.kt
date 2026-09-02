package com.sucharu.sucharupro.domain.model.delivery.pod

/**
 * Aggregate root representing a Proof of Delivery (POD) transaction (Module 08 Step 08).
 *
 * @param proofId Unique identifier for this POD aggregate.
 * @param projectId Project boundary isolation identifier.
 * @param deliveryOrderId Reference to the upstream Delivery Order.
 * @param deliveryChallanId Reference to the Delivery Challan.
 * @param dispatchExecutionId Reference to the Dispatch Execution.
 * @param deliveryShipmentId Reference to the physical Delivery Shipment.
 * @param verificationId Optional reference to Item Verification (Module 08 Step 04).
 * @param customerId Optional reference to customer account.
 * @param proofNo Human-readable unique POD number within project scope.
 * @param proofType Method of delivery verification (Signature, Photo, OTP, Document, etc.).
 * @param proofStatus Controlled lifecycle status.
 * @param recipientName Name of person receiving the goods.
 * @param recipientPhone Contact phone number of recipient.
 * @param recipientType Classification of recipient relationship.
 * @param deliveredAt Physical handover timestamp (epoch millis).
 * @param receivedAt Recipient acknowledgment timestamp (epoch millis).
 * @param notes Operational remarks or delivery notes.
 * @param rejectionReason Reason given if POD is rejected during review.
 * @param reviewNotes Remarks recorded during review/verification.
 * @param createdBy User ID who created the POD record.
 * @param createdAt Creation timestamp (epoch millis).
 * @param updatedBy User ID who last modified the POD record.
 * @param updatedAt Modification timestamp (epoch millis).
 * @param submittedAt Timestamp when submitted for review (epoch millis).
 * @param reviewedAt Timestamp when review began (epoch millis).
 * @param verifiedAt Timestamp when verified (epoch millis).
 * @param acceptedAt Timestamp when final acceptance was recorded (epoch millis).
 * @param rejectedAt Timestamp when rejection occurred (epoch millis).
 * @param cancelledAt Timestamp when cancelled (epoch millis).
 * @param reviewedBy User ID who reviewed the POD.
 * @param verifiedBy User ID who verified the POD.
 * @param acceptedBy User ID who accepted the POD.
 * @param rejectedBy User ID who rejected the POD.
 */
data class DeliveryProof(
    val proofId: String,
    val projectId: String,
    val deliveryOrderId: String,
    val deliveryChallanId: String,
    val dispatchExecutionId: String,
    val deliveryShipmentId: String,
    val verificationId: String? = null,
    val customerId: String? = null,
    val proofNo: String,
    val proofType: DeliveryProofType = DeliveryProofType.SIGNATURE,
    val proofStatus: DeliveryProofStatus = DeliveryProofStatus.DRAFT,
    val recipientName: String? = null,
    val recipientPhone: String? = null,
    val recipientType: DeliveryProofRecipientType = DeliveryProofRecipientType.PRIMARY_CONTACT,
    val deliveredAt: Long? = null,
    val receivedAt: Long? = null,
    val notes: String? = null,
    val rejectionReason: String? = null,
    val reviewNotes: String? = null,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedBy: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val submittedAt: Long? = null,
    val reviewedAt: Long? = null,
    val verifiedAt: Long? = null,
    val acceptedAt: Long? = null,
    val rejectedAt: Long? = null,
    val cancelledAt: Long? = null,
    val reviewedBy: String? = null,
    val verifiedBy: String? = null,
    val acceptedBy: String? = null,
    val rejectedBy: String? = null
) {
    init {
        require(proofId.isNotBlank()) { "Proof ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(deliveryOrderId.isNotBlank()) { "Delivery Order ID cannot be blank." }
        require(deliveryChallanId.isNotBlank()) { "Delivery Challan ID cannot be blank." }
        require(dispatchExecutionId.isNotBlank()) { "Dispatch Execution ID cannot be blank." }
        require(deliveryShipmentId.isNotBlank()) { "Delivery Shipment ID cannot be blank." }
        require(proofNo.isNotBlank()) { "Proof Number cannot be blank." }
        require(createdBy.isNotBlank()) { "Created By cannot be blank." }
        require(createdAt > 0) { "Creation timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp cannot precede creation." }
    }
}
