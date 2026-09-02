package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLine
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryReturnLineValidationTest {

    @Test
    fun `valid return line passes validation`() {
        val line = DeliveryReturnLine(
            returnLineId = "RL-1",
            returnId = "RET-1",
            projectId = "PRJ-01",
            deliveryOrderLineId = "DOL-1",
            productId = "PROD-1",
            returnedQuantity = 100.0,
            receivedQuantity = 100.0,
            acceptedQuantity = 90.0,
            rejectedQuantity = 10.0,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        assertTrue(DeliveryReturnLineValidator.validateLine(line) is DomainResult.Success)
    }

    @Test
    fun `return quantity exceeding eligible delivered quantity is rejected`() {
        val line = DeliveryReturnLine(
            returnLineId = "RL-1",
            returnId = "RET-1",
            projectId = "PRJ-01",
            deliveryOrderLineId = "DOL-1",
            productId = "PROD-1",
            returnedQuantity = 150.0,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val result = DeliveryReturnLineValidator.validateLine(line, maxEligibleReturnQuantity = 100.0)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("exceeds max eligible returnable quantity"))
    }

    @Test
    fun `accepted plus rejected quantity exceeding received quantity is rejected`() {
        val line = DeliveryReturnLine(
            returnLineId = "RL-1",
            returnId = "RET-1",
            projectId = "PRJ-01",
            deliveryOrderLineId = "DOL-1",
            productId = "PROD-1",
            returnedQuantity = 100.0,
            receivedQuantity = 100.0,
            acceptedQuantity = 80.0,
            rejectedQuantity = 30.0, // 80 + 30 = 110 > 100
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val result = DeliveryReturnLineValidator.validateLine(line)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("cannot exceed available quantity"))
    }
}
