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
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentAttemptStatus
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryShipmentConcurrencyTest {

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
                dispatchExecutionId = "DISP-CONCUR",
                projectId = "PRJ-01",
                dispatchNo = "DN-CONCUR",
                deliveryOrderId = "DO-01",
                deliveryChallanId = "CH-01",
                customerId = "CUST-01",
                sourceWarehouseId = "WH-01",
                sourceLocationId = "LOC-01",
                dispatchType = DispatchExecutionType.STANDARD,
                status = DispatchExecutionStatus.DISPATCHED,
                stockOutId = "SO-CONCUR",
                dispatchDate = 1000L,
                notes = null,
                createdBy = "user-1",
                createdAt = 1000L,
                updatedAt = 1000L,
                dispatchedBy = "operator",
                dispatchedAt = 1000L
            )
            val dLine = DispatchExecutionLine("DL-1", "PRJ-01", "DISP-CONCUR", "CL-1", "DOL-1", "PROD-1", 100.0, 100.0, null, null, "LOC-01", 1000L)
            dispatchDataSource.insertDispatch(dispatch, listOf(dLine))

            val shipment = DeliveryShipment(
                shipmentId = "SHP-CONCUR",
                projectId = "PRJ-01",
                shipmentNo = "S-CONCUR",
                deliveryOrderId = "DO-01",
                deliveryChallanId = "CH-01",
                dispatchExecutionId = "DISP-CONCUR",
                currentStatus = DeliveryShipmentStatus.OUT_FOR_DELIVERY,
                createdBy = "user-1",
                createdAt = 1000L,
                updatedAt = 1000L
            )
            shipmentDataSource.insertShipment(shipment)
        }
    }

    @Test
    fun `concurrent tracking events and attempt recordings preserve integrity`() = runBlocking {
        val jobs = listOf(
            async(Dispatchers.IO) {
                repository.recordDeliveryAttempt(
                    shipmentId = "SHP-CONCUR",
                    status = DeliveryShipmentAttemptStatus.RECIPIENT_UNAVAILABLE,
                    reason = "Gate locked",
                    notes = null,
                    attemptedAt = 2000L,
                    actorId = "op-1",
                    callerRole = UserRole.WAREHOUSE
                )
            },
            async(Dispatchers.IO) {
                repository.addTrackingEvent(
                    shipmentId = "SHP-CONCUR",
                    eventType = com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentEventType.NOTE_ADDED,
                    locationText = "Sector 7",
                    description = "Customer called to reschedule",
                    eventTime = 2050L,
                    actorId = "op-2",
                    callerRole = UserRole.WAREHOUSE
                )
            }
        )

        val results = jobs.awaitAll()
        assertTrue(results.all { it is DomainResult.Success })

        val attempts = (repository.getDeliveryAttempts("SHP-CONCUR", UserRole.ADMIN) as DomainResult.Success).data
        assertEquals(1, attempts.size)
        assertEquals(1, attempts[0].attemptNo)

        val events = (repository.getTrackingEvents("SHP-CONCUR", UserRole.ADMIN) as DomainResult.Success).data
        assertTrue(events.size >= 2)
    }
}
