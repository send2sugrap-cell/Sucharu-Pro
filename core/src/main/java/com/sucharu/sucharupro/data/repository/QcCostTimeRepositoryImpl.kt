package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.FinalQcDataSource
import com.sucharu.sucharupro.data.datasource.ProductionDefectDataSource
import com.sucharu.sucharupro.data.datasource.ProductionJobDataSource
import com.sucharu.sucharupro.data.datasource.ProductionQcDataSource
import com.sucharu.sucharupro.data.datasource.ProductionReQcDataSource
import com.sucharu.sucharupro.data.datasource.ProductionReworkDataSource
import com.sucharu.sucharupro.data.datasource.QcChecklistDataSource
import com.sucharu.sucharupro.data.datasource.QcCostTimeDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcCostEntry
import com.sucharu.sucharupro.domain.model.qc.QcCostStatus
import com.sucharu.sucharupro.domain.model.qc.QcCostTimeActivityEvent
import com.sucharu.sucharupro.domain.model.qc.QcCostTimeActivityType
import com.sucharu.sucharupro.domain.model.qc.QcCostTimeReconciliation
import com.sucharu.sucharupro.domain.model.qc.QcCostTimeSnapshot
import com.sucharu.sucharupro.domain.model.qc.QcCostType
import com.sucharu.sucharupro.domain.model.qc.QcTimeEntry
import com.sucharu.sucharupro.domain.model.qc.QcTimeEntryType
import com.sucharu.sucharupro.domain.model.qc.QcTimeStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.QcCostTimeRepository
import com.sucharu.sucharupro.domain.validation.QcCostEntryValidator
import com.sucharu.sucharupro.domain.validation.QcCostTimeReconciliationValidator
import com.sucharu.sucharupro.domain.validation.QcTimeEntryValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Mutex-protected thread-safe implementation of [QcCostTimeRepository] (Module 06 Step 08).
 */
