package com.sucharu.sucharupro.domain.model.returns

/**
 * Root cause justification for returns (Module 11 Step 01).
 * Includes both manufacturing defects and customer complaints.
 */
enum class ReturnReason(val defaultLabel: String) {
    PRINTING_DEFECT("Printing Defect"),
    BINDING_DEFECT("Binding Defect"),
    MISSING_PAGE("Missing Page(s)"),
    WRONG_PRODUCT("Wrong Product"),
    DAMAGED("Damaged Goods"),
    QUANTITY_ISSUE("Quantity Issue"),
    CUSTOMER_COMPLAINT("Customer Complaint"),
    OTHER("Other Justification");

    val displayName: String
        get() = defaultLabel
}
