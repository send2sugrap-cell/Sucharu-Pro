package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanLine
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProof
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofStatus
import com.sucharu.sucharupro.domain.service.delivery.DeliveryReconciliationCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class DeliveryReconciliationQuantityTest {

    @Test
    fun `deterministic calculation computes quantities accurately`() {
        val order = DeliveryOrder(
            deliveryOrderId = "DO-1",
            projectId = "PRJ-01",
            deliveryOrderNo = "DON-1",
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
        val lines = listOf(
            DeliveryOrderLine("DOL-1", "DO-1", "PRJ-01", "PROD-A", 100.0, null),
            DeliveryOrderLine("DOL-2", "DO-1", "PRJ-01", "PROD-B", 50.0, null)
        )
        val challans = listOf(
            DeliveryChallanLine("CL-1", "CH-1", "PRJ-01", "DOL-1", "PROD-A", 100.0),
            DeliveryChallanLine("CL-2", "CH-1", "PRJ-01", "DOL-2", "PROD-B", 50.0)
        )
        val dispatches = listOf(
            DispatchExecutionLine("DL-1", "PRJ-01", "DISP-1", "CL-1", "DOL-1", "PROD-A", 100.0, 100.0, null, null, "LOC-1", 1000L),
            DispatchExecutionLine("DL-2", "PRJ-01", "DISP-1", "CL-2", "DOL-2", "PROD-B", 50.0, 50.0, null, null, "LOC-1", 1000L)
        )
        val proofs = listOf(
            DeliveryProof(
                proofId = "POD-1",
                projectId = "PRJ-01",
                deliveryOrderId = "DO-1",
                deliveryChallanId = "CH-1",
                dispatchExecutionId = "DISP-1",
                deliveryShipmentId = "SHP-1",
                proofNo = "POD-NO-1",
                proofStatus = DeliveryProofStatus.ACCEPTED,
                createdBy = "u1",
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val result = DeliveryReconciliationCalculator.calculateReconciliation(
            reconciliationId = "REC-1",
            deliveryOrder = order,
            orderLines = lines,
            challanLines = challans,
            dispatchLines = dispatches,
            proofs = proofs,
            actorId = "u1"
        )

        assertEquals(150.0, result.aggregate.orderedQuantity, 0.001)
        assertEquals(150.0, result.aggregate.challanedQuantity, 0.001)
        assertEquals(150.0, result.aggregate.dispatchedQuantity, 0.001)
        assertEquals(0.0, result.aggregate.outstandingQuantity, 0.001)
    }
}
