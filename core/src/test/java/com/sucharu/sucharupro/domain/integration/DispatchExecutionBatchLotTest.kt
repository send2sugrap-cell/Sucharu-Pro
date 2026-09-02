package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeDeliveryChallanDataSource
import com.sucharu.sucharupro.data.datasource.FakeDispatchExecutionDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryLocationDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryProductDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockAdjustmentDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockOutDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockTransferDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryTraceabilityDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryWarehouseDataSource
import com.sucharu.sucharupro.data.repository.DispatchExecutionRepositoryImpl
import com.sucharu.sucharupro.data.repository.InventoryStockOutRepositoryImpl
import com.sucharu.sucharupro.data.repository.InventoryTraceabilityRepositoryImpl
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
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryBatch
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryLot
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryMovementType
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryTraceabilityStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DispatchExecutionRepository
import com.sucharu.sucharupro.domain.repository.InventoryStockOutRepository
import com.sucharu.sucharupro.domain.repository.InventoryTraceabilityRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class DispatchExecutionBatchLotTest {

    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var productDataSource: FakeInventoryProductDataSource
    private lateinit var warehouseDataSource: FakeInventoryWarehouseDataSource
    private lateinit var locationDataSource: FakeInventoryLocationDataSource
    private lateinit var traceabilityDataSource: FakeInventoryTraceabilityDataSource
    private lateinit var challanDataSource: FakeDeliveryChallanDataSource
    private lateinit var dispatchDataSource: FakeDispatchExecutionDataSource
    private lateinit var stockOutRepository: InventoryStockOutRepository
    private lateinit var traceabilityRepository: InventoryTraceabilityRepository
    private lateinit var dispatchRepository: DispatchExecutionRepository

    @Before
    fun setUp() {
        runBlocking {
            stockOutDataSource = FakeInventoryStockOutDataSource()
            receivingDataSource = FakeInventoryReceivingDataSource()
            productDataSource = FakeInventoryProductDataSource()
            warehouseDataSource = FakeInventoryWarehouseDataSource()
            locationDataSource = FakeInventoryLocationDataSource()
            traceabilityDataSource = FakeInventoryTraceabilityDataSource()
            challanDataSource = FakeDeliveryChallanDataSource()
            dispatchDataSource = FakeDispatchExecutionDataSource()

            stockOutRepository = InventoryStockOutRepositoryImpl(
                stockOutDataSource, receivingDataSource, productDataSource, warehouseDataSource, locationDataSource
            )

            traceabilityRepository = InventoryTraceabilityRepositoryImpl(
                traceabilityDataSource = traceabilityDataSource,
                productDataSource = productDataSource,
                receivingDataSource = receivingDataSource,
                stockOutDataSource = stockOutDataSource,
                stockTransferDataSource = FakeInventoryStockTransferDataSource(),
                stockAdjustmentDataSource = FakeInventoryStockAdjustmentDataSource()
            )

            dispatchRepository = DispatchExecutionRepositoryImpl(
                dispatchDataSource = dispatchDataSource,
                challanDataSource = challanDataSource,
                stockOutRepository = stockOutRepository,
                traceabilityRepository = traceabilityRepository
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
                    name = "Catalog",
                    isActive = true,
                    isStockTracked = true,
                    createdBy = "admin",
                    createdAt = "1000",
                    updatedAt = "1000"
                )
            )
            receivingDataSource.insertStockInRecord(
                InventoryStockInRecord(
                    stockInId = UUID.randomUUID().toString(),
                    receivingId = "REC-1",
                    receivingLineId = "RL-1",
                    projectId = "PRJ-01",
                    inventoryProductId = "PROD-01",
                    warehouseId = "WH-01",
                    locationId = "LOC-01",
                    quantity = 100,
                    unit = InventoryUnit.PCS,
                    createdBy = "admin",
                    createdAt = "1000"
                )
            )

            // Create Batch & Lot in Step 07
            val batch = InventoryBatch("BATCH-01", "B-2026-X", "PRJ-01", "PROD-01", null, null, InventoryTraceabilityStatus.ACTIVE, "1000")
            val lot = InventoryLot("LOT-01", "L-2026-Y", "PRJ-01", "PROD-01", "BATCH-01", InventoryTraceabilityStatus.ACTIVE, "1000")
            traceabilityRepository.createBatch(batch, "admin", "Admin")
            traceabilityRepository.createLot(lot, "admin", "Admin")

            val challan = DeliveryChallan("CH-01", "PRJ-01", "CH-01", "DO-01", null, null, null, DeliveryChallanType.STANDARD, DeliveryChallanStatus.APPROVED, 1000L, null, "user-1", 1000L, 1000L)
            val challanLine = DeliveryChallanLine("CL-01", "CH-01", "PRJ-01", "DOL-01", "PROD-01", 40.0, null, "BATCH-01", "LOT-01")
            challanDataSource.insertChallan(challan, listOf(challanLine))
        }
    }

    @Test
    fun `dispatch execution with batch and lot automatically creates traceability linkage`() = runBlocking {
        val dispatch = DispatchExecution("DISP-TRACE", "PRJ-01", "DN-TRACE", "DO-01", "CH-01", null, "WH-01", "LOC-01", DispatchExecutionType.STANDARD, DispatchExecutionStatus.READY_FOR_EXECUTION, null, 1000L, null, "user-1", 1000L, 1000L)
        val line = DispatchExecutionLine("DL-TRACE", "PRJ-01", "DISP-TRACE", "CL-01", "DOL-01", "PROD-01", 40.0, 40.0, "BATCH-01", "LOT-01", "LOC-01", 1000L)
        dispatchDataSource.insertDispatch(dispatch, listOf(line))

        val result = dispatchRepository.executeDispatch("DISP-TRACE", "operator", UserRole.WAREHOUSE)
        assertTrue(result is DomainResult.Success)

        // Verify Traceability Record was created and mapped to STOCK_OUT
        val traceRecords = traceabilityDataSource.observeTraceRecords("PRJ-01").first()
        assertEquals(1, traceRecords.size)

        val record = traceRecords[0]
        assertEquals("BATCH-01", record.batchId)
        assertEquals("LOT-01", record.lotId)
        assertEquals("PROD-01", record.productId)
        assertEquals("LOC-01", record.locationId)
        assertEquals(InventoryMovementType.STOCK_OUT, record.movementType)
        assertEquals(40.0, record.quantity, 0.001)
    }
}
