package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.FinancialReportActivityEvent
import com.sucharu.sucharupro.domain.model.finance.FinancialReportExportRequest
import com.sucharu.sucharupro.domain.model.finance.FinancialReportSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Data source interface for Financial Reporting audit snapshots, activity events, and exports.
 */
interface FinancialReportingDataSource {

    suspend fun saveSnapshot(snapshot: FinancialReportSnapshot)

    suspend fun getSnapshotById(snapshotId: String): FinancialReportSnapshot?

    suspend fun getSnapshotByRequestId(projectId: String, snapshotRequestId: String): FinancialReportSnapshot?

    fun observeSnapshots(projectId: String): Flow<List<FinancialReportSnapshot>>

    suspend fun insertActivityEvent(event: FinancialReportActivityEvent)

    fun observeActivityEvents(projectId: String): Flow<List<FinancialReportActivityEvent>>

    suspend fun recordExportRequest(request: FinancialReportExportRequest)
}
