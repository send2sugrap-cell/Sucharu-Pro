package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.InventoryStockAdjustmentRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.*
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustment
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * RBAC authorization tests for [InventoryStockAdjustmentRepository] (Module 07 Step 06).
 */
class InventoryStockAdjustmentRBACTest {

    private lateinit var repository: InventoryStockAdjustmentRepository

    @Before
    fun setup() {
        runBlocking {
            val adjustmentDataSource = FakeInventoryStockAdjustmentDataSource()
            val stockOutDataSource = FakeInventoryStockOutDataSource()
            val transferDataSource = FakeInventoryStockTransferDataSource()
            val receivingDataSource = FakeInventoryReceivingDataSource()
            val productDataSource = FakeInventoryProductDataSource()
            val warehouseDataSource = FakeInventoryWarehouseDataSource()
            val locationDataSource = FakeInventoryLocationDataSource()
            
            repository = InventoryStockAdjustmentRepositoryImpl(
                adjustmentDataSource = adjustmentDataSource,
                stockOutDataSource = stockOutDataSource,
                transferDataSource = transferDataSource,
                receivingDataSource = receivingDataSource,
                productDataSource = productDataSource,
                warehouseDataSource = warehouseDataSource,
                locationDataSource = locationDataSource
            )
            
            warehouseDataSource.insertWarehouse(buildWarehouse())
            locationDataSource.insertLocation(buildLocation())
            productDataSource.insertProduct(buildProduct())
        }
    }

    @Test
    fun `ADMIN can create adjustment`() = runBlocking {
        val result = repository.createStockAdjustment(buildAdjustment(), UserRole.ADMIN)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `MANAGER can create adjustment`() = runBlocking {
        val result = repository.createStockAdjustment(buildAdjustment(), UserRole.MANAGER)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `WAREHOUSE can create adjustment`() = runBlocking {
        val result = repository.createStockAdjustment(buildAdjustment(), UserRole.WAREHOUSE)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `STAFF cannot create adjustment`() = runBlocking {
        val result = repository.createStockAdjustment(buildAdjustment(), UserRole.STAFF)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("not authorized"))
    }

    @Test
    fun `only ADMIN and MANAGER can approve adjustment`() = runBlocking {
        repository.createStockAdjustment(buildAdjustment("ADJ-RBAC"), UserRole.ADMIN)
        val warehouseResult = repository.approveStockAdjustment("ADJ-RBAC", "wh", "2026-08-17T09:00:00Z", UserRole.WAREHOUSE)
        assertTrue(warehouseResult is DomainResult.Error)
        assertTrue((warehouseResult as DomainResult.Error).message.contains("not authorized"))
    }

    @Test
    fun `STAFF can view adjustments`() = runBlocking {
        repository.createStockAdjustment(buildAdjustment("ADJ-VIEW"), UserRole.ADMIN)
        val result = repository.getStockAdjustment("ADJ-VIEW", UserRole.STAFF)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `DESIGNER cannot view adjustments`() = runBlocking {
        repository.createStockAdjustment(buildAdjustment("ADJ-DENY"), UserRole.ADMIN)
        val result = repository.getStockAdjustment("ADJ-DENY", UserRole.DESIGNER)
        assertTrue(result is DomainResult.Error)
    }

    private fun buildAdjustment(id: String = "ADJ-001") = InventoryStockAdjustment(
        adjustmentId = id, projectId = "PRJ-01", adjustmentReference = id,
        warehouseId = "WH-01", adjustmentDate = "2026-08-17", createdBy = "admin",
        createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z"
    )

    private fun buildWarehouse() = InventoryWarehouse(
        id = "WH-01", projectId = "PRJ-01", code = "WH001", name = "Main WH",
        type = InventoryWarehouseType.FINISHED_GOODS, createdBy = "admin",
        createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z"
    )

    private fun buildLocation() = InventoryLocation(
        id = "LOC-01", projectId = "PRJ-01", warehouseId = "WH-01", code = "LOC-A1",
        name = "Shelf A1", type = InventoryLocationType.SHELF, createdBy = "admin",
        createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z"
    )

    private fun buildProduct() = InventoryProduct(
        id = "PROD-01", sku = "SKU-001", name = "Test Book", isActive = true, isStockTracked = true,
        createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z", createdBy = "admin"
    )
}
