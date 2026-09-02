package com.sucharu.sucharupro.domain.model.qc

/**
 * Outcome status of an inspection item response (Module 06 Step 03).
 */
enum class QcResponseStatus(val defaultLabel: String) {
    PENDING("Pending Evaluation"),
    PASS("Pass"),
    FAIL("Fail"),
    NOT_APPLICABLE("Not Applicable")
}
