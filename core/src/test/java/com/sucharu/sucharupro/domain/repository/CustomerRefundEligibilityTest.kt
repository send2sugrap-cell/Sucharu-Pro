package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.validation.RefundEligibilityValidator
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CustomerRefundEligibilityTest {

    @Test
    fun `refund within available refundable balance is approved as eligible`() {
        val res = RefundEligibilityValidator.validateRefundAmount(
            requestedRefundAmount = Money(BigDecimal("4000.00")),
            refundableAvailableBalance = Money(BigDecimal("5000.00"))
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun `refund exceeding available refundable balance is rejected`() {
        val res = RefundEligibilityValidator.validateRefundAmount(
            requestedRefundAmount = Money(BigDecimal("6000.00")),
            refundableAvailableBalance = Money(BigDecimal("5000.00"))
        )
        assertTrue(res is DomainResult.Error)
    }
}
