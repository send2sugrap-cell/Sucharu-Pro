package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryReturnLifecycleTest {

    @Test
    fun `valid happy path lifecycle progression succeeds`() {
        val steps = listOf(
            DeliveryReturnStatus.DRAFT to DeliveryReturnStatus.PENDING,
            DeliveryReturnStatus.PENDING to DeliveryReturnStatus.APPROVED,
            DeliveryReturnStatus.APPROVED to DeliveryReturnStatus.RECEIVING,
            DeliveryReturnStatus.RECEIVING to DeliveryReturnStatus.RECEIVED,
            DeliveryReturnStatus.RECEIVED to DeliveryReturnStatus.INSPECTING,
            DeliveryReturnStatus.INSPECTING to DeliveryReturnStatus.INSPECTED,
            DeliveryReturnStatus.INSPECTED to DeliveryReturnStatus.DISPOSITION_PENDING,
            DeliveryReturnStatus.DISPOSITION_PENDING to DeliveryReturnStatus.PROCESSING,
            DeliveryReturnStatus.PROCESSING to DeliveryReturnStatus.COMPLETED
        )

        for ((from, to) in steps) {
            val res = DeliveryReturnLifecycleValidator.validateTransition(from, to)
            assertTrue("Transition $from -> $to should be valid", res is DomainResult.Success)
        }
    }

    @Test
    fun `rejection from pending is valid`() {
        assertTrue(DeliveryReturnLifecycleValidator.validateTransition(DeliveryReturnStatus.PENDING, DeliveryReturnStatus.REJECTED) is DomainResult.Success)
    }

    @Test
    fun `terminal status cannot transition further`() {
        assertTrue(DeliveryReturnLifecycleValidator.validateTransition(DeliveryReturnStatus.COMPLETED, DeliveryReturnStatus.DRAFT) is DomainResult.Error)
        assertTrue(DeliveryReturnLifecycleValidator.validateTransition(DeliveryReturnStatus.CANCELLED, DeliveryReturnStatus.DRAFT) is DomainResult.Error)
        assertTrue(DeliveryReturnLifecycleValidator.validateTransition(DeliveryReturnStatus.REJECTED, DeliveryReturnStatus.APPROVED) is DomainResult.Error)
    }
}
