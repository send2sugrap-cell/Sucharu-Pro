package com.sucharu.sucharupro.domain.model.qc

/**
 * Inspection outcome decision recorded by an authorized QC Inspector.
 */
enum class QcDecision(val defaultLabel: String) {
    /** Inspection not yet evaluated or completed. */
    PENDING("Pending Decision"),

    /** Quality inspection passed all criteria. */
    PASS("Pass"),

    /** Quality inspection failed one or more criteria. */
    FAIL("Fail")
}
