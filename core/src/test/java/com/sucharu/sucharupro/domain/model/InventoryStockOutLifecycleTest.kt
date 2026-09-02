package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOut
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutStatus
import com.sucharu.sucharupro.domain.validation.InventoryStockOutLifecycleValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * State transition rule tests for [InventoryStockOut] (Module 07 Step 04).
 */
class InventoryStockOutLifecycleTest {

    @Test
    fun `valid transitions from DRAFT`() {
        assertTrue(InventoryStockOutLifecycleValidator.validateTransition(InventoryStockOutStatus.DRAFT, InventoryStockOutStatus.PENDING) is DomainResult.Success)
        assertTrue(InventoryStockOutLifecycleValidator.validateTransition(InventoryStockOutStatus.DRAFT, InventoryStockOutStatus.CANCELLED) is DomainResult.Success)
    }

    @Test
    fun `invalid transitions from DRAFT`() {
        assertTrue(InventoryStockOutLifecycleValidator.validateTransition(InventoryStockOutStatus.DRAFT, InventoryStockOutStatus.ISSUING) is DomainResult.Error)
        assertTrue(InventoryStockOutLifecycleValidator.validateTransition(InventoryStockOutStatus.DRAFT, InventoryStockOutStatus.COMPLETED) is DomainResult.Error)
    }

    @Test
    fun `valid transition from PENDING`() {
        assertTrue(InventoryStockOutLifecycleValidator.validateTransition(InventoryStockOutStatus.PENDING, InventoryStockOutStatus.ISSUING) is DomainResult.Success)
        assertTrue(InventoryStockOutLifecycleValidator.validateTransition(InventoryStockOutStatus.PENDING, InventoryStockOutStatus.CANCELLED) is DomainResult.Success)
    }

    @Test
    fun `valid transition from ISSUING`() {
        assertTrue(InventoryStockOutLifecycleValidator.validateTransition(InventoryStockOutStatus.ISSUING, InventoryStockOutStatus.COMPLETED) is DomainResult.Success)
    }

    @Test
    fun `invalid transitions from terminal states`() {
        assertTrue(InventoryStockOutLifecycleValidator.validateTransition(InventoryStockOutStatus.COMPLETED, InventoryStockOutStatus.DRAFT) is DomainResult.Error)
        assertTrue(InventoryStockOutLifecycleValidator.validateTransition(InventoryStockOutStatus.CANCELLED, InventoryStockOutStatus.DRAFT) is DomainResult.Error)
    }

    @Test
    fun `canMutate returns false for terminal states`() {
        assertFalse(InventoryStockOutLifecycleValidator.canMutate(InventoryStockOutStatus.COMPLETED))
        assertFalse(InventoryStockOutLifecycleValidator.canMutate(InventoryStockOutStatus.CANCELLED))
        assertTrue(InventoryStockOutLifecycleValidator.canMutate(InventoryStockOutStatus.DRAFT))
        assertTrue(InventoryStockOutLifecycleValidator.canMutate(InventoryStockOutStatus.PENDING))
        assertTrue(InventoryStockOutLifecycleValidator.canMutate(InventoryStockOutStatus.ISSUING))
    }
}