class QcCostTimeRepositoryImpl(
    private val costTimeDataSource: QcCostTimeDataSource,
    private val productionJobDataSource: ProductionJobDataSource? = null,
    private val qcDataSource: ProductionQcDataSource? = null,
    private val checklistDataSource: QcChecklistDataSource? = null,
    private val defectDataSource: ProductionDefectDataSource? = null,
    private val reworkDataSource: ProductionReworkDataSource? = null,
    private val reQcDataSource: ProductionReQcDataSource? = null,
    private val finalQcDataSource: FinalQcDataSource? = null
) : QcCostTimeRepository {

    private val mutex = Mutex()

    private suspend fun recordActivity(
        productionJobId: String,
        projectId: String,
        actorId: String,
        actorName: String? = null,
        activityType: QcCostTimeActivityType,
        notes: String? = null,
        costEntryId: String? = null,
        timeEntryId: String? = null,
        reconciliationId: String? = null,
        snapshotId: String? = null,
        timestamp: String,
        metadata: Map<String, String> = emptyMap()
    ) {
        val event = QcCostTimeActivityEvent(
            eventId = "act-ct-" + UUID.randomUUID().toString(),
            productionJobId = productionJobId,
            projectId = projectId,
            costEntryId = costEntryId,
            timeEntryId = timeEntryId,
            reconciliationId = reconciliationId,
            snapshotId = snapshotId,
            actorId = actorId,
            actorName = actorName,
            activityType = activityType,
            notes = notes,
            timestamp = timestamp,
            metadata = metadata
        )
        costTimeDataSource.insertActivityEvent(event)
    }

    // ==========================================
    // Cost Entries
    // ==========================================

    override fun observeCostEntries(): Flow<List<QcCostEntry>> =
        costTimeDataSource.observeCostEntries()

    override fun observeCostEntriesForJob(productionJobId: String): Flow<List<QcCostEntry>> =
        costTimeDataSource.observeCostEntriesForJob(productionJobId)

    override suspend fun getCostEntriesForJob(productionJobId: String): DomainResult<List<QcCostEntry>> {
        val entries = costTimeDataSource.observeCostEntriesForJob(productionJobId).first()
        return DomainResult.Success(entries)
    }

    override suspend fun findCostEntryById(id: String): DomainResult<QcCostEntry> {
        val entry = costTimeDataSource.findCostEntryById(id)
            ?: return DomainResult.Error(message = "QC Cost entry not found: $id")
        return DomainResult.Success(entry)
    }

    override suspend fun createCostEntry(
        projectId: String,
        productionJobId: String,
        costType: QcCostType,
        description: String,
        quantity: Double,
        unitCost: Double,
        currency: String,
        qcId: String?,
        inspectionChecklistId: String?,
        productionDefectId: String?,
        productionReworkId: String?,
        reQcId: String?,
        finalQcId: String?,
        recordedBy: String,
        recordedByName: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcCostEntry> = mutex.withLock {
        // 1. RBAC Check
        val rbac = QcCostTimeReconciliationValidator.validateRecordPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        // 2. Parameter Validation
        val paramVal = QcCostEntryValidator.validateCreation(
            projectId = projectId,
            productionJobId = productionJobId,
            costType = costType,
            description = description,
            quantity = quantity,
            unitCost = unitCost,
            currency = currency,
            recordedBy = recordedBy,
            recordedAt = timestamp
        )
        if (paramVal is DomainResult.Error) return paramVal

        // 3. Job Validation if DataSource available
        if (productionJobDataSource != null) {
            val jobResult = productionJobDataSource.fetchJobById(productionJobId)
            if (jobResult is DomainResult.Error) {
                return DomainResult.Error(message = "Referenced production job not found: $productionJobId")
            }
            val job = (jobResult as DomainResult.Success).data
            if (job.orderId != projectId) {
                return DomainResult.Error(message = "Project ID mismatch: Job belongs to ${job.orderId}, but $projectId provided.")
            }
        }

        // 4. Cross-Job Source Reference Checks
        if (qcId != null && qcDataSource != null) {
            val qc = qcDataSource.fetchQcById(qcId)
            if (qc is DomainResult.Success && qc.data.productionJobId != productionJobId) {
                return DomainResult.Error(
                    message = "Cross-job reference violation: QC inspection '$qcId' belongs to job '${qc.data.productionJobId}', not '$productionJobId'."
                )
            }
        }

        if (productionDefectId != null && defectDataSource != null) {
            val defect = defectDataSource.fetchDefectById(productionDefectId)
            if (defect is DomainResult.Success && defect.data.productionJobId != productionJobId) {
                return DomainResult.Error(
                    message = "Cross-job reference violation: Defect '$productionDefectId' belongs to job '${defect.data.productionJobId}', not '$productionJobId'."
                )
            }
        }

        if (productionReworkId != null && reworkDataSource != null) {
            val rework = reworkDataSource.fetchReworkById(productionReworkId)
            if (rework is DomainResult.Success && rework.data.productionJobId != productionJobId) {
                return DomainResult.Error(
                    message = "Cross-job reference violation: Rework '$productionReworkId' belongs to job '${rework.data.productionJobId}', not '$productionJobId'."
                )
            }
        }

        if (reQcId != null && reQcDataSource != null) {
            val reQc = reQcDataSource.fetchReQcById(reQcId)
            if (reQc is DomainResult.Success && reQc.data.productionJobId != productionJobId) {
                return DomainResult.Error(
                    message = "Cross-job reference violation: Re-QC '$reQcId' belongs to job '${reQc.data.productionJobId}', not '$productionJobId'."
                )
            }
        }

        if (finalQcId != null && finalQcDataSource != null) {
            val finalQc = finalQcDataSource.findFinalQcById(finalQcId)
            if (finalQc != null && finalQc.productionJobId != productionJobId) {
                return DomainResult.Error(
                    message = "Cross-job reference violation: Final QC '$finalQcId' belongs to job '${finalQc.productionJobId}', not '$productionJobId'."
                )
            }
        }

        val id = "qcc-" + UUID.randomUUID().toString()
        val entry = QcCostEntry(
            id = id,
            productionJobId = productionJobId,
            projectId = projectId,
            qcId = qcId,
            inspectionChecklistId = inspectionChecklistId,
            productionDefectId = productionDefectId,
            productionReworkId = productionReworkId,
            reQcId = reQcId,
            finalQcId = finalQcId,
            costType = costType,
            description = description,
            quantity = quantity,
            unitCost = unitCost,
            totalCost = quantity * unitCost,
            currency = currency,
            status = QcCostStatus.RECORDED,
            recordedBy = recordedBy,
            recordedByName = recordedByName,
            recordedAt = timestamp,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        costTimeDataSource.insertCostEntry(entry)

        recordActivity(
            productionJobId = productionJobId,
            projectId = projectId,
            actorId = recordedBy,
            actorName = recordedByName,
            activityType = QcCostTimeActivityType.QC_COST_ENTRY_CREATED,
            notes = "Recorded ${entry.costType.defaultLabel}: ${entry.totalCost} $currency",
            costEntryId = id,
            timestamp = timestamp
        )

        return DomainResult.Success(entry)
    }

    override suspend fun updateCostEntry(
        id: String,
        description: String,
        quantity: Double,
        unitCost: Double,
        adjustmentReason: String?,
        updatedBy: String,
        updatedByName: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcCostEntry> = mutex.withLock {
        val rbac = QcCostTimeReconciliationValidator.validateAdjustmentPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = costTimeDataSource.findCostEntryById(id)
            ?: return DomainResult.Error(message = "QC Cost entry not found: $id")

        val immutability = QcCostEntryValidator.validateImmutability(current)
        if (immutability is DomainResult.Error) return immutability

        if (quantity <= 0.0) {
            return DomainResult.Error(message = "Quantity must be greater than zero.")
        }
        if (unitCost < 0.0) {
            return DomainResult.Error(message = "Unit cost cannot be negative.")
        }

        val updated = current.copy(
            description = description,
            quantity = quantity,
            unitCost = unitCost,
            totalCost = quantity * unitCost,
            status = if (current.status == QcCostStatus.RECONCILED) QcCostStatus.ADJUSTED else current.status,
            adjustmentReason = adjustmentReason ?: current.adjustmentReason,
            updatedAt = timestamp
        )

        costTimeDataSource.updateCostEntry(updated)

        recordActivity(
            productionJobId = current.productionJobId,
            projectId = current.projectId,
            actorId = updatedBy,
            actorName = updatedByName,
            activityType = QcCostTimeActivityType.QC_COST_ENTRY_UPDATED,
            notes = "Updated cost entry. Reason: $adjustmentReason",
            costEntryId = id,
            timestamp = timestamp
        )

        return DomainResult.Success(updated)
    }

    override suspend fun changeCostStatus(
        id: String,
        targetStatus: QcCostStatus,
        notes: String?,
        actorId: String,
        actorName: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcCostEntry> = mutex.withLock {
        val rbac = QcCostTimeReconciliationValidator.validateAdjustmentPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = costTimeDataSource.findCostEntryById(id)
            ?: return DomainResult.Error(message = "QC Cost entry not found: $id")

        val transition = QcCostEntryValidator.validateStatusTransition(current.status, targetStatus)
        if (transition is DomainResult.Error) return transition

        val updated = current.copy(
            status = targetStatus,
            updatedAt = timestamp
        )
        costTimeDataSource.updateCostEntry(updated)

        recordActivity(
            productionJobId = current.productionJobId,
            projectId = current.projectId,
            actorId = actorId,
            actorName = actorName,
            activityType = QcCostTimeActivityType.QC_COST_ENTRY_UPDATED,
            notes = "Changed status to ${targetStatus.defaultLabel}. $notes",
            costEntryId = id,
            timestamp = timestamp
        )

        return DomainResult.Success(updated)
    }

    // ==========================================
    // Time Entries
    // ==========================================

    override fun observeTimeEntries(): Flow<List<QcTimeEntry>> =
        costTimeDataSource.observeTimeEntries()

    override fun observeTimeEntriesForJob(productionJobId: String): Flow<List<QcTimeEntry>> =
        costTimeDataSource.observeTimeEntriesForJob(productionJobId)

    override suspend fun getTimeEntriesForJob(productionJobId: String): DomainResult<List<QcTimeEntry>> {
        val entries = costTimeDataSource.observeTimeEntriesForJob(productionJobId).first()
        return DomainResult.Success(entries)
    }

    override suspend fun findTimeEntryById(id: String): DomainResult<QcTimeEntry> {
        val entry = costTimeDataSource.findTimeEntryById(id)
            ?: return DomainResult.Error(message = "QC Time entry not found: $id")
        return DomainResult.Success(entry)
    }

    override suspend fun createTimeEntry(
        projectId: String,
        productionJobId: String,
        entryType: QcTimeEntryType,
        actorId: String,
        actorName: String?,
        startedAt: String,
        endedAt: String?,
        durationMinutes: Long,
        qcId: String?,
        inspectionChecklistId: String?,
        productionDefectId: String?,
        productionReworkId: String?,
        reQcId: String?,
        finalQcId: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcTimeEntry> = mutex.withLock {
        // 1. RBAC Check
        val rbac = QcCostTimeReconciliationValidator.validateRecordPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        // 2. Parameter Validation
        val paramVal = QcTimeEntryValidator.validateCreation(
            projectId = projectId,
            productionJobId = productionJobId,
            entryType = entryType,
            actorId = actorId,
            startedAt = startedAt,
            endedAt = endedAt,
            durationMinutes = durationMinutes
        )
        if (paramVal is DomainResult.Error) return paramVal

        // 3. Job Validation if DataSource available
        if (productionJobDataSource != null) {
            val jobResult = productionJobDataSource.fetchJobById(productionJobId)
            if (jobResult is DomainResult.Error) {
                return DomainResult.Error(message = "Referenced production job not found: $productionJobId")
            }
            val job = (jobResult as DomainResult.Success).data
            if (job.orderId != projectId) {
                return DomainResult.Error(message = "Project ID mismatch: Job belongs to ${job.orderId}, but $projectId provided.")
            }
        }

        // 4. Cross-Job Source Reference Checks
        if (qcId != null && qcDataSource != null) {
            val qc = qcDataSource.fetchQcById(qcId)
            if (qc is DomainResult.Success && qc.data.productionJobId != productionJobId) {
                return DomainResult.Error(
                    message = "Cross-job reference violation: QC inspection '$qcId' belongs to job '${qc.data.productionJobId}', not '$productionJobId'."
                )
            }
        }

        if (productionDefectId != null && defectDataSource != null) {
            val defect = defectDataSource.fetchDefectById(productionDefectId)
            if (defect is DomainResult.Success && defect.data.productionJobId != productionJobId) {
                return DomainResult.Error(
                    message = "Cross-job reference violation: Defect '$productionDefectId' belongs to job '${defect.data.productionJobId}', not '$productionJobId'."
                )
            }
        }

        if (productionReworkId != null && reworkDataSource != null) {
            val rework = reworkDataSource.fetchReworkById(productionReworkId)
            if (rework is DomainResult.Success && rework.data.productionJobId != productionJobId) {
                return DomainResult.Error(
                    message = "Cross-job reference violation: Rework '$productionReworkId' belongs to job '${rework.data.productionJobId}', not '$productionJobId'."
                )
            }
        }

        if (reQcId != null && reQcDataSource != null) {
            val reQc = reQcDataSource.fetchReQcById(reQcId)
            if (reQc is DomainResult.Success && reQc.data.productionJobId != productionJobId) {
                return DomainResult.Error(
                    message = "Cross-job reference violation: Re-QC '$reQcId' belongs to job '${reQc.data.productionJobId}', not '$productionJobId'."
                )
            }
        }

        if (finalQcId != null && finalQcDataSource != null) {
            val finalQc = finalQcDataSource.findFinalQcById(finalQcId)
            if (finalQc != null && finalQc.productionJobId != productionJobId) {
                return DomainResult.Error(
                    message = "Cross-job reference violation: Final QC '$finalQcId' belongs to job '${finalQc.productionJobId}', not '$productionJobId'."
                )
            }
        }

        val id = "qct-" + UUID.randomUUID().toString()
        val entry = QcTimeEntry(
            id = id,
            productionJobId = productionJobId,
            projectId = projectId,
            qcId = qcId,
            inspectionChecklistId = inspectionChecklistId,
            productionDefectId = productionDefectId,
            productionReworkId = productionReworkId,
            reQcId = reQcId,
            finalQcId = finalQcId,
            entryType = entryType,
            actorId = actorId,
            actorName = actorName,
            startedAt = startedAt,
            endedAt = endedAt,
            durationMinutes = durationMinutes,
            status = QcTimeStatus.RECORDED,
            notes = notes,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        costTimeDataSource.insertTimeEntry(entry)

        recordActivity(
            productionJobId = productionJobId,
            projectId = projectId,
            actorId = actorId,
            actorName = actorName,
            activityType = QcCostTimeActivityType.QC_TIME_ENTRY_CREATED,
            notes = "Recorded ${entry.entryType.defaultLabel}: $durationMinutes mins",
            timeEntryId = id,
            timestamp = timestamp
        )

        return DomainResult.Success(entry)
    }

    override suspend fun updateTimeEntry(
        id: String,
        durationMinutes: Long,
        endedAt: String?,
        notes: String?,
        updatedBy: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcTimeEntry> = mutex.withLock {
        val rbac = QcCostTimeReconciliationValidator.validateAdjustmentPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = costTimeDataSource.findTimeEntryById(id)
            ?: return DomainResult.Error(message = "QC Time entry not found: $id")

        val immutability = QcTimeEntryValidator.validateImmutability(current)
        if (immutability is DomainResult.Error) return immutability

        if (durationMinutes < 0L) {
            return DomainResult.Error(message = "Duration in minutes cannot be negative.")
        }

        val updated = current.copy(
            durationMinutes = durationMinutes,
            endedAt = endedAt ?: current.endedAt,
            notes = notes ?: current.notes,
            updatedAt = timestamp
        )

        costTimeDataSource.updateTimeEntry(updated)

        recordActivity(
            productionJobId = current.productionJobId,
            projectId = current.projectId,
            actorId = updatedBy,
            activityType = QcCostTimeActivityType.QC_TIME_ENTRY_RECORDED,
            notes = "Updated time entry duration to $durationMinutes mins",
            timeEntryId = id,
            timestamp = timestamp
        )

        return DomainResult.Success(updated)
    }

    override suspend fun changeTimeStatus(
        id: String,
        targetStatus: QcTimeStatus,
        notes: String?,
        actorId: String,
        actorName: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcTimeEntry> = mutex.withLock {
        val rbac = QcCostTimeReconciliationValidator.validateAdjustmentPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = costTimeDataSource.findTimeEntryById(id)
            ?: return DomainResult.Error(message = "QC Time entry not found: $id")

        val transition = QcTimeEntryValidator.validateStatusTransition(current.status, targetStatus)
        if (transition is DomainResult.Error) return transition

        val updated = current.copy(
            status = targetStatus,
            updatedAt = timestamp
        )
        costTimeDataSource.updateTimeEntry(updated)

        recordActivity(
            productionJobId = current.productionJobId,
            projectId = current.projectId,
            actorId = actorId,
            actorName = actorName,
            activityType = QcCostTimeActivityType.QC_TIME_ENTRY_RECORDED,
            notes = "Changed time status to ${targetStatus.defaultLabel}. $notes",
            timeEntryId = id,
            timestamp = timestamp
        )

        return DomainResult.Success(updated)
    }

    // ==========================================
    // Reconciliation Operations
    // ==========================================

    override fun observeReconciliations(): Flow<List<QcCostTimeReconciliation>> =
        costTimeDataSource.observeReconciliations()

    override fun observeReconciliationForJob(productionJobId: String): Flow<QcCostTimeReconciliation?> =
        costTimeDataSource.observeReconciliationForJob(productionJobId)

    override suspend fun getReconciliation(productionJobId: String): DomainResult<QcCostTimeReconciliation?> {
        val recon = costTimeDataSource.findReconciliationByJob(productionJobId)
        return DomainResult.Success(recon)
    }

    override suspend fun calculateReconciliation(
        productionJobId: String,
        plannedCost: Double,
        plannedMinutes: Long,
        reconciledBy: String,
        reconciledByName: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcCostTimeReconciliation> = mutex.withLock {
        // 1. RBAC Check
        val rbac = QcCostTimeReconciliationValidator.validateReconcilePermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        // 2. Validate Benchmark Parameters
        val paramVal = QcCostTimeReconciliationValidator.validateCalculationParams(plannedCost, plannedMinutes)
        if (paramVal is DomainResult.Error) return paramVal

        // 3. Existing Reconciliation Lock Check
        val existing = costTimeDataSource.findReconciliationByJob(productionJobId)
        if (existing != null && existing.isLocked) {
            return DomainResult.Error(message = "Reconciliation for job '$productionJobId' is already LOCKED and cannot be recalculated.")
        }

        // 4. Collect cost and time entries
        val costEntries = costTimeDataSource.observeCostEntriesForJob(productionJobId).first()
        val timeEntries = costTimeDataSource.observeTimeEntriesForJob(productionJobId).first()

        val projectId = costEntries.firstOrNull()?.projectId
            ?: timeEntries.firstOrNull()?.projectId
            ?: run {
                if (productionJobDataSource != null) {
                    val job = productionJobDataSource.fetchJobById(productionJobId)
                    if (job is DomainResult.Success) job.data.orderId else "UNKNOWN_PROJECT"
                } else {
                    "UNKNOWN_PROJECT"
                }
            }

        // 5. Query counts from auxiliary data sources
        val defects = defectDataSource?.observeDefects()?.first()?.filter { it.productionJobId == productionJobId } ?: emptyList()
        val reworks = reworkDataSource?.observeReworks()?.first()?.filter { it.productionJobId == productionJobId } ?: emptyList()
        val reQcs = reQcDataSource?.observeReQcList()?.first()?.filter { it.productionJobId == productionJobId } ?: emptyList()
        val finalQcs = finalQcDataSource?.observeFinalQcList()?.first()?.filter { it.productionJobId == productionJobId } ?: emptyList()

        val reconciliationId = existing?.id ?: ("recon-" + UUID.randomUUID().toString())

        val reconciliation = QcCostTimeReconciliation.calculate(
            id = reconciliationId,
            productionJobId = productionJobId,
            projectId = projectId,
            plannedCost = plannedCost,
            plannedMinutes = plannedMinutes,
            costEntries = costEntries,
            timeEntries = timeEntries,
            defectCount = defects.size,
            reworkCount = reworks.size,
            reQcCycleCount = reQcs.size,
            finalQcCount = finalQcs.size,
            reconciledBy = reconciledBy,
            reconciledByName = reconciledByName,
            timestamp = timestamp,
            notes = notes
        )

        if (existing != null) {
            costTimeDataSource.updateReconciliation(reconciliation)
        } else {
            costTimeDataSource.insertReconciliation(reconciliation)
        }

        // Mark included cost and time entries as RECONCILED if in RECORDED state
        for (cost in costEntries) {
            if (cost.status == QcCostStatus.RECORDED) {
                costTimeDataSource.updateCostEntry(cost.copy(status = QcCostStatus.RECONCILED, reconciledBy = reconciledBy, reconciledAt = timestamp))
            }
        }
        for (time in timeEntries) {
            if (time.status == QcTimeStatus.RECORDED) {
                costTimeDataSource.updateTimeEntry(time.copy(status = QcTimeStatus.RECONCILED))
            }
        }

        recordActivity(
            productionJobId = productionJobId,
            projectId = projectId,
            actorId = reconciledBy,
            actorName = reconciledByName,
            activityType = QcCostTimeActivityType.QC_RECONCILIATION_COMPLETED,
            notes = "Reconciliation completed. Actual Cost: ${reconciliation.actualCost}, Actual Time: ${reconciliation.actualMinutes}m",
            reconciliationId = reconciliationId,
            timestamp = timestamp
        )

        return DomainResult.Success(reconciliation)
    }

    override suspend fun adjustReconciliation(
        reconciliationId: String,
        adjustedPlannedCost: Double?,
        adjustedPlannedMinutes: Long?,
        adjustmentReason: String,
        adjustedBy: String,
        adjustedByName: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcCostTimeReconciliation> = mutex.withLock {
        val rbac = QcCostTimeReconciliationValidator.validateAdjustmentPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = costTimeDataSource.findReconciliationById(reconciliationId)
            ?: return DomainResult.Error(message = "Reconciliation record not found: $reconciliationId")

        val adjVal = QcCostTimeReconciliationValidator.validateAdjustmentPrerequisites(current, adjustmentReason)
        if (adjVal is DomainResult.Error) return adjVal

        val newPlannedCost = adjustedPlannedCost ?: current.plannedCost
        val newPlannedMinutes = adjustedPlannedMinutes ?: current.plannedMinutes

        val updated = current.copy(
            plannedCost = newPlannedCost,
            costVariance = current.actualCost - newPlannedCost,
            plannedMinutes = newPlannedMinutes,
            timeVarianceMinutes = current.actualMinutes - newPlannedMinutes,
            status = QcCostStatus.ADJUSTED,
            notes = "${current.notes ?: ""}\nAdjustment: $adjustmentReason".trim(),
            updatedAt = timestamp
        )

        costTimeDataSource.updateReconciliation(updated)

        recordActivity(
            productionJobId = current.productionJobId,
            projectId = current.projectId,
            actorId = adjustedBy,
            actorName = adjustedByName,
            activityType = QcCostTimeActivityType.QC_RECONCILIATION_ADJUSTED,
            notes = "Adjusted reconciliation: $adjustmentReason",
            reconciliationId = reconciliationId,
            timestamp = timestamp
        )

        return DomainResult.Success(updated)
    }

    override suspend fun lockReconciliation(
        reconciliationId: String,
        lockedBy: String,
        lockedByName: String?,
        lockNotes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcCostTimeSnapshot> = mutex.withLock {
        // 1. RBAC Check (Separation of Duties - Inspector denied)
        val rbac = QcCostTimeReconciliationValidator.validateLockPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = costTimeDataSource.findReconciliationById(reconciliationId)
            ?: return DomainResult.Error(message = "Reconciliation record not found: $reconciliationId")

        // 2. Idempotency: If already locked and snapshot exists, return existing snapshot
        if (current.isLocked && current.snapshotId != null) {
            val existingSnapshot = costTimeDataSource.findSnapshotById(current.snapshotId)
            if (existingSnapshot != null) {
                return DomainResult.Success(existingSnapshot)
            }
        }

        // 3. Lock Prerequisites Check
        val lockVal = QcCostTimeReconciliationValidator.validateLockPrerequisites(current)
        if (lockVal is DomainResult.Error) return lockVal

        // 4. Collect referenced source entries and lock them
        val costEntries = costTimeDataSource.observeCostEntriesForJob(current.productionJobId).first()
        val timeEntries = costTimeDataSource.observeTimeEntriesForJob(current.productionJobId).first()

        for (cost in costEntries) {
            if (cost.status != QcCostStatus.CANCELLED) {
                costTimeDataSource.updateCostEntry(cost.copy(status = QcCostStatus.LOCKED, updatedAt = timestamp))
            }
        }

        for (time in timeEntries) {
            if (time.status != QcTimeStatus.CANCELLED) {
                costTimeDataSource.updateTimeEntry(time.copy(status = QcTimeStatus.LOCKED, updatedAt = timestamp))
            }
        }

        // Query defects, reworks, Re-QCs, and final QC
        val defects = defectDataSource?.observeDefects()?.first()?.filter { it.productionJobId == current.productionJobId } ?: emptyList()
        val reworks = reworkDataSource?.observeReworks()?.first()?.filter { it.productionJobId == current.productionJobId } ?: emptyList()
        val reQcs = reQcDataSource?.observeReQcList()?.first()?.filter { it.productionJobId == current.productionJobId } ?: emptyList()
        val finalQcs = finalQcDataSource?.observeFinalQcList()?.first()?.filter { it.productionJobId == current.productionJobId } ?: emptyList()

        // 5. Create immutable snapshot
        val snapshotId = "snap-ct-" + UUID.randomUUID().toString()
        val snapshot = QcCostTimeSnapshot(
            snapshotId = snapshotId,
            reconciliationId = reconciliationId,
            productionJobId = current.productionJobId,
            projectId = current.projectId,
            plannedCost = current.plannedCost,
            actualCost = current.actualCost,
            costVariance = current.costVariance,
            plannedMinutes = current.plannedMinutes,
            actualMinutes = current.actualMinutes,
            timeVarianceMinutes = current.timeVarianceMinutes,
            costEntryIds = costEntries.filter { it.status != QcCostStatus.CANCELLED }.map { it.id },
            timeEntryIds = timeEntries.filter { it.status != QcTimeStatus.CANCELLED }.map { it.id },
            defectIds = defects.map { it.defectId },
            reworkIds = reworks.map { it.reworkId },
            reQcIds = reQcs.map { it.reQcId },
            finalQcId = finalQcs.firstOrNull()?.finalQcId,
            currency = "BDT",
            lockedBy = lockedBy,
            lockedByName = lockedByName,
            lockedAt = timestamp,
            notes = lockNotes ?: current.notes,
            createdAt = timestamp
        )
        costTimeDataSource.insertSnapshot(snapshot)

        // 6. Update reconciliation to LOCKED
        val updatedRecon = current.copy(
            status = QcCostStatus.LOCKED,
            lockedBy = lockedBy,
            lockedByName = lockedByName,
            lockedAt = timestamp,
            snapshotId = snapshotId,
            updatedAt = timestamp
        )
        costTimeDataSource.updateReconciliation(updatedRecon)

        // 7. Audit Logging
        recordActivity(
            productionJobId = current.productionJobId,
            projectId = current.projectId,
            actorId = lockedBy,
            actorName = lockedByName,
            activityType = QcCostTimeActivityType.QC_COST_TIME_SNAPSHOT_CREATED,
            notes = "Snapshot $snapshotId created and permanently locked.",
            reconciliationId = reconciliationId,
            snapshotId = snapshotId,
            timestamp = timestamp
        )
        recordActivity(
            productionJobId = current.productionJobId,
            projectId = current.projectId,
            actorId = lockedBy,
            actorName = lockedByName,
            activityType = QcCostTimeActivityType.QC_RECONCILIATION_LOCKED,
            notes = "Reconciliation permanently sealed and locked. Notes: $lockNotes",
            reconciliationId = reconciliationId,
            snapshotId = snapshotId,
            timestamp = timestamp
        )

        return DomainResult.Success(snapshot)
    }

    // ==========================================
    // Snapshots
    // ==========================================

    override fun observeSnapshots(): Flow<List<QcCostTimeSnapshot>> =
        costTimeDataSource.observeSnapshots()

    override fun observeSnapshotForJob(productionJobId: String): Flow<QcCostTimeSnapshot?> =
        costTimeDataSource.observeSnapshotForJob(productionJobId)

    override suspend fun getSnapshot(productionJobId: String): DomainResult<QcCostTimeSnapshot?> {
        val snapshot = costTimeDataSource.findSnapshotByJob(productionJobId)
        return DomainResult.Success(snapshot)
    }

    override suspend fun findSnapshotById(snapshotId: String): DomainResult<QcCostTimeSnapshot> {
        val snapshot = costTimeDataSource.findSnapshotById(snapshotId)
            ?: return DomainResult.Error(message = "Snapshot not found: $snapshotId")
        return DomainResult.Success(snapshot)
    }

    // ==========================================
    // Audit History
    // ==========================================

    override fun observeActivityEvents(productionJobId: String): Flow<List<QcCostTimeActivityEvent>> =
        costTimeDataSource.observeActivityEvents(productionJobId)
}
