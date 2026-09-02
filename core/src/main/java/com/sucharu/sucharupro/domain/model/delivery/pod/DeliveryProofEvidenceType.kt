package com.sucharu.sucharupro.domain.model.delivery.pod

/**
 * Supported evidence classification types for POD (Module 08 Step 08).
 */
enum class DeliveryProofEvidenceType(val defaultLabel: String) {
    SIGNATURE_IMAGE("Recipient Signature Image"),
    DELIVERY_PHOTO("Package / Gate Delivery Photo"),
    OTP_CONFIRMATION("OTP Verification Log"),
    SIGNED_DOCUMENT("Signed Challan / POD Document"),
    RECIPIENT_ID_CARD("Recipient ID / Gate Pass Photo"),
    OTHER("Other Supporting Document")
}
