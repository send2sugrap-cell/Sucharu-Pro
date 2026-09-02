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
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentActivityType
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryShipmentAuditTest {

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
                dispatchExecutionId = "DISP-AUDIT",
                projectId = "PRJ-01",
                dispatchNo = "DN-AUDIT",
                deliveryOrderId = "DO-01",
                deliveryChallanId = "CH-01",
                customerId = null,
                sourceWarehouseId = "WH-01",
                sourceLocationId = "LOC-01",
                dispatchType = DispatchExecutionType.STANDARD,
                status = DispatchExecutionStatus.DISPATCHED,
                stockOutId = "SO-AUDIT",
                dispatchDate = 1000L,
                notes = null,
                createdBy = "user-1",
                createdAt = 1000L,
                updatedAt = 1000L,
                dispatchedBy = "operator",
                dispatchedAt = 1000L
            )
            val dLine = DispatchExecutionLine("DL-1", "PRJ-01", "DISP-AUDIT", "CL-1", "DOL-1", "PROD-1", 100.0, 100.0, null, null, "LOC-01", 1000L)
            dispatchDataSource.insertDispatch(dispatch, listOf(dLine))
        }
    }

    @Test
    fun `shipment operations generate comprehensive audit trail`() = runBlocking {
        val s = DeliveryShipment(
            shipmentId = "SHP-AUDIT",
            projectId = "PRJ-01",
            shipmentNo = "S-AUDIT",
            deliveryOrderId = "DO-01",
            deliveryChallanId = "CH-01",
            dispatchExecutionId = "DISP-AUDIT",
            currentStatus = DeliveryShipmentStatus.DRAFT,
            createdBy = "user-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        // 1. Create
        repository.createShipment(s, UserRole.ADMIN)
        // 2. Ready
        repository.markReady("SHP-AUDIT", "operator", UserRole.WAREHOUSE)
        // 3. Dispatched
        repository.markDispatched("SHP-AUDIT", 2000L, "operator", UserRole.WAREHOUSE)
        // 4. In Transit
        repository.markInTransit("SHP-AUDIT", "Hub 1", null, "operator", UserRole.WAREHOUSE)
        // 5. Out For Delivery
        repository.markOutForDelivery("SHP-AUDIT", "Van 4", null, "operator", UserRole.WAREHOUSE)
        // 6. Delivered
        repository.markDelivered("SHP-AUDIT", 5000L, null, "operator", UserRole.WAREHOUSE)

        val activities = (repository.getActivityEvents("SHP-AUDIT", UserRole.ADMIN) as DomainResult.Success).data
        val types = activities.map { it.activityType }

        assertTrue(types.contains(DeliveryShipmentActivityType.CREATED))
        assertTrue(types.contains(DeliveryShipmentActivityType.READY))
        assertTrue(types.contains(DeliveryShipmentActivityType.DISPATCHED))
        assertTrue(types.contains(DeliveryShipmentActivityType.IN_TRANSIT))
        assertTrue(types.contains(DeliveryShipmentActivityType.OUT_FOR_DELIVERY))
        assertTrue(types.contains(DeliveryShipmentActivityType.DELIVERED))
    }
}
