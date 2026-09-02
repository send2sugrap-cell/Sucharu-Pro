package com.sucharu.sucharupro.domain.service.delivery

import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanLine
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySettlementStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class DeliveryPartialQuantityCalculationTest {

    @Test
    fun `deterministic calculation computes correct pending and short quantities`() {
        val doLine = DeliveryOrderLine("DOL-1", "DO-1", "PRJ-1", "PROD-1", 1000.0, null)
        val cLine = DeliveryChallanLine("CL-1", "CH-1", "PRJ-1", "DOL-1", "PROD-1", 600.0)
        val dLine = DispatchExecutionLine("DL-1", "PRJ-1", "DISP-1", "CL-1", "DOL-1", "PROD-1", 600.0, 600.0, null, null, "LOC-1", 1000L)

        val result = DeliveryPartialSettlementCalculator.calculateLineSettlement(
            settlementId = "SETTLE-1",
            orderLine = doLine,
            challanLines = listOf(cLine),
            dispatchLines = listOf(dLine),
            recordedDeliveredQuantity = 400.0
        )

        assertEquals(1000.0, result.orderedQuantity, 0.001)
        assertEquals(600.0, result.allocatedQuantity, 0.001)
        assertEquals(600.0, result.dispatchedQuantity, 0.001)
        assertEquals(400.0, result.deliveredQuantity, 0.001)
        assertEquals(200.0, result.shortQuantity, 0.001) // 600 dispatched - 400 delivered = 200 short
        assertEquals(600.0, result.pendingQuantity, 0.001) // 1000 ordered - 400 delivered = 600 pending
        assertEquals(DeliverySettlementStatus.PARTIALLY_DELIVERED, result.status)
    }
}
