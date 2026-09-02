package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryPartialSettlementDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryLocationDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryProductDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockOutDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryWarehouseDataSource
import com.sucharu.sucharupro.data.repository.DeliveryPartialSettlementRepositoryImpl
import com.sucharu.sucharupro.data.repository.InventoryStockOutRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySplitDispatchLine
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationType
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseType
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryStockInRecord
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryPartialSettlementRepository
import com.sucharu.sucharupro.domain.repository.InventoryStockOutRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class DeliveryPartialSettlementInventoryBoundaryTest {

    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var productDataSource: FakeInventoryProductDataSource
    private lateinit var warehouseDataSource: FakeInventoryWarehouseDataSource
    private lateinit var locationDataSource: FakeInventoryLocationDataSource
    private lateinit var doDataSource: FakeDeliveryOrderDataSource
    private lateinit var settlementDataSource: FakeDeliveryPartialSettlementDataSource

    private lateinit var stockOutRepository: InventoryStockOutRepository
    private lateinit var settlementRepository: DeliveryPartialSettlementRepository

    @Before
    fun setUp() {
        runBlocking {
            stockOutDataSource = FakeInventoryStockOutDataSource()
            receivingDataSource = FakeInventoryReceivingDataSource()
            productDataSource = FakeInventoryProductDataSource()
            warehouseDataSource = FakeInventoryWarehouseDataSource()
            locationDataSource = FakeInventoryLocationDataSource()
            doDataSource = FakeDeliveryOrderDataSource()
            settlementDataSource = FakeDeliveryPartialSettlementDataSource()

            stockOutRepository = InventoryStockOutRepositoryImpl(
                stockOutDataSource, receivingDataSource, productDataSource, warehouseDataSource, locationDataSource
            )

            settlementRepository = DeliveryPartialSettlementRepositoryImpl(
                settlementDataSource = settlementDataSource,
                doDataSource = doDataSource
            )

            // Seed inventory
            warehouseDataSource.insertWarehouse(
                InventoryWarehouse("WH-01", "PRJ-BOUND", "WH-1", "Main WH", null, InventoryWarehouseType.FINISHED_GOODS, createdBy = "admin", createdAt = "1000", updatedAt = "1000")
            )
            locationDataSource.insertLocation(
                InventoryLocation(
                    id = "LOC-01",
                    projectId = "PRJ-BOUND",
                    warehouseId = "WH-01",
                    code = "LOC-1",
                    name = "Bay 1",
                    type = InventoryLocationType.SHELF,
                    createdBy = "admin",
                    createdAt = "1000",
                    updatedAt = "1000"
                )
            )
            productDataSource.insertProduct(
                InventoryProduct(
                    id = "PROD-BOUND",
                    sku = "SKU-1",
                    name = "Flyer",
                    createdBy = "admin",
                    createdAt = "1000",
                    updatedAt = "1000"
                )
            )
            receivingDataSource.insertStockInRecord(
                InventoryStockInRecord(UUID.randomUUID().toString(), "REC-1", "RL-1", "PRJ-BOUND", "PROD-BOUND", "WH-01", "LOC-01", 500, InventoryUnit.PCS, "admin", "1000")
            )

            val doOrder = DeliveryOrder("DO-BOUND", "PRJ-BOUND", "DON-BOUND", "CUST-01", "SO-01", "SALES_ORDER", DeliveryOrderType.CUSTOMER_DELIVERY, DeliveryPriority.NORMAL, DeliveryOrderStatus.APPROVED, 2000L, null, "user-1", 1000L, 1000L)
            val doLine = DeliveryOrderLine("DOL-BOUND", "DO-BOUND", "PRJ-BOUND", "PROD-BOUND", 500.0, null)
            doDataSource.insertDeliveryOrder(doOrder, listOf(doLine))
        }
    }

    @Test
    fun `settlement operations perform zero direct inventory mutations and preserve warehouse balances`() = runBlocking {
        val initialAvailable = stockOutRepository.getAvailableQuantity("PRJ-BOUND", "WH-01", "LOC-01", "PROD-BOUND")
        assertEquals(500, initialAvailable)

        // 1. Initialize settlement
        val initRes = settlementRepository.initializeSettlementForDeliveryOrder("DO-BOUND", "user-1", UserRole.ADMIN)
        assertTrue(initRes is DomainResult.Success)
        val sId = (initRes as DomainResult.Success).data.settlementId

        // 2. Create Split
        val sLine = DeliverySplitDispatchLine("SDL-1", "PRJ-BOUND", "", "DOL-BOUND", "PROD-BOUND", 200.0, createdAt = 1000L)
        settlementRepository.createSplitDispatch("DO-BOUND", listOf(sLine), actorId = "user-1", callerRole = UserRole.WAREHOUSE)

        // 3. Record Partial Delivery
        settlementRepository.recordPartialDelivery(sId, "DOL-BOUND", 200.0, "user-1", UserRole.WAREHOUSE)

        // 4. Finalize
        settlementRepository.finalizeSettlement(sId, "Completed", "mgr", UserRole.MANAGER)

        // PROVE: Zero StockOut records created by settlement
        val stockOutRecords = stockOutDataSource.observeStockOuts().first()
        assertEquals(0, stockOutRecords.size)

        // PROVE: Warehouse stock balance remains exactly 500
        val finalAvailable = stockOutRepository.getAvailableQuantity("PRJ-BOUND", "WH-01", "LOC-01", "PROD-BOUND")
        assertEquals(500, finalAvailable)
    }
}
