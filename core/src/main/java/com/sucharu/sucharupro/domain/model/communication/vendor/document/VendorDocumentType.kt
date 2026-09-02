package com.sucharu.sucharupro.domain.model.communication.vendor.document

/**
 * Business-semantic categories of Vendor and Supplier documents (Module 10 Step 06).
 */
enum class VendorDocumentType(
    val defaultLabel: String,
    val defaultRequired: Boolean = false,
    val requiresExpiry: Boolean = false,
    val defaultRenewalReminderDays: List<Int> = listOf(90, 60, 30, 7)
) {
    // Legal & Registration
    BUSINESS_LICENSE("Business License", defaultRequired = true, requiresExpiry = true),
    TRADE_LICENSE("Trade License", defaultRequired = true, requiresExpiry = true),
    COMPANY_REGISTRATION("Company Registration Certificate", defaultRequired = true),
    ADDRESS_PROOF("Address Proof / Utility Bill", defaultRequired = false, requiresExpiry = true),
    NID_OR_IDENTITY_DOCUMENT("National ID / Identity Document", defaultRequired = true),

    // Tax & Financial
    TAX_DOCUMENT("Tax Document / Clearance", defaultRequired = false, requiresExpiry = true),
    VAT_DOCUMENT("VAT Registration / Return", defaultRequired = false, requiresExpiry = true),
    TIN_CERTIFICATE("TIN Certificate", defaultRequired = true),
    BIN_CERTIFICATE("BIN Certificate", defaultRequired = true),
    BANK_INFORMATION("Bank Account Information / Void Cheque", defaultRequired = true),
    BANK_CERTIFICATE("Bank Solvency Certificate", defaultRequired = false, requiresExpiry = true),

    // Commercial & Contractual
    CONTRACT("Vendor / Supplier Contract", defaultRequired = true, requiresExpiry = true),
    AGREEMENT("Service Level Agreement (SLA)", defaultRequired = false, requiresExpiry = true),
    INSURANCE_DOCUMENT("Insurance Policy / Certificate", defaultRequired = false, requiresExpiry = true),

    // Quality & Technical Compliance
    QUALITY_CERTIFICATE("Quality Management Certificate (e.g. ISO)", defaultRequired = false, requiresExpiry = true),
    PRODUCT_CERTIFICATE("Product Certification / Test Report", defaultRequired = false, requiresExpiry = true),
    COMPLIANCE_CERTIFICATE("Regulatory Compliance Certificate", defaultRequired = false, requiresExpiry = true),
    SAFETY_CERTIFICATE("Health & Safety Compliance Certificate", defaultRequired = false, requiresExpiry = true),

    // Transaction Supporting Documents
    INVOICE_SUPPORTING_DOCUMENT("Invoice Supporting Document", defaultRequired = false),
    DELIVERY_SUPPORTING_DOCUMENT("Delivery Supporting Document", defaultRequired = false),

    // Other
    OTHER("Other Compliance / Supporting Document", defaultRequired = false)
}
