package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcCostEntry
import com.sucharu.sucharupro.domain.model.qc.QcCostStatus
import com.sucharu.sucharupro.domain.model.qc.QcCostTimeActivityEvent
import com.sucharu.sucharupro.domain.model.qc.QcCostTimeReconciliation
import com.sucharu.sucharupro.domain.model.qc.QcCostTimeSnapshot
import com.sucharu.sucharupro.domain.model.qc.QcCostType
import com.sucharu.sucharupro.domain.model.qc.QcTimeEntry
import com.sucharu.sucharupro.domain.model.qc.QcTimeEntryType
import com.sucharu.sucharupro.domain.model.qc.QcTimeStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface contract for QC Cost & Time Reconciliation Foundation (Module 06 Step 08).
 */
interface QcCostTimeRepository {

    // ==========================================
    // Cost Entries
    // ==========================================

    fun observeCostEntries(): Flow<List<QcCostEntry>>

    fun observeCostEntriesForJob(productionJobId: String): Flow<List<QcCostEntry>>

    suspend fun getCostEntriesForJob(productionJobId: String): DomainResult<List<QcCostEntry>>

    suspend fun findCostEntryById(id: String): DomainResult<QcCostEntry>

    suspend fun createCostEntry(
        projectId: String,
        productionJobId: String,
        costType: QcCostType,
        description: String,
        quantity: Double,
        unitCost: Double,
        currency: String = "BDT",
        qcId: String? = null,
        inspectionChecklistId: String? = null,
        productionDefectId: String? = null,
        productionReworkId: String? = null,
        reQcId: String? = null,
        finalQcId: String? = null,
        recordedBy: String,
        recordedByName: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcCostEntry>

    suspend fun updateCostEntry(
        id: String,
        description: String,
        quantity: Double,
        unitCost: Double,
        adjustmentReason: String?,
        updatedBy: String,
        updatedByName: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcCostEntry>

    suspend fun changeCostStatus(
        id: String,
        targetStatus: QcCostStatus,
        notes: String?,
        actorId: String,
        actorName: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcCostEntry>

    // ==========================================
    // Time Entries
    // ==========================================

    fun observeTimeEntries(): Flow<List<QcTimeEntry>>

    fun observeTimeEntriesForJob(productionJobId: String): Flow<List<QcTimeEntry>>

    suspend fun getTimeEntriesForJob(productionJobId: String): DomainResult<List<QcTimeEntry>>

    suspend fun findTimeEntryById(id: String): DomainResult<QcTimeEntry>

    suspend fun createTimeEntry(
        projectId: String,
        productionJobId: String,
        entryType: QcTimeEntryType,
        actorId: String,
        actorName: String? = null,
        startedAt: String,
        endedAt: String? = null,
        durationMinutes: Long,
        qcId: String? = null,
        inspectionChecklistId: String? = null,
        productionDefectId: String? = null,
        productionReworkId: String? = null,
        reQcId: String? = null,
        finalQcId: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcTimeEntry>

    suspend fun updateTimeEntry(
        id: String,
        durationMinutes: Long,
        endedAt: String?,
        notes: String?,
        updatedBy: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcTimeEntry>

    suspend fun changeTimeStatus(
        id: String,
        targetStatus: QcTimeStatus,
        notes: String?,
        actorId: String,
        actorName: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcTimeEntry>

    // ==========================================
    // Reconciliation Operations
    // ==========================================

    fun observeReconciliations(): Flow<List<QcCostTimeReconciliation>>

    fun observeReconciliationForJob(productionJobId: String): Flow<QcCostTimeReconciliation?>

    suspend fun getReconciliation(productionJobId: String): DomainResult<QcCostTimeReconciliation?>

    suspend fun calculateReconciliation(
        productionJobId: String,
        plannedCost: Double,
        plannedMinutes: Long,
        reconciledBy: String,
        reconciledByName: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcCostTimeReconciliation>

    suspend fun adjustReconciliation(
        reconciliationId: String,
        adjustedPlannedCost: Double?,
        adjustedPlannedMinutes: Long?,
        adjustmentReason: String,
        adjustedBy: String,
        adjustedByName: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcCostTimeReconciliation>

    suspend fun lockReconciliation(
        reconciliationId: String,
        lockedBy: String,
        lockedByName: String? = null,
        lockNotes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcCostTimeSnapshot>

    // ==========================================
    // Snapshots
    // ==========================================

    fun observeSnapshots(): Flow<List<QcCostTimeSnapshot>>

    fun observeSnapshotForJob(productionJobId: String): Flow<QcCostTimeSnapshot?>

    suspend fun getSnapshot(productionJobId: String): DomainResult<QcCostTimeSnapshot?>

    suspend fun findSnapshotById(snapshotId: String): DomainResult<QcCostTimeSnapshot>

    // ==========================================
    // Audit History
    // ==========================================

    fun observeActivityEvents(productionJobId: String): Flow<List<QcCostTimeActivityEvent>>
}
