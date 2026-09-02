package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeDeliveryChallanDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeDispatchExecutionDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryLocationDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryProductDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockAdjustmentDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockOutDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockTransferDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryTraceabilityDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryWarehouseDataSource
import com.sucharu.sucharupro.data.repository.DeliveryChallanRepositoryImpl
import com.sucharu.sucharupro.data.repository.DeliveryOrderRepositoryImpl
import com.sucharu.sucharupro.data.repository.DispatchExecutionRepositoryImpl
import com.sucharu.sucharupro.data.repository.InventoryStockOutRepositoryImpl
import com.sucharu.sucharupro.data.repository.InventoryTraceabilityRepositoryImpl
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
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionActivityType
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
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryTraceabilityStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryChallanRepository
import com.sucharu.sucharupro.domain.repository.DeliveryOrderRepository
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

class DispatchExecutionEndToEndTest {

    private lateinit var doDataSource: FakeDeliveryOrderDataSource
    private lateinit var challanDataSource: FakeDeliveryChallanDataSource
    private lateinit var dispatchDataSource: FakeDispatchExecutionDataSource
    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var productDataSource: FakeInventoryProductDataSource
    private lateinit var warehouseDataSource: FakeInventoryWarehouseDataSource
    private lateinit var locationDataSource: FakeInventoryLocationDataSource
    private lateinit var traceabilityDataSource: FakeInventoryTraceabilityDataSource

    private lateinit var doRepository: DeliveryOrderRepository
    private lateinit var challanRepository: DeliveryChallanRepository
    private lateinit var stockOutRepository: InventoryStockOutRepository
    private lateinit var traceabilityRepository: InventoryTraceabilityRepository
    private lateinit var dispatchRepository: DispatchExecutionRepository

    @Before
    fun setUp() {
        runBlocking {
            doDataSource = FakeDeliveryOrderDataSource()
            challanDataSource = FakeDeliveryChallanDataSource()
            dispatchDataSource = FakeDispatchExecutionDataSource()
            stockOutDataSource = FakeInventoryStockOutDataSource()
            receivingDataSource = FakeInventoryReceivingDataSource()
            productDataSource = FakeInventoryProductDataSource()
            warehouseDataSource = FakeInventoryWarehouseDataSource()
            locationDataSource = FakeInventoryLocationDataSource()
            traceabilityDataSource = FakeInventoryTraceabilityDataSource()

            doRepository = DeliveryOrderRepositoryImpl(doDataSource)
            challanRepository = DeliveryChallanRepositoryImpl(challanDataSource, doDataSource)

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

            // Seed Warehouse, Location, Products
            warehouseDataSource.insertWarehouse(
                InventoryWarehouse(
                    id = "WH-E2E",
                    projectId = "PRJ-E2E",
                    code = "WH-E2E",
                    name = "Main Logistics WH",
                    type = InventoryWarehouseType.FINISHED_GOODS,
                    createdBy = "admin",
                    createdAt = "1000",
                    updatedAt = "1000"
                )
            )
            locationDataSource.insertLocation(
                InventoryLocation(
                    id = "LOC-E2E",
                    projectId = "PRJ-E2E",
                    warehouseId = "WH-E2E",
                    code = "LOC-E2E",
                    name = "Loading Dock 1",
                    type = InventoryLocationType.SHELF,
                    createdBy = "admin",
                    createdAt = "1000",
                    updatedAt = "1000"
                )
            )
            productDataSource.insertProduct(
                InventoryProduct(
                    id = "PROD-E2E",
                    sku = "SKU-E2E",
                    name = "Annual Report 2026",
                    isActive = true,
                    isStockTracked = true,
                    createdBy = "admin",
                    createdAt = "1000",
                    updatedAt = "1000"
                )
            )

            // Seed inventory stock: 1,000 units
            receivingDataSource.insertStockInRecord(
                InventoryStockInRecord(
                    stockInId = UUID.randomUUID().toString(),
                    receivingId = "REC-E2E",
                    receivingLineId = "RL-E2E",
                    projectId = "PRJ-E2E",
                    inventoryProductId = "PROD-E2E",
                    warehouseId = "WH-E2E",
                    locationId = "LOC-E2E",
                    quantity = 1000,
                    unit = InventoryUnit.PCS,
                    createdBy = "admin",
                    createdAt = "1000"
                )
            )

            // Seed Batch & Lot
            traceabilityRepository.createBatch(
                InventoryBatch("BATCH-E2E", "B-2026-FINAL", "PRJ-E2E", "PROD-E2E", null, null, InventoryTraceabilityStatus.ACTIVE, "1000"),
                "admin",
                "Admin"
            )
            traceabilityRepository.createLot(
                InventoryLot("LOT-E2E", "L-2026-FINAL", "PRJ-E2E", "PROD-E2E", "BATCH-E2E", InventoryTraceabilityStatus.ACTIVE, "1000"),
                "admin",
                "Admin"
            )
        }
    }

