package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class DispatchExecutionLifecycleTest {

    @Test
    fun `valid progression from draft through dispatched succeeds`() {
        // DRAFT -> PENDING
        assertTrue(
            DispatchExecutionLifecycleValidator.validateTransition(
                DispatchExecutionStatus.DRAFT,
                DispatchExecutionStatus.PENDING
            ) is DomainResult.Success
        )

        // PENDING -> APPROVED
        assertTrue(
            DispatchExecutionLifecycleValidator.validateTransition(
                DispatchExecutionStatus.PENDING,
                DispatchExecutionStatus.APPROVED
            ) is DomainResult.Success
        )

        // APPROVED -> READY_FOR_EXECUTION
        assertTrue(
            DispatchExecutionLifecycleValidator.validateTransition(
                DispatchExecutionStatus.APPROVED,
                DispatchExecutionStatus.READY_FOR_EXECUTION
            ) is DomainResult.Success
        )

        // READY_FOR_EXECUTION -> DISPATCHED
        assertTrue(
            DispatchExecutionLifecycleValidator.validateTransition(
                DispatchExecutionStatus.READY_FOR_EXECUTION,
                DispatchExecutionStatus.DISPATCHED
            ) is DomainResult.Success
        )
    }

    @Test
    fun `reversion from pending to draft succeeds`() {
        assertTrue(
            DispatchExecutionLifecycleValidator.validateTransition(
                DispatchExecutionStatus.PENDING,
                DispatchExecutionStatus.DRAFT
            ) is DomainResult.Success
        )
    }

    @Test
    fun `cancellation succeeds from non terminal states`() {
        for (status in listOf(
            DispatchExecutionStatus.DRAFT,
            DispatchExecutionStatus.PENDING,
            DispatchExecutionStatus.APPROVED,
            DispatchExecutionStatus.READY_FOR_EXECUTION,
            DispatchExecutionStatus.EXECUTING
        )) {
            assertTrue(
                DispatchExecutionLifecycleValidator.validateTransition(
                    status,
                    DispatchExecutionStatus.CANCELLED
                ) is DomainResult.Success
            )
        }
    }

    @Test
    fun `transitions from terminal states are rejected`() {
        assertTrue(
            DispatchExecutionLifecycleValidator.validateTransition(
                DispatchExecutionStatus.DISPATCHED,
                DispatchExecutionStatus.DRAFT
            ) is DomainResult.Error
        )
        assertTrue(
            DispatchExecutionLifecycleValidator.validateTransition(
                DispatchExecutionStatus.CANCELLED,
                DispatchExecutionStatus.APPROVED
            ) is DomainResult.Error
        )
    }
}
