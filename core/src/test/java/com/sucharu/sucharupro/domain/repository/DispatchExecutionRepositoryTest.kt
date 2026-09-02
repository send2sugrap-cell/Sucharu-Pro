package com.sucharu.sucharupro.domain.repository

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class DispatchExecutionRepositoryTest {

    private lateinit var dispatchDataSource: FakeDispatchExecutionDataSource
    private lateinit var challanDataSource: FakeDeliveryChallanDataSource
    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var productDataSource: FakeInventoryProductDataSource
    private lateinit var warehouseDataSource: FakeInventoryWarehouseDataSource
    private lateinit var locationDataSource: FakeInventoryLocationDataSource
    private lateinit var stockOutRepository: InventoryStockOutRepository
    private lateinit var dispatchRepository: DispatchExecutionRepository

    @Before
    fun setUp() {
        runBlocking {
            dispatchDataSource = FakeDispatchExecutionDataSource()
            challanDataSource = FakeDeliveryChallanDataSource()
            stockOutDataSource = FakeInventoryStockOutDataSource()
            receivingDataSource = FakeInventoryReceivingDataSource()
            productDataSource = FakeInventoryProductDataSource()
            warehouseDataSource = FakeInventoryWarehouseDataSource()
            locationDataSource = FakeInventoryLocationDataSource()

            stockOutRepository = InventoryStockOutRepositoryImpl(
                stockOutDataSource = stockOutDataSource,
                receivingDataSource = receivingDataSource,
                productDataSource = productDataSource,
                warehouseDataSource = warehouseDataSource,
                locationDataSource = locationDataSource
            )

            dispatchRepository = DispatchExecutionRepositoryImpl(
                dispatchDataSource = dispatchDataSource,
                challanDataSource = challanDataSource,
                stockOutRepository = stockOutRepository
            )

            // Seed Warehouse, Location, Product, and Stock In (500 units)
            val warehouse = InventoryWarehouse(
                id = "WH-01",
                projectId = "PRJ-01",
                code = "WH01",
                name = "Main Warehouse",
                type = InventoryWarehouseType.FINISHED_GOODS,
                createdBy = "admin",
                createdAt = "1000",
                updatedAt = "1000"
            )
            warehouseDataSource.insertWarehouse(warehouse)

            val location = InventoryLocation(
                id = "LOC-01",
                projectId = "PRJ-01",
                warehouseId = "WH-01",
                code = "LOC01",
                name = "Dispatch Bay",
                type = InventoryLocationType.SHELF,
                createdBy = "admin",
                createdAt = "1000",
                updatedAt = "1000"
            )
            locationDataSource.insertLocation(location)

            val product = InventoryProduct(
                id = "PROD-01",
                sku = "SKU-01",
                name = "Color Brochure",
                isActive = true,
                isStockTracked = true,
                createdBy = "admin",
                createdAt = "1000",
                updatedAt = "1000"
            )
            productDataSource.insertProduct(product)

            // 500 units in stock at WH-01, LOC-01
            val stockIn = InventoryStockInRecord(
                stockInId = UUID.randomUUID().toString(),
                receivingId = "REC-01",
                receivingLineId = "RECLINE-01",
                projectId = "PRJ-01",
                inventoryProductId = "PROD-01",
                warehouseId = "WH-01",
                locationId = "LOC-01",
                quantity = 500,
                unit = InventoryUnit.PCS,
                createdBy = "admin",
                createdAt = "1000"
            )
            receivingDataSource.insertStockInRecord(stockIn)

            // Seed Approved Delivery Challan
            val challan = DeliveryChallan(
                challanId = "CH-01",
                projectId = "PRJ-01",
                challanNo = "CHAL-001",
                deliveryOrderId = "DO-01",
                customerId = "CUST-01",
                sourceReferenceId = null,
                sourceReferenceType = null,
                challanType = DeliveryChallanType.STANDARD,
                status = DeliveryChallanStatus.APPROVED,
                issueDate = 1000L,
                notes = null,
                createdBy = "user-1",
                createdAt = 1000L,
                updatedAt = 1000L
            )
            val challanLine = DeliveryChallanLine(
                lineId = "CLINE-01",
                challanId = "CH-01",
                projectId = "PRJ-01",
                deliveryOrderLineId = "DOLINE-01",
                productId = "PROD-01",
                quantity = 100.0
            )
            challanDataSource.insertChallan(challan, listOf(challanLine))
        }
    }

    private fun sampleDispatch(
        dispatchId: String = "DISP-01",
        dispatchNo: String = "DN-001"
    ): DispatchExecution {
        return DispatchExecution(
            dispatchExecutionId = dispatchId,
            projectId = "PRJ-01",
            dispatchNo = dispatchNo,
            deliveryOrderId = "DO-01",
            deliveryChallanId = "CH-01",
            customerId = "CUST-01",
            sourceWarehouseId = "WH-01",
            sourceLocationId = "LOC-01",
            dispatchType = DispatchExecutionType.STANDARD,
            status = DispatchExecutionStatus.DRAFT,
            dispatchDate = 1000L,
            notes = null,
            createdBy = "user-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
    }

    private fun sampleLine(
        lineId: String = "DLINE-01",
        dispatchId: String = "DISP-01",
        qty: Double = 100.0
    ): DispatchExecutionLine {
        return DispatchExecutionLine(
            dispatchExecutionLineId = lineId,
            projectId = "PRJ-01",
            dispatchExecutionId = dispatchId,
            deliveryChallanLineId = "CLINE-01",
            deliveryOrderLineId = "DOLINE-01",
            productId = "PROD-01",
            requestedQuantity = 100.0,
            dispatchQuantity = qty,
            sourceLocationId = "LOC-01",
            createdAt = 1000L
        )
    }

    @Test
    fun `createDispatch successfully creates dispatch and lines`() = runBlocking {
        val dispatch = sampleDispatch()
        val lines = listOf(sampleLine())

        val result = dispatchRepository.createDispatch(dispatch, lines, UserRole.ADMIN)
        assertTrue(result is DomainResult.Success)

        val fetched = dispatchRepository.getDispatch(dispatch.dispatchExecutionId, UserRole.ADMIN)
        assertTrue(fetched is DomainResult.Success)
        assertEquals(dispatch.dispatchNo, (fetched as DomainResult.Success).data.dispatchNo)
    }

    @Test
    fun `duplicate dispatchNo in same project is rejected`() = runBlocking {
        val d1 = sampleDispatch(dispatchId = "D-1", dispatchNo = "DN-DUP")
        val lines1 = listOf(sampleLine(lineId = "L-1", dispatchId = "D-1"))
        dispatchRepository.createDispatch(d1, lines1, UserRole.ADMIN)

        val d2 = sampleDispatch(dispatchId = "D-2", dispatchNo = "DN-DUP")
        val lines2 = listOf(sampleLine(lineId = "L-2", dispatchId = "D-2"))
        val result = dispatchRepository.createDispatch(d2, lines2, UserRole.ADMIN)

        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("already exists"))
    }

    @Test
    fun `executeDispatch completes stock out and marks dispatch and challan as DISPATCHED`() = runBlocking {
        val dispatch = sampleDispatch()
        val lines = listOf(sampleLine(qty = 100.0))
        dispatchRepository.createDispatch(dispatch, lines, UserRole.ADMIN)

        dispatchRepository.submitDispatch(dispatch.dispatchExecutionId, "user-1", UserRole.ADMIN)
        dispatchRepository.approveDispatch(dispatch.dispatchExecutionId, "mgr-1", UserRole.MANAGER)
        dispatchRepository.markReadyForExecution(dispatch.dispatchExecutionId, "wh-1", UserRole.WAREHOUSE)

        val execResult = dispatchRepository.executeDispatch(dispatch.dispatchExecutionId, "wh-1", UserRole.WAREHOUSE)
        assertTrue(execResult is DomainResult.Success)

        val dispatched = (execResult as DomainResult.Success).data
        assertEquals(DispatchExecutionStatus.DISPATCHED, dispatched.status)
        assertTrue(dispatched.stockOutId != null)

        // Verify Challan transitioned to DISPATCHED
        val challan = challanDataSource.getChallan("CH-01")
        assertEquals(DeliveryChallanStatus.DISPATCHED, challan?.status)

        // Verify StockOutRecord was generated in Module 07
        val stockOutRecords = stockOutRepository.observeStockOutRecords("PRJ-01").first()
        assertEquals(1, stockOutRecords.size)
        assertEquals(100, stockOutRecords[0].quantity)

        // Verify remaining available inventory is now 400 (500 - 100)
        val remainingStock = stockOutRepository.getAvailableQuantity("PRJ-01", "WH-01", "LOC-01", "PROD-01")
        assertEquals(400, remainingStock)
    }
}
