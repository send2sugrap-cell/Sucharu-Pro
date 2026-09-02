package com.sucharu.sucharupro.domain.service.delivery

import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturn
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLine
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class DeliveryReturnEligibilityTest {

    @Test
    fun `remaining returnable quantity properly deducts active completed returns`() {
        val ret1 = DeliveryReturn("RET-1", "PRJ-01", "RN-1", "DO-1", status = DeliveryReturnStatus.COMPLETED, requestedBy = "u1", createdAt = 1000L, updatedAt = 1000L)
        val l1 = DeliveryReturnLine("RL-1", "RET-1", "PRJ-01", "DOL-1", productId = "P-1", returnedQuantity = 30.0, createdAt = 1000L, updatedAt = 1000L)

        val retCancelled = DeliveryReturn("RET-2", "PRJ-01", "RN-2", "DO-1", status = DeliveryReturnStatus.CANCELLED, requestedBy = "u1", createdAt = 2000L, updatedAt = 2000L)
        val l2 = DeliveryReturnLine("RL-2", "RET-2", "PRJ-01", "DOL-1", productId = "P-1", returnedQuantity = 20.0, createdAt = 2000L, updatedAt = 2000L)

        val eligible = DeliveryReturnEligibilityCalculator.calculateEligibleReturnQuantity(
            deliveryOrderLineId = "DOL-1",
            deliveredQuantity = 100.0,
            existingReturns = listOf(ret1 to listOf(l1), retCancelled to listOf(l2))
        )

        // 100 delivered - 30 (from RET-1) = 70. Cancelled RET-2 ignored.
        assertEquals(70.0, eligible, 0.001)
    }
}
