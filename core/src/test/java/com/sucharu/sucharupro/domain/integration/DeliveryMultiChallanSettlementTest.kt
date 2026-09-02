package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeDeliveryChallanDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryPartialSettlementDataSource
import com.sucharu.sucharupro.data.repository.DeliveryPartialSettlementRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallan
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanLine
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanStatus
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryPartialSettlementRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryMultiChallanSettlementTest {

    private lateinit var settlementDataSource: FakeDeliveryPartialSettlementDataSource
    private lateinit var doDataSource: FakeDeliveryOrderDataSource
    private lateinit var challanDataSource: FakeDeliveryChallanDataSource
    private lateinit var repository: DeliveryPartialSettlementRepository

    @Before
    fun setUp() {
        runBlocking {
            settlementDataSource = FakeDeliveryPartialSettlementDataSource()
            doDataSource = FakeDeliveryOrderDataSource()
            challanDataSource = FakeDeliveryChallanDataSource()

            repository = DeliveryPartialSettlementRepositoryImpl(
                settlementDataSource = settlementDataSource,
                doDataSource = doDataSource,
                challanDataSource = challanDataSource
            )

            // Order 1000 pcs
            val doOrder = DeliveryOrder("DO-MULTI-CH", "PRJ-01", "DON-MC", "CUST-01", "SO-01", "SALES_ORDER", DeliveryOrderType.CUSTOMER_DELIVERY, DeliveryPriority.NORMAL, DeliveryOrderStatus.APPROVED, 2000L, null, "user-1", 1000L, 1000L)
            val doLine = DeliveryOrderLine("DOL-MC", "DO-MULTI-CH", "PRJ-01", "PROD-01", 1000.0, null)
            doDataSource.insertDeliveryOrder(doOrder, listOf(doLine))

            // Challan 1: 400 pcs
            val ch1 = DeliveryChallan("CH-1", "PRJ-01", "CHN-1", "DO-MULTI-CH", "CUST-01", "SO-01", "SALES_ORDER", DeliveryChallanType.STANDARD, DeliveryChallanStatus.APPROVED, 2000L, null, "user-1", 1000L, 1000L)
            val cl1 = DeliveryChallanLine("CL-1", "CH-1", "PRJ-01", "DOL-MC", "PROD-01", 400.0)
            challanDataSource.insertChallan(ch1, listOf(cl1))

            // Challan 2: 350 pcs
            val ch2 = DeliveryChallan("CH-2", "PRJ-01", "CHN-2", "DO-MULTI-CH", "CUST-01", "SO-01", "SALES_ORDER", DeliveryChallanType.STANDARD, DeliveryChallanStatus.APPROVED, 3000L, null, "user-1", 2000L, 2000L)
            val cl2 = DeliveryChallanLine("CL-2", "CH-2", "PRJ-01", "DOL-MC", "PROD-01", 350.0)
            challanDataSource.insertChallan(ch2, listOf(cl2))
        }
    }

    @Test
    fun `settlement aggregates multiple challan allocation quantities accurately`() = runBlocking {
        val res = repository.initializeSettlementForDeliveryOrder("DO-MULTI-CH", "user-1", UserRole.ADMIN)
        assertTrue(res is DomainResult.Success)

        val settlement = (res as DomainResult.Success).data
        assertEquals(1000.0, settlement.totalOrderedQuantity, 0.001)
        assertEquals(750.0, settlement.totalAllocatedQuantity, 0.001) // 400 + 350 = 750
    }
}
