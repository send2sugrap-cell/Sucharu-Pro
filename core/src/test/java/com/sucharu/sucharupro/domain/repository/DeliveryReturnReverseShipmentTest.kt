package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryReturnDataSource
import com.sucharu.sucharupro.data.repository.DeliveryReturnRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturn
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnActivityType
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLine
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnShipment
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnShipmentStatus
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryReturnReverseShipmentTest {

    private lateinit var returnDataSource: FakeDeliveryReturnDataSource
    private lateinit var doDataSource: FakeDeliveryOrderDataSource
    private lateinit var repository: DeliveryReturnRepository

    @Before
    fun setUp() {
        runBlocking {
            returnDataSource = FakeDeliveryReturnDataSource()
            doDataSource = FakeDeliveryOrderDataSource()
            repository = DeliveryReturnRepositoryImpl(returnDataSource, doDataSource)

            val doOrder = DeliveryOrder(
                deliveryOrderId = "DO-REV",
                projectId = "PRJ-01",
                deliveryOrderNo = "DON-REV",
                customerId = "CUST-01",
                sourceReferenceId = "SO-01",
                sourceReferenceType = "SALES_ORDER",
                deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
                priority = DeliveryPriority.NORMAL,
                status = DeliveryOrderStatus.APPROVED,
                requestedDeliveryDate = 2000L,
                notes = null,
                createdBy = "user-1",
                createdAt = 1000L,
                updatedAt = 1000L
            )
            val doLine = DeliveryOrderLine("DOL-REV", "DO-REV", "PRJ-01", "PROD-01", 100.0, null)
            doDataSource.insertDeliveryOrder(doOrder, listOf(doLine))

            val ret = DeliveryReturn(
                returnId = "RET-REV",
                projectId = "PRJ-01",
                returnNo = "RN-REV",
                deliveryOrderId = "DO-REV",
                status = DeliveryReturnStatus.APPROVED,
                requestedBy = "user-1",
                createdAt = 1000L,
                updatedAt = 1000L
            )
            val line = DeliveryReturnLine(
                returnLineId = "RL-REV",
                returnId = "RET-REV",
                projectId = "PRJ-01",
                deliveryOrderLineId = "DOL-REV",
                productId = "PROD-01",
                returnedQuantity = 20.0,
                createdAt = 1000L,
                updatedAt = 1000L
            )
            returnDataSource.insertReturn(ret, listOf(line))
        }
    }

    @Test
    fun `create and progress reverse shipment through valid lifecycle states`() = runBlocking {
        val shipment = DeliveryReturnShipment(
            reverseShipmentId = "REV-SHP-01",
            returnId = "RET-REV",
            projectId = "PRJ-01",
            carrierName = "Sundarban Courier",
            trackingNumber = "SUN-998877",
            pickupAddress = "Customer Warehouse",
            destinationAddress = "Main Factory Warehouse",
            status = DeliveryReturnShipmentStatus.READY,
            createdBy = "user-1"
        )

        val createRes = repository.createReverseShipment(shipment, "user-1", UserRole.WAREHOUSE)
        assertTrue(createRes is DomainResult.Success)
        assertEquals("REV-SHP-01", (createRes as DomainResult.Success).data.reverseShipmentId)

        // Progress to PICKED_UP
        val pickupRes = repository.updateReverseShipmentStatus(
            returnId = "RET-REV",
            newStatus = DeliveryReturnShipmentStatus.PICKED_UP,
            notes = "Picked up by driver",
            actorId = "user-1",
            callerRole = UserRole.WAREHOUSE
        )
        assertTrue(pickupRes is DomainResult.Success)
        assertEquals(DeliveryReturnShipmentStatus.PICKED_UP, (pickupRes as DomainResult.Success).data.status)

        // Progress to IN_TRANSIT
        val inTransitRes = repository.updateReverseShipmentStatus(
            returnId = "RET-REV",
            newStatus = DeliveryReturnShipmentStatus.IN_TRANSIT,
            notes = "In transit to factory",
            actorId = "user-1",
            callerRole = UserRole.WAREHOUSE
        )
        assertTrue(inTransitRes is DomainResult.Success)

        // Progress to DELIVERED_TO_WAREHOUSE
        val deliveredRes = repository.updateReverseShipmentStatus(
            returnId = "RET-REV",
            newStatus = DeliveryReturnShipmentStatus.DELIVERED_TO_WAREHOUSE,
            notes = "Delivered to warehouse dock",
            actorId = "user-1",
            callerRole = UserRole.WAREHOUSE
        )
        assertTrue(deliveredRes is DomainResult.Success)

        // Verify audit trail
        val eventsRes = repository.getEvents("RET-REV", UserRole.ADMIN)
        assertTrue(eventsRes is DomainResult.Success)
        val eventTypes = (eventsRes as DomainResult.Success).data.map { it.activityType }
        assertTrue(eventTypes.contains(DeliveryReturnActivityType.REVERSE_SHIPMENT_CREATED))
        assertTrue(eventTypes.contains(DeliveryReturnActivityType.REVERSE_SHIPMENT_UPDATED))
    }

    @Test
    fun `reverse shipment rejects cross project creation`() = runBlocking {
        val shipment = DeliveryReturnShipment(
            reverseShipmentId = "REV-SHP-BAD",
            returnId = "RET-REV",
            projectId = "PRJ-WRONG",
            carrierName = "DHL",
            createdBy = "user-1"
        )
        val res = repository.createReverseShipment(shipment, "user-1", UserRole.WAREHOUSE)
        assertTrue(res is DomainResult.Error)
    }
}
