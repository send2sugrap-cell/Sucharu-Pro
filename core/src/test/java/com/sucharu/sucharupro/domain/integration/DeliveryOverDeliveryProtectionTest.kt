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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryOverDeliveryProtectionTest {

    private lateinit var settlementDataSource: FakeDeliveryPartialSettlementDataSource
    private lateinit var doDataSource: FakeDeliveryOrderDataSource
    private lateinit var repository: DeliveryPartialSettlementRepository

    @Before
    fun setUp() {
        runBlocking {
            settlementDataSource = FakeDeliveryPartialSettlementDataSource()
            doDataSource = FakeDeliveryOrderDataSource()
            repository = DeliveryPartialSettlementRepositoryImpl(settlementDataSource, doDataSource)

            val doOrder = DeliveryOrder("DO-OVER", "PRJ-01", "DON-OVER", "CUST-01", "SO-01", "SALES_ORDER", DeliveryOrderType.CUSTOMER_DELIVERY, DeliveryPriority.NORMAL, DeliveryOrderStatus.APPROVED, 2000L, null, "user-1", 1000L, 1000L)
            val doLine = DeliveryOrderLine("DOL-OVER", "DO-OVER", "PRJ-01", "PROD-01", 100.0, null)
            doDataSource.insertDeliveryOrder(doOrder, listOf(doLine))
        }
    }

    @Test
    fun `delivering more than authorized ordered quantity is strictly rejected`() = runBlocking {
        val initRes = repository.initializeSettlementForDeliveryOrder("DO-OVER", "user-1", UserRole.ADMIN)
        assertTrue(initRes is DomainResult.Success)
        val settlement = (initRes as DomainResult.Success).data

        // Record 60 pcs -> Success
        val p1 = repository.recordPartialDelivery(settlement.settlementId, "DOL-OVER", 60.0, "user-1", UserRole.WAREHOUSE)
        assertTrue(p1 is DomainResult.Success)

        // Record 50 pcs more (60 + 50 = 110 > 100) -> Must Error
        val p2 = repository.recordPartialDelivery(settlement.settlementId, "DOL-OVER", 50.0, "user-1", UserRole.WAREHOUSE)
        assertTrue(p2 is DomainResult.Error)
        assertTrue((p2 as DomainResult.Error).message.contains("Over-delivery rejected"))
    }
}
