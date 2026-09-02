package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProof
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofStatus
import com.sucharu.sucharupro.domain.service.delivery.DeliveryReconciliationCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class DeliveryReconciliationSplitDispatchTest {

    @Test
    fun `split dispatches aggregate correctly against single order`() {
        val order = DeliveryOrder(
            deliveryOrderId = "DO-SPLIT",
            projectId = "PRJ-01",
            deliveryOrderNo = "DON-S",
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
        val lines = listOf(DeliveryOrderLine("DOL-1", "DO-SPLIT", "PRJ-01", "PROD-1", 1000.0, null))
        val dispatches = listOf(
            DispatchExecutionLine("DL-1", "PRJ-01", "DISP-1", "CL-1", "DOL-1", "PROD-1", 600.0, 600.0, null, null, "LOC-1", 1000L),
            DispatchExecutionLine("DL-2", "PRJ-01", "DISP-2", "CL-2", "DOL-1", "PROD-1", 400.0, 400.0, null, null, "LOC-1", 1000L)
        )
        val proofs = listOf(
            DeliveryProof("POD-1", "PRJ-01", "DO-SPLIT", "CH-1", "DISP-1", "SHP-1", null, null, "POD-1", proofStatus = DeliveryProofStatus.ACCEPTED, createdBy = "u1", createdAt = 1000L, updatedAt = 1000L)
        )

        val result = DeliveryReconciliationCalculator.calculateReconciliation(
            reconciliationId = "REC-S",
            deliveryOrder = order,
            orderLines = lines,
            dispatchLines = dispatches,
            proofs = proofs,
            actorId = "u1"
        )

        assertEquals(1000.0, result.aggregate.orderedQuantity, 0.001)
        assertEquals(1000.0, result.aggregate.dispatchedQuantity, 0.001)
        assertEquals(1000.0, result.aggregate.deliveredQuantity, 0.001)
        assertEquals(0.0, result.aggregate.outstandingQuantity, 0.001)
    }
}
