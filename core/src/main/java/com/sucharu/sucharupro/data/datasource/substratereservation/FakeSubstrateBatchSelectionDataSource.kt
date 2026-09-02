package com.sucharu.sucharupro.data.datasource.substratereservation

import com.sucharu.sucharupro.domain.model.substratereservation.BatchLotSelectionResult
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory multi-tenant implementation of SubstrateBatchSelectionDataSource for testing.
 * Module 19 Step 03.
 */
class FakeSubstrateBatchSelectionDataSource : SubstrateBatchSelectionDataSource {

    private val selectionMap = ConcurrentHashMap<String, MutableMap<String, BatchLotSelectionResult>>()

    private fun getTenantStore(tenantId: String): MutableMap<String, BatchLotSelectionResult> {
        return selectionMap.computeIfAbsent(tenantId) { ConcurrentHashMap() }
    }

    override suspend fun saveSelectionResult(result: BatchLotSelectionResult): BatchLotSelectionResult {
        getTenantStore(result.tenantId)[result.selectionId] = result
        return result
    }

    override suspend fun findSelectionById(tenantId: String, selectionId: String): BatchLotSelectionResult? {
        return getTenantStore(tenantId)[selectionId]
    }

    override suspend fun findSelectionsByOrder(tenantId: String, orderId: String): List<BatchLotSelectionResult> {
        return getTenantStore(tenantId).values
            .filter { it.specification.orderId == orderId }
            .sortedByDescending { it.selectedAt }
    }

    override suspend fun findSelectionsByJob(tenantId: String, executionJobId: String): List<BatchLotSelectionResult> {
        return getTenantStore(tenantId).values
            .filter { it.specification.executionJobId == executionJobId }
            .sortedByDescending { it.selectedAt }
    }

    override suspend fun listAllSelections(tenantId: String, limit: Int): List<BatchLotSelectionResult> {
        return getTenantStore(tenantId).values
            .sortedByDescending { it.selectedAt }
            .take(limit)
    }

    override suspend fun updateSelectionConfirmation(
        tenantId: String,
        selectionId: String,
        isConfirmed: Boolean,
        confirmedBy: String?,
        confirmedAt: Long?
    ): Boolean {
        val existing = getTenantStore(tenantId)[selectionId] ?: return false
        val updated = existing.copy(
            isConfirmedAndAllocated = isConfirmed,
            confirmedBy = confirmedBy,
            confirmedAt = confirmedAt
        )
        getTenantStore(tenantId)[selectionId] = updated
        return true
    }
}
