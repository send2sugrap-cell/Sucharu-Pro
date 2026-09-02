package com.sucharu.sucharupro.data.datasource.substratereservation

import com.sucharu.sucharupro.domain.model.substratereservation.BatchLotSelectionResult

/**
 * Data source interface for persisting Substrate Batch/Lot Selection results and allocations.
 * Module 19 Step 03.
 */
interface SubstrateBatchSelectionDataSource {
    suspend fun saveSelectionResult(result: BatchLotSelectionResult): BatchLotSelectionResult
    suspend fun findSelectionById(tenantId: String, selectionId: String): BatchLotSelectionResult?
    suspend fun findSelectionsByOrder(tenantId: String, orderId: String): List<BatchLotSelectionResult>
    suspend fun findSelectionsByJob(tenantId: String, executionJobId: String): List<BatchLotSelectionResult>
    suspend fun listAllSelections(tenantId: String, limit: Int = 50): List<BatchLotSelectionResult>
    suspend fun updateSelectionConfirmation(
        tenantId: String,
        selectionId: String,
        isConfirmed: Boolean,
        confirmedBy: String?,
        confirmedAt: Long?
    ): Boolean
}
