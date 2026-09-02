package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.validation.RefundEligibilityValidator
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CustomerRefundOverRefundTest {

    @Test
    fun `over-refund validation fails when cumulative refund exceeds source amount`() {
        val originalCreditBalance = Money(BigDecimal("10000.00"))
        val previousRefunds = Money(BigDecimal("8000.00"))
        val remainingRefundable = originalCreditBalance.amount.subtract(previousRefunds.amount)

        val attemptRes = RefundEligibilityValidator.validateRefundAmount(
            requestedRefundAmount = Money(BigDecimal("3000.00")),
            refundableAvailableBalance = Money(remainingRefundable)
        )
        assertTrue(attemptRes is DomainResult.Error)
    }
}
