package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcCostEntry
import com.sucharu.sucharupro.domain.model.qc.QcCostTimeActivityEvent
import com.sucharu.sucharupro.domain.model.qc.QcCostTimeReconciliation
import com.sucharu.sucharupro.domain.model.qc.QcCostTimeSnapshot
import com.sucharu.sucharupro.domain.model.qc.QcTimeEntry
import kotlinx.coroutines.flow.Flow

/**
 * Data source contract for QC Cost & Time Reconciliation foundation (Module 06 Step 08).
 */
interface QcCostTimeDataSource {

    // ==========================================
    // Cost Entries
    // ==========================================

    fun observeCostEntries(): Flow<List<QcCostEntry>>

    fun observeCostEntriesForJob(productionJobId: String): Flow<List<QcCostEntry>>

    suspend fun findCostEntryById(id: String): QcCostEntry?

    suspend fun insertCostEntry(entry: QcCostEntry): DomainResult<QcCostEntry>

    suspend fun updateCostEntry(entry: QcCostEntry): DomainResult<QcCostEntry>

    // ==========================================
    // Time Entries
    // ==========================================

    fun observeTimeEntries(): Flow<List<QcTimeEntry>>

    fun observeTimeEntriesForJob(productionJobId: String): Flow<List<QcTimeEntry>>

    suspend fun findTimeEntryById(id: String): QcTimeEntry?

    suspend fun insertTimeEntry(entry: QcTimeEntry): DomainResult<QcTimeEntry>

    suspend fun updateTimeEntry(entry: QcTimeEntry): DomainResult<QcTimeEntry>

    // ==========================================
    // Reconciliation
    // ==========================================

    fun observeReconciliations(): Flow<List<QcCostTimeReconciliation>>

    fun observeReconciliationForJob(productionJobId: String): Flow<QcCostTimeReconciliation?>

    suspend fun findReconciliationById(id: String): QcCostTimeReconciliation?

    suspend fun findReconciliationByJob(productionJobId: String): QcCostTimeReconciliation?

    suspend fun insertReconciliation(reconciliation: QcCostTimeReconciliation): DomainResult<QcCostTimeReconciliation>

    suspend fun updateReconciliation(reconciliation: QcCostTimeReconciliation): DomainResult<QcCostTimeReconciliation>

    // ==========================================
    // Snapshots
    // ==========================================

    fun observeSnapshots(): Flow<List<QcCostTimeSnapshot>>

    fun observeSnapshotForJob(productionJobId: String): Flow<QcCostTimeSnapshot?>

    suspend fun findSnapshotById(snapshotId: String): QcCostTimeSnapshot?

    suspend fun findSnapshotByJob(productionJobId: String): QcCostTimeSnapshot?

    suspend fun insertSnapshot(snapshot: QcCostTimeSnapshot): DomainResult<QcCostTimeSnapshot>

    // ==========================================
    // Audit Activity Events
    // ==========================================

    fun observeActivityEvents(productionJobId: String): Flow<List<QcCostTimeActivityEvent>>

    suspend fun insertActivityEvent(event: QcCostTimeActivityEvent): DomainResult<QcCostTimeActivityEvent>
}
