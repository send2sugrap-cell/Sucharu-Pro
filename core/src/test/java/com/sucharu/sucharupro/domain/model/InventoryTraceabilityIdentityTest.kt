package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryBatch
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryLot
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryTraceabilityStatus
import com.sucharu.sucharupro.domain.validation.InventoryTraceabilityValidator
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Identity and uniqueness rule tests for Batch & Lot numbers (Module 07 Step 07).
 */
class InventoryTraceabilityIdentityTest {

    @Test
    fun `batch number must be unique within a project`() {
        val existing = listOf(
            buildBatch(batchId = "B1", batchNo = "BATCH-001", projectId = "PRJ-01")
        )

        // Same project, same batch number -> Error
        val result = InventoryTraceabilityValidator.validateBatchUniqueness(
            batchNo = "BATCH-001",
            projectId = "PRJ-01",
            currentBatchId = null,
            existingBatches = existing
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("already exists"))

        // Different project, same batch number -> Success
        val resultDiffPrj = InventoryTraceabilityValidator.validateBatchUniqueness(
            batchNo = "BATCH-001",
            projectId = "PRJ-02",
            currentBatchId = null,
            existingBatches = existing
        )
        assertTrue(resultDiffPrj is DomainResult.Success)

        // Same project, different batch number -> Success
        val resultDiffNo = InventoryTraceabilityValidator.validateBatchUniqueness(
            batchNo = "BATCH-002",
            projectId = "PRJ-01",
            currentBatchId = null,
            existingBatches = existing
        )
        assertTrue(resultDiffNo is DomainResult.Success)
    }

    @Test
    fun `lot number must be unique within a project`() {
        val existing = listOf(
            buildLot(lotId = "L1", lotNo = "LOT-001", projectId = "PRJ-01")
        )

        // Same project, same lot number -> Error
        val result = InventoryTraceabilityValidator.validateLotUniqueness(
            lotNo = "LOT-001",
            projectId = "PRJ-01",
            currentLotId = null,
            existingLots = existing
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("already exists"))

        // Different project, same lot number -> Success
        val resultDiffPrj = InventoryTraceabilityValidator.validateLotUniqueness(
            lotNo = "LOT-001",
            projectId = "PRJ-02",
            currentLotId = null,
            existingLots = existing
        )
        assertTrue(resultDiffPrj is DomainResult.Success)
    }

    @Test
    fun `batch uniqueness is case insensitive`() {
        val existing = listOf(
            buildBatch(batchId = "B1", batchNo = "batch-001", projectId = "PRJ-01")
        )

        val result = InventoryTraceabilityValidator.validateBatchUniqueness(
            batchNo = "BATCH-001",
            projectId = "PRJ-01",
            currentBatchId = null,
            existingBatches = existing
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `batch uniqueness ignores leading and trailing spaces`() {
        val existing = listOf(
            buildBatch(batchId = "B1", batchNo = "BATCH-001", projectId = "PRJ-01")
        )

        val result = InventoryTraceabilityValidator.validateBatchUniqueness(
            batchNo = " BATCH-001 ",
            projectId = "PRJ-01",
            currentBatchId = null,
            existingBatches = existing
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `project isolation violation detected`() {
        val batch = buildBatch(batchId = "B1", batchNo = "B-01", projectId = "PRJ-01")
        
        // Correct project and product -> Success
        val successResult = InventoryTraceabilityValidator.validateProjectIsolation(
            projectId = "PRJ-01",
            productId = "PROD-01",
            batch = batch
        )
        assertTrue(successResult is DomainResult.Success)

        // Wrong project -> Error
        val wrongPrjResult = InventoryTraceabilityValidator.validateProjectIsolation(
            projectId = "PRJ-02",
            productId = "PROD-01",
            batch = batch
        )
        assertTrue(wrongPrjResult is DomainResult.Error)

        // Wrong product -> Error
        val wrongProdResult = InventoryTraceabilityValidator.validateProjectIsolation(
            projectId = "PRJ-01",
            productId = "PROD-02",
            batch = batch
        )
        assertTrue(wrongProdResult is DomainResult.Error)
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
