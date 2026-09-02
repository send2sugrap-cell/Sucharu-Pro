package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeDeliveryChallanDataSource
import com.sucharu.sucharupro.data.datasource.FakeDispatchExecutionDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryLocationDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryProductDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockOutDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryWarehouseDataSource
import com.sucharu.sucharupro.data.repository.DispatchExecutionRepositoryImpl
import com.sucharu.sucharupro.data.repository.InventoryStockOutRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallan
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanLine
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanStatus
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanType
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionStatus
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionType
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationType
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseType
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryStockInRecord
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DispatchExecutionRepository
import com.sucharu.sucharupro.domain.repository.InventoryStockOutRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class DispatchExecutionInventoryIntegrationTest {

    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var productDataSource: FakeInventoryProductDataSource
    private lateinit var warehouseDataSource: FakeInventoryWarehouseDataSource
    private lateinit var locationDataSource: FakeInventoryLocationDataSource
    private lateinit var challanDataSource: FakeDeliveryChallanDataSource
    private lateinit var dispatchDataSource: FakeDispatchExecutionDataSource
    private lateinit var stockOutRepository: InventoryStockOutRepository
    private lateinit var dispatchRepository: DispatchExecutionRepository

    @Before
    fun setUp() {
        runBlocking {
            stockOutDataSource = FakeInventoryStockOutDataSource()
            receivingDataSource = FakeInventoryReceivingDataSource()
            productDataSource = FakeInventoryProductDataSource()
            warehouseDataSource = FakeInventoryWarehouseDataSource()
            locationDataSource = FakeInventoryLocationDataSource()
            challanDataSource = FakeDeliveryChallanDataSource()
            dispatchDataSource = FakeDispatchExecutionDataSource()

            stockOutRepository = InventoryStockOutRepositoryImpl(
                stockOutDataSource, receivingDataSource, productDataSource, warehouseDataSource, locationDataSource
            )

            dispatchRepository = DispatchExecutionRepositoryImpl(
                dispatchDataSource, challanDataSource, stockOutRepository
            )

            warehouseDataSource.insertWarehouse(
                InventoryWarehouse(
                    id = "WH-01",
                    projectId = "PRJ-01",
                    code = "WH-1",
                    name = "Main WH",
                    type = InventoryWarehouseType.FINISHED_GOODS,
                    createdBy = "admin",
                    createdAt = "1000",
                    updatedAt = "1000"
                )
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
                    name = "Poster",
                    isActive = true,
                    isStockTracked = true,
                    createdBy = "admin",
                    createdAt = "1000",
                    updatedAt = "1000"
                )
            )

            // Seed initial 50 units
            receivingDataSource.insertStockInRecord(
                InventoryStockInRecord(
                    stockInId = UUID.randomUUID().toString(),
                    receivingId = "REC-1",
                    receivingLineId = "RL-1",
                    projectId = "PRJ-01",
                    inventoryProductId = "PROD-01",
                    warehouseId = "WH-01",
                    locationId = "LOC-01",
                    quantity = 50,
                    unit = InventoryUnit.PCS,
                    createdBy = "admin",
                    createdAt = "1000"
                )
            )

            val challan = DeliveryChallan("CH-01", "PRJ-01", "CH-01", "DO-01", null, null, null, DeliveryChallanType.STANDARD, DeliveryChallanStatus.APPROVED, 1000L, null, "user-1", 1000L, 1000L)
            val challanLine = DeliveryChallanLine("CL-01", "CH-01", "PRJ-01", "DOL-01", "PROD-01", 100.0)
            challanDataSource.insertChallan(challan, listOf(challanLine))
        }
    }

    @Test
    fun `insufficient stock prevents dispatch execution and creates zero stock out records`() = runBlocking {
        // Attempting to dispatch 60 units when only 50 in stock
        val dispatch = DispatchExecution("DISP-OVER", "PRJ-01", "DN-OVER", "DO-01", "CH-01", null, "WH-01", "LOC-01", DispatchExecutionType.STANDARD, DispatchExecutionStatus.READY_FOR_EXECUTION, null, 1000L, null, "user-1", 1000L, 1000L)
        val line = DispatchExecutionLine("DL-OVER", "PRJ-01", "DISP-OVER", "CL-01", "DOL-01", "PROD-01", 100.0, 60.0, null, null, "LOC-01", 1000L)
        dispatchDataSource.insertDispatch(dispatch, listOf(line))

        val result = dispatchRepository.executeDispatch("DISP-OVER", "operator", UserRole.WAREHOUSE)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Insufficient stock"))

        // Verify zero stock-out records created
        val stockOutRecords = stockOutDataSource.observeStockOuts().first()
        assertEquals(0, stockOutRecords.size)

        // Verify Challan remains APPROVED (not DISPATCHED)
        val challan = challanDataSource.getChallan("CH-01")
        assertEquals(DeliveryChallanStatus.APPROVED, challan?.status)
    }

    @Test
    fun `sufficient stock executes dispatch and deducts inventory through Module 07 StockOut`() = runBlocking {
        // Dispatch 30 units (out of 50 available)
        val dispatch = DispatchExecution("DISP-OK", "PRJ-01", "DN-OK", "DO-01", "CH-01", null, "WH-01", "LOC-01", DispatchExecutionType.STANDARD, DispatchExecutionStatus.READY_FOR_EXECUTION, null, 1000L, null, "user-1", 1000L, 1000L)
        val line = DispatchExecutionLine("DL-OK", "PRJ-01", "DISP-OK", "CL-01", "DOL-01", "PROD-01", 100.0, 30.0, null, null, "LOC-01", 1000L)
        dispatchDataSource.insertDispatch(dispatch, listOf(line))

        val result = dispatchRepository.executeDispatch("DISP-OK", "operator", UserRole.WAREHOUSE)
        assertTrue(result is DomainResult.Success)

        // Verify 30 units deducted -> remaining 20 units
        val available = stockOutRepository.getAvailableQuantity("PRJ-01", "WH-01", "LOC-01", "PROD-01")
        assertEquals(20, available)

        // Verify Challan status updated to DISPATCHED
        val challan = challanDataSource.getChallan("CH-01")
        assertEquals(DeliveryChallanStatus.DISPATCHED, challan?.status)
    }
}
