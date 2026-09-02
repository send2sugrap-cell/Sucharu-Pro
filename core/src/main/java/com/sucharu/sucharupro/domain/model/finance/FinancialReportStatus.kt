package com.sucharu.sucharupro.domain.model.finance

/**
 * Lifecycle status of a generated financial report (Module 09 Step 09).
 *
 * A READY report contains complete, deterministic calculations.
 * A CONTROL_EXCEPTION report explicitly identifies the failed financial control — never silently hides errors.
 */
enum class FinancialReportStatus(val defaultLabel: String) {
    GENERATING("Generating"),
    READY("Ready"),
    CONTROL_EXCEPTION("Control Exception"),
    SUPERSEDED("Superseded"),
    FAILED("Failed");

    val isUsable: Boolean
        get() = this == READY

    val hasException: Boolean
        get() = this == CONTROL_EXCEPTION
}
