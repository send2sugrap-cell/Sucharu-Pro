package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentStatus

/**
 * Finite state machine validator for Customer Refund lifecycle transitions (Module 09 Step 07).
 */
object CustomerRefundLifecycleValidator {

    fun validateTransition(
        from: FinancialAdjustmentStatus,
        to: FinancialAdjustmentStatus
    ): DomainResult<Unit> {
        return FinancialAdjustmentLifecycleValidator.validateTransition(from, to)
    }
}
