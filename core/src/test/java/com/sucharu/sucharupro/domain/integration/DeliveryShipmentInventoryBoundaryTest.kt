package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeDeliveryShipmentDataSource
import com.sucharu.sucharupro.data.datasource.FakeDispatchExecutionDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryLocationDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryProductDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockOutDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryWarehouseDataSource
import com.sucharu.sucharupro.data.repository.DeliveryShipmentRepositoryImpl
import com.sucharu.sucharupro.data.repository.InventoryStockOutRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionStatus
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionType
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipment
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentAttemptStatus
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentEventType
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationType
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseType
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryStockInRecord
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryShipmentRepository
import com.sucharu.sucharupro.domain.repository.InventoryStockOutRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class DeliveryShipmentInventoryBoundaryTest {

    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var productDataSource: FakeInventoryProductDataSource
    private lateinit var warehouseDataSource: FakeInventoryWarehouseDataSource
    private lateinit var locationDataSource: FakeInventoryLocationDataSource
    private lateinit var dispatchDataSource: FakeDispatchExecutionDataSource
    private lateinit var shipmentDataSource: FakeDeliveryShipmentDataSource

    private lateinit var stockOutRepository: InventoryStockOutRepository
    private lateinit var shipmentRepository: DeliveryShipmentRepository

    @Before
    fun setUp() {
        runBlocking {
            stockOutDataSource = FakeInventoryStockOutDataSource()
            receivingDataSource = FakeInventoryReceivingDataSource()
            productDataSource = FakeInventoryProductDataSource()
            warehouseDataSource = FakeInventoryWarehouseDataSource()
            locationDataSource = FakeInventoryLocationDataSource()
            dispatchDataSource = FakeDispatchExecutionDataSource()
            shipmentDataSource = FakeDeliveryShipmentDataSource()

            stockOutRepository = InventoryStockOutRepositoryImpl(
                stockOutDataSource, receivingDataSource, productDataSource, warehouseDataSource, locationDataSource
            )

            shipmentRepository = DeliveryShipmentRepositoryImpl(
                shipmentDataSource = shipmentDataSource,
                dispatchDataSource = dispatchDataSource
            )

            // Seed inventory
            warehouseDataSource.insertWarehouse(
                InventoryWarehouse("WH-01", "PRJ-01", "WH-1", "Main WH", null, InventoryWarehouseType.FINISHED_GOODS, createdBy = "admin", createdAt = "1000", updatedAt = "1000")
            )
            locationDataSource.insertLocation(
                InventoryLocation(
                    id = "LOC-01",
                    projectId = "PRJ-01",
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
                    id = "PROD-01",
                    sku = "SKU-1",
                    name = "Flyer",
                    createdBy = "admin",
                    createdAt = "1000",
                    updatedAt = "1000"
                )
            )
            receivingDataSource.insertStockInRecord(
                InventoryStockInRecord(UUID.randomUUID().toString(), "REC-1", "RL-1", "PRJ-01", "PROD-01", "WH-01", "LOC-01", 500, InventoryUnit.PCS, "admin", "1000")
            )

            // Dispatched 100 units in Step 03
            val dispatch = DispatchExecution(
                dispatchExecutionId = "DISP-BOUND",
                projectId = "PRJ-01",
                dispatchNo = "DN-BOUND",
                deliveryOrderId = "DO-01",
                deliveryChallanId = "CH-01",
                customerId = null,
                sourceWarehouseId = "WH-01",
                sourceLocationId = "LOC-01",
                dispatchType = DispatchExecutionType.STANDARD,
                status = DispatchExecutionStatus.DISPATCHED,
                stockOutId = "SO-BOUND",
                dispatchDate = 1000L,
                notes = null,
                createdBy = "user-1",
                createdAt = 1000L,
                updatedAt = 1000L,
                dispatchedBy = "operator",
                dispatchedAt = 1000L
            )
            val dLine = DispatchExecutionLine("DL-1", "PRJ-01", "DISP-BOUND", "CL-1", "DOL-1", "PROD-1", 100.0, 100.0, null, null, "LOC-01", 1000L)
            dispatchDataSource.insertDispatch(dispatch, listOf(dLine))
        }
    }

    @Test
    fun `shipment tracking operations perform zero inventory mutations and preserve warehouse balances`() = runBlocking {
        val initialAvailable = stockOutRepository.getAvailableQuantity("PRJ-01", "WH-01", "LOC-01", "PROD-01")
        assertEquals(500, initialAvailable)

        val shipment = DeliveryShipment(
            shipmentId = "S-BOUND",
            projectId = "PRJ-01",
            shipmentNo = "SHP-BOUND",
            deliveryOrderId = "DO-01",
            deliveryChallanId = "CH-01",
            dispatchExecutionId = "DISP-BOUND",
            currentStatus = DeliveryShipmentStatus.DRAFT,
            createdBy = "user-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )

        // 1. Create
        val createRes = shipmentRepository.createShipment(shipment, UserRole.ADMIN)
        assertTrue(createRes is DomainResult.Success)

        // 2. Mark Ready -> Dispatched -> In Transit -> Out For Delivery
        shipmentRepository.markReady("S-BOUND", "operator", UserRole.WAREHOUSE)
        shipmentRepository.markDispatched("S-BOUND", 2000L, "operator", UserRole.WAREHOUSE)
        shipmentRepository.markInTransit("S-BOUND", "Hub 1", null, "operator", UserRole.WAREHOUSE)
        shipmentRepository.markOutForDelivery("S-BOUND", "Van 1", null, "operator", UserRole.WAREHOUSE)

        // 3. Record attempt
        shipmentRepository.recordDeliveryAttempt(
            shipmentId = "S-BOUND",
            status = DeliveryShipmentAttemptStatus.RECIPIENT_UNAVAILABLE,
            reason = "Customer unavailable",
            notes = null,
            attemptedAt = 3000L,
            actorId = "courier",
            callerRole = UserRole.WAREHOUSE
        )

        // 4. Mark Delivered
        shipmentRepository.markDelivered("S-BOUND", 5000L, null, "operator", UserRole.WAREHOUSE)

        // PROVE: Zero StockOut records created by shipment tracking
        val stockOutRecords = stockOutDataSource.observeStockOuts().first()
        assertEquals(0, stockOutRecords.size)

        // PROVE: Warehouse stock balance remains exactly 500
        val finalAvailable = stockOutRepository.getAvailableQuantity("PRJ-01", "WH-01", "LOC-01", "PROD-01")
        assertEquals(500, finalAvailable)
    }
}
