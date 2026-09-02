package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryLocationLifecycleTest {

    @Test
    fun `valid location lifecycle transitions pass`() {
        assertTrue(
            InventoryLocationValidator.validateLocationTransition(
                InventoryLocationStatus.ACTIVE,
                InventoryLocationStatus.INACTIVE
            ) is DomainResult.Success
        )
        assertTrue(
            InventoryLocationValidator.validateLocationTransition(
                InventoryLocationStatus.INACTIVE,
                InventoryLocationStatus.ACTIVE
            ) is DomainResult.Success
        )
        assertTrue(
            InventoryLocationValidator.validateLocationTransition(
                InventoryLocationStatus.INACTIVE,
                InventoryLocationStatus.ARCHIVED
            ) is DomainResult.Success
        )
    }

    @Test
    fun `terminal archived location cannot transition`() {
        val result = InventoryLocationValidator.validateLocationTransition(
            InventoryLocationStatus.ARCHIVED,
            InventoryLocationStatus.ACTIVE
        )
        assertTrue(result is DomainResult.Error)
    }
}
