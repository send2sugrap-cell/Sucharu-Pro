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
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryPartialSettlementConcurrencyTest {

    private lateinit var settlementDataSource: FakeDeliveryPartialSettlementDataSource
    private lateinit var doDataSource: FakeDeliveryOrderDataSource
    private lateinit var repository: DeliveryPartialSettlementRepository

    @Before
    fun setUp() {
        runBlocking {
            settlementDataSource = FakeDeliveryPartialSettlementDataSource()
            doDataSource = FakeDeliveryOrderDataSource()
            repository = DeliveryPartialSettlementRepositoryImpl(settlementDataSource, doDataSource)

            val doOrder = DeliveryOrder("DO-CONCUR", "PRJ-01", "DON-CONCUR", "CUST-01", "SO-01", "SALES_ORDER", DeliveryOrderType.CUSTOMER_DELIVERY, DeliveryPriority.NORMAL, DeliveryOrderStatus.APPROVED, 2000L, null, "user-1", 1000L, 1000L)
            val doLine = DeliveryOrderLine("DOL-CONCUR", "DO-CONCUR", "PRJ-01", "PROD-01", 1000.0, null)
            doDataSource.insertDeliveryOrder(doOrder, listOf(doLine))
            repository.initializeSettlementForDeliveryOrder("DO-CONCUR", "user-1", UserRole.ADMIN)
        }
    }

    @Test
    fun `concurrent partial deliveries under mutex preserve atomic consistency`() = runBlocking {
        val settlement = (repository.getSettlementByDeliveryOrder("DO-CONCUR", UserRole.ADMIN) as DomainResult.Success).data

        val jobs = listOf(
            async(Dispatchers.IO) {
                repository.recordPartialDelivery(settlement.settlementId, "DOL-CONCUR", 200.0, "user-1", UserRole.WAREHOUSE)
            },
            async(Dispatchers.IO) {
                repository.recordPartialDelivery(settlement.settlementId, "DOL-CONCUR", 300.0, "user-2", UserRole.WAREHOUSE)
            }
        )

        val results = jobs.awaitAll()
        assertTrue(results.all { it is DomainResult.Success })

        val finalSettlement = (repository.getSettlement(settlement.settlementId, UserRole.ADMIN) as DomainResult.Success).data
        assertEquals(500.0, finalSettlement.totalDeliveredQuantity, 0.001)
        assertEquals(500.0, finalSettlement.totalPendingQuantity, 0.001)
    }
}
