package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryPartialSettlementDataSource
import com.sucharu.sucharupro.data.repository.DeliveryPartialSettlementRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryPartialSettlementRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryPartialSettlementProjectIsolationTest {

    private lateinit var settlementDataSource: FakeDeliveryPartialSettlementDataSource
    private lateinit var doDataSource: FakeDeliveryOrderDataSource
    private lateinit var repository: DeliveryPartialSettlementRepository

    @Before
    fun setUp() {
        runBlocking {
            settlementDataSource = FakeDeliveryPartialSettlementDataSource()
            doDataSource = FakeDeliveryOrderDataSource()
            repository = DeliveryPartialSettlementRepositoryImpl(settlementDataSource, doDataSource)

            val doA = DeliveryOrder("DO-A", "PRJ-A", "DON-A", "CUST-A", "SO-A", "SALES_ORDER", DeliveryOrderType.CUSTOMER_DELIVERY, DeliveryPriority.NORMAL, DeliveryOrderStatus.APPROVED, 2000L, null, "user-1", 1000L, 1000L)
            val dlA = DeliveryOrderLine("DLA", "DO-A", "PRJ-A", "PROD-A", 100.0, null)
            doDataSource.insertDeliveryOrder(doA, listOf(dlA))

            val doB = DeliveryOrder("DO-B", "PRJ-B", "DON-B", "CUST-B", "SO-B", "SALES_ORDER", DeliveryOrderType.CUSTOMER_DELIVERY, DeliveryPriority.NORMAL, DeliveryOrderStatus.APPROVED, 2000L, null, "user-2", 1000L, 1000L)
            val dlB = DeliveryOrderLine("DLB", "DO-B", "PRJ-B", "PROD-B", 200.0, null)
            doDataSource.insertDeliveryOrder(doB, listOf(dlB))
        }
    }

    @Test
    fun `observeSettlements strictly filters by project`() = runBlocking {
        repository.initializeSettlementForDeliveryOrder("DO-A", "user-1", UserRole.ADMIN, "PRJ-A")
        repository.initializeSettlementForDeliveryOrder("DO-B", "user-2", UserRole.ADMIN, "PRJ-B")

        val listA = repository.observeSettlements("PRJ-A").first()
        val listB = repository.observeSettlements("PRJ-B").first()

        assertEquals(1, listA.size)
        assertEquals("DO-A", listA[0].deliveryOrderId)

        assertEquals(1, listB.size)
        assertEquals("DO-B", listB[0].deliveryOrderId)
    }

    @Test
    fun `cross project getSettlement is denied`() = runBlocking {
        val sA = (repository.initializeSettlementForDeliveryOrder("DO-A", "user-1", UserRole.ADMIN, "PRJ-A") as DomainResult.Success).data

        val result = repository.getSettlement(sA.settlementId, UserRole.ADMIN, "PRJ-B")
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Access denied"))
    }
}
