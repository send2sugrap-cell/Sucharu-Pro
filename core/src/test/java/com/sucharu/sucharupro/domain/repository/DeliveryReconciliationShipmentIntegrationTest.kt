package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDeliveryChallanDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryProofDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryReconciliationDataSource
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
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProof
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofStatus
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationStatus
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipment
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryReconciliationShipmentIntegrationTest {

    private lateinit var reconciliationDataSource: FakeDeliveryReconciliationDataSource
    private lateinit var orderDataSource: FakeDeliveryOrderDataSource
    private lateinit var challanDataSource: FakeDeliveryChallanDataSource
    private lateinit var dispatchDataSource: FakeDispatchExecutionDataSource
    private lateinit var shipmentDataSource: FakeDeliveryShipmentDataSource
    private lateinit var proofDataSource: FakeDeliveryProofDataSource
    private lateinit var repository: DeliveryReconciliationRepository

    @Before
    fun setUp() = runBlocking {
        reconciliationDataSource = FakeDeliveryReconciliationDataSource()
        orderDataSource = FakeDeliveryOrderDataSource()
        challanDataSource = FakeDeliveryChallanDataSource()
        dispatchDataSource = FakeDispatchExecutionDataSource()
        shipmentDataSource = FakeDeliveryShipmentDataSource()
        proofDataSource = FakeDeliveryProofDataSource()

        repository = DeliveryReconciliationRepositoryImpl(
            reconciliationDataSource = reconciliationDataSource,
            orderDataSource = orderDataSource,
            challanDataSource = challanDataSource,
            dispatchDataSource = dispatchDataSource,
            shipmentDataSource = shipmentDataSource,
            proofDataSource = proofDataSource
        )

        val order = DeliveryOrder(
            deliveryOrderId = "DO-SHP",
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
        val line = DeliveryOrderLine("DOL-1", "DO-SHP", "PRJ-01", "P-1", 100.0, null)
        orderDataSource.insertDeliveryOrder(order, listOf(line))

        val challan = DeliveryChallan(
            challanId = "CH-1",
            projectId = "PRJ-01",
            challanNo = "CN-1",
            deliveryOrderId = "DO-SHP",
            customerId = "CUST-1",
            sourceReferenceId = "SO-1",
            sourceReferenceType = "SO",
            challanType = DeliveryChallanType.STANDARD,
            status = DeliveryChallanStatus.DELIVERED,
            issueDate = 1000L,
            notes = null,
            createdBy = "wh-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val cLine = DeliveryChallanLine("CL-1", "CH-1", "PRJ-01", "DOL-1", "P-1", 100.0)
        challanDataSource.insertChallan(challan, listOf(cLine))

        val dispatch = DispatchExecution(
            dispatchExecutionId = "DISP-1",
            projectId = "PRJ-01",
            dispatchNo = "DN-1",
            deliveryOrderId = "DO-SHP",
            deliveryChallanId = "CH-1",
            customerId = "CUST-1",
            sourceWarehouseId = "WH-1",
            sourceLocationId = "LOC-1",
            dispatchType = DispatchExecutionType.STANDARD,
            status = DispatchExecutionStatus.DISPATCHED,
            stockOutId = "SOUT-1",
            dispatchDate = 1000L,
            notes = null,
            createdBy = "wh-1",
            createdAt = 1000L,
            updatedAt = 1000L,
            dispatchedAt = 1000L,
            dispatchedBy = "wh-1"
        )
        val dLine = DispatchExecutionLine("DL-1", "PRJ-01", "DISP-1", "CL-1", "DOL-1", "P-1", 100.0, 100.0, null, null, "LOC-1", 1000L)
        dispatchDataSource.insertDispatch(dispatch, listOf(dLine))

        val shipment = DeliveryShipment(
            shipmentId = "SHP-1",
            projectId = "PRJ-01",
            shipmentNo = "S-1",
            deliveryOrderId = "DO-SHP",
            deliveryChallanId = "CH-1",
            dispatchExecutionId = "DISP-1",
            currentStatus = DeliveryShipmentStatus.DELIVERED,
            createdBy = "u1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        shipmentDataSource.insertShipment(shipment)

        val proof = DeliveryProof(
            proofId = "POD-1",
            projectId = "PRJ-01",
            deliveryOrderId = "DO-SHP",
            deliveryChallanId = "CH-1",
            dispatchExecutionId = "DISP-1",
            deliveryShipmentId = "SHP-1",
            proofNo = "POD-1",
            proofStatus = DeliveryProofStatus.ACCEPTED,
            createdBy = "u1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        proofDataSource.insertProof(proof)
    }

    @Test
    fun `delivered shipment with accepted POD reconciles automatically to RECONCILED`() = runBlocking {
        val createRes = repository.createReconciliation("DO-SHP", "op-1", UserRole.WAREHOUSE)
        assertTrue(createRes is DomainResult.Success)
        val rec = (createRes as DomainResult.Success).data

        assertEquals(100.0, rec.deliveredQuantity, 0.001)
        assertEquals(100.0, rec.acceptedPodQuantity, 0.001)
        assertEquals(0.0, rec.outstandingQuantity, 0.001)
        assertEquals(DeliveryReconciliationStatus.RECONCILED, rec.reconciliationStatus)
    }
}
