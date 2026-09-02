package com.sucharu.sucharupro.domain.model.delivery.verification

/**
 * Deterministic result classification for a verified delivery line (Module 08 Step 04).
 */
enum class DeliveryItemVerificationResultType(val defaultLabel: String) {
    VERIFIED("Verified"),
    SHORT("Short Quantity"),
    EXCESS("Excess Quantity"),
    MISMATCH("Identity/Batch Mismatch"),
    DAMAGED("Damaged Items"),
    MISSING("Missing Items")
}
