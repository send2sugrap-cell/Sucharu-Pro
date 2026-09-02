package com.sucharu.sucharupro.domain.service.delivery

import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import org.junit.Assert.assertEquals
import org.junit.Test

class DeliveryRemainingQuantityTest {

    @Test
    fun `sequential partial deliveries accurately reduce remaining pending quantity`() {
        val doLine = DeliveryOrderLine("DOL-1", "DO-1", "PRJ-1", "PROD-1", 1000.0, null)

        // Delivery 1: 400 pcs
        val res1 = DeliveryPartialSettlementCalculator.calculateLineSettlement(
            settlementId = "S-1",
            orderLine = doLine,
            recordedDeliveredQuantity = 400.0
        )
        assertEquals(600.0, res1.pendingQuantity, 0.001)

        // Delivery 2: 350 more (total 750)
        val res2 = DeliveryPartialSettlementCalculator.calculateLineSettlement(
            settlementId = "S-1",
            orderLine = doLine,
            recordedDeliveredQuantity = 750.0
        )
        assertEquals(250.0, res2.pendingQuantity, 0.001)

        // Delivery 3: 250 more (total 1000)
        val res3 = DeliveryPartialSettlementCalculator.calculateLineSettlement(
            settlementId = "S-1",
            orderLine = doLine,
            recordedDeliveredQuantity = 1000.0
        )
        assertEquals(0.0, res3.pendingQuantity, 0.001)
        assertEquals(com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySettlementStatus.FULLY_DELIVERED, res3.status)
    }
}
