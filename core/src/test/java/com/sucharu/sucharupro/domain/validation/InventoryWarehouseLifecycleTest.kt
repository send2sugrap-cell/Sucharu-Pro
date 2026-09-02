package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryWarehouseLifecycleTest {

    @Test
    fun `valid warehouse lifecycle transitions succeed`() {
        assertTrue(
            InventoryWarehouseValidator.validateWarehouseTransition(
                InventoryWarehouseStatus.ACTIVE,
                InventoryWarehouseStatus.INACTIVE
            ) is DomainResult.Success
        )
        assertTrue(
            InventoryWarehouseValidator.validateWarehouseTransition(
                InventoryWarehouseStatus.INACTIVE,
                InventoryWarehouseStatus.ACTIVE
            ) is DomainResult.Success
        )
        assertTrue(
            InventoryWarehouseValidator.validateWarehouseTransition(
                InventoryWarehouseStatus.INACTIVE,
                InventoryWarehouseStatus.ARCHIVED
            ) is DomainResult.Success
        )
    }

    @Test
    fun `terminal archived warehouse cannot transition back`() {
        val result = InventoryWarehouseValidator.validateWarehouseTransition(
            InventoryWarehouseStatus.ARCHIVED,
            InventoryWarehouseStatus.ACTIVE
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("terminal", ignoreCase = true))
    }
}
