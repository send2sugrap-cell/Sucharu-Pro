package com.sucharu.sucharupro.domain.validation.communication.automation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.automation.AutomationExecutionStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunicationAutomationLifecycleTest {

    @Test
    fun transition_sameStatus_alwaysSucceeds() {
        AutomationExecutionStatus.entries.forEach { status ->
            val result = CommunicationAutomationLifecycleValidator.validateTransition(status, status)
            assertTrue("Same status transition should succeed for $status", result is DomainResult.Success)
        }
    }

    @Test
    fun transition_received_to_evaluating_succeeds() {
        val result = CommunicationAutomationLifecycleValidator.validateTransition(
            AutomationExecutionStatus.RECEIVED,
            AutomationExecutionStatus.EVALUATING
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun transition_evaluating_to_matched_succeeds() {
        val result = CommunicationAutomationLifecycleValidator.validateTransition(
            AutomationExecutionStatus.EVALUATING,
            AutomationExecutionStatus.MATCHED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun transition_matched_to_dispatching_succeeds() {
        val result = CommunicationAutomationLifecycleValidator.validateTransition(
            AutomationExecutionStatus.MATCHED,
            AutomationExecutionStatus.DISPATCHING
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun transition_dispatching_to_completed_succeeds() {
        val result = CommunicationAutomationLifecycleValidator.validateTransition(
            AutomationExecutionStatus.DISPATCHING,
            AutomationExecutionStatus.COMPLETED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun transition_fromCompleted_toAnyOther_fails() {
        AutomationExecutionStatus.entries.filter { it != AutomationExecutionStatus.COMPLETED }.forEach { target ->
            val result = CommunicationAutomationLifecycleValidator.validateTransition(
                AutomationExecutionStatus.COMPLETED,
                target
            )
            assertTrue("Completed terminal state cannot transition to $target", result is DomainResult.Error)
        }
    }
}
