package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.InventoryReorderRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.inventory.reorder.*
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryStockInRecord
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutRecord
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryReorderRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * End-to-End integration test for Reorder Alert & Stock Level Management (Module 07 Step 08).
 *
 * Flow:
 * 1. Configure stock level policy (Min=10, Reorder=20, Target=50).
 * 2. Seed initial stock (60 units -> Healthy).
 * 3. Issue stock (Remove 45 units -> 15 remains -> REORDER_REQUIRED).
 * 4. Verify alert generation.
 * 5. Acknowledge alert.
 * 6. Replenish stock (Add 40 units -> 55 remains -> Healthy).
 * 7. Verify alert auto-resolution.
 */
class InventoryReorderEndToEndTest {

    private lateinit var repository: InventoryReorderRepository
    private lateinit var reorderDataSource: FakeInventoryReorderDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var transferDataSource: FakeInventoryStockTransferDataSource
    private lateinit var adjustmentDataSource: FakeInventoryStockAdjustmentDataSource

    private val projectId = "PROJ-E2E-7"
    private val productId = "PROD-E2E-7"
    private val locationId = "LOC-E2E-7"
    private val warehouseId = "WH-E2E-7"

    @Before
    fun setup() {
        reorderDataSource = FakeInventoryReorderDataSource()
        receivingDataSource = FakeInventoryReceivingDataSource()
        stockOutDataSource = FakeInventoryStockOutDataSource()
        transferDataSource = FakeInventoryStockTransferDataSource()
        adjustmentDataSource = FakeInventoryStockAdjustmentDataSource()

        repository = InventoryReorderRepositoryImpl(
            reorderDataSource,
            receivingDataSource,
            stockOutDataSource,
            transferDataSource,
            adjustmentDataSource,
            FakeInventoryProductDataSource(),
            FakeInventoryWarehouseDataSource(),
            FakeInventoryLocationDataSource()
        )
    }

    @Test
    fun `full reorder alert lifecycle`() = runBlocking {
        // 1. Configure Policy
        val policy = InventoryStockLevelPolicy(
            policyId = "POL-E2E",
            projectId = projectId,
            productId = productId,
            locationId = locationId,
            minimumStockLevel = 10.0,
            reorderPoint = 20.0,
            targetStockLevel = 50.0,
            maximumStockLevel = 100.0
        )
        repository.createPolicy(policy, UserRole.ADMIN)

        // 2. Seed initial stock: 60 units
        receivingDataSource.insertStockInRecord(buildStockIn("SIN-1", 60, "RL-1"))
        
        // Evaluate: should be healthy
        repository.evaluatePolicies(projectId)
        var alerts = repository.observeAlerts(projectId).first()
        assertTrue("No alerts should exist for healthy stock", alerts.isEmpty())

        // 3. Issue stock: 45 units (Leaves 15 -> REORDER_REQUIRED)
        stockOutDataSource.insertStockOutRecord(buildStockOut("SOR-1", 45))
        
        // 4. Evaluate and Verify Alert
        repository.evaluatePolicies(projectId)
        alerts = repository.observeAlerts(projectId).first()
        assertEquals(1, alerts.size)
        val alert = alerts[0]
        assertEquals(InventoryReorderAlertType.REORDER_REQUIRED, alert.alertType)
        assertEquals(15.0, alert.availableQuantity, 0.0)
        assertEquals(InventoryReorderAlertStatus.OPEN, alert.status)

        // 5. Acknowledge Alert
        repository.acknowledgeAlert(alert.alertId, "user-01", UserRole.WAREHOUSE)
        val acknowledgedAlert = repository.getAlert(alert.alertId, UserRole.WAREHOUSE)
        assertEquals(InventoryReorderAlertStatus.ACKNOWLEDGED, (acknowledgedAlert as DomainResult.Success).data.status)

        // 6. Replenish stock: 40 units (Leaves 15 + 40 = 55 -> Healthy)
        receivingDataSource.insertStockInRecord(buildStockIn("SIN-2", 40, "RL-2"))

        // 7. Evaluate and Verify Resolution
        repository.evaluatePolicies(projectId)
        alerts = repository.observeAlerts(projectId).first()
        assertEquals(1, alerts.size)
        assertEquals(InventoryReorderAlertStatus.RESOLVED, alerts[0].status)
        assertEquals("SYSTEM_AUTO_RESOLVE", alerts[0].resolvedBy)
    }

    private fun buildStockIn(id: String, qty: Int, lineId: String) = InventoryStockInRecord(
        stockInId = id,
        receivingId = "RCV-1",
        receivingLineId = lineId,
        projectId = projectId,
        inventoryProductId = productId,
        warehouseId = warehouseId,
        locationId = locationId,
        quantity = qty,
        unit = InventoryUnit.PCS,
        createdBy = "admin",
        createdAt = "2026-08-17T10:00:00Z"
    )

    private fun buildStockOut(id: String, qty: Int) = InventoryStockOutRecord(
        stockOutRecordId = id,
        stockOutId = "SO-1",
        stockOutLineId = "SL-1",
        projectId = projectId,
        inventoryProductId = productId,
        warehouseId = warehouseId,
        locationId = locationId,
        quantity = qty,
        unit = InventoryUnit.PCS,
        createdBy = "admin",
        createdAt = "2026-08-17T11:00:00Z"
    )
}
