package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.inventory.reorder.InventoryStockLevelPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class InventoryStockLevelPolicyValidationTest {

    @Test
    fun `valid policy creates successfully`() {
        val policy = InventoryStockLevelPolicy(
            policyId = "POL-001",
            projectId = "PROJ-1",
            productId = "PROD-1",
            minimumStockLevel = 10.0,
            reorderPoint = 20.0,
            criticalStockLevel = 5.0,
            targetStockLevel = 50.0,
            maximumStockLevel = 100.0
        )
        assertEquals("POL-001", policy.policyId)
    }

    @Test
    fun `negative thresholds throw exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            InventoryStockLevelPolicy(
                policyId = "POL-001",
                projectId = "PROJ-1",
                productId = "PROD-1",
                minimumStockLevel = -1.0
            )
        }
    }

    @Test
    fun `invalid logical ordering throws exception - max less than target`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            InventoryStockLevelPolicy(
                policyId = "POL-001",
                projectId = "PROJ-1",
                productId = "PROD-1",
                targetStockLevel = 50.0,
                maximumStockLevel = 40.0
            )
        }
        assertEquals("maximumStockLevel cannot be less than targetStockLevel.", exception.message)
    }

    @Test
    fun `invalid logical ordering throws exception - target less than reorder`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            InventoryStockLevelPolicy(
                policyId = "POL-001",
                projectId = "PROJ-1",
                productId = "PROD-1",
                reorderPoint = 30.0,
                targetStockLevel = 20.0,
                maximumStockLevel = 100.0
            )
        }
        assertEquals("targetStockLevel cannot be less than reorderPoint.", exception.message)
    }
}
