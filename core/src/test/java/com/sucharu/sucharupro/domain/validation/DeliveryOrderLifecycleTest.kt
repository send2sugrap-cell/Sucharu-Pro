package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryOrderLifecycleTest {

    @Test
    fun `valid lifecycle progression succeeds`() {
        // DRAFT -> PENDING
        assertTrue(
            DeliveryOrderValidator.validateStatusTransition(
                DeliveryOrderStatus.DRAFT,
                DeliveryOrderStatus.PENDING
            ) is DomainResult.Success
        )

        // PENDING -> APPROVED
        assertTrue(
            DeliveryOrderValidator.validateStatusTransition(
                DeliveryOrderStatus.PENDING,
                DeliveryOrderStatus.APPROVED
            ) is DomainResult.Success
        )

        // APPROVED -> READY_FOR_DISPATCH
        assertTrue(
            DeliveryOrderValidator.validateStatusTransition(
                DeliveryOrderStatus.APPROVED,
                DeliveryOrderStatus.READY_FOR_DISPATCH
            ) is DomainResult.Success
        )

        // READY_FOR_DISPATCH -> DISPATCHED
        assertTrue(
            DeliveryOrderValidator.validateStatusTransition(
                DeliveryOrderStatus.READY_FOR_DISPATCH,
                DeliveryOrderStatus.DISPATCHED
            ) is DomainResult.Success
        )

        // DISPATCHED -> DELIVERED
        assertTrue(
            DeliveryOrderValidator.validateStatusTransition(
                DeliveryOrderStatus.DISPATCHED,
                DeliveryOrderStatus.DELIVERED
            ) is DomainResult.Success
        )
    }

    @Test
    fun `reversion to draft from pending succeeds`() {
        assertTrue(
            DeliveryOrderValidator.validateStatusTransition(
                DeliveryOrderStatus.PENDING,
                DeliveryOrderStatus.DRAFT
            ) is DomainResult.Success
        )
    }

    @Test
    fun `cancellation from non-terminal states succeeds`() {
        assertTrue(
            DeliveryOrderValidator.validateStatusTransition(
                DeliveryOrderStatus.DRAFT,
                DeliveryOrderStatus.CANCELLED
            ) is DomainResult.Success
        )
        assertTrue(
            DeliveryOrderValidator.validateStatusTransition(
                DeliveryOrderStatus.PENDING,
                DeliveryOrderStatus.CANCELLED
            ) is DomainResult.Success
        )
        assertTrue(
            DeliveryOrderValidator.validateStatusTransition(
                DeliveryOrderStatus.APPROVED,
                DeliveryOrderStatus.CANCELLED
            ) is DomainResult.Success
        )
        assertTrue(
            DeliveryOrderValidator.validateStatusTransition(
                DeliveryOrderStatus.READY_FOR_DISPATCH,
                DeliveryOrderStatus.CANCELLED
            ) is DomainResult.Success
        )
    }

    @Test
    fun `invalid lifecycle jumps fail`() {
        // DRAFT -> DELIVERED (illegal skip)
        assertTrue(
            DeliveryOrderValidator.validateStatusTransition(
                DeliveryOrderStatus.DRAFT,
                DeliveryOrderStatus.DELIVERED
            ) is DomainResult.Error
        )

        // DRAFT -> APPROVED (must go through PENDING)
        assertTrue(
            DeliveryOrderValidator.validateStatusTransition(
                DeliveryOrderStatus.DRAFT,
                DeliveryOrderStatus.APPROVED
            ) is DomainResult.Error
        )

        // DELIVERED -> DRAFT (terminal state)
        assertTrue(
            DeliveryOrderValidator.validateStatusTransition(
                DeliveryOrderStatus.DELIVERED,
                DeliveryOrderStatus.DRAFT
            ) is DomainResult.Error
        )

        // CANCELLED -> APPROVED (terminal state)
        assertTrue(
            DeliveryOrderValidator.validateStatusTransition(
                DeliveryOrderStatus.CANCELLED,
                DeliveryOrderStatus.APPROVED
            ) is DomainResult.Error
        )
    }
}
