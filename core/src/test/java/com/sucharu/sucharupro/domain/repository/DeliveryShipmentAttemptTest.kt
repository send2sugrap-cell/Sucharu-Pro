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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryShipmentAttemptTest {

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
                dispatchExecutionId = "DISP-ATT",
                projectId = "PRJ-01",
                dispatchNo = "DN-ATT",
                deliveryOrderId = "DO-01",
                deliveryChallanId = "CH-01",
                customerId = null,
                sourceWarehouseId = "WH-01",
                sourceLocationId = "LOC-01",
                dispatchType = DispatchExecutionType.STANDARD,
                status = DispatchExecutionStatus.DISPATCHED,
                stockOutId = "SO-ATT",
                dispatchDate = 1000L,
                notes = null,
                createdBy = "user-1",
                createdAt = 1000L,
                updatedAt = 1000L,
                dispatchedBy = "operator",
                dispatchedAt = 1000L
            )
            val dLine = DispatchExecutionLine("DL-1", "PRJ-01", "DISP-ATT", "CL-1", "DOL-1", "PROD-1", 100.0, 100.0, null, null, "LOC-01", 1000L)
            dispatchDataSource.insertDispatch(dispatch, listOf(dLine))

            val shipment = DeliveryShipment(
                shipmentId = "SHP-ATT",
                projectId = "PRJ-01",
                shipmentNo = "S-ATT",
                deliveryOrderId = "DO-01",
                deliveryChallanId = "CH-01",
                dispatchExecutionId = "DISP-ATT",
                currentStatus = DeliveryShipmentStatus.OUT_FOR_DELIVERY,
                createdBy = "user-1",
                createdAt = 1000L,
                updatedAt = 1000L
            )
            shipmentDataSource.insertShipment(shipment)
        }
    }

    @Test
    fun `sequential delivery attempts are recorded and increment attemptNo`() = runBlocking {
        // Attempt #1: Failed / Unavailable
        val att1Res = repository.recordDeliveryAttempt(
            shipmentId = "SHP-ATT",
            status = DeliveryShipmentAttemptStatus.RECIPIENT_UNAVAILABLE,
            reason = "Customer unreachable",
            notes = "Will retry next day",
            attemptedAt = 2000L,
            actorId = "courier",
            callerRole = UserRole.WAREHOUSE
        )
        assertTrue(att1Res is DomainResult.Success)
        assertEquals(1, (att1Res as DomainResult.Success).data.attemptNo)

        // Attempt #2: Successful
        val att2Res = repository.recordDeliveryAttempt(
            shipmentId = "SHP-ATT",
            status = DeliveryShipmentAttemptStatus.SUCCESSFUL,
            reason = null,
            notes = "Delivered to security desk",
            attemptedAt = 4000L,
            actorId = "courier",
            callerRole = UserRole.WAREHOUSE
        )
        assertTrue(att2Res is DomainResult.Success)
        assertEquals(2, (att2Res as DomainResult.Success).data.attemptNo)

        val fetchedShipment = (repository.getShipment("SHP-ATT", UserRole.ADMIN) as DomainResult.Success).data
        assertEquals(DeliveryShipmentStatus.DELIVERED, fetchedShipment.currentStatus)
    }
}
