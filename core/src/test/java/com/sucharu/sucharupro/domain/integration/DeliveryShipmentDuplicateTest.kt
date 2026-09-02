package com.sucharu.sucharupro.domain.integration

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
import com.sucharu.sucharupro.domain.repository.DeliveryShipmentRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryShipmentDuplicateTest {

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
                dispatchExecutionId = "DISP-DUP",
                projectId = "PRJ-01",
                dispatchNo = "DN-DUP",
                deliveryOrderId = "DO-01",
                deliveryChallanId = "CH-01",
                customerId = null,
                sourceWarehouseId = "WH-01",
                sourceLocationId = "LOC-01",
                dispatchType = DispatchExecutionType.STANDARD,
                status = DispatchExecutionStatus.DISPATCHED,
                stockOutId = "SO-DUP",
                dispatchDate = 1000L,
                notes = null,
                createdBy = "user-1",
                createdAt = 1000L,
                updatedAt = 1000L,
                dispatchedBy = "operator",
                dispatchedAt = 1000L
            )
            val dLine = DispatchExecutionLine("DL-1", "PRJ-01", "DISP-DUP", "CL-1", "DOL-1", "PROD-1", 100.0, 100.0, null, null, "LOC-01", 1000L)
            dispatchDataSource.insertDispatch(dispatch, listOf(dLine))
        }
    }

    @Test
    fun `duplicate shipment number within same project is rejected`() = runBlocking {
        val s1 = DeliveryShipment("S-1", "PRJ-01", "SHP-DUP", "DO-01", "CH-01", "DISP-DUP", currentStatus = DeliveryShipmentStatus.DRAFT, createdBy = "user-1", createdAt = 1000L, updatedAt = 1000L)
        val res1 = repository.createShipment(s1, UserRole.ADMIN)
        assertTrue(res1 is DomainResult.Success)

        val s2 = DeliveryShipment("S-2", "PRJ-01", "SHP-DUP", "DO-01", "CH-01", "DISP-DUP", currentStatus = DeliveryShipmentStatus.DRAFT, createdBy = "user-2", createdAt = 1000L, updatedAt = 1000L)
        val res2 = repository.createShipment(s2, UserRole.ADMIN)
        assertTrue(res2 is DomainResult.Error)
        assertTrue((res2 as DomainResult.Error).message.contains("already exists"))
    }

    @Test
    fun `duplicate tracking number within same project is rejected`() = runBlocking {
        val s1 = DeliveryShipment("S-1", "PRJ-01", "SHP-01", "DO-01", "CH-01", "DISP-DUP", trackingNumber = "TRK-9999", currentStatus = DeliveryShipmentStatus.DRAFT, createdBy = "user-1", createdAt = 1000L, updatedAt = 1000L)
        val res1 = repository.createShipment(s1, UserRole.ADMIN)
        assertTrue(res1 is DomainResult.Success)

        val s2 = DeliveryShipment("S-2", "PRJ-01", "SHP-02", "DO-01", "CH-01", "DISP-DUP", trackingNumber = "TRK-9999", currentStatus = DeliveryShipmentStatus.DRAFT, createdBy = "user-2", createdAt = 1000L, updatedAt = 1000L)
        val res2 = repository.createShipment(s2, UserRole.ADMIN)
        assertTrue(res2 is DomainResult.Error)
        assertTrue((res2 as DomainResult.Error).message.contains("Tracking number 'TRK-9999' already exists"))
    }
}
