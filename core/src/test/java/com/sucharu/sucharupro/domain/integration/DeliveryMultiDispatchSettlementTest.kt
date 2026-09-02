package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryPartialSettlementDataSource
import com.sucharu.sucharupro.data.datasource.FakeDispatchExecutionDataSource
import com.sucharu.sucharupro.data.repository.DeliveryPartialSettlementRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionStatus
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryPartialSettlementRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryMultiDispatchSettlementTest {

    private lateinit var settlementDataSource: FakeDeliveryPartialSettlementDataSource
    private lateinit var doDataSource: FakeDeliveryOrderDataSource
    private lateinit var dispatchDataSource: FakeDispatchExecutionDataSource
    private lateinit var repository: DeliveryPartialSettlementRepository

    @Before
    fun setUp() {
        runBlocking {
            settlementDataSource = FakeDeliveryPartialSettlementDataSource()
            doDataSource = FakeDeliveryOrderDataSource()
            dispatchDataSource = FakeDispatchExecutionDataSource()

            repository = DeliveryPartialSettlementRepositoryImpl(
                settlementDataSource = settlementDataSource,
                doDataSource = doDataSource,
                dispatchDataSource = dispatchDataSource
            )

            // Order 1000 pcs
            val doOrder = DeliveryOrder("DO-MULTI-DISP", "PRJ-01", "DON-MD", "CUST-01", "SO-01", "SALES_ORDER", DeliveryOrderType.CUSTOMER_DELIVERY, DeliveryPriority.NORMAL, DeliveryOrderStatus.APPROVED, 2000L, null, "user-1", 1000L, 1000L)
            val doLine = DeliveryOrderLine("DOL-MD", "DO-MULTI-DISP", "PRJ-01", "PROD-01", 1000.0, null)
            doDataSource.insertDeliveryOrder(doOrder, listOf(doLine))

            // Dispatch 1: 500 pcs
            val d1 = DispatchExecution(
                dispatchExecutionId = "DISP-1",
                projectId = "PRJ-01",
                dispatchNo = "DN-1",
                deliveryOrderId = "DO-MULTI-DISP",
                deliveryChallanId = "CH-1",
                customerId = "CUST-01",
                sourceWarehouseId = "WH-01",
                sourceLocationId = "LOC-01",
                dispatchType = DispatchExecutionType.STANDARD,
                status = DispatchExecutionStatus.DISPATCHED,
                stockOutId = "SO-1",
                dispatchDate = 1000L,
                notes = null,
                createdBy = "user-1",
                createdAt = 1000L,
                updatedAt = 1000L,
                dispatchedAt = 1000L,
                dispatchedBy = "operator"
            )
            val dl1 = DispatchExecutionLine("DL-1", "PRJ-01", "DISP-1", "CL-1", "DOL-MD", "PROD-01", 500.0, 500.0, null, null, "LOC-01", 1000L)
            dispatchDataSource.insertDispatch(d1, listOf(dl1))

            // Dispatch 2: 300 pcs
            val d2 = DispatchExecution(
                dispatchExecutionId = "DISP-2",
                projectId = "PRJ-01",
                dispatchNo = "DN-2",
                deliveryOrderId = "DO-MULTI-DISP",
                deliveryChallanId = "CH-2",
                customerId = "CUST-01",
                sourceWarehouseId = "WH-01",
                sourceLocationId = "LOC-01",
                dispatchType = DispatchExecutionType.STANDARD,
                status = DispatchExecutionStatus.DISPATCHED,
                stockOutId = "SO-2",
                dispatchDate = 2000L,
                notes = null,
                createdBy = "user-1",
                createdAt = 2000L,
                updatedAt = 2000L,
                dispatchedAt = 2000L,
                dispatchedBy = "operator"
            )
            val dl2 = DispatchExecutionLine("DL-2", "PRJ-01", "DISP-2", "CL-2", "DOL-MD", "PROD-01", 300.0, 300.0, null, null, "LOC-01", 2000L)
            dispatchDataSource.insertDispatch(d2, listOf(dl2))
        }
    }

    @Test
    fun `settlement aggregates multiple dispatch quantities accurately`() = runBlocking {
        val res = repository.initializeSettlementForDeliveryOrder("DO-MULTI-DISP", "user-1", UserRole.ADMIN)
        assertTrue(res is DomainResult.Success)

        val settlement = (res as DomainResult.Success).data
        assertEquals(1000.0, settlement.totalOrderedQuantity, 0.001)
        assertEquals(800.0, settlement.totalDispatchedQuantity, 0.001) // 500 + 300 = 800
    }
}
