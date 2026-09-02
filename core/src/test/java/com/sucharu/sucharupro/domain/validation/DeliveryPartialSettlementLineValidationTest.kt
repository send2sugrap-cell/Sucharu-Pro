package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliveryPartialSettlementLine
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryPartialSettlementLineValidationTest {

    @Test
    fun `valid settlement line passes validation`() {
        val line = DeliveryPartialSettlementLine(
            settlementLineId = "SL-1",
            projectId = "PRJ-01",
            settlementId = "SETTLE-01",
            deliveryOrderLineId = "DOL-1",
            productId = "PROD-01",
            orderedQuantity = 500.0,
            allocatedQuantity = 200.0,
            dispatchedQuantity = 200.0,
            deliveredQuantity = 200.0,
            pendingQuantity = 300.0,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        assertTrue(DeliveryPartialSettlementLineValidator.validateLine(line) is DomainResult.Success)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative ordered quantity is rejected at model level`() {
        DeliveryPartialSettlementLine(
            settlementLineId = "SL-1",
            projectId = "PRJ-01",
            settlementId = "SETTLE-01",
            deliveryOrderLineId = "DOL-1",
            productId = "PROD-01",
            orderedQuantity = -10.0,
            createdAt = 1000L,
            updatedAt = 1000L
        )
    }
}