    @Test
    fun `complete end-to-end flow from DeliveryOrder to Challan to Dispatch Execution with Module 07 StockOut`() = runBlocking {
        // Step 1: Create & Approve Delivery Order
        val deliveryOrder = DeliveryOrder(
            deliveryOrderId = "DO-E2E-FULL",
            projectId = "PRJ-E2E",
            deliveryOrderNo = "DEL-2026-FULL",
            customerId = "CUST-CORP",
            sourceReferenceId = "SO-CORP-01",
            sourceReferenceType = "SALES_ORDER",
            deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
            priority = DeliveryPriority.HIGH,
            status = DeliveryOrderStatus.DRAFT,
            requestedDeliveryDate = 50000L,
            notes = "Annual report full print run",
            createdBy = "sales-mgr",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val doLine = DeliveryOrderLine(
            lineId = "DOL-E2E-FULL",
            deliveryOrderId = "DO-E2E-FULL",
            projectId = "PRJ-E2E",
            productId = "PROD-E2E",
            requestedQuantity = 500.0,
            notes = null
        )
        doRepository.createDeliveryOrder(deliveryOrder, listOf(doLine), UserRole.ADMIN)
        doRepository.submitDeliveryOrder("DO-E2E-FULL", "sales-mgr", UserRole.ADMIN)
        doRepository.approveDeliveryOrder("DO-E2E-FULL", "general-mgr", UserRole.MANAGER)

        // Step 2: Create & Approve Delivery Challan
        val challan = DeliveryChallan(
            challanId = "CH-E2E-FULL",
            projectId = "PRJ-E2E",
            challanNo = "CHAL-2026-FULL",
            deliveryOrderId = "DO-E2E-FULL",
            customerId = "CUST-CORP",
            sourceReferenceId = "SO-CORP-01",
            sourceReferenceType = "SALES_ORDER",
            challanType = DeliveryChallanType.STANDARD,
            status = DeliveryChallanStatus.DRAFT,
            issueDate = 2000L,
            notes = "Full authorized batch",
            createdBy = "dispatch-mgr",
            createdAt = 2000L,
            updatedAt = 2000L
        )
        val challanLine = DeliveryChallanLine(
            lineId = "CL-E2E-FULL",
            challanId = "CH-E2E-FULL",
            projectId = "PRJ-E2E",
            deliveryOrderLineId = "DOL-E2E-FULL",
            productId = "PROD-E2E",
            quantity = 500.0,
            notes = null,
            batchId = "BATCH-E2E",
            lotId = "LOT-E2E"
        )
        challanRepository.createChallan(challan, listOf(challanLine), UserRole.MANAGER)
        challanRepository.submitChallan("CH-E2E-FULL", "dispatch-mgr", UserRole.MANAGER)
        challanRepository.approveChallan("CH-E2E-FULL", "general-mgr", UserRole.MANAGER)
        challanRepository.markReadyForDispatch("CH-E2E-FULL", "wh-mgr", UserRole.WAREHOUSE)

        // Step 3: Create & Prepare Dispatch Execution
        val dispatch = DispatchExecution(
            dispatchExecutionId = "DISP-E2E-FULL",
            projectId = "PRJ-E2E",
            dispatchNo = "DN-2026-FULL",
            deliveryOrderId = "DO-E2E-FULL",
            deliveryChallanId = "CH-E2E-FULL",
            customerId = "CUST-CORP",
            sourceWarehouseId = "WH-E2E",
            sourceLocationId = "LOC-E2E",
            dispatchType = DispatchExecutionType.CUSTOMER_DELIVERY,
            status = DispatchExecutionStatus.DRAFT,
            dispatchDate = 3000L,
            notes = "Final shipment loading",
            createdBy = "dispatch-lead",
            createdAt = 3000L,
            updatedAt = 3000L
        )
        val dispatchLine = DispatchExecutionLine(
            dispatchExecutionLineId = "DL-E2E-FULL",
            projectId = "PRJ-E2E",
            dispatchExecutionId = "DISP-E2E-FULL",
            deliveryChallanLineId = "CL-E2E-FULL",
            deliveryOrderLineId = "DOL-E2E-FULL",
            productId = "PROD-E2E",
            requestedQuantity = 500.0,
            dispatchQuantity = 500.0,
            batchId = "BATCH-E2E",
            lotId = "LOT-E2E",
            sourceLocationId = "LOC-E2E",
            createdAt = 3000L
        )
        val createDispRes = dispatchRepository.createDispatch(dispatch, listOf(dispatchLine), UserRole.MANAGER)
        assertTrue(createDispRes is DomainResult.Success)

        dispatchRepository.submitDispatch("DISP-E2E-FULL", "dispatch-lead", UserRole.MANAGER)
        dispatchRepository.approveDispatch("DISP-E2E-FULL", "general-mgr", UserRole.MANAGER)
        dispatchRepository.markReadyForExecution("DISP-E2E-FULL", "wh-lead", UserRole.WAREHOUSE)

        // Step 4: Execute Dispatch Operation
        val executeRes = dispatchRepository.executeDispatch("DISP-E2E-FULL", "operator-1", UserRole.WAREHOUSE)
        assertTrue(executeRes is DomainResult.Success)

        val completedDispatch = (executeRes as DomainResult.Success).data
        assertEquals(DispatchExecutionStatus.DISPATCHED, completedDispatch.status)
        assertTrue(completedDispatch.stockOutId != null)

        // Step 5: Verify Inventory Stock Out & Remaining Quantity
        val stockOutRecords = stockOutRepository.observeStockOutRecords("PRJ-E2E").first()
        assertEquals(1, stockOutRecords.size)
        assertEquals(500, stockOutRecords[0].quantity)

        val remainingAvailable = stockOutRepository.getAvailableQuantity("PRJ-E2E", "WH-E2E", "LOC-E2E", "PROD-E2E")
        assertEquals(500, remainingAvailable) // 1000 - 500 = 500

        // Step 6: Verify Delivery Challan state
        val updatedChallan = challanDataSource.getChallan("CH-E2E-FULL")
        assertEquals(DeliveryChallanStatus.DISPATCHED, updatedChallan?.status)

        // Step 7: Verify Traceability Linkage
        val traceRecords = traceabilityDataSource.observeTraceRecords("PRJ-E2E").first()
        assertEquals(1, traceRecords.size)
        assertEquals("BATCH-E2E", traceRecords[0].batchId)
        assertEquals("LOT-E2E", traceRecords[0].lotId)
        assertEquals(500.0, traceRecords[0].quantity, 0.001)

        // Step 8: Verify Audit History
        val auditEventsRes = dispatchRepository.getActivityEvents("DISP-E2E-FULL", UserRole.ADMIN)
        assertTrue(auditEventsRes is DomainResult.Success)
        val auditEvents = (auditEventsRes as DomainResult.Success).data
        assertTrue(auditEvents.any { it.activityType == DispatchExecutionActivityType.DISPATCHED })
        assertTrue(auditEvents.any { it.activityType == DispatchExecutionActivityType.STOCK_OUT_CREATED })
    }
}
