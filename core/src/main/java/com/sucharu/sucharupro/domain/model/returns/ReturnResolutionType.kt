package com.sucharu.sucharupro.domain.model.returns

/**
 * Commercial, financial, and operational resolution types for Customer Return Requests (Module 11 Step 05).
 */
enum class ReturnResolutionType(val defaultLabel: String) {
    CREDIT_NOTE("Credit Note"),
    REFUND("Direct Refund"),
    REPLACEMENT("Replacement Order"),
    REWORK("Production Rework"),
    SCRAP_WRITE_OFF("Scrap / Write-off");

    val displayName: String
        get() = defaultLabel
}
