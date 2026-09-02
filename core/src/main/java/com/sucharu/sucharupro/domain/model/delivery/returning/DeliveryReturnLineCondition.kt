package com.sucharu.sucharupro.domain.model.delivery.returning

/**
 * Physical item condition observed during return inspection (Module 08 Step 07).
 */
enum class DeliveryReturnLineCondition(val defaultLabel: String) {
    GOOD("Good / Pristine"),
    DAMAGED("Physically Damaged"),
    DEFECTIVE("Manufacturing / Print Defect"),
    OPENED("Opened / Unsealed"),
    USED("Used / Altered"),
    MISSING("Missing Items"),
    UNKNOWN("Pending Inspection")
}
