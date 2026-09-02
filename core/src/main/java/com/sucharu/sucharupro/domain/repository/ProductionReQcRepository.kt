package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ReQcActivityEvent
import com.sucharu.sucharupro.domain.model.qc.ReQcCycleType
import com.sucharu.sucharupro.domain.model.qc.ReQcFailureReason
import com.sucharu.sucharupro.domain.model.qc.ReQcFailureRecord
import com.sucharu.sucharupro.domain.model.qc.ReQcInspection
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface contract for Re-QC & Failure Loops (Module 06 Step 06).
 */
interface ProductionReQcRepository {

    /** Reactive stream of all Re-QC inspection records. */
    fun observeReQcList(): Flow<List<ReQcInspection>>

    /** Reactive stream of a single Re-QC record by [reQcId]. */
    fun observeReQcById(reQcId: String): Flow<ReQcInspection?>

    /** Convenience alias for observing a single Re-QC record. */
    fun getReQcById(reQcId: String): Flow<ReQcInspection?> = observeReQcById(reQcId)

    /** Direct one-shot lookup of a Re-QC record by [reQcId]. */
    suspend fun findReQcById(reQcId: String): DomainResult<ReQcInspection>

    /** Reactive stream of Re-QC records for a specific [productionJobId]. */
    fun observeReQcByJob(productionJobId: String): Flow<List<ReQcInspection>>

    /** Reactive stream of Re-QC records for a specific [projectId]. */
    fun observeReQcByProject(projectId: String): Flow<List<ReQcInspection>>

    /** Reactive stream of Re-QC records originating from a specific [reworkId]. */
    fun observeReQcByRework(reworkId: String): Flow<List<ReQcInspection>>

    /** Reactive stream of Re-QC cycles for a job, ordered by cycleNumber. */
    fun observeReQcCycles(productionJobId: String): Flow<List<ReQcInspection>>

    /** Direct lookup of the latest Re-QC cycle for a job. */
    suspend fun getLatestReQcCycle(productionJobId: String): DomainResult<ReQcInspection?>

    /**
     * Creates and registers an initial Re-QC cycle (Cycle 1) for a returned rework.
     */
    suspend fun createReQc(
        projectId: String,
        productionJobId: String,
        productionReworkId: String,
        cycleType: ReQcCycleType = ReQcCycleType.POST_REWORK,
        originalQcId: String? = null,
        originalDefectId: String? = null,
        checklistId: String? = null,
        affectedQuantity: Int? = null,
        quantityUnit: String = "pcs",
        createdBy: String,
        createdByName: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ReQcInspection>

    /**
     * Creates the next sequential Re-QC cycle (Cycle N+1) following a failure loop.
     */
    suspend fun createNextCycle(
        projectId: String,
        productionJobId: String,
        productionReworkId: String,
        previousReQcId: String,
        cycleType: ReQcCycleType = ReQcCycleType.REPEAT_FAILURE,
        originalQcId: String? = null,
        originalDefectId: String? = null,
        checklistId: String? = null,
        affectedQuantity: Int? = null,
        quantityUnit: String = "pcs",
        createdBy: String,
        createdByName: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ReQcInspection>

    /**
     * Assigns a Re-QC inspection to a designated QC inspector.
     */
    suspend fun assignReQc(
        reQcId: String,
        inspectorId: String,
        inspectorName: String,
        assignedBy: String,
        assignedByName: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ReQcInspection>

    /**
     * Reassigns a Re-QC inspection to a new QC inspector.
     */
    suspend fun reassignReQc(
        reQcId: String,
        newInspectorId: String,
        newInspectorName: String,
        reassignedBy: String,
        reassignedByName: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ReQcInspection>

    /**
     * Unassigns an active inspector assignment.
     */
    suspend fun unassignReQc(
        reQcId: String,
        unassignedBy: String,
        unassignedByName: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ReQcInspection>

    /**
     * Starts execution of the Re-QC inspection.
     */
    suspend fun startInspection(
        reQcId: String,
        inspectorId: String,
        inspectorName: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ReQcInspection>

    /**
     * Marks Re-QC inspection as PASS (terminal success for this cycle).
     */
    suspend fun passReQc(
        reQcId: String,
        inspectorId: String,
        inspectorName: String? = null,
        passNotes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ReQcInspection>

    /**
     * Marks Re-QC inspection as FAIL, creates immutable failure record, and transitions to FAILED.
     */
    suspend fun failReQc(
        reQcId: String,
        failureReason: ReQcFailureReason,
        failureNotes: String,
        affectedQuantity: Int,
        quantityUnit: String = "pcs",
        failedItemIds: List<String> = emptyList(),
        inspectorId: String,
        inspectorName: String? = null,
        nextAction: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ReQcInspection>

    /**
     * Transitions a FAILED Re-QC to RETURNED_TO_REWORK to enable subsequent rework cycle.
     */
    suspend fun returnToRework(
        reQcId: String,
        actorId: String,
        actorName: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ReQcInspection>

    /**
     * Cancels a pre-terminal Re-QC inspection.
     */
    suspend fun cancelReQc(
        reQcId: String,
        reason: String,
        cancelledBy: String,
        cancelledByName: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ReQcInspection>

    /**
     * Manually records or attaches a failure record to a Re-QC.
     */
    suspend fun recordFailure(
        reQcId: String,
        failureReason: ReQcFailureReason,
        failureNotes: String,
        affectedQuantity: Int,
        quantityUnit: String = "pcs",
        failedItemIds: List<String> = emptyList(),
        detectedBy: String,
        detectedByName: String? = null,
        nextAction: String? = null,
        linkedReworkId: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ReQcFailureRecord>

    /** Reactive stream of failure records filtered optionally by [reQcId] or [productionJobId]. */
    fun observeFailureHistory(reQcId: String? = null, productionJobId: String? = null): Flow<List<ReQcFailureRecord>>

    /** Lookup a specific failure record. */
    suspend fun findFailureRecordById(failureRecordId: String): DomainResult<ReQcFailureRecord>

    /** Reactive stream of audit activity events for a specific Re-QC record. */
    fun observeReQcActivity(reQcId: String): Flow<List<ReQcActivityEvent>>
}
