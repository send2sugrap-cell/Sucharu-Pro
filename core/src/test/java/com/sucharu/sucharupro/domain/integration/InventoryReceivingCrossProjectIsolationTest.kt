package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeInventoryLocationDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryProductDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryWarehouseDataSource
import com.sucharu.sucharupro.data.repository.InventoryReceivingRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationType
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseType
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceiving
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingLine
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryReceivingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Cross-project isolation integration tests (Module 07 Step 03).
 *
 * Validates that warehouse and location belonging to project PRJ-99
 * cannot be used in a receiving for project PRJ-01, and vice versa.
 */
class InventoryReceivingCrossProjectIsolationTest {

    private lateinit var warehouseDataSource: FakeInventoryWarehouseDataSource
    private lateinit var locationDataSource: FakeInventoryLocationDataSource
    private lateinit var productDataSource: FakeInventoryProductDataSource
    private lateinit var repository: InventoryReceivingRepository

    @Before
    fun setup() {
        runBlocking {
            val receivingDataSource = FakeInventoryReceivingDataSource()
            productDataSource = FakeInventoryProductDataSource()
            warehouseDataSource = FakeInventoryWarehouseDataSource()
            locationDataSource = FakeInventoryLocationDataSource()
            repository = InventoryReceivingRepositoryImpl(receivingDataSource, productDataSource, warehouseDataSource, locationDataSource)
            // Seed project PRJ-01
            warehouseDataSource.insertWarehouse(buildWarehouse("WH-01", "PRJ-01"))
            locationDataSource.insertLocation(buildLocation("LOC-01", "WH-01", "PRJ-01"))
            // Seed project PRJ-99 (different project)
            warehouseDataSource.insertWarehouse(buildWarehouse("WH-99", "PRJ-99"))
            locationDataSource.insertLocation(buildLocation("LOC-99", "WH-99", "PRJ-99"))
            productDataSource.insertProduct(buildProduct())
        }
    }

    @Test
    fun `creating receiving with warehouse in different project fails`() = runBlocking {
        val receiving = buildReceiving(projectId = "PRJ-01", warehouseId = "WH-99")
        val result = repository.createReceiving(receiving, UserRole.MANAGER)
        assertTrue(result is DomainResult.Error)
        val msg = (result as DomainResult.Error).message
        assertTrue(msg.contains("Cross-project") || msg.contains("forbidden") || msg.contains("'PRJ-99'"))
    }

    @Test
    fun `adding line with location from different project fails`() = runBlocking {
        repository.createReceiving(buildReceiving(projectId = "PRJ-01", warehouseId = "WH-01"), UserRole.MANAGER)
        val line = buildLine(receivingId = "RCV-001", projectId = "PRJ-01", warehouseId = "WH-01", locationId = "LOC-99")
        val result = repository.addReceivingLine(line, UserRole.MANAGER)
        assertTrue(result is DomainResult.Error)
        val msg = (result as DomainResult.Error).message
        assertTrue(msg.contains("Cross-project") || msg.contains("forbidden") || msg.contains("PRJ-99"))
    }

    @Test
    fun `observeReceivings only returns receivings for requested project`() = runBlocking {
        repository.createReceiving(buildReceiving("RCV-PRJ01", "PRJ-01", "WH-01"), UserRole.MANAGER)
        // Create a warehouse in PRJ-99 for the second receiving
        repository.createReceiving(buildReceiving("RCV-PRJ99", "PRJ-99", "WH-99", reference = "RCV-REF-099"), UserRole.MANAGER)
        val prj01 = repository.observeReceivings("PRJ-01").first()
        val prj99 = repository.observeReceivings("PRJ-99").first()
        assertEquals(1, prj01.size)
        assertEquals("RCV-PRJ01", prj01.first().receivingId)
        assertEquals(1, prj99.size)
        assertEquals("RCV-PRJ99", prj99.first().receivingId)
    }

    @Test
    fun `using location from correct project but wrong warehouse fails`() = runBlocking {
        // LOC-01 belongs to WH-01 (PRJ-01); WH-02 is also in PRJ-01 but different warehouse
        warehouseDataSource.insertWarehouse(buildWarehouse("WH-02", "PRJ-01", code = "WH002"))
        repository.createReceiving(buildReceiving("RCV-WH02", "PRJ-01", "WH-02"), UserRole.MANAGER)
        // Try to use LOC-01 (which belongs to WH-01) in a receiving for WH-02
        val line = buildLine("RCV-WH02", "PRJ-01", "WH-02", "LOC-01")
        val result = repository.addReceivingLine(line, UserRole.MANAGER)
        assertTrue(result is DomainResult.Error)
        val msg = (result as DomainResult.Error).message
        assertTrue(msg.contains("warehouseId") || msg.contains("WH-01") || msg.contains("WH-02"))
    }

    private fun buildReceiving(
        id: String = "RCV-001",
        projectId: String = "PRJ-01",
        warehouseId: String = "WH-01",
        reference: String = "RCV-REF-001"
    ) = InventoryReceiving(
        receivingId = id, projectId = projectId, receivingReference = reference,
        warehouseId = warehouseId, receivingDate = "2026-08-17", createdBy = "admin-01",
        createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z"
    )

    private fun buildLine(
        receivingId: String, projectId: String, warehouseId: String, locationId: String
    ) = InventoryReceivingLine(
        receivingLineId = "LINE-001", receivingId = receivingId, projectId = projectId,
        inventoryProductId = "PROD-01", warehouseId = warehouseId, locationId = locationId,
        unit = InventoryUnit.PCS, createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z"
    )

    private fun buildWarehouse(id: String, projectId: String, code: String = "WH001") = InventoryWarehouse(
        id = id, projectId = projectId, code = code, name = "Warehouse $id",
        type = InventoryWarehouseType.FINISHED_GOODS, createdBy = "admin-01",
        createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z"
    )

    private fun buildLocation(id: String, warehouseId: String, projectId: String) = InventoryLocation(
        id = id, projectId = projectId, warehouseId = warehouseId, code = id,
        name = "Location $id", type = InventoryLocationType.SHELF, createdBy = "admin-01",
        createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z"
    )

    private fun buildProduct() = InventoryProduct(
        id = "PROD-01", sku = "SKU-001", name = "Test Book", isActive = true, isStockTracked = true,
        createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z", createdBy = "admin-01"
    )
}
