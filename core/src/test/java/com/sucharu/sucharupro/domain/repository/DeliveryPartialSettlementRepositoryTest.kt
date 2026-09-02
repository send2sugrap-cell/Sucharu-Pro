package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryPartialSettlementDataSource
import com.sucharu.sucharupro.data.repository.DeliveryPartialSettlementRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySettlementStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryPartialSettlementRepositoryTest {

    private lateinit var settlementDataSource: FakeDeliveryPartialSettlementDataSource
    private lateinit var doDataSource: FakeDeliveryOrderDataSource
    private lateinit var repository: DeliveryPartialSettlementRepository

    @Before
    fun setUp() {
        runBlocking {
            settlementDataSource = FakeDeliveryPartialSettlementDataSource()
            doDataSource = FakeDeliveryOrderDataSource()
            repository = DeliveryPartialSettlementRepositoryImpl(settlementDataSource, doDataSource)

            val doOrder = DeliveryOrder("DO-REPO", "PRJ-01", "DON-REPO", "CUST-01", "SO-01", "SALES_ORDER", DeliveryOrderType.CUSTOMER_DELIVERY, DeliveryPriority.NORMAL, DeliveryOrderStatus.APPROVED, 2000L, null, "user-1", 1000L, 1000L)
            val doLine = DeliveryOrderLine("DOL-REPO", "DO-REPO", "PRJ-01", "PROD-01", 500.0, null)
            doDataSource.insertDeliveryOrder(doOrder, listOf(doLine))
        }
    }

    @Test
    fun `full lifecycle from initialization to finalization works`() = runBlocking {
        // 1. Initialize
        val initRes = repository.initializeSettlementForDeliveryOrder("DO-REPO", "user-1", UserRole.ADMIN)
        assertTrue(initRes is DomainResult.Success)
        val sId = (initRes as DomainResult.Success).data.settlementId

        // 2. Record Partial Delivery (300 pcs)
        val partRes = repository.recordPartialDelivery(sId, "DOL-REPO", 300.0, "user-1", UserRole.WAREHOUSE)
        assertTrue(partRes is DomainResult.Success)
        val sAfterPart = (partRes as DomainResult.Success).data
        assertEquals(300.0, sAfterPart.totalDeliveredQuantity, 0.001)
        assertEquals(200.0, sAfterPart.totalPendingQuantity, 0.001)
        assertEquals(DeliverySettlementStatus.PARTIALLY_DELIVERED, sAfterPart.status)

        // 3. Record Remaining Delivery (200 pcs)
        val fullRes = repository.recordPartialDelivery(sId, "DOL-REPO", 200.0, "user-1", UserRole.WAREHOUSE)
        assertTrue(fullRes is DomainResult.Success)
        val sAfterFull = (fullRes as DomainResult.Success).data
        assertEquals(500.0, sAfterFull.totalDeliveredQuantity, 0.001)
        assertEquals(0.0, sAfterFull.totalPendingQuantity, 0.001)
        assertEquals(DeliverySettlementStatus.FULLY_DELIVERED, sAfterFull.status)

        // 4. Finalize
        val finalRes = repository.finalizeSettlement(sId, "All copies received", "mgr", UserRole.MANAGER)
        assertTrue(finalRes is DomainResult.Success)
        val sFinal = (finalRes as DomainResult.Success).data
        assertEquals(DeliverySettlementStatus.SETTLED, sFinal.status)
    }
}
