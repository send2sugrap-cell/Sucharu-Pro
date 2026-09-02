package com.sucharu.sucharupro.domain.model.qc

/**
 * Status of an individual Pre-Production QC verification check item.
 */
enum class PreProductionItemStatus(val defaultLabel: String) {
    PENDING("Pending Check"),
    PASS("Pass"),
    FAIL("Fail"),
    NOT_APPLICABLE("Not Applicable")
}
