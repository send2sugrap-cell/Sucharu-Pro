package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.AccountingPeriodStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountingPeriodLifecycleTest {

    @Test
    fun `legal state transitions succeed`() {
        // OPEN -> CLOSING
        assertTrue(AccountingPeriodLifecycleValidator.validateTransition(AccountingPeriodStatus.OPEN, AccountingPeriodStatus.CLOSING) is DomainResult.Success)
        // CLOSING -> CLOSED
        assertTrue(AccountingPeriodLifecycleValidator.validateTransition(AccountingPeriodStatus.CLOSING, AccountingPeriodStatus.CLOSED) is DomainResult.Success)
        // CLOSING -> OPEN (abort closing review)
        assertTrue(AccountingPeriodLifecycleValidator.validateTransition(AccountingPeriodStatus.CLOSING, AccountingPeriodStatus.OPEN) is DomainResult.Success)
        // CLOSED -> REOPENED
        assertTrue(AccountingPeriodLifecycleValidator.validateTransition(AccountingPeriodStatus.CLOSED, AccountingPeriodStatus.REOPENED) is DomainResult.Success)
        // REOPENED -> CLOSING
        assertTrue(AccountingPeriodLifecycleValidator.validateTransition(AccountingPeriodStatus.REOPENED, AccountingPeriodStatus.CLOSING) is DomainResult.Success)
    }

    @Test
    fun `illegal state transitions are blocked`() {
        // CLOSED -> OPEN (must go through REOPENED via controlled request)
        assertTrue(AccountingPeriodLifecycleValidator.validateTransition(AccountingPeriodStatus.CLOSED, AccountingPeriodStatus.OPEN) is DomainResult.Error)
        // CLOSED -> CLOSING
        assertTrue(AccountingPeriodLifecycleValidator.validateTransition(AccountingPeriodStatus.CLOSED, AccountingPeriodStatus.CLOSING) is DomainResult.Error)
    }
}
