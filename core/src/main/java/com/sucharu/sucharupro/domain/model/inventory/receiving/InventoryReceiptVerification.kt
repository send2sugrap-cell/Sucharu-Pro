package com.sucharu.sucharupro.domain.model.inventory.receiving

/**
 * Immutable verification record capturing the decision that led to stock acceptance or rejection
 * (Module 07 Step 03).
 *
 * Created when a verifier reviews a receiving line's received quantity and determines
 * the accepted and rejected split. This record is the authoritative source of the
 * verification decision and is preserved permanently in audit history.
 *
 * Quantity invariants:
 *   acceptedQuantity >= 0
 *   rejectedQuantity >= 0
 *   acceptedQuantity + rejectedQuantity <= receivedQuantity
 *   For a finalized verification: acceptedQuantity + rejectedQuantity == receivedQuantity
 */
data class InventoryReceiptVerification(
    val verificationId: String,
    val receivingId: String,
    val receivingLineId: String,
    val projectId: String,
    val verifiedBy: String,
    val verifiedAt: String,
    val receivedQuantity: Int,
    val acceptedQuantity: Int,
    val rejectedQuantity: Int,
    val verificationNotes: String? = null
) {
    init {
        require(verificationId.isNotBlank()) { "Verification ID cannot be blank." }
        require(receivingId.isNotBlank()) { "Receiving ID cannot be blank." }
        require(receivingLineId.isNotBlank()) { "Receiving line ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(verifiedBy.isNotBlank()) { "verifiedBy actor cannot be blank." }
        require(verifiedAt.isNotBlank()) { "verifiedAt timestamp cannot be blank." }
        require(receivedQuantity > 0) { "receivedQuantity must be greater than zero." }
        require(acceptedQuantity >= 0) { "acceptedQuantity cannot be negative." }
        require(rejectedQuantity >= 0) { "rejectedQuantity cannot be negative." }
        require(acceptedQuantity + rejectedQuantity <= receivedQuantity) {
            "acceptedQuantity ($acceptedQuantity) + rejectedQuantity ($rejectedQuantity) " +
                "cannot exceed receivedQuantity ($receivedQuantity)."
        }
    }
}
