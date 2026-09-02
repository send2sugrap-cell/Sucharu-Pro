package com.sucharu.sucharupro.domain.repository.substratereservation

import com.sucharu.sucharupro.domain.model.substratereservation.BatchLotSelectionResult

/**
 * Repository interface for Substrate Batch/Lot Selection lifecycle.
 * Module 19 Step 03.
 */
interface SubstrateBatchSelectionRepository {
    suspend fun saveSelectionResult(result: BatchLotSelectionResult): BatchLotSelectionResult
    suspend fun getSelectionById(tenantId: String, selectionId: String): BatchLotSelectionResult?
    suspend fun getSelectionsByOrder(tenantId: String, orderId: String): List<BatchLotSelectionResult>
    suspend fun getSelectionsByJob(tenantId: String, executionJobId: String): List<BatchLotSelectionResult>
    suspend fun listAllSelections(tenantId: String, limit: Int = 50): List<BatchLotSelectionResult>
    suspend fun confirmSelection(
        tenantId: String,
        selectionId: String,
        confirmedBy: String,
        confirmedAt: Long = System.currentTimeMillis()
    ): Boolean
}
