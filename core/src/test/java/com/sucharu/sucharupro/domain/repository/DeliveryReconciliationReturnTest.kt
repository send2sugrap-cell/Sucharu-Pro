package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProof
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofStatus
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLine
import com.sucharu.sucharupro.domain.service.delivery.DeliveryReconciliationCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class DeliveryReconciliationReturnTest {

    @Test
    fun `returns are correctly captured and reflected in reconciliation metrics`() {
        val order = DeliveryOrder(
            deliveryOrderId = "DO-RET",
            projectId = "PRJ-01",
            deliveryOrderNo = "DON-R",
            customerId = "CUST-1",
            sourceReferenceId = "SO-1",
            sourceReferenceType = "SO",
            deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
            priority = DeliveryPriority.NORMAL,
            status = DeliveryOrderStatus.APPROVED,
            requestedDeliveryDate = 2000L,
            notes = null,
            createdBy = "u1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val lines = listOf(DeliveryOrderLine("DOL-1", "DO-RET", "PRJ-01", "PROD-1", 100.0, null))
        val dispatches = listOf(
            DispatchExecutionLine("DL-1", "PRJ-01", "DISP-1", "CL-1", "DOL-1", "PROD-1", 100.0, 100.0, null, null, "LOC-1", 1000L)
        )
        val returns = listOf(
            DeliveryReturnLine(
                returnLineId = "RL-1",
                returnId = "RET-1",
                projectId = "PRJ-01",
                deliveryOrderLineId = "DOL-1",
                productId = "PROD-1",
                returnedQuantity = 20.0,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )
        val proofs = listOf(
            DeliveryProof("POD-1", "PRJ-01", "DO-RET", "CH-1", "DISP-1", "SHP-1", null, null, "POD-1", proofStatus = DeliveryProofStatus.ACCEPTED, createdBy = "u1", createdAt = 1000L, updatedAt = 1000L)
        )

        val result = DeliveryReconciliationCalculator.calculateReconciliation(
            reconciliationId = "REC-R",
            deliveryOrder = order,
            orderLines = lines,
            dispatchLines = dispatches,
            returnLines = returns,
            proofs = proofs,
            actorId = "u1"
        )

        assertEquals(100.0, result.aggregate.orderedQuantity, 0.001)
        assertEquals(100.0, result.aggregate.deliveredQuantity, 0.001)
        assertEquals(20.0, result.aggregate.returnedQuantity, 0.001)
    }
}
