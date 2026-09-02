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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryShipmentProjectIsolationTest {

    private lateinit var shipmentDataSource: FakeDeliveryShipmentDataSource
    private lateinit var dispatchDataSource: FakeDispatchExecutionDataSource
    private lateinit var repository: DeliveryShipmentRepository

    @Before
    fun setUp() {
        runBlocking {
            shipmentDataSource = FakeDeliveryShipmentDataSource()
            dispatchDataSource = FakeDispatchExecutionDataSource()
            repository = DeliveryShipmentRepositoryImpl(shipmentDataSource, dispatchDataSource)

            val dispA = DispatchExecution(
                dispatchExecutionId = "DISP-A",
                projectId = "PRJ-A",
                dispatchNo = "DN-A",
                deliveryOrderId = "DO-A",
                deliveryChallanId = "CH-A",
                customerId = null,
                sourceWarehouseId = "WH-A",
                sourceLocationId = "LOC-A",
                dispatchType = DispatchExecutionType.STANDARD,
                status = DispatchExecutionStatus.DISPATCHED,
                stockOutId = "SO-A",
                dispatchDate = 1000L,
                notes = null,
                createdBy = "user-1",
                createdAt = 1000L,
                updatedAt = 1000L,
                dispatchedBy = "operator",
                dispatchedAt = 1000L
            )
            val dLineA = DispatchExecutionLine("DLA", "PRJ-A", "DISP-A", "CLA", "DOLA", "PROD-A", 10.0, 10.0, null, null, "LOC-A", 1000L)
            dispatchDataSource.insertDispatch(dispA, listOf(dLineA))

            val dispB = DispatchExecution(
                dispatchExecutionId = "DISP-B",
                projectId = "PRJ-B",
                dispatchNo = "DN-B",
                deliveryOrderId = "DO-B",
                deliveryChallanId = "CH-B",
                customerId = null,
                sourceWarehouseId = "WH-B",
                sourceLocationId = "LOC-B",
                dispatchType = DispatchExecutionType.STANDARD,
                status = DispatchExecutionStatus.DISPATCHED,
                stockOutId = "SO-B",
                dispatchDate = 1000L,
                notes = null,
                createdBy = "user-2",
                createdAt = 1000L,
                updatedAt = 1000L,
                dispatchedBy = "operator",
                dispatchedAt = 1000L
            )
            val dLineB = DispatchExecutionLine("DLB", "PRJ-B", "DISP-B", "CLB", "DOLB", "PROD-B", 20.0, 20.0, null, null, "LOC-B", 1000L)
            dispatchDataSource.insertDispatch(dispB, listOf(dLineB))
        }
    }

    @Test
    fun `observeShipments returns strictly project isolated list`() = runBlocking {
        val sA = DeliveryShipment("S-A", "PRJ-A", "SN-A", "DO-A", "CH-A", "DISP-A", currentStatus = DeliveryShipmentStatus.DRAFT, createdBy = "user-1", createdAt = 1000L, updatedAt = 1000L)
        val sB = DeliveryShipment("S-B", "PRJ-B", "SN-B", "DO-B", "CH-B", "DISP-B", currentStatus = DeliveryShipmentStatus.DRAFT, createdBy = "user-2", createdAt = 1000L, updatedAt = 1000L)

        repository.createShipment(sA, UserRole.ADMIN, "PRJ-A")
        repository.createShipment(sB, UserRole.ADMIN, "PRJ-B")

        val listA = repository.observeShipments("PRJ-A").first()
        val listB = repository.observeShipments("PRJ-B").first()

        assertEquals(1, listA.size)
        assertEquals("S-A", listA[0].shipmentId)

        assertEquals(1, listB.size)
        assertEquals("S-B", listB[0].shipmentId)
    }

    @Test
    fun `cross project getShipment is denied`() = runBlocking {
        val sA = DeliveryShipment("S-A", "PRJ-A", "SN-A", "DO-A", "CH-A", "DISP-A", currentStatus = DeliveryShipmentStatus.DRAFT, createdBy = "user-1", createdAt = 1000L, updatedAt = 1000L)
        repository.createShipment(sA, UserRole.ADMIN, "PRJ-A")

        val result = repository.getShipment("S-A", UserRole.ADMIN, "PRJ-B")
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Access denied"))
    }
}
