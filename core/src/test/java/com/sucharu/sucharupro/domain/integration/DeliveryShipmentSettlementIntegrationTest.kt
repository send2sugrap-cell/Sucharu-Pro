package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryPartialSettlementDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryShipmentDataSource
import com.sucharu.sucharupro.data.repository.DeliveryPartialSettlementRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipment
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryPartialSettlementRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryShipmentSettlementIntegrationTest {

    private lateinit var settlementDataSource: FakeDeliveryPartialSettlementDataSource
    private lateinit var doDataSource: FakeDeliveryOrderDataSource
    private lateinit var shipmentDataSource: FakeDeliveryShipmentDataSource
    private lateinit var repository: DeliveryPartialSettlementRepository

    @Before
    fun setUp() {
        runBlocking {
            settlementDataSource = FakeDeliveryPartialSettlementDataSource()
            doDataSource = FakeDeliveryOrderDataSource()
            shipmentDataSource = FakeDeliveryShipmentDataSource()

            repository = DeliveryPartialSettlementRepositoryImpl(
                settlementDataSource = settlementDataSource,
                doDataSource = doDataSource,
                shipmentDataSource = shipmentDataSource
            )

            val doOrder = DeliveryOrder("DO-SHP-INT", "PRJ-01", "DON-SI", "CUST-01", "SO-01", "SALES_ORDER", DeliveryOrderType.CUSTOMER_DELIVERY, DeliveryPriority.NORMAL, DeliveryOrderStatus.APPROVED, 2000L, null, "user-1", 1000L, 1000L)
            val doLine = DeliveryOrderLine("DOL-SI", "DO-SHP-INT", "PRJ-01", "PROD-01", 1000.0, null)
            doDataSource.insertDeliveryOrder(doOrder, listOf(doLine))

            val shipment = DeliveryShipment("SHP-1", "PRJ-01", "SN-1", "DO-SHP-INT", "CH-1", "DISP-1", currentStatus = DeliveryShipmentStatus.DELIVERED, createdBy = "user-1", createdAt = 1000L, updatedAt = 1000L)
            shipmentDataSource.insertShipment(shipment)
        }
    }

    @Test
    fun `split dispatch references delivery shipment ID successfully`() = runBlocking {
        val splitLine = com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySplitDispatchLine("SDL-1", "PRJ-01", "", "DOL-SI", "PROD-01", 500.0, createdAt = 1000L)
        val res = repository.createSplitDispatch(
            deliveryOrderId = "DO-SHP-INT",
            lines = listOf(splitLine),
            shipmentId = "SHP-1",
            actorId = "user-1",
            callerRole = UserRole.WAREHOUSE
        )
        assertTrue(res is DomainResult.Success)
        assertEquals("SHP-1", (res as DomainResult.Success).data.shipmentId)
    }
}
