package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDeliveryShipmentDataSource
import com.sucharu.sucharupro.data.datasource.FakeDispatchExecutionDataSource
import com.sucharu.sucharupro.data.repository.DeliveryShipmentRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionStatus
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionType
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipment
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryShipmentRepositoryTest {

    private lateinit var shipmentDataSource: FakeDeliveryShipmentDataSource
    private lateinit var dispatchDataSource: FakeDispatchExecutionDataSource
    private lateinit var repository: DeliveryShipmentRepository

    @Before
    fun setUp() {
        runBlocking {
            shipmentDataSource = FakeDeliveryShipmentDataSource()
            dispatchDataSource = FakeDispatchExecutionDataSource()
            repository = DeliveryShipmentRepositoryImpl(
                shipmentDataSource = shipmentDataSource,
                dispatchDataSource = dispatchDataSource
            )

            val dispatch = DispatchExecution(
                dispatchExecutionId = "DISP-01",
                projectId = "PRJ-01",
                dispatchNo = "DN-01",
                deliveryOrderId = "DO-01",
                deliveryChallanId = "CH-01",
                customerId = "CUST-01",
                sourceWarehouseId = "WH-01",
                sourceLocationId = "LOC-01",
                dispatchType = DispatchExecutionType.STANDARD,
                status = DispatchExecutionStatus.DISPATCHED,
                stockOutId = "SO-01",
                dispatchDate = 1000L,
                notes = null,
                createdBy = "user-1",
                createdAt = 1000L,
                updatedAt = 1000L,
                dispatchedBy = "operator",
                dispatchedAt = 1000L
            )
            val dLine = DispatchExecutionLine(
                dispatchExecutionLineId = "DL-01",
                projectId = "PRJ-01",
                dispatchExecutionId = "DISP-01",
                deliveryChallanLineId = "CL-01",
                deliveryOrderLineId = "DOL-01",
                productId = "PROD-01",
                requestedQuantity = 100.0,
                dispatchQuantity = 100.0,
                batchId = null,
                lotId = null,
                sourceLocationId = "LOC-01",
                createdAt = 1000L
            )
            dispatchDataSource.insertDispatch(dispatch, listOf(dLine))
        }
    }

    private fun sampleShipment(id: String = "SHP-01", no: String = "S-001") = DeliveryShipment(
        shipmentId = id,
        projectId = "PRJ-01",
        shipmentNo = no,
        deliveryOrderId = "DO-01",
        deliveryChallanId = "CH-01",
        dispatchExecutionId = "DISP-01",
        currentStatus = DeliveryShipmentStatus.DRAFT,
        createdBy = "user-1",
        createdAt = 1000L,
        updatedAt = 1000L
    )

    @Test
    fun `createShipment creates record and initial tracking event`() = runBlocking {
        val shipment = sampleShipment()
        val result = repository.createShipment(shipment, UserRole.ADMIN)
        assertTrue(result is DomainResult.Success)

        val fetched = repository.getShipment(shipment.shipmentId, UserRole.ADMIN)
        assertTrue(fetched is DomainResult.Success)
        assertEquals(shipment.shipmentNo, (fetched as DomainResult.Success).data.shipmentNo)

        val eventsRes = repository.getTrackingEvents(shipment.shipmentId, UserRole.ADMIN)
        assertTrue(eventsRes is DomainResult.Success)
        assertEquals(1, (eventsRes as DomainResult.Success).data.size)
    }

    @Test
    fun `full operational workflow transitions correctly`() = runBlocking {
        val shipment = sampleShipment()
        repository.createShipment(shipment, UserRole.ADMIN)

        // 1. Mark Ready
        val readyRes = repository.markReady(shipment.shipmentId, "operator", UserRole.WAREHOUSE)
        assertTrue(readyRes is DomainResult.Success)
        assertEquals(DeliveryShipmentStatus.READY, (readyRes as DomainResult.Success).data.currentStatus)

        // 2. Mark Dispatched
        val dispRes = repository.markDispatched(shipment.shipmentId, 2000L, "operator", UserRole.WAREHOUSE)
        assertTrue(dispRes is DomainResult.Success)
        assertEquals(DeliveryShipmentStatus.DISPATCHED, (dispRes as DomainResult.Success).data.currentStatus)

        // 3. Mark In Transit
        val transitRes = repository.markInTransit(shipment.shipmentId, "Highway Hub", null, "operator", UserRole.WAREHOUSE)
        assertTrue(transitRes is DomainResult.Success)
        assertEquals(DeliveryShipmentStatus.IN_TRANSIT, (transitRes as DomainResult.Success).data.currentStatus)

        // 4. Mark Out for Delivery
        val outRes = repository.markOutForDelivery(shipment.shipmentId, "Local Dispatch Van", null, "operator", UserRole.WAREHOUSE)
        assertTrue(outRes is DomainResult.Success)
        assertEquals(DeliveryShipmentStatus.OUT_FOR_DELIVERY, (outRes as DomainResult.Success).data.currentStatus)

        // 5. Mark Delivered
        val deliveredRes = repository.markDelivered(shipment.shipmentId, 5000L, "Handed over to recipient", "operator", UserRole.WAREHOUSE)
        assertTrue(deliveredRes is DomainResult.Success)
        assertEquals(DeliveryShipmentStatus.DELIVERED, (deliveredRes as DomainResult.Success).data.currentStatus)

        // Verify events timeline count
        val events = (repository.getTrackingEvents(shipment.shipmentId, UserRole.ADMIN) as DomainResult.Success).data
        assertEquals(6, events.size)
    }
}
