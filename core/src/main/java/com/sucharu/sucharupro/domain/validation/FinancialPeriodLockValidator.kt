package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.AccountingPeriod
import com.sucharu.sucharupro.domain.model.finance.AccountingPeriodStatus

/**
 * Enforces period-lock rules preventing financial mutations in closed accounting periods (Module 09 Step 08).
 */
object FinancialPeriodLockValidator {

    fun validateMutationAllowed(
        period: AccountingPeriod?,
        transactionDate: Long
    ): DomainResult<Unit> {
        if (period == null) {
            return DomainResult.Success(Unit)
        }

        if (period.status == AccountingPeriodStatus.CLOSED) {
            return DomainResult.Error(
                message = "Accounting period '${period.periodName}' (#${period.periodNo}) is CLOSED and LOCKED. Financial mutations are forbidden."
            )
        }

        if (transactionDate < period.startDate || transactionDate > period.endDate) {
            return DomainResult.Error(
                message = "Transaction date ($transactionDate) is outside accounting period '${period.periodName}' range (${period.startDate} to ${period.endDate})."
            )
        }

        return DomainResult.Success(Unit)
    }
}
