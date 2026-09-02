package com.sucharu.sucharupro.domain.model.delivery.verification

/**
 * Discrepancy issue classification for delivery verification lines (Module 08 Step 04).
 */
enum class DeliveryItemVerificationIssueType(val defaultLabel: String) {
    NONE("No Issue"),
    QUANTITY_SHORTAGE("Quantity Shortage"),
    QUANTITY_EXCESS("Quantity Excess"),
    PRODUCT_MISMATCH("Product Mismatch"),
    BATCH_MISMATCH("Batch Number Mismatch"),
    LOT_MISMATCH("Lot Number Mismatch"),
    DAMAGED("Damaged Goods"),
    MISSING("Completely Missing"),
    OTHER("Other Discrepancy")
}
