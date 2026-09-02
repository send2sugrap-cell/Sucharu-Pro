package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.InventoryReorderRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.inventory.reorder.*
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryStockInRecord
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InventoryReorderRepositoryTest {

    private lateinit var repository: InventoryReorderRepository
    private lateinit var reorderDataSource: FakeInventoryReorderDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var transferDataSource: FakeInventoryStockTransferDataSource
    private lateinit var adjustmentDataSource: FakeInventoryStockAdjustmentDataSource
    private lateinit var productDataSource: FakeInventoryProductDataSource
    private lateinit var warehouseDataSource: FakeInventoryWarehouseDataSource
    private lateinit var locationDataSource: FakeInventoryLocationDataSource

    private val projectId = "PROJ-7"
    private val productId = "PROD-1"
    private val locationId = "LOC-1"
    private val warehouseId = "WH-1"

    @Before
    fun setup() {
        reorderDataSource = FakeInventoryReorderDataSource()
        receivingDataSource = FakeInventoryReceivingDataSource()
        stockOutDataSource = FakeInventoryStockOutDataSource()
        transferDataSource = FakeInventoryStockTransferDataSource()
        adjustmentDataSource = FakeInventoryStockAdjustmentDataSource()
        productDataSource = FakeInventoryProductDataSource()
        warehouseDataSource = FakeInventoryWarehouseDataSource()
        locationDataSource = FakeInventoryLocationDataSource()

        repository = InventoryReorderRepositoryImpl(
            reorderDataSource,
            receivingDataSource,
            stockOutDataSource,
            transferDataSource,
            adjustmentDataSource,
            productDataSource,
            warehouseDataSource,
            locationDataSource
        )
    }

    @Test
    fun `policy CRUD operations work correctly`() = runBlocking {
        val policy = InventoryStockLevelPolicy(
            policyId = "POL-1",
            projectId = projectId,
            productId = productId,
            locationId = locationId,
            reorderPoint = 20.0,
            targetStockLevel = 50.0,
            maximumStockLevel = 100.0
        )

        // Create
        val createResult = repository.createPolicy(policy, null)
        assertTrue(createResult is DomainResult.Success)
        
        // Read
        val getResult = repository.getPolicy("POL-1", null)
        assertTrue(getResult is DomainResult.Success)
        assertEquals(productId, (getResult as DomainResult.Success).data.productId)

        // Update
        val updatedPolicy = policy.copy(reorderPoint = 30.0)
        repository.updatePolicy(updatedPolicy, null)
        val afterUpdate = repository.getPolicy("POL-1", null)
        assertEquals(30.0, (afterUpdate as DomainResult.Success).data.reorderPoint, 0.0)

        // Delete
        repository.deletePolicy("POL-1", null)
        val afterDelete = repository.getPolicy("POL-1", null)
        assertTrue(afterDelete is DomainResult.Error)
    }

    @Test
    fun `evaluatePolicies detects LOW_STOCK alert correctly`() = runBlocking {
        // 1. Configure Policy (Min Stock = 10)
        val policy = InventoryStockLevelPolicy(
            policyId = "POL-1",
            projectId = projectId,
            productId = productId,
            locationId = locationId,
            minimumStockLevel = 10.0,
            reorderPoint = 5.0,
            targetStockLevel = 15.0,
            maximumStockLevel = 20.0
        )
        repository.createPolicy(policy, null)

        // 2. Seed Stock (8 units)
        receivingDataSource.insertStockInRecord(InventoryStockInRecord(
            stockInId = "SIN-1",
            receivingId = "REC-1",
            receivingLineId = "LINE-1",
            projectId = projectId,
            inventoryProductId = productId,
            warehouseId = warehouseId,
            locationId = locationId,
            quantity = 8,
            unit = InventoryUnit.PCS,
            createdBy = "tester",
            createdAt = "2026-08-17T10:00:00Z"
        ))

        // 3. Evaluate
        repository.evaluatePolicies(projectId)

        // 4. Verify Alert
        val alerts = repository.observeAlerts(projectId).first()
        assertEquals(1, alerts.size)
        assertEquals(InventoryReorderAlertType.LOW_STOCK, alerts[0].alertType)
        assertEquals(8.0, alerts[0].availableQuantity, 0.0)
        assertEquals(10.0, alerts[0].thresholdQuantity, 0.0)
    }

    @Test
    fun `evaluatePolicies prevents duplicate open alerts`() = runBlocking {
        val policy = InventoryStockLevelPolicy(
            policyId = "POL-1",
            projectId = projectId,
            productId = productId,
            locationId = locationId,
            reorderPoint = 10.0,
            targetStockLevel = 20.0,
            maximumStockLevel = 30.0
        )
        repository.createPolicy(policy, null)

        // Evaluate twice
        repository.evaluatePolicies(projectId)
        repository.evaluatePolicies(projectId)

        val alerts = repository.observeAlerts(projectId).first()
        assertEquals(1, alerts.size)
    }

    @Test
    fun `healthy stock auto-resolves alerts`() = runBlocking {
        // 1. Setup Policy and Alert (Stock = 0, Policy Reorder = 10)
        val policy = InventoryStockLevelPolicy(
            policyId = "POL-1",
            projectId = projectId,
            productId = productId,
            locationId = locationId,
            reorderPoint = 10.0,
            targetStockLevel = 20.0,
            maximumStockLevel = 30.0
        )
        repository.createPolicy(policy, null)
        repository.evaluatePolicies(projectId)
        
        var alerts = repository.observeAlerts(projectId).first()
        assertEquals(1, alerts.size)
        assertEquals(InventoryReorderAlertStatus.OPEN, alerts[0].status)

        // 2. Replenish Stock (15 units)
        receivingDataSource.insertStockInRecord(InventoryStockInRecord(
            stockInId = "SIN-1",
            receivingId = "REC-1",
            receivingLineId = "LINE-1",
            projectId = projectId,
            inventoryProductId = productId,
            warehouseId = warehouseId,
            locationId = locationId,
            quantity = 15,
            unit = InventoryUnit.PCS,
            createdBy = "tester",
            createdAt = "2026-08-17T10:00:00Z"
        ))

        // 3. Re-evaluate
        repository.evaluatePolicies(projectId)

        // 4. Verify Resolution
        alerts = repository.observeAlerts(projectId).first()
        assertEquals(1, alerts.size)
        assertEquals(InventoryReorderAlertStatus.RESOLVED, alerts[0].status)
        assertEquals("SYSTEM_AUTO_RESOLVE", alerts[0].resolvedBy)
    }

    @Test
    fun `alert detection across movement types - StockOut triggers alert`() = runBlocking {
        val policy = InventoryStockLevelPolicy(
            policyId = "POL-1",
            projectId = projectId,
            productId = productId,
            locationId = locationId,
            minimumStockLevel = 20.0,
            reorderPoint = 15.0,
            targetStockLevel = 30.0,
            maximumStockLevel = 40.0
        )
        repository.createPolicy(policy, null)

        // 1. Seed 25 units (Healthy)
        receivingDataSource.insertStockInRecord(InventoryStockInRecord(
            stockInId = "SIN-1",
            receivingId = "REC-1",
            receivingLineId = "LINE-1",
            projectId = projectId,
            inventoryProductId = productId,
            warehouseId = warehouseId,
            locationId = locationId,
            quantity = 25,
            unit = InventoryUnit.PCS,
            createdBy = "tester",
            createdAt = "2026-08-17T10:00:00Z"
        ))
        repository.evaluatePolicies(projectId)
        assertTrue(repository.observeAlerts(projectId).first().isEmpty())

        // 2. Stock Out 10 units (Leaves 15 -> REORDER_REQUIRED)
        stockOutDataSource.insertStockOutRecord(InventoryStockOutRecord(
            stockOutRecordId = "SOR-1",
            stockOutId = "OUT-1",
            stockOutLineId = "LINE-1",
            projectId = projectId,
            inventoryProductId = productId,
            warehouseId = warehouseId,
            locationId = locationId,
            quantity = 10,
            unit = InventoryUnit.PCS,
            createdBy = "tester",
            createdAt = "2026-08-17T11:00:00Z"
        ))
        repository.evaluatePolicies(projectId)

        val alerts = repository.observeAlerts(projectId).first()
        assertEquals(1, alerts.size)
        assertEquals(InventoryReorderAlertType.REORDER_REQUIRED, alerts[0].alertType)
    }
}
