package com.sucharu.sucharupro.domain.model.delivery.returning

/**
 * Root cause justification for returns (Module 08 Step 07).
 */
enum class DeliveryReturnReason(val defaultLabel: String) {
    CUSTOMER_REQUEST("Customer Request"),
    DAMAGED("Damaged Goods"),
    DEFECTIVE("Defective / Printing Flaw"),
    WRONG_ITEM("Wrong Product Delivered"),
    WRONG_QUANTITY("Incorrect Quantity"),
    QUALITY_ISSUE("Quality Threshold Failure"),
    DELIVERY_FAILURE("Delivery Failed / Undeliverable"),
    CUSTOMER_REFUSAL("Customer Refused Consignment"),
    PACKAGING_DAMAGE("Packaging Compromised"),
    BATCH_ERROR("Batch Discrepancy"),
    LOT_ERROR("Lot Discrepancy"),
    DUPLICATE_DELIVERY("Duplicate Delivery"),
    EXCESS_DELIVERY("Excess Items Delivered"),
    OTHER("Other Justification")
}
