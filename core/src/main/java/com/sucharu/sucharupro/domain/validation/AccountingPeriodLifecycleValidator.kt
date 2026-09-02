package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.AccountingPeriodStatus

/**
 * Validates state transitions in the Accounting Period lifecycle (Module 09 Step 08).
 */
object AccountingPeriodLifecycleValidator {

    fun validateTransition(
        currentStatus: AccountingPeriodStatus,
        targetStatus: AccountingPeriodStatus
    ): DomainResult<Unit> {
        if (currentStatus == targetStatus) {
            return DomainResult.Success(Unit)
        }

        val isValid = when (currentStatus) {
            AccountingPeriodStatus.OPEN -> {
                targetStatus in listOf(
                    AccountingPeriodStatus.CLOSING,
                    AccountingPeriodStatus.CLOSED,
                    AccountingPeriodStatus.REOPENED
                )
            }
            AccountingPeriodStatus.CLOSING -> {
                targetStatus in listOf(
                    AccountingPeriodStatus.OPEN,
                    AccountingPeriodStatus.CLOSED
                )
            }
            AccountingPeriodStatus.CLOSED -> {
                targetStatus == AccountingPeriodStatus.REOPENED
            }
            AccountingPeriodStatus.REOPENED -> {
                targetStatus in listOf(
                    AccountingPeriodStatus.CLOSING,
                    AccountingPeriodStatus.OPEN,
                    AccountingPeriodStatus.CLOSED
                )
            }
        }

        return if (isValid) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Illegal accounting period transition from ${currentStatus.name} to ${targetStatus.name}."
            )
        }
    }
}
