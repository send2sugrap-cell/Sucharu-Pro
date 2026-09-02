package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.InventoryTraceabilityRepositoryImpl
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.inventory.traceability.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Repository integration tests for [InventoryTraceabilityRepository] (Module 07 Step 07).
 */
class InventoryTraceabilityRepositoryTest {

    private lateinit var repository: InventoryTraceabilityRepository
    private lateinit var traceabilityDataSource: FakeInventoryTraceabilityDataSource

    @Before
    fun setup() {
        traceabilityDataSource = FakeInventoryTraceabilityDataSource()
        repository = InventoryTraceabilityRepositoryImpl(
            traceabilityDataSource = traceabilityDataSource,
            productDataSource = FakeInventoryProductDataSource(),
            receivingDataSource = FakeInventoryReceivingDataSource(),
            stockOutDataSource = FakeInventoryStockOutDataSource(),
            stockTransferDataSource = FakeInventoryStockTransferDataSource(),
            stockAdjustmentDataSource = FakeInventoryStockAdjustmentDataSource()
        )
    }

    @Test
    fun `can create and retrieve batch`() = runBlocking {
        val batch = buildBatch("B1", "BATCH-01", "PRJ-01")
        repository.createBatch(batch, "actor-1", "Admin")

        val retrieved = repository.getBatchDetails("B1")
        assertNotNull(retrieved)
        assertEquals("BATCH-01", retrieved?.batchNo)
    }

    @Test
    fun `can create and retrieve lot`() = runBlocking {
        val lot = buildLot("L1", "LOT-01", "PRJ-01")
        repository.createLot(lot, "actor-1", "Admin")

        val retrieved = repository.getLotDetails("L1")
        assertNotNull(retrieved)
        assertEquals("LOT-01", retrieved?.lotNo)
    }

    @Test
    fun `can update batch status`() = runBlocking {
        val batch = buildBatch("B1", "BATCH-01", "PRJ-01")
        repository.createBatch(batch, "actor-1", "Admin")

        repository.updateBatchStatus("B1", InventoryTraceabilityStatus.HOLD, "actor-1", "Admin")

        val retrieved = repository.getBatchDetails("B1")
        assertEquals(InventoryTraceabilityStatus.HOLD, retrieved?.status)
    }

    @Test
    fun `search batches filters correctly`() = runBlocking {
        repository.createBatch(buildBatch("B1", "ALPHA", "PRJ-01"), "id", "name")
        repository.createBatch(buildBatch("B2", "BETA", "PRJ-01"), "id", "name")
        repository.createBatch(buildBatch("B3", "AL-PHO", "PRJ-01"), "id", "name")

        val results = repository.searchBatches("PRJ-01", "AL").first()
        assertEquals(2, results.size)
        assertTrue(results.any { it.batchNo == "ALPHA" })
        assertTrue(results.any { it.batchNo == "AL-PHO" })
    }

    @Test
    fun `search lots filters correctly`() = runBlocking {
        repository.createLot(buildLot("L1", "LX-100", "PRJ-01"), "id", "name")
        repository.createLot(buildLot("L2", "LX-200", "PRJ-01"), "id", "name")
        repository.createLot(buildLot("L3", "MX-300", "PRJ-01"), "id", "name")

        val results = repository.searchLots("PRJ-01", "LX").first()
        assertEquals(2, results.size)
    }

    @Test
    fun `link movement creates trace record`() = runBlocking {
        val record = InventoryTraceabilityRecord(
            traceRecordId = "T1",
            batchId = "B1",
            lotId = "L1",
            projectId = "PRJ-01",
            productId = "PROD-01",
            locationId = "LOC-01",
            movementRecordId = "REC-01",
            movementType = InventoryMovementType.STOCK_IN,
            quantity = 100.0,
            unit = InventoryUnit.PCS,
            actorId = "actor-1",
            timestamp = "2026-08-17T12:00:00Z"
        )

        repository.linkMovementToTraceability(record)

        val allRecords = repository.observeTraceRecords("PRJ-01").first()
        assertEquals(1, allRecords.size)
        assertEquals("T1", allRecords[0].traceRecordId)
    }

    private fun buildBatch(batchId: String, batchNo: String, projectId: String) = InventoryBatch(
        batchId = batchId,
        batchNo = batchNo,
        projectId = projectId,
        productId = "PROD-01",
        productionReferenceId = null,
        productionReferenceType = null,
        status = InventoryTraceabilityStatus.ACTIVE,
        createdAt = "2026-08-17T10:00:00Z"
    )

    private fun buildLot(lotId: String, lotNo: String, projectId: String) = InventoryLot(
        lotId = lotId,
        lotNo = lotNo,
        projectId = projectId,
        productId = "PROD-01",
        batchId = null,
        status = InventoryTraceabilityStatus.ACTIVE,
        createdAt = "2026-08-17T10:00:00Z"
    )
}
