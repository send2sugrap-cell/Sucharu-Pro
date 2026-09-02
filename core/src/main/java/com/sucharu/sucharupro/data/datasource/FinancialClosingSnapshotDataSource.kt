package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.FinancialPeriodClosingSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Data Source contract for immutable Financial Period Closing Snapshot persistence (Module 09 Step 08).
 */
interface FinancialClosingSnapshotDataSource {
    suspend fun insertSnapshot(snapshot: FinancialPeriodClosingSnapshot): Boolean
    suspend fun getSnapshotById(snapshotId: String): FinancialPeriodClosingSnapshot?
    suspend fun getSnapshotByPeriod(periodId: String): FinancialPeriodClosingSnapshot?
    suspend fun getSnapshotsByProject(projectId: String): List<FinancialPeriodClosingSnapshot>
    fun observeSnapshots(projectId: String): Flow<List<FinancialPeriodClosingSnapshot>>
    suspend fun generateNextSnapshotNo(projectId: String): String
}
