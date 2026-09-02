package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivableStatus
import com.sucharu.sucharupro.domain.validation.CustomerReceivableLifecycleValidator
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerReceivableLifecycleTest {

    @Test
    fun `legal customer receivable transitions pass validation`() {
        assertTrue(
            CustomerReceivableLifecycleValidator.validateTransition(
                CustomerReceivableStatus.OPEN,
                CustomerReceivableStatus.PARTIALLY_SETTLED
            ) is DomainResult.Success
        )
        assertTrue(
            CustomerReceivableLifecycleValidator.validateTransition(
                CustomerReceivableStatus.OPEN,
                CustomerReceivableStatus.OVERDUE
            ) is DomainResult.Success
        )
        assertTrue(
            CustomerReceivableLifecycleValidator.validateTransition(
                CustomerReceivableStatus.OPEN,
                CustomerReceivableStatus.SETTLED
            ) is DomainResult.Success
        )
        assertTrue(
            CustomerReceivableLifecycleValidator.validateTransition(
                CustomerReceivableStatus.OPEN,
                CustomerReceivableStatus.CANCELLED
            ) is DomainResult.Success
        )
        assertTrue(
            CustomerReceivableLifecycleValidator.validateTransition(
                CustomerReceivableStatus.PARTIALLY_SETTLED,
                CustomerReceivableStatus.SETTLED
            ) is DomainResult.Success
        )
        assertTrue(
            CustomerReceivableLifecycleValidator.validateTransition(
                CustomerReceivableStatus.OVERDUE,
                CustomerReceivableStatus.SETTLED
            ) is DomainResult.Success
        )
    }

    @Test
    fun `illegal terminal transitions are rejected`() {
        assertTrue(
            CustomerReceivableLifecycleValidator.validateTransition(
                CustomerReceivableStatus.SETTLED,
                CustomerReceivableStatus.OPEN
            ) is DomainResult.Error
        )
        assertTrue(
            CustomerReceivableLifecycleValidator.validateTransition(
                CustomerReceivableStatus.CANCELLED,
                CustomerReceivableStatus.OPEN
            ) is DomainResult.Error
        )
    }
}
