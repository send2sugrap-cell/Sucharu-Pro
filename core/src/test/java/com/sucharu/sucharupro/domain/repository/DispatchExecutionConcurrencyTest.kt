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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

class DispatchExecutionConcurrencyTest {

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

            warehouseDataSource.insertWarehouse(
                InventoryWarehouse(
                    id = "WH-01",
                    projectId = "PRJ-01",
                    code = "WH-1",
                    name = "WH 1",
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
                    id = "PROD-CONCUR",
                    sku = "SKU-CONCUR",
                    name = "Test Product",
                    isActive = true,
                    isStockTracked = true,
                    createdBy = "admin",
                    createdAt = "1000",
                    updatedAt = "1000"
                )
            )

            // Seed exactly 100 units
            receivingDataSource.insertStockInRecord(
                InventoryStockInRecord(
                    stockInId = UUID.randomUUID().toString(),
                    receivingId = "REC-1",
                    receivingLineId = "RL-1",
                    projectId = "PRJ-01",
                    inventoryProductId = "PROD-CONCUR",
                    warehouseId = "WH-01",
                    locationId = "LOC-01",
                    quantity = 100,
                    unit = InventoryUnit.PCS,
                    createdBy = "admin",
                    createdAt = "1000"
                )
            )

            // Seed Challans
            val ch1 = DeliveryChallan(
                challanId = "CH-C1",
                projectId = "PRJ-01",
                challanNo = "CHAL-C1",
                deliveryOrderId = "DO-1",
                customerId = null,
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
            val line1 = DeliveryChallanLine("CL-1", "CH-C1", "PRJ-01", "DOL-1", "PROD-CONCUR", 80.0)
            challanDataSource.insertChallan(ch1, listOf(line1))

            val ch2 = DeliveryChallan(
                challanId = "CH-C2",
                projectId = "PRJ-01",
                challanNo = "CHAL-C2",
                deliveryOrderId = "DO-1",
                customerId = null,
                sourceReferenceId = null,
                sourceReferenceType = null,
                challanType = DeliveryChallanType.STANDARD,
                status = DeliveryChallanStatus.APPROVED,
                issueDate = 1000L,
                notes = null,
                createdBy = "user-2",
                createdAt = 1000L,
                updatedAt = 1000L
            )
            val line2 = DeliveryChallanLine("CL-2", "CH-C2", "PRJ-01", "DOL-1", "PROD-CONCUR", 80.0)
            challanDataSource.insertChallan(ch2, listOf(line2))
        }
    }

    @Test
    fun `concurrent execution of competing dispatches on limited inventory allows only one to succeed`() = runBlocking {
        val disp1 = DispatchExecution(
            dispatchExecutionId = "DISP-C1",
            projectId = "PRJ-01",
            dispatchNo = "DN-C1",
            deliveryOrderId = "DO-1",
            deliveryChallanId = "CH-C1",
            customerId = null,
            sourceWarehouseId = "WH-01",
            sourceLocationId = "LOC-01",
            dispatchType = DispatchExecutionType.STANDARD,
            status = DispatchExecutionStatus.READY_FOR_EXECUTION,
            dispatchDate = 1000L,
            notes = null,
            createdBy = "user-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val dline1 = DispatchExecutionLine("DL-1", "PRJ-01", "DISP-C1", "CL-1", "DOL-1", "PROD-CONCUR", 80.0, 80.0, null, null, "LOC-01", 1000L)
        dispatchDataSource.insertDispatch(disp1, listOf(dline1))

        val disp2 = DispatchExecution(
            dispatchExecutionId = "DISP-C2",
            projectId = "PRJ-01",
            dispatchNo = "DN-C2",
            deliveryOrderId = "DO-1",
            deliveryChallanId = "CH-C2",
            customerId = null,
            sourceWarehouseId = "WH-01",
            sourceLocationId = "LOC-01",
            dispatchType = DispatchExecutionType.STANDARD,
            status = DispatchExecutionStatus.READY_FOR_EXECUTION,
            dispatchDate = 1000L,
            notes = null,
            createdBy = "user-2",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val dline2 = DispatchExecutionLine("DL-2", "PRJ-01", "DISP-C2", "CL-2", "DOL-1", "PROD-CONCUR", 80.0, 80.0, null, null, "LOC-01", 1000L)
        dispatchDataSource.insertDispatch(disp2, listOf(dline2))

        val results = listOf("DISP-C1", "DISP-C2").map { id ->
            async(Dispatchers.IO) {
                dispatchRepository.executeDispatch(id, "operator", UserRole.WAREHOUSE)
            }
        }.awaitAll()

        val successCount = results.count { it is DomainResult.Success }
        val errorCount = results.count { it is DomainResult.Error }

        assertEquals(1, successCount)
        assertEquals(1, errorCount)

        val remainingStock = stockOutRepository.getAvailableQuantity("PRJ-01", "WH-01", "LOC-01", "PROD-CONCUR")
        assertEquals(20, remainingStock)

        val stockOutRecords = stockOutRepository.observeStockOutRecords("PRJ-01").first()
        assertEquals(1, stockOutRecords.size)
        assertEquals(80, stockOutRecords[0].quantity)
    }
}
