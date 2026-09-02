package com.sucharu.sucharupro.domain.model.finance

/**
 * Status of an Accounting Period in the financial control lifecycle (Module 09 Step 08).
 */
enum class AccountingPeriodStatus(val defaultLabel: String) {
    OPEN("Open"),
    CLOSING("Closing in Progress"),
    CLOSED("Closed / Locked"),
    REOPENED("Reopened Under Audit");

    val isTerminal: Boolean
        get() = this == CLOSED

    val isOpenForPosting: Boolean
        get() = this == OPEN || this == REOPENED
}
