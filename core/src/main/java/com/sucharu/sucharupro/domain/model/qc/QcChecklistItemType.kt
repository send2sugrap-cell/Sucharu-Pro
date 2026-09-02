package com.sucharu.sucharupro.domain.model.qc

/**
 * Supported data/response types for QC checklist inspection items (Module 06 Step 03).
 */
enum class QcChecklistItemType(val defaultLabel: String) {
    PASS_FAIL("Pass / Fail"),
    YES_NO("Yes / No"),
    NUMERIC("Numeric Measurement"),
    TEXT("Text Verification"),
    SELECT("Selection / Option"),
    NOT_APPLICABLE("Not Applicable Option")
}
