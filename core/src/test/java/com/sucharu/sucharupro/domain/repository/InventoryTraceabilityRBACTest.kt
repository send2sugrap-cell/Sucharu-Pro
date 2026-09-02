package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.InventoryTraceabilityRepositoryImpl
import com.sucharu.sucharupro.domain.model.inventory.traceability.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * RBAC authorization tests for [InventoryTraceabilityRepository] (Module 07 Step 07).
 */
class InventoryTraceabilityRBACTest {

    private lateinit var repository: InventoryTraceabilityRepository

    @Before
    fun setup() {
        repository = InventoryTraceabilityRepositoryImpl(
            traceabilityDataSource = FakeInventoryTraceabilityDataSource(),
            productDataSource = FakeInventoryProductDataSource(),
            receivingDataSource = FakeInventoryReceivingDataSource(),
            stockOutDataSource = FakeInventoryStockOutDataSource(),
            stockTransferDataSource = FakeInventoryStockTransferDataSource(),
            stockAdjustmentDataSource = FakeInventoryStockAdjustmentDataSource()
        )
    }

    @Test
    fun `ADMIN can register batch`() = runBlocking {
        repository.createBatch(buildBatch("B1", "PRJ-01"), "id", "name", UserRole.ADMIN)
        assertTrue(repository.getBatchDetails("B1") != null)
    }

    @Test
    fun `WAREHOUSE can register batch`() = runBlocking {
        repository.createBatch(buildBatch("B1", "PRJ-01"), "id", "name", UserRole.WAREHOUSE)
        assertTrue(repository.getBatchDetails("B1") != null)
    }

    @Test
    fun `STAFF cannot register batch`() = runBlocking {
        repository.createBatch(buildBatch("B1", "PRJ-01"), "id", "name", UserRole.STAFF)
        assertTrue(repository.getBatchDetails("B1") == null)
    }

    @Test
    fun `DESIGNER cannot register batch`() = runBlocking {
        repository.createBatch(buildBatch("B1", "PRJ-01"), "id", "name", UserRole.DESIGNER)
        assertTrue(repository.getBatchDetails("B1") == null)
    }

    @Test
    fun `ADMIN can change batch status`() = runBlocking {
        repository.createBatch(buildBatch("B1", "PRJ-01"), "id", "name", UserRole.ADMIN)
        repository.updateBatchStatus("B1", InventoryTraceabilityStatus.HOLD, "id", "name", UserRole.ADMIN)
        assertTrue(repository.getBatchDetails("B1")?.status == InventoryTraceabilityStatus.HOLD)
    }

    @Test
    fun `STAFF cannot change batch status`() = runBlocking {
        repository.createBatch(buildBatch("B1", "PRJ-01"), "id", "name", UserRole.ADMIN)
        repository.updateBatchStatus("B1", InventoryTraceabilityStatus.HOLD, "id", "name", UserRole.STAFF)
        assertTrue(repository.getBatchDetails("B1")?.status == InventoryTraceabilityStatus.ACTIVE)
    }

    @Test
    fun `STAFF cannot view trace history`() = runBlocking {
        val history = repository.getTraceHistory("B1", "BATCH", UserRole.STAFF)
        assertTrue(history.isEmpty())
    }

    private fun buildBatch(batchId: String, projectId: String) = InventoryBatch(
        batchId = batchId,
        batchNo = "B-NO",
        projectId = projectId,
        productId = "PROD-01",
        productionReferenceId = null,
        productionReferenceType = null,
        status = InventoryTraceabilityStatus.ACTIVE,
        createdAt = "2026-08-17T10:00:00Z"
    )
}
