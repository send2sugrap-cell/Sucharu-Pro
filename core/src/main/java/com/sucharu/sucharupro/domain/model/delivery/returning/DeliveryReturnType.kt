package com.sucharu.sucharupro.domain.model.delivery.returning

/**
 * Business classification of return requests (Module 08 Step 07).
 */
enum class DeliveryReturnType(val defaultLabel: String) {
    CUSTOMER_RETURN("Customer Return"),
    DELIVERY_REJECTION("Delivery Rejection"),
    DAMAGED_RETURN("Damaged Return"),
    WRONG_PRODUCT("Wrong Product Delivered"),
    WRONG_QUANTITY("Wrong Quantity Delivered"),
    BATCH_MISMATCH("Batch Mismatch"),
    LOT_MISMATCH("Lot Mismatch"),
    QUALITY_REJECTION("Quality Rejection"),
    EXCESS_RETURN("Excess Return"),
    FAILED_DELIVERY_RETURN("Failed Delivery Return"),
    REPLACEMENT_RETURN("Replacement Return"),
    INTERNAL_RETURN("Internal Return"),
    OTHER("Other")
}
