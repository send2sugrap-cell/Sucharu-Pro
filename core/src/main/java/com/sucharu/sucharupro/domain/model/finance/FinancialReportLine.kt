package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Represents a single line in a structured financial report (Module 09 Step 09).
 *
 * Lines compose sections (headers, details, subtotals, totals) in P&L, Balance Sheet,
 * Trial Balance, Cash Flow, and General Ledger reports.
 */
data class FinancialReportLine(
    val lineId: String,
    val label: String,
    val amount: Money,
    val lineType: FinancialReportLineType,
    val indentLevel: Int = 0,
    /** Optional account head identifier for drill-down. */
    val accountHead: String? = null,
    /** Optional reference for detail traceability. */
    val referenceId: String? = null,
    val note: String? = null
) {
    init {
        require(lineId.isNotBlank()) { "Report line ID cannot be blank." }
        require(label.isNotBlank()) { "Report line label cannot be blank." }
        require(indentLevel >= 0) { "Indent level cannot be negative." }
    }
}

/**
 * Classification of a report line within its parent section (Module 09 Step 09).
 */
enum class FinancialReportLineType(val defaultLabel: String) {
    SECTION_HEADER("Section Header"),
    DETAIL("Detail Line"),
    SUBTOTAL("Subtotal"),
    TOTAL("Total"),
    GRAND_TOTAL("Grand Total"),
    VARIANCE("Variance / Difference"),
    CONTROL_CHECK("Control Check")
}
