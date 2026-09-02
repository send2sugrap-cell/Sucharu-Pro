package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionStatus
import com.sucharu.sucharupro.domain.validation.FinancialTransactionLifecycleValidator
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialTransactionLifecycleTest {

    @Test
    fun `legal lifecycle state transitions succeed`() {
        assertTrue(
            FinancialTransactionLifecycleValidator.validateTransition(
                FinancialTransactionStatus.DRAFT,
                FinancialTransactionStatus.PENDING
            ) is DomainResult.Success
        )

        assertTrue(
            FinancialTransactionLifecycleValidator.validateTransition(
                FinancialTransactionStatus.PENDING,
                FinancialTransactionStatus.POSTED
            ) is DomainResult.Success
        )

        assertTrue(
            FinancialTransactionLifecycleValidator.validateTransition(
                FinancialTransactionStatus.PENDING,
                FinancialTransactionStatus.REJECTED
            ) is DomainResult.Success
        )

        assertTrue(
            FinancialTransactionLifecycleValidator.validateTransition(
                FinancialTransactionStatus.DRAFT,
                FinancialTransactionStatus.CANCELLED
            ) is DomainResult.Success
        )
    }

    @Test
    fun `illegal state transitions fail`() {
        assertTrue(
            FinancialTransactionLifecycleValidator.validateTransition(
                FinancialTransactionStatus.POSTED,
                FinancialTransactionStatus.DRAFT
            ) is DomainResult.Error
        )

        assertTrue(
            FinancialTransactionLifecycleValidator.validateTransition(
                FinancialTransactionStatus.CANCELLED,
                FinancialTransactionStatus.POSTED
            ) is DomainResult.Error
        )

        assertTrue(
            FinancialTransactionLifecycleValidator.validateTransition(
                FinancialTransactionStatus.REJECTED,
                FinancialTransactionStatus.POSTED
            ) is DomainResult.Error
        )
    }
}
