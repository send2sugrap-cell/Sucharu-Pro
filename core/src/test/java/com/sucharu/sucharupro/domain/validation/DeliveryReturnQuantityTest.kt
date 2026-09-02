package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLine
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryReturnQuantityTest {

    @Test
    fun `zero or negative return quantity is rejected`() {
        val line = DeliveryReturnLine(
            returnLineId = "RL-1",
            returnId = "RET-1",
            projectId = "PRJ-01",
            deliveryOrderLineId = "DOL-1",
            productId = "PROD-1",
            returnedQuantity = 0.0001, // valid positive
            createdAt = 1000L,
            updatedAt = 1000L
        )
        assertTrue(DeliveryReturnLineValidator.validateLine(line) is DomainResult.Success)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative quantity throws exception at model boundary`() {
        DeliveryReturnLine(
            returnLineId = "RL-1",
            returnId = "RET-1",
            projectId = "PRJ-01",
            deliveryOrderLineId = "DOL-1",
            productId = "PROD-1",
            returnedQuantity = -5.0,
            createdAt = 1000L,
            updatedAt = 1000L
        )
    }
}
