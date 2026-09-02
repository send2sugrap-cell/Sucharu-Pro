package com.sucharu.sucharupro.domain.model.delivery.pod

/**
 * Supported proof of delivery methods (Module 08 Step 08).
 */
enum class DeliveryProofType(val defaultLabel: String) {
    SIGNATURE("Physical / Digital Signature"),
    PHOTO("Delivery Photo / Gate Proof"),
    OTP("One-Time Password (OTP)"),
    DIGITAL_CONFIRMATION("Digital Recipient Confirmation"),
    DOCUMENT("Signed Delivery Challan / Waybill"),
    RECIPIENT_CONFIRMATION("Direct Recipient Acknowledgment"),
    COMBINED("Combined Multi-Factor Proof"),
    OTHER("Other Verified Proof")
}
