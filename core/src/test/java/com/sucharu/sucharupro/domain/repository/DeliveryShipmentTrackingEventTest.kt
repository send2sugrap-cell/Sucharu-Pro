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
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentEventType
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryShipmentTrackingEventTest {

    private lateinit var shipmentDataSource: FakeDeliveryShipmentDataSource
    private lateinit var dispatchDataSource: FakeDispatchExecutionDataSource
    private lateinit var repository: DeliveryShipmentRepository

    @Before
    fun setUp() {
        runBlocking {
            shipmentDataSource = FakeDeliveryShipmentDataSource()
            dispatchDataSource = FakeDispatchExecutionDataSource()
            repository = DeliveryShipmentRepositoryImpl(shipmentDataSource, dispatchDataSource)

            val dispatch = DispatchExecution(
                dispatchExecutionId = "DISP-EVT",
                projectId = "PRJ-01",
                dispatchNo = "DN-EVT",
                deliveryOrderId = "DO-01",
                deliveryChallanId = "CH-01",
                customerId = null,
                sourceWarehouseId = "WH-01",
                sourceLocationId = "LOC-01",
                dispatchType = DispatchExecutionType.STANDARD,
                status = DispatchExecutionStatus.DISPATCHED,
                stockOutId = "SO-EVT",
                dispatchDate = 1000L,
                notes = null,
                createdBy = "user-1",
                createdAt = 1000L,
                updatedAt = 1000L,
                dispatchedBy = "operator",
                dispatchedAt = 1000L
            )
            val dLine = DispatchExecutionLine("DL-1", "PRJ-01", "DISP-EVT", "CL-1", "DOL-1", "PROD-1", 100.0, 100.0, null, null, "LOC-01", 1000L)
            dispatchDataSource.insertDispatch(dispatch, listOf(dLine))

            val shipment = DeliveryShipment(
                shipmentId = "SHP-EVT",
                projectId = "PRJ-01",
                shipmentNo = "S-EVT",
                deliveryOrderId = "DO-01",
                deliveryChallanId = "CH-01",
                dispatchExecutionId = "DISP-EVT",
                currentStatus = DeliveryShipmentStatus.DISPATCHED,
                createdBy = "user-1",
                createdAt = 1000L,
                updatedAt = 1000L
            )
            shipmentDataSource.insertShipment(shipment)
        }
    }

    @Test
    fun `tracking events append monotonically and preserve chronological order`() = runBlocking {
        repository.addTrackingEvent("SHP-EVT", DeliveryShipmentEventType.PICKED_UP, "Origin Hub", "Courier collected items", 2000L, "agent", UserRole.WAREHOUSE)
        repository.addTrackingEvent("SHP-EVT", DeliveryShipmentEventType.ARRIVED_AT_HUB, "Sorting Facility A", "Sorted for transit", 3000L, "agent", UserRole.WAREHOUSE)
        repository.addTrackingEvent("SHP-EVT", DeliveryShipmentEventType.DEPARTED_HUB, "Sorting Facility A", "On vehicle to destination", 4000L, "agent", UserRole.WAREHOUSE)

        val eventsResult = repository.getTrackingEvents("SHP-EVT", UserRole.ADMIN)
        assertTrue(eventsResult is DomainResult.Success)
        val events = (eventsResult as DomainResult.Success).data

        assertEquals(3, events.size)
        assertEquals(DeliveryShipmentEventType.PICKED_UP, events[0].eventType)
        assertEquals(DeliveryShipmentEventType.ARRIVED_AT_HUB, events[1].eventType)
        assertEquals(DeliveryShipmentEventType.DEPARTED_HUB, events[2].eventType)
    }
}
