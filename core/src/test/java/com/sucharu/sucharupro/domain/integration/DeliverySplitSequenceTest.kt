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
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySplitDispatchLine
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryPartialSettlementRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliverySplitSequenceTest {

    private lateinit var settlementDataSource: FakeDeliveryPartialSettlementDataSource
    private lateinit var doDataSource: FakeDeliveryOrderDataSource
    private lateinit var repository: DeliveryPartialSettlementRepository

    @Before
    fun setUp() {
        runBlocking {
            settlementDataSource = FakeDeliveryPartialSettlementDataSource()
            doDataSource = FakeDeliveryOrderDataSource()
            repository = DeliveryPartialSettlementRepositoryImpl(settlementDataSource, doDataSource)

            val doOrder = DeliveryOrder("DO-SEQ", "PRJ-01", "DON-SEQ", "CUST-01", "SO-01", "SALES_ORDER", DeliveryOrderType.CUSTOMER_DELIVERY, DeliveryPriority.NORMAL, DeliveryOrderStatus.APPROVED, 2000L, null, "user-1", 1000L, 1000L)
            val doLine = DeliveryOrderLine("DOL-SEQ", "DO-SEQ", "PRJ-01", "PROD-01", 1000.0, null)
            doDataSource.insertDeliveryOrder(doOrder, listOf(doLine))
        }
    }

    @Test
    fun `sequential split dispatches automatically assign incrementing split sequence numbers`() = runBlocking {
        // Split 1
        val line1 = DeliverySplitDispatchLine("SDL-1", "PRJ-01", "", "DOL-SEQ", "PROD-01", 400.0, createdAt = 1000L)
        val s1 = repository.createSplitDispatch("DO-SEQ", listOf(line1), actorId = "user-1", callerRole = UserRole.WAREHOUSE)
        assertTrue(s1 is DomainResult.Success)
        assertEquals(1, (s1 as DomainResult.Success).data.splitSequence)

        // Split 2
        val line2 = DeliverySplitDispatchLine("SDL-2", "PRJ-01", "", "DOL-SEQ", "PROD-01", 300.0, createdAt = 2000L)
        val s2 = repository.createSplitDispatch("DO-SEQ", listOf(line2), actorId = "user-1", callerRole = UserRole.WAREHOUSE)
        assertTrue(s2 is DomainResult.Success)
        assertEquals(2, (s2 as DomainResult.Success).data.splitSequence)

        val allSplits = repository.getSplitDispatches("DO-SEQ", UserRole.ADMIN)
        assertTrue(allSplits is DomainResult.Success)
        assertEquals(2, (allSplits as DomainResult.Success).data.size)
    }
}
