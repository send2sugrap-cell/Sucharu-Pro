package com.sucharu.sucharupro.data.repository.substratereservation

import com.sucharu.sucharupro.data.datasource.substratereservation.SubstrateBatchSelectionDataSource
import com.sucharu.sucharupro.domain.model.substratereservation.BatchLotSelectionResult
import com.sucharu.sucharupro.domain.repository.substratereservation.SubstrateBatchSelectionRepository

/**
 * Implementation of SubstrateBatchSelectionRepository backed by SubstrateBatchSelectionDataSource.
 * Module 19 Step 03.
 */
class SubstrateBatchSelectionRepositoryImpl(
    private val dataSource: SubstrateBatchSelectionDataSource
) : SubstrateBatchSelectionRepository {

    override suspend fun saveSelectionResult(result: BatchLotSelectionResult): BatchLotSelectionResult {
        return dataSource.saveSelectionResult(result)
    }

    override suspend fun getSelectionById(tenantId: String, selectionId: String): BatchLotSelectionResult? {
        return dataSource.findSelectionById(tenantId, selectionId)
    }

    override suspend fun getSelectionsByOrder(tenantId: String, orderId: String): List<BatchLotSelectionResult> {
        return dataSource.findSelectionsByOrder(tenantId, orderId)
    }

    override suspend fun getSelectionsByJob(tenantId: String, executionJobId: String): List<BatchLotSelectionResult> {
        return dataSource.findSelectionsByJob(tenantId, executionJobId)
    }

    override suspend fun listAllSelections(tenantId: String, limit: Int): List<BatchLotSelectionResult> {
        return dataSource.listAllSelections(tenantId, limit)
    }

    override suspend fun confirmSelection(
        tenantId: String,
        selectionId: String,
        confirmedBy: String,
        confirmedAt: Long
    ): Boolean {
        return dataSource.updateSelectionConfirmation(tenantId, selectionId, true, confirmedBy, confirmedAt)
    }
}
