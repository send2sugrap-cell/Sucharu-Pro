package com.sucharu.sucharupro.domain.model.delivery.returning

/**
 * Operational priority of return processing (Module 08 Step 07).
 */
enum class DeliveryReturnPriority(val defaultLabel: String) {
    LOW("Low"),
    NORMAL("Normal"),
    HIGH("High"),
    URGENT("Urgent")
}
