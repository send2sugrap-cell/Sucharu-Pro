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
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class DispatchExecutionAuditTest {

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
                    name = "Booklet",
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

            val challan = DeliveryChallan("CH-01", "PRJ-01", "CH-01", "DO-01", null, null, null, DeliveryChallanType.STANDARD, DeliveryChallanStatus.APPROVED, 1000L, null, "user-1", 1000L, 1000L)
            val challanLine = DeliveryChallanLine("CL-01", "CH-01", "PRJ-01", "DOL-01", "PROD-01", 50.0)
            challanDataSource.insertChallan(challan, listOf(challanLine))
        }
    }

    @Test
    fun `full dispatch execution workflow records complete audit trail`() = runBlocking {
        val dispatch = DispatchExecution("DISP-AUDIT", "PRJ-01", "DN-AUDIT", "DO-01", "CH-01", null, "WH-01", "LOC-01", DispatchExecutionType.STANDARD, DispatchExecutionStatus.DRAFT, null, 1000L, null, "creator", 1000L, 1000L)
        val line = DispatchExecutionLine("DL-AUDIT", "PRJ-01", "DISP-AUDIT", "CL-01", "DOL-01", "PROD-01", 50.0, 50.0, null, null, "LOC-01", 1000L)

        // 1. Create
        dispatchRepository.createDispatch(dispatch, listOf(line), UserRole.ADMIN)

        // 2. Submit
        dispatchRepository.submitDispatch("DISP-AUDIT", "submitter", UserRole.ADMIN)

        // 3. Approve
        dispatchRepository.approveDispatch("DISP-AUDIT", "approver", UserRole.MANAGER)

        // 4. Ready for execution
        dispatchRepository.markReadyForExecution("DISP-AUDIT", "warehouse-lead", UserRole.WAREHOUSE)

        // 5. Execute
        dispatchRepository.executeDispatch("DISP-AUDIT", "dispatcher", UserRole.WAREHOUSE)

        val eventsResult = dispatchRepository.getActivityEvents("DISP-AUDIT", UserRole.ADMIN)
        assertTrue(eventsResult is DomainResult.Success)
        val events = (eventsResult as DomainResult.Success).data

        val types = events.map { it.activityType }
        assertTrue(types.contains(DispatchExecutionActivityType.CREATED))
        assertTrue(types.contains(DispatchExecutionActivityType.SUBMITTED))
        assertTrue(types.contains(DispatchExecutionActivityType.APPROVED))
        assertTrue(types.contains(DispatchExecutionActivityType.READY_FOR_EXECUTION))
        assertTrue(types.contains(DispatchExecutionActivityType.STOCK_OUT_CREATED))
        assertTrue(types.contains(DispatchExecutionActivityType.DISPATCHED))
    }
}
