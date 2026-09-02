package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryChallanLifecycleTest {

    @Test
    fun `valid lifecycle progression succeeds`() {
        // DRAFT -> PENDING
        assertTrue(
            DeliveryChallanLifecycleValidator.validateTransition(
                DeliveryChallanStatus.DRAFT,
                DeliveryChallanStatus.PENDING
            ) is DomainResult.Success
        )

        // PENDING -> APPROVED
        assertTrue(
            DeliveryChallanLifecycleValidator.validateTransition(
                DeliveryChallanStatus.PENDING,
                DeliveryChallanStatus.APPROVED
            ) is DomainResult.Success
        )

        // APPROVED -> READY_FOR_DISPATCH
        assertTrue(
            DeliveryChallanLifecycleValidator.validateTransition(
                DeliveryChallanStatus.APPROVED,
                DeliveryChallanStatus.READY_FOR_DISPATCH
            ) is DomainResult.Success
        )

        // READY_FOR_DISPATCH -> DISPATCHED
        assertTrue(
            DeliveryChallanLifecycleValidator.validateTransition(
                DeliveryChallanStatus.READY_FOR_DISPATCH,
                DeliveryChallanStatus.DISPATCHED
            ) is DomainResult.Success
        )

        // DISPATCHED -> DELIVERED
        assertTrue(
            DeliveryChallanLifecycleValidator.validateTransition(
                DeliveryChallanStatus.DISPATCHED,
                DeliveryChallanStatus.DELIVERED
            ) is DomainResult.Success
        )
    }

    @Test
    fun `reversion from pending to draft succeeds`() {
        assertTrue(
            DeliveryChallanLifecycleValidator.validateTransition(
                DeliveryChallanStatus.PENDING,
                DeliveryChallanStatus.DRAFT
            ) is DomainResult.Success
        )
    }

    @Test
    fun `cancellation succeeds from allowed states`() {
        assertTrue(
            DeliveryChallanLifecycleValidator.validateTransition(
                DeliveryChallanStatus.DRAFT,
                DeliveryChallanStatus.CANCELLED
            ) is DomainResult.Success
        )
        assertTrue(
            DeliveryChallanLifecycleValidator.validateTransition(
                DeliveryChallanStatus.PENDING,
                DeliveryChallanStatus.CANCELLED
            ) is DomainResult.Success
        )
        assertTrue(
            DeliveryChallanLifecycleValidator.validateTransition(
                DeliveryChallanStatus.APPROVED,
                DeliveryChallanStatus.CANCELLED
            ) is DomainResult.Success
        )
        assertTrue(
            DeliveryChallanLifecycleValidator.validateTransition(
                DeliveryChallanStatus.READY_FOR_DISPATCH,
                DeliveryChallanStatus.CANCELLED
            ) is DomainResult.Success
        )
    }

    @Test
    fun `invalid transitions fail`() {
        // DRAFT -> DELIVERED (illegal skip)
        assertTrue(
            DeliveryChallanLifecycleValidator.validateTransition(
                DeliveryChallanStatus.DRAFT,
                DeliveryChallanStatus.DELIVERED
            ) is DomainResult.Error
        )

        // DRAFT -> APPROVED (must go through PENDING)
        assertTrue(
            DeliveryChallanLifecycleValidator.validateTransition(
                DeliveryChallanStatus.DRAFT,
                DeliveryChallanStatus.APPROVED
            ) is DomainResult.Error
        )

        // CANCELLED -> APPROVED (terminal state)
        assertTrue(
            DeliveryChallanLifecycleValidator.validateTransition(
                DeliveryChallanStatus.CANCELLED,
                DeliveryChallanStatus.APPROVED
            ) is DomainResult.Error
        )
    }
}
