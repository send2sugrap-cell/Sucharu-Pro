package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySettlementStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliverySettlementLifecycleTest {

    @Test
    fun `valid progression from open to settled succeeds`() {
        assertTrue(DeliverySettlementLifecycleValidator.validateTransition(DeliverySettlementStatus.OPEN, DeliverySettlementStatus.PARTIALLY_DELIVERED) is DomainResult.Success)
        assertTrue(DeliverySettlementLifecycleValidator.validateTransition(DeliverySettlementStatus.PARTIALLY_DELIVERED, DeliverySettlementStatus.FULLY_DELIVERED) is DomainResult.Success)
        assertTrue(DeliverySettlementLifecycleValidator.validateTransition(DeliverySettlementStatus.FULLY_DELIVERED, DeliverySettlementStatus.SETTLED) is DomainResult.Success)
    }

    @Test
    fun `dispute transition is supported`() {
        assertTrue(DeliverySettlementLifecycleValidator.validateTransition(DeliverySettlementStatus.PARTIALLY_DELIVERED, DeliverySettlementStatus.DISPUTED) is DomainResult.Success)
        assertTrue(DeliverySettlementLifecycleValidator.validateTransition(DeliverySettlementStatus.DISPUTED, DeliverySettlementStatus.OPEN) is DomainResult.Success)
    }

    @Test
    fun `terminal states cannot transition further`() {
        assertTrue(DeliverySettlementLifecycleValidator.validateTransition(DeliverySettlementStatus.SETTLED, DeliverySettlementStatus.OPEN) is DomainResult.Error)
        assertTrue(DeliverySettlementLifecycleValidator.validateTransition(DeliverySettlementStatus.CANCELLED, DeliverySettlementStatus.OPEN) is DomainResult.Error)
    }
}
