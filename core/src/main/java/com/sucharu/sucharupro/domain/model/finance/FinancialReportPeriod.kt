package com.sucharu.sucharupro.domain.model.finance

/**
 * Strongly typed report period discriminator (Module 09 Step 09).
 *
 * All period-based report calculations accept an explicit [FinancialReportPeriod]
 * rather than relying on device time. Timestamps are resolved externally and injected.
 *
 * For ACCOUNTING_PERIOD, the [accountingPeriodId] must be non-null.
 * For CUSTOM, both [customStartDate] and [customEndDate] must be non-null.
 */
sealed class FinancialReportPeriod(val defaultLabel: String) {

    data object Today : FinancialReportPeriod("Today")
    data object Yesterday : FinancialReportPeriod("Yesterday")
    data object CurrentWeek : FinancialReportPeriod("Current Week")
    data object PreviousWeek : FinancialReportPeriod("Previous Week")
    data object CurrentMonth : FinancialReportPeriod("Current Month")
    data object PreviousMonth : FinancialReportPeriod("Previous Month")
    data object CurrentQuarter : FinancialReportPeriod("Current Quarter")
    data object PreviousQuarter : FinancialReportPeriod("Previous Quarter")
    data object CurrentFinancialYear : FinancialReportPeriod("Current Financial Year")
    data object PreviousFinancialYear : FinancialReportPeriod("Previous Financial Year")

    data class Custom(
        val customStartDate: Long,
        val customEndDate: Long
    ) : FinancialReportPeriod("Custom Range") {
        init {
            require(customStartDate > 0) { "Custom start date must be positive." }
            require(customEndDate >= customStartDate) { "Custom end date cannot precede start date." }
        }
    }

    data class AccountingPeriodBound(
        val accountingPeriodId: String,
        val periodName: String
    ) : FinancialReportPeriod("Accounting Period: $periodName") {
        init {
            require(accountingPeriodId.isNotBlank()) { "Accounting period ID cannot be blank." }
        }
    }
}
