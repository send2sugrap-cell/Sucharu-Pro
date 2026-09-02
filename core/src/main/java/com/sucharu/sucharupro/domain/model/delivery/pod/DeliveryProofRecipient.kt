package com.sucharu.sucharupro.domain.model.delivery.pod

/**
 * Model representing the receiving party acknowledging delivery (Module 08 Step 08).
 *
 * @param recipientId Unique identifier for this recipient snapshot.
 * @param proofId Reference to parent DeliveryProof.
 * @param projectId Project boundary context.
 * @param recipientName Name of the person who accepted the delivery.
 * @param recipientPhone Contact phone number of the recipient.
 * @param recipientType Classification of recipient relationship.
 * @param relationshipToCustomer Specific explanation if not primary customer.
 * @param confirmationMethod Method used by recipient (e.g., SIGNATURE, OTP, VERBAL, BADGE).
 * @param confirmedAt Timestamp of recipient acknowledgment (epoch millis).
 * @param confirmedBy User ID or actor recording recipient acknowledgment.
 */
data class DeliveryProofRecipient(
    val recipientId: String,
    val proofId: String,
    val projectId: String,
    val recipientName: String,
    val recipientPhone: String? = null,
    val recipientType: DeliveryProofRecipientType = DeliveryProofRecipientType.PRIMARY_CONTACT,
    val relationshipToCustomer: String? = null,
    val confirmationMethod: String? = null,
    val confirmedAt: Long? = null,
    val confirmedBy: String? = null
) {
    init {
        require(recipientId.isNotBlank()) { "Recipient ID cannot be blank." }
        require(proofId.isNotBlank()) { "Proof ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(recipientName.isNotBlank()) { "Recipient name cannot be blank." }
    }
}
