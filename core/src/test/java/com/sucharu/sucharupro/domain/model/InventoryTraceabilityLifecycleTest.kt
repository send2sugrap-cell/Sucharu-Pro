package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryTraceabilityStatus
import com.sucharu.sucharupro.domain.validation.InventoryTraceabilityValidator
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Status transition and lifecycle rule tests for Batches & Lots (Module 07 Step 07).
 */
class InventoryTraceabilityLifecycleTest {

    @Test
    fun `valid transition from ACTIVE to HOLD`() {
        val result = InventoryTraceabilityValidator.validateStatusTransition(
            currentStatus = InventoryTraceabilityStatus.ACTIVE,
            newStatus = InventoryTraceabilityStatus.HOLD
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `valid transition from HOLD to ACTIVE`() {
        val result = InventoryTraceabilityValidator.validateStatusTransition(
            currentStatus = InventoryTraceabilityStatus.HOLD,
            newStatus = InventoryTraceabilityStatus.ACTIVE
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `valid transition from ACTIVE to CLOSED`() {
        val result = InventoryTraceabilityValidator.validateStatusTransition(
            currentStatus = InventoryTraceabilityStatus.ACTIVE,
            newStatus = InventoryTraceabilityStatus.CLOSED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `cannot transition from terminal state CLOSED`() {
        val result = InventoryTraceabilityValidator.validateStatusTransition(
            currentStatus = InventoryTraceabilityStatus.CLOSED,
            newStatus = InventoryTraceabilityStatus.ACTIVE
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("terminal state"))
    }

    @Test
    fun `cannot transition from terminal state EXHAUSTED`() {
        val result = InventoryTraceabilityValidator.validateStatusTransition(
            currentStatus = InventoryTraceabilityStatus.EXHAUSTED,
            newStatus = InventoryTraceabilityStatus.ACTIVE
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("terminal state"))
    }

    @Test
    fun `status badge labels are correct`() {
        assertTrue(InventoryTraceabilityStatus.ACTIVE.defaultLabel == "Active")
        assertTrue(InventoryTraceabilityStatus.HOLD.defaultLabel == "On Hold")
        assertTrue(InventoryTraceabilityStatus.CLOSED.defaultLabel == "Closed")
        assertTrue(InventoryTraceabilityStatus.EXHAUSTED.defaultLabel == "Exhausted")
    }

    @Test
    fun `terminal state flags are correct`() {
        assertTrue(!InventoryTraceabilityStatus.ACTIVE.isTerminal)
        assertTrue(!InventoryTraceabilityStatus.HOLD.isTerminal)
        assertTrue(InventoryTraceabilityStatus.CLOSED.isTerminal)
        assertTrue(InventoryTraceabilityStatus.EXHAUSTED.isTerminal)
        assertTrue(InventoryTraceabilityStatus.CANCELLED.isTerminal)
    }
}
