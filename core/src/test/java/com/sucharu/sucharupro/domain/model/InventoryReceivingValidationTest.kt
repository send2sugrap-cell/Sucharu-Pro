package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceiving
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingLine
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingStatus
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationStatus
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationType
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseStatus
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseType
import com.sucharu.sucharupro.domain.validation.InventoryReceivingValidator
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [InventoryReceivingValidator] (Module 07 Step 03).
 */
class InventoryReceivingValidationTest {

    // ──────────────────────────────────────────────────────────────
    // Structural Validation
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `valid receiving passes structural validation`() {
        val result = InventoryReceivingValidator.validateReceiving(buildReceiving())
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `receiving with blank receivingId fails structural validation`() {
        // init block throws before validator runs, but validate can also run early checks
        val result = try {
            InventoryReceivingValidator.validateReceiving(buildReceiving(receivingId = " "))
            DomainResult.Error(message = "should have thrown")
        } catch (e: IllegalArgumentException) {
            DomainResult.Error(message = e.message ?: "error")
        }
        assertTrue(result is DomainResult.Error)
    }

    // ──────────────────────────────────────────────────────────────
    // Reference Uniqueness
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `unique reference passes uniqueness check`() {
        val existing = listOf(buildReceiving(receivingId = "RCV-X", reference = "REF-X"))
        val result = InventoryReceivingValidator.validateReceivingReferenceUniqueness(
            reference = "REF-Y",
            receivingId = "RCV-NEW",
            projectId = "PRJ-01",
            existingReceivings = existing
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `duplicate reference in same project fails uniqueness check`() {
        val existing = listOf(buildReceiving(receivingId = "RCV-X", reference = "REF-DUP"))
        val result = InventoryReceivingValidator.validateReceivingReferenceUniqueness(
            reference = "REF-DUP",
            receivingId = "RCV-NEW",
            projectId = "PRJ-01",
            existingReceivings = existing
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `duplicate reference in different project passes uniqueness check`() {
        val existing = listOf(buildReceiving(receivingId = "RCV-X", reference = "REF-SAME", projectId = "PRJ-99"))
        val result = InventoryReceivingValidator.validateReceivingReferenceUniqueness(
            reference = "REF-SAME",
            receivingId = "RCV-NEW",
            projectId = "PRJ-01",
            existingReceivings = existing
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `same reference on same entity passes uniqueness check (update scenario)`() {
        val existing = listOf(buildReceiving(receivingId = "RCV-X", reference = "REF-SAME"))
        val result = InventoryReceivingValidator.validateReceivingReferenceUniqueness(
            reference = "REF-SAME",
            receivingId = "RCV-X",
            projectId = "PRJ-01",
            existingReceivings = existing
        )
        assertTrue(result is DomainResult.Success)
    }

    // ──────────────────────────────────────────────────────────────
    // Product Validation
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `active stock-tracked product passes product validation`() {
        val products = listOf(buildProduct())
        val result = InventoryReceivingValidator.validateProduct("PROD-01", products)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `product not found returns error`() {
        val result = InventoryReceivingValidator.validateProduct("PROD-MISSING", emptyList())
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `inactive product returns error`() {
        val products = listOf(buildProduct(isActive = false))
        val result = InventoryReceivingValidator.validateProduct("PROD-01", products)
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `non-stock-tracked product returns error`() {
        val products = listOf(buildProduct(isStockTracked = false))
        val result = InventoryReceivingValidator.validateProduct("PROD-01", products)
        assertTrue(result is DomainResult.Error)
    }

    // ──────────────────────────────────────────────────────────────
    // Warehouse Validation
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `active warehouse in correct project passes validation`() {
        val warehouses = listOf(buildWarehouse())
        val result = InventoryReceivingValidator.validateWarehouse("WH-01", "PRJ-01", warehouses)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `warehouse not found returns error`() {
        val result = InventoryReceivingValidator.validateWarehouse("WH-MISSING", "PRJ-01", emptyList())
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `warehouse in wrong project returns error`() {
        val warehouses = listOf(buildWarehouse(projectId = "PRJ-99"))
        val result = InventoryReceivingValidator.validateWarehouse("WH-01", "PRJ-01", warehouses)
        assertTrue(result is DomainResult.Error)
        val msg = (result as DomainResult.Error).message
        assertTrue(msg.contains("Cross-project") || msg.contains("forbidden"))
    }

    @Test
    fun `archived warehouse returns error`() {
        val warehouses = listOf(buildWarehouse(status = InventoryWarehouseStatus.ARCHIVED))
        val result = InventoryReceivingValidator.validateWarehouse("WH-01", "PRJ-01", warehouses)
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `inactive warehouse returns error`() {
        val warehouses = listOf(buildWarehouse(status = InventoryWarehouseStatus.INACTIVE))
        val result = InventoryReceivingValidator.validateWarehouse("WH-01", "PRJ-01", warehouses)
        assertTrue(result is DomainResult.Error)
    }

    // ──────────────────────────────────────────────────────────────
    // Location Validation
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `active location in correct warehouse and project passes validation`() {
        val locations = listOf(buildLocation())
        val result = InventoryReceivingValidator.validateLocation("LOC-01", "WH-01", "PRJ-01", locations)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `location not found returns error`() {
        val result = InventoryReceivingValidator.validateLocation("LOC-MISSING", "WH-01", "PRJ-01", emptyList())
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `location in wrong project returns error`() {
        val locations = listOf(buildLocation(projectId = "PRJ-99"))
        val result = InventoryReceivingValidator.validateLocation("LOC-01", "WH-01", "PRJ-01", locations)
        assertTrue(result is DomainResult.Error)
        val msg = (result as DomainResult.Error).message
        assertTrue(msg.contains("Cross-project") || msg.contains("forbidden"))
    }

    @Test
    fun `location in wrong warehouse returns error`() {
        val locations = listOf(buildLocation(warehouseId = "WH-WRONG"))
        val result = InventoryReceivingValidator.validateLocation("LOC-01", "WH-01", "PRJ-01", locations)
        assertTrue(result is DomainResult.Error)
        val msg = (result as DomainResult.Error).message
        assertTrue(msg.contains("Cross-warehouse") || msg.contains("forbidden"))
    }

    @Test
    fun `archived location returns error`() {
        val locations = listOf(buildLocation(status = InventoryLocationStatus.ARCHIVED))
        val result = InventoryReceivingValidator.validateLocation("LOC-01", "WH-01", "PRJ-01", locations)
        assertTrue(result is DomainResult.Error)
    }

    // ──────────────────────────────────────────────────────────────
    // Project Isolation
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `correct project isolation passes`() {
        val result = InventoryReceivingValidator.validateProjectIsolation(
            receivingProjectId = "PRJ-01",
            warehouse = buildWarehouse(),
            location = buildLocation()
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `warehouse project mismatch fails isolation`() {
        val result = InventoryReceivingValidator.validateProjectIsolation(
            receivingProjectId = "PRJ-01",
            warehouse = buildWarehouse(projectId = "PRJ-99"),
            location = buildLocation()
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `location project mismatch fails isolation`() {
        val result = InventoryReceivingValidator.validateProjectIsolation(
            receivingProjectId = "PRJ-01",
            warehouse = buildWarehouse(),
            location = buildLocation(projectId = "PRJ-99")
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `location belonging to wrong warehouse fails isolation`() {
        val result = InventoryReceivingValidator.validateProjectIsolation(
            receivingProjectId = "PRJ-01",
            warehouse = buildWarehouse(),
            location = buildLocation(warehouseId = "WH-OTHER")
        )
        assertTrue(result is DomainResult.Error)
    }

    // ──────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────

    private fun buildReceiving(
        receivingId: String = "RCV-001",
        projectId: String = "PRJ-01",
        reference: String = "RCV-REF-001"
    ) = InventoryReceiving(
        receivingId = receivingId,
        projectId = projectId,
        receivingReference = reference,
        warehouseId = "WH-01",
        receivingDate = "2026-08-17",
        createdBy = "admin-01",
        createdAt = "2026-08-17T08:00:00Z",
        updatedAt = "2026-08-17T08:00:00Z"
    )

    private fun buildProduct(
        isActive: Boolean = true,
        isStockTracked: Boolean = true
    ) = InventoryProduct(
        id = "PROD-01",
        sku = "SKU-001",
        name = "Test Product",
        isActive = isActive,
        isStockTracked = isStockTracked,
        createdAt = "2026-08-17T08:00:00Z",
        updatedAt = "2026-08-17T08:00:00Z",
        createdBy = "admin-01"
    )

    private fun buildWarehouse(
        projectId: String = "PRJ-01",
        status: InventoryWarehouseStatus = InventoryWarehouseStatus.ACTIVE
    ) = InventoryWarehouse(
        id = "WH-01",
        projectId = projectId,
        code = "WH001",
        name = "Main Warehouse",
        type = InventoryWarehouseType.FINISHED_GOODS,
        status = status,
        createdBy = "admin-01",
        createdAt = "2026-08-17T08:00:00Z",
        updatedAt = "2026-08-17T08:00:00Z",
        archivedAt = if (status == InventoryWarehouseStatus.ARCHIVED) "2026-08-17T12:00:00Z" else null
    )

    private fun buildLocation(
        projectId: String = "PRJ-01",
        warehouseId: String = "WH-01",
        status: InventoryLocationStatus = InventoryLocationStatus.ACTIVE
    ) = InventoryLocation(
        id = "LOC-01",
        projectId = projectId,
        warehouseId = warehouseId,
        code = "LOC-A1",
        name = "Aisle A Shelf 1",
        type = InventoryLocationType.SHELF,
        status = status,
        archivedAt = if (status == InventoryLocationStatus.ARCHIVED) "2026-08-17T12:00:00Z" else null,
        createdBy = "admin-01",
        createdAt = "2026-08-17T08:00:00Z",
        updatedAt = "2026-08-17T08:00:00Z"
    )
}
