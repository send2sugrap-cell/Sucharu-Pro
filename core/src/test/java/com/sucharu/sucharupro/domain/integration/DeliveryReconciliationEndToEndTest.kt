package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeDeliveryChallanDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryItemVerificationDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryProofDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryReconciliationDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryReturnDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryShipmentDataSource
import com.sucharu.sucharupro.data.datasource.FakeDispatchExecutionDataSource
import com.sucharu.sucharupro.data.repository.DeliveryReconciliationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallan
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanLine
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanStatus
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanType
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionStatus
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionType
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySettlementStatus
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProof
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofEvidence
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofEvidenceType
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofRecipient
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofStatus
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofType
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationActivityType
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationStatus
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipment
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerification
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationLine
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryReconciliationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryReconciliationEndToEndTest {

    private lateinit var reconciliationDataSource: FakeDeliveryReconciliationDataSource
    private lateinit var orderDataSource: FakeDeliveryOrderDataSource
    private lateinit var challanDataSource: FakeDeliveryChallanDataSource
    private lateinit var dispatchDataSource: FakeDispatchExecutionDataSource
    private lateinit var shipmentDataSource: FakeDeliveryShipmentDataSource
    private lateinit var verificationDataSource: FakeDeliveryItemVerificationDataSource
    private lateinit var returnDataSource: FakeDeliveryReturnDataSource
    private lateinit var proofDataSource: FakeDeliveryProofDataSource
    private lateinit var repository: DeliveryReconciliationRepository

    @Before
    fun setUp() = runBlocking {
        reconciliationDataSource = FakeDeliveryReconciliationDataSource()
        orderDataSource = FakeDeliveryOrderDataSource()
        challanDataSource = FakeDeliveryChallanDataSource()
        dispatchDataSource = FakeDispatchExecutionDataSource()
        shipmentDataSource = FakeDeliveryShipmentDataSource()
        verificationDataSource = FakeDeliveryItemVerificationDataSource()
        returnDataSource = FakeDeliveryReturnDataSource()
        proofDataSource = FakeDeliveryProofDataSource()

        repository = DeliveryReconciliationRepositoryImpl(
            reconciliationDataSource = reconciliationDataSource,
            orderDataSource = orderDataSource,
            challanDataSource = challanDataSource,
            dispatchDataSource = dispatchDataSource,
            shipmentDataSource = shipmentDataSource,
            verificationDataSource = verificationDataSource,
            returnDataSource = returnDataSource,
            proofDataSource = proofDataSource
        )

        // 1. Upstream DeliveryOrder (Step 01)
        val order = DeliveryOrder(
            deliveryOrderId = "DO-E2E-FULL",
            projectId = "PRJ-01",
            deliveryOrderNo = "DON-2026-001",
            customerId = "CUST-001",
            sourceReferenceId = "SO-001",
            sourceReferenceType = "SALES_ORDER",
            deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
            priority = DeliveryPriority.HIGH,
            status = DeliveryOrderStatus.APPROVED,
            requestedDeliveryDate = 2000L,
            notes = "Priority shipment",
            createdBy = "sales-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val line1 = DeliveryOrderLine("DOL-E2E-1", "DO-E2E-FULL", "PRJ-01", "PROD-A", 500.0, null)
        val line2 = DeliveryOrderLine("DOL-E2E-2", "DO-E2E-FULL", "PRJ-01", "PROD-B", 200.0, null)
        orderDataSource.insertDeliveryOrder(order, listOf(line1, line2))

        // 2. Delivery Challan (Step 02)
        val challan = DeliveryChallan(
            challanId = "CH-E2E-1",
            projectId = "PRJ-01",
            challanNo = "CN-2026-001",
            deliveryOrderId = "DO-E2E-FULL",
            customerId = "CUST-001",
            sourceReferenceId = "SO-001",
            sourceReferenceType = "SALES_ORDER",
            challanType = DeliveryChallanType.STANDARD,
            status = DeliveryChallanStatus.DELIVERED,
            issueDate = 1000L,
            notes = null,
            createdBy = "wh-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val cLine1 = DeliveryChallanLine("CL-E2E-1", "CH-E2E-1", "PRJ-01", "DOL-E2E-1", "PROD-A", 500.0)
        val cLine2 = DeliveryChallanLine("CL-E2E-2", "CH-E2E-1", "PRJ-01", "DOL-E2E-2", "PROD-B", 200.0)
        challanDataSource.insertChallan(challan, listOf(cLine1, cLine2))

        // 3. Dispatch Execution (Step 03)
        val dispatch = DispatchExecution(
            dispatchExecutionId = "DISP-E2E-1",
            projectId = "PRJ-01",
            dispatchNo = "DN-2026-001",
            deliveryOrderId = "DO-E2E-FULL",
            deliveryChallanId = "CH-E2E-1",
            customerId = "CUST-001",
            sourceWarehouseId = "WH-01",
            sourceLocationId = "LOC-01",
            dispatchType = DispatchExecutionType.STANDARD,
            status = DispatchExecutionStatus.DISPATCHED,
            stockOutId = "SOUT-E2E-1",
            dispatchDate = 1000L,
            notes = null,
            createdBy = "wh-1",
            createdAt = 1000L,
            updatedAt = 1000L,
            dispatchedAt = 1000L,
            dispatchedBy = "wh-1"
        )
        val dLine1 = DispatchExecutionLine("DL-E2E-1", "PRJ-01", "DISP-E2E-1", "CL-E2E-1", "DOL-E2E-1", "PROD-A", 500.0, 500.0, null, null, "LOC-01", 1000L)
        val dLine2 = DispatchExecutionLine("DL-E2E-2", "PRJ-01", "DISP-E2E-1", "CL-E2E-2", "DOL-E2E-2", "PROD-B", 200.0, 200.0, null, null, "LOC-01", 1000L)
        dispatchDataSource.insertDispatch(dispatch, listOf(dLine1, dLine2))

        // 4. Delivery Item Verification (Step 04)
        val verification = DeliveryItemVerification(
            verificationId = "VER-E2E-1",
            projectId = "PRJ-01",
            verificationNo = "VN-2026-001",
            deliveryOrderId = "DO-E2E-FULL",
            deliveryChallanId = "CH-E2E-1",
            dispatchExecutionId = "DISP-E2E-1",
            status = DeliveryItemVerificationStatus.VERIFIED,
            createdBy = "qc-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val vLine1 = DeliveryItemVerificationLine("VL-E2E-1", "VER-E2E-1", "PRJ-01", "DL-E2E-1", "CL-E2E-1", "DOL-E2E-1", "PROD-A", expectedQuantity = 500.0, verifiedQuantity = 500.0)
        val vLine2 = DeliveryItemVerificationLine("VL-E2E-2", "VER-E2E-1", "PRJ-01", "DL-E2E-2", "CL-E2E-2", "DOL-E2E-2", "PROD-B", expectedQuantity = 200.0, verifiedQuantity = 200.0)
        verificationDataSource.insertVerification(verification, listOf(vLine1, vLine2))

        // 5. Delivery Shipment (Step 05)
        val shipment = DeliveryShipment(
            shipmentId = "SHP-E2E-1",
            projectId = "PRJ-01",
            shipmentNo = "SN-2026-001",
            deliveryOrderId = "DO-E2E-FULL",
            deliveryChallanId = "CH-E2E-1",
            dispatchExecutionId = "DISP-E2E-1",
            verificationId = "VER-E2E-1",
            currentStatus = DeliveryShipmentStatus.DELIVERED,
            createdBy = "wh-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        shipmentDataSource.insertShipment(shipment)

        // 6. Accepted Proof of Delivery (Step 08)
        val proof = DeliveryProof(
            proofId = "POD-E2E-ACCEPTED",
            projectId = "PRJ-01",
            deliveryOrderId = "DO-E2E-FULL",
            deliveryChallanId = "CH-E2E-1",
            dispatchExecutionId = "DISP-E2E-1",
            deliveryShipmentId = "SHP-E2E-1",
            verificationId = "VER-E2E-1",
            customerId = "CUST-001",
            proofNo = "POD-2026-001",
            proofType = DeliveryProofType.SIGNATURE,
            proofStatus = DeliveryProofStatus.ACCEPTED,
            recipientName = "Customer Director",
            createdBy = "wh-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val evidence = DeliveryProofEvidence("EVD-1", "POD-E2E-ACCEPTED", "PRJ-01", DeliveryProofEvidenceType.SIGNATURE_IMAGE, "gs://b/sig.png", "sig.png", "image/png", isPrimary = true, uploadedBy = "wh-1", uploadedAt = 1000L)
        val recipient = DeliveryProofRecipient("REC-1", "POD-E2E-ACCEPTED", "PRJ-01", "Customer Director", confirmedAt = 1000L, confirmedBy = "wh-1")
        proofDataSource.insertProof(proof)
        proofDataSource.insertEvidence(evidence)
        proofDataSource.insertRecipient(recipient)
    }

    @Test
    fun `full end to end delivery reconciliation workflow produces deterministic settlement`() = runBlocking {
        // Step 1: Create Reconciliation
        val createRes = repository.createReconciliation("DO-E2E-FULL", "operator-1", UserRole.WAREHOUSE)
        assertTrue(createRes is DomainResult.Success)
        val rec = (createRes as DomainResult.Success).data
        val recId = rec.reconciliationId

        // Initial state verified
        assertEquals(700.0, rec.orderedQuantity, 0.001)
        assertEquals(700.0, rec.challanedQuantity, 0.001)
        assertEquals(700.0, rec.dispatchedQuantity, 0.001)
        assertEquals(700.0, rec.deliveredQuantity, 0.001)
        assertEquals(700.0, rec.acceptedPodQuantity, 0.001)
        assertEquals(0.0, rec.outstandingQuantity, 0.001)
        assertEquals(0.0, rec.discrepancyQuantity, 0.001)
        assertEquals(DeliveryReconciliationStatus.RECONCILED, rec.reconciliationStatus)
        assertEquals(DeliverySettlementStatus.SETTLED, rec.settlementStatus)

        // Step 2: Refresh Calculation
        val refreshRes = repository.refreshCalculation(recId, "operator-1", UserRole.WAREHOUSE)
        assertTrue(refreshRes is DomainResult.Success)

        // Step 3: Close Reconciliation by Manager
        val closeRes = repository.closeReconciliation(recId, "manager-1", "Reconciled with 100% accepted POD", UserRole.MANAGER)
        assertTrue(closeRes is DomainResult.Success)
        val finalRec = (closeRes as DomainResult.Success).data
        assertEquals(DeliveryReconciliationStatus.CLOSED, finalRec.reconciliationStatus)
        assertEquals("manager-1", finalRec.closedBy)
        assertNotNull(finalRec.closedAt)

        // Step 4: Verify Audit Events Chronology
        val eventsRes = repository.getActivityEvents(recId, UserRole.ADMIN)
        assertTrue(eventsRes is DomainResult.Success)
        val events = (eventsRes as DomainResult.Success).data
        assertTrue(events.size >= 3)
        val eventTypes = events.map { it.activityType }
        assertTrue(eventTypes.contains(DeliveryReconciliationActivityType.CREATED))
        assertTrue(eventTypes.contains(DeliveryReconciliationActivityType.CALCULATION_REFRESHED))
        assertTrue(eventTypes.contains(DeliveryReconciliationActivityType.CLOSED))

        // Step 5: Verify Summary
        val summary = repository.observeSummary("PRJ-01").first()
        assertEquals(1, summary.totalReconciliations)
        assertEquals(1, summary.closedCount)
        assertEquals(0, summary.totalDiscrepancyCount)
    }
}
