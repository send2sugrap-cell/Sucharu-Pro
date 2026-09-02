package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentStatus
import com.sucharu.sucharupro.domain.validation.InventoryStockAdjustmentLifecycleValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * State transition rule tests for [InventoryStockAdjustmentStatus] (Module 07 Step 06).
 */
class InventoryStockAdjustmentLifecycleTest {

    @Test
    fun `valid transitions from DRAFT`() {
        assertTrue(InventoryStockAdjustmentLifecycleValidator.validateAdjustmentTransition(InventoryStockAdjustmentStatus.DRAFT, InventoryStockAdjustmentStatus.PENDING) is DomainResult.Success)
        assertTrue(InventoryStockAdjustmentLifecycleValidator.validateAdjustmentTransition(InventoryStockAdjustmentStatus.DRAFT, InventoryStockAdjustmentStatus.CANCELLED) is DomainResult.Success)
    }

    @Test
    fun `invalid transitions from DRAFT`() {
        assertTrue(InventoryStockAdjustmentLifecycleValidator.validateAdjustmentTransition(InventoryStockAdjustmentStatus.DRAFT, InventoryStockAdjustmentStatus.APPROVED) is DomainResult.Error)
        assertTrue(InventoryStockAdjustmentLifecycleValidator.validateAdjustmentTransition(InventoryStockAdjustmentStatus.DRAFT, InventoryStockAdjustmentStatus.COMPLETED) is DomainResult.Error)
    }

    @Test
    fun `valid transitions from PENDING`() {
        assertTrue(InventoryStockAdjustmentLifecycleValidator.validateAdjustmentTransition(InventoryStockAdjustmentStatus.PENDING, InventoryStockAdjustmentStatus.APPROVED) is DomainResult.Success)
        assertTrue(InventoryStockAdjustmentLifecycleValidator.validateAdjustmentTransition(InventoryStockAdjustmentStatus.PENDING, InventoryStockAdjustmentStatus.CANCELLED) is DomainResult.Success)
    }

    @Test
    fun `valid transitions from APPROVED`() {
        assertTrue(InventoryStockAdjustmentLifecycleValidator.validateAdjustmentTransition(InventoryStockAdjustmentStatus.APPROVED, InventoryStockAdjustmentStatus.ADJUSTING) is DomainResult.Success)
        assertTrue(InventoryStockAdjustmentLifecycleValidator.validateAdjustmentTransition(InventoryStockAdjustmentStatus.APPROVED, InventoryStockAdjustmentStatus.CANCELLED) is DomainResult.Success)
    }

    @Test
    fun `valid transition from ADJUSTING`() {
        assertTrue(InventoryStockAdjustmentLifecycleValidator.validateAdjustmentTransition(InventoryStockAdjustmentStatus.ADJUSTING, InventoryStockAdjustmentStatus.COMPLETED) is DomainResult.Success)
    }

    @Test
    fun `invalid transitions from terminal states`() {
        assertTrue(InventoryStockAdjustmentLifecycleValidator.validateAdjustmentTransition(InventoryStockAdjustmentStatus.COMPLETED, InventoryStockAdjustmentStatus.DRAFT) is DomainResult.Error)
        assertTrue(InventoryStockAdjustmentLifecycleValidator.validateAdjustmentTransition(InventoryStockAdjustmentStatus.CANCELLED, InventoryStockAdjustmentStatus.DRAFT) is DomainResult.Error)
    }

    @Test
    fun `terminal status identification`() {
        assertTrue(InventoryStockAdjustmentStatus.COMPLETED.isTerminal)
        assertTrue(InventoryStockAdjustmentStatus.CANCELLED.isTerminal)
        assertFalse(InventoryStockAdjustmentStatus.DRAFT.isTerminal)
        assertFalse(InventoryStockAdjustmentStatus.PENDING.isTerminal)
        assertFalse(InventoryStockAdjustmentStatus.APPROVED.isTerminal)
        assertFalse(InventoryStockAdjustmentStatus.ADJUSTING.isTerminal)
    }
}
