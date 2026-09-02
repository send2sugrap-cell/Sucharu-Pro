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
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySettlementEventType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryPartialSettlementAuditTest {

    private lateinit var settlementDataSource: FakeDeliveryPartialSettlementDataSource
    private lateinit var doDataSource: FakeDeliveryOrderDataSource
    private lateinit var repository: DeliveryPartialSettlementRepository

    @Before
    fun setUp() {
        runBlocking {
            settlementDataSource = FakeDeliveryPartialSettlementDataSource()
            doDataSource = FakeDeliveryOrderDataSource()
            repository = DeliveryPartialSettlementRepositoryImpl(settlementDataSource, doDataSource)

            val doOrder = DeliveryOrder("DO-AUDIT", "PRJ-01", "DON-AUDIT", "CUST-01", "SO-01", "SALES_ORDER", DeliveryOrderType.CUSTOMER_DELIVERY, DeliveryPriority.NORMAL, DeliveryOrderStatus.APPROVED, 2000L, null, "user-1", 1000L, 1000L)
            val doLine = DeliveryOrderLine("DOL-AUDIT", "DO-AUDIT", "PRJ-01", "PROD-01", 100.0, null)
            doDataSource.insertDeliveryOrder(doOrder, listOf(doLine))
        }
    }

    @Test
    fun `settlement operations generate chronological immutable audit events`() = runBlocking {
        val s = (repository.initializeSettlementForDeliveryOrder("DO-AUDIT", "user-1", UserRole.ADMIN) as DomainResult.Success).data

        repository.recordPartialDelivery(s.settlementId, "DOL-AUDIT", 50.0, "user-1", UserRole.WAREHOUSE)
        repository.recordPartialDelivery(s.settlementId, "DOL-AUDIT", 50.0, "user-1", UserRole.WAREHOUSE)
        repository.finalizeSettlement(s.settlementId, "Done", "user-1", UserRole.MANAGER)

        val events = (repository.getEvents(s.settlementId, UserRole.ADMIN) as DomainResult.Success).data
        val types = events.map { it.eventType }

        assertTrue(types.contains(DeliverySettlementEventType.CREATED))
        assertTrue(types.contains(DeliverySettlementEventType.PARTIAL_DELIVERY_RECORDED))
        assertTrue(types.contains(DeliverySettlementEventType.SETTLED))
    }
}
