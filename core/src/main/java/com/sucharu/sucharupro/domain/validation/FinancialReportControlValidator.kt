package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.BalanceSheetReport
import com.sucharu.sucharupro.domain.model.finance.FinancialReportFilter
import com.sucharu.sucharupro.domain.model.finance.FinancialReportStatus
import com.sucharu.sucharupro.domain.model.finance.TrialBalanceReport

/**
 * Validates financial controls and equations for reports (Module 09 Step 09).
 */
object FinancialReportControlValidator {

    fun validateFilter(filter: FinancialReportFilter): DomainResult<Unit> {
        if (filter.projectId.isBlank() || filter.projectId == "*") {
            return DomainResult.Error(message = "Invalid report filter: Project ID must be a specific, non-blank project.")
        }
        if (filter.resolvedStartDate != null && filter.resolvedEndDate != null) {
            if (filter.resolvedStartDate <= 0) {
                return DomainResult.Error(message = "Invalid report date range: Start date must be positive.")
            }
            if (filter.resolvedEndDate < filter.resolvedStartDate) {
                return DomainResult.Error(message = "Invalid report date range: End date cannot precede start date.")
            }
        }
        return DomainResult.Success(Unit)
    }

    fun validateTrialBalance(
        totalDebit: Money,
        totalCredit: Money
    ): Pair<FinancialReportStatus, List<String>> {
        val exceptions = mutableListOf<String>()
        if (totalDebit != totalCredit) {
            val variance = totalDebit.minus(totalCredit)
            exceptions.add("Trial Balance discrepancy: Debit (${totalDebit.formatted()}) does not match Credit (${totalCredit.formatted()}). Variance: ${variance.formatted()}")
            return FinancialReportStatus.CONTROL_EXCEPTION to exceptions
        }
        return FinancialReportStatus.READY to emptyList()
    }

    fun validateBalanceSheet(
        totalAssets: Money,
        totalLiabilities: Money,
        totalEquity: Money
    ): Pair<FinancialReportStatus, List<String>> {
        val exceptions = mutableListOf<String>()
        val liabilitiesPlusEquity = totalLiabilities.plus(totalEquity)
        if (totalAssets != liabilitiesPlusEquity) {
            val variance = totalAssets.minus(liabilitiesPlusEquity)
            exceptions.add("Balance Sheet discrepancy: Assets (${totalAssets.formatted()}) does not match Liabilities + Equity (${liabilitiesPlusEquity.formatted()}). Variance: ${variance.formatted()}")
            return FinancialReportStatus.CONTROL_EXCEPTION to exceptions
        }
        return FinancialReportStatus.READY to emptyList()
    }
}
