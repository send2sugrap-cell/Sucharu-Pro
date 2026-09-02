package com.sucharu.sucharupro.domain.service.delivery

import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturn
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnDisposition
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLine
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLineCondition
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryReturnInspectionTest {

    @Test
    fun `inspection summary aggregates accepted, rejected and returned quantities correctly`() {
        val ret = DeliveryReturn("RET-1", "PRJ-01", "RN-1", "DO-1", status = DeliveryReturnStatus.INSPECTED, requestedBy = "u1", createdAt = 1000L, updatedAt = 1000L)
        val l1 = DeliveryReturnLine(
            returnLineId = "RL-1",
            returnId = "RET-1",
            projectId = "PRJ-01",
            deliveryOrderLineId = "DOL-1",
            productId = "P-1",
            returnedQuantity = 100.0,
            receivedQuantity = 100.0,
            acceptedQuantity = 85.0,
            rejectedQuantity = 15.0,
            condition = DeliveryReturnLineCondition.GOOD,
            disposition = DeliveryReturnDisposition.RESTOCK,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val summary = DeliveryReturnEligibilityCalculator.buildSummary(ret, listOf(l1))
        assertEquals(100.0, summary.totalReturnedQuantity, 0.001)
        assertEquals(100.0, summary.totalReceivedQuantity, 0.001)
        assertEquals(85.0, summary.totalAcceptedQuantity, 0.001)
        assertEquals(15.0, summary.totalRejectedQuantity, 0.001)
        assertTrue(l1.isInspectionComplete)
    }
}
