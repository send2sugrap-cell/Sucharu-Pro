package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProof
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofStatus
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationDiscrepancyType
import com.sucharu.sucharupro.domain.service.delivery.DeliveryReconciliationCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryReconciliationPodIntegrationTest {

    @Test
    fun `missing accepted POD creates POD_MISSING discrepancy`() {
        val order = DeliveryOrder(
            deliveryOrderId = "DO-POD",
            projectId = "PRJ-01",
            deliveryOrderNo = "DON-P",
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
        val lines = listOf(DeliveryOrderLine("DOL-1", "DO-POD", "PRJ-01", "PROD-1", 100.0, null))
        val dispatches = listOf(
            DispatchExecutionLine("DL-1", "PRJ-01", "DISP-1", "CL-1", "DOL-1", "PROD-1", 100.0, 100.0, null, null, "LOC-1", 1000L)
        )
        // No accepted proof, only draft proof
        val proofs = listOf(
            DeliveryProof("POD-DRAFT", "PRJ-01", "DO-POD", "CH-1", "DISP-1", "SHP-1", null, null, "POD-1", proofStatus = DeliveryProofStatus.DRAFT, createdBy = "u1", createdAt = 1000L, updatedAt = 1000L)
        )

        val result = DeliveryReconciliationCalculator.calculateReconciliation(
            reconciliationId = "REC-POD",
            deliveryOrder = order,
            orderLines = lines,
            dispatchLines = dispatches,
            proofs = proofs,
            actorId = "u1"
        )

        assertEquals(0.0, result.aggregate.acceptedPodQuantity, 0.001)
        assertTrue(result.discrepancies.any { it.discrepancyType == DeliveryReconciliationDiscrepancyType.POD_MISSING })
    }

    @Test
    fun `accepted POD recognizes full acceptedPodQuantity without discrepancy`() {
        val order = DeliveryOrder(
            deliveryOrderId = "DO-POD-OK",
            projectId = "PRJ-01",
            deliveryOrderNo = "DON-P",
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
        val lines = listOf(DeliveryOrderLine("DOL-1", "DO-POD-OK", "PRJ-01", "PROD-1", 100.0, null))
        val dispatches = listOf(
            DispatchExecutionLine("DL-1", "PRJ-01", "DISP-1", "CL-1", "DOL-1", "PROD-1", 100.0, 100.0, null, null, "LOC-1", 1000L)
        )
        val proofs = listOf(
            DeliveryProof("POD-OK", "PRJ-01", "DO-POD-OK", "CH-1", "DISP-1", "SHP-1", null, null, "POD-1", proofStatus = DeliveryProofStatus.ACCEPTED, createdBy = "u1", createdAt = 1000L, updatedAt = 1000L)
        )

        val result = DeliveryReconciliationCalculator.calculateReconciliation(
            reconciliationId = "REC-POD-OK",
            deliveryOrder = order,
            orderLines = lines,
            dispatchLines = dispatches,
            proofs = proofs,
            actorId = "u1"
        )

        assertEquals(100.0, result.aggregate.acceptedPodQuantity, 0.001)
        assertTrue(result.discrepancies.none { it.discrepancyType == DeliveryReconciliationDiscrepancyType.POD_MISSING })
    }
}
