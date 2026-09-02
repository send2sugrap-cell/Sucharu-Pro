package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface contract for Production Job Management in Sucharu Pro ERP (Module 04).
 */
interface ProductionJobRepository {

    /** Reactive stream of all production jobs. */
    fun observeJobs(): Flow<List<ProductionJob>>

    /** Reactive stream observing a single job by [jobId]. */
    fun getJobById(jobId: String): Flow<ProductionJob?>

    /** Direct lookup of a job by [jobId]. */
    suspend fun findJobById(jobId: String): DomainResult<ProductionJob>

    /** Reactive stream of jobs associated with a specific commercial order. */
    fun getJobsForOrder(orderId: String): Flow<List<ProductionJob>>

    /** Reactive stream of the job associated with a specific handoff snapshot. */
    fun getJobForHandoff(handoffId: String): Flow<ProductionJob?>

    /**
     * Persists a new [ProductionJob] after validating domain integrity.
     */
    suspend fun createJob(job: ProductionJob): DomainResult<ProductionJob>

    /**
     * Converts a validated [OrderJobHandoff] into an isolated [ProductionJob]
     * with canonical 13 stages initialized to PENDING.
     */
    suspend fun createJobFromHandoff(
        handoff: com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoff,
        title: String? = null,
        description: String? = null,
        createdBy: String? = null,
        timestamp: String
    ): DomainResult<ProductionJob>

    /**
     * Updates an existing [ProductionJob] after validating domain integrity.
     */
    suspend fun updateJob(job: ProductionJob): DomainResult<ProductionJob>

    /**
     * Starts execution of a specific production stage. Automatically transitions
     * Job status from READY_FOR_PRODUCTION to IN_PROGRESS if appropriate.
     */
    suspend fun startStage(
        jobId: String,
        stageId: String,
        actorId: String? = null,
        actorName: String? = null,
        notes: String? = null,
        timestamp: String
    ): DomainResult<ProductionJob>

    /**
     * Completes execution of an in-progress stage. Automatically recalculates progress
     * and synchronizes Job status to READY or DELIVERED if canonical milestones are reached.
     */
    suspend fun completeStage(
        jobId: String,
        stageId: String,
        actorId: String? = null,
        notes: String? = null,
        timestamp: String
    ): DomainResult<ProductionJob>

    /**
     * Skips an applicable production stage (e.g. Lamination for uncoated jobs).
     */
    suspend fun skipStage(
        jobId: String,
        stageId: String,
        actorId: String? = null,
        notes: String? = null,
        timestamp: String
    ): DomainResult<ProductionJob>

    /**
     * Places an active Job on hold without losing stage execution state.
     */
    suspend fun holdJob(
        jobId: String,
        reason: String? = null,
        timestamp: String
    ): DomainResult<ProductionJob>

    /**
     * Resumes an on-hold Job back to IN_PROGRESS.
     */
    suspend fun resumeJob(
        jobId: String,
        timestamp: String
    ): DomainResult<ProductionJob>

    /**
     * Cancels a Job with a mandatory non-blank reason. Preserves historical records.
     */
    suspend fun cancelJob(
        jobId: String,
        reason: String,
        timestamp: String
    ): DomainResult<ProductionJob>

    /**
     * Marks a job READY after all manufacturing stages through sequence 12 are completed.
     */
    suspend fun markJobReady(
        jobId: String,
        timestamp: String
    ): DomainResult<ProductionJob>

    /**
     * Marks a job DELIVERED after sequence 13 stage execution.
     */
    suspend fun deliverJob(
        jobId: String,
        timestamp: String
    ): DomainResult<ProductionJob>

    /**
     * Assigns an operator to a specific production stage.
     */
    suspend fun assignStageOperator(
        jobId: String,
        stageId: String,
        operatorId: String,
        operatorName: String,
        assignedBy: String? = null,
        notes: String? = null,
        timestamp: String
    ): DomainResult<ProductionJob>

    /**
     * Reassigns an actively assigned production stage to a new operator, preserving assignment history.
     */
    suspend fun reassignStageOperator(
        jobId: String,
        stageId: String,
        newOperatorId: String,
        newOperatorName: String,
        reassignedBy: String? = null,
        notes: String? = null,
        timestamp: String
    ): DomainResult<ProductionJob>

    /**
     * Removes an active operator assignment from a pending production stage.
     */
    suspend fun unassignStageOperator(
        jobId: String,
        stageId: String,
        unassignedBy: String? = null,
        reason: String? = null,
        timestamp: String
    ): DomainResult<ProductionJob>

    /**
     * Retrieves the active assignment for a stage.
     */
    fun getStageAssignment(jobId: String, stageId: String): Flow<com.sucharu.sucharupro.domain.model.job.ProductionStageAssignment?>

    /**
     * Retrieves all assignment history records for a job.
     */
    fun getAssignmentsForJob(jobId: String): Flow<List<com.sucharu.sucharupro.domain.model.job.ProductionStageAssignment>>

    /**
     * Retrieves all active/historical assignments for an operator.
     */
    fun getAssignmentsForOperator(operatorId: String): Flow<List<com.sucharu.sucharupro.domain.model.job.ProductionStageAssignment>>

    /**
     * Reactive stream of all stage assignments.
     */
    fun observeStageAssignments(): Flow<List<com.sucharu.sucharupro.domain.model.job.ProductionStageAssignment>>

    /**
     * Returns the list of standard available production operators.
     */
    fun getAvailableOperators(): List<com.sucharu.sucharupro.domain.model.job.ProductionOperator>

    /**
     * Retrieves the latest execution record for a specific stage.
     */
    fun getStageExecution(jobId: String, stageId: String): Flow<com.sucharu.sucharupro.domain.model.job.ProductionStageExecution?>

    /**
     * Retrieves all stage execution records for a job.
     */
    fun getStageExecutionsForJob(jobId: String): Flow<List<com.sucharu.sucharupro.domain.model.job.ProductionStageExecution>>

    /**
     * Reactive stream of all stage execution records.
     */
    fun observeStageExecutions(): Flow<List<com.sucharu.sucharupro.domain.model.job.ProductionStageExecution>>

    /**
     * Retrieves chronological production activity timeline events for a job.
     */
    fun getProductionActivityEvents(jobId: String): Flow<List<com.sucharu.sucharupro.domain.model.job.ProductionActivityEvent>>

    /**
     * Reactive stream of all production activity timeline events.
     */
    fun observeProductionActivityEvents(): Flow<List<com.sucharu.sucharupro.domain.model.job.ProductionActivityEvent>>

    /**
     * Appends an operational execution note to a stage and records the corresponding activity event.
     */
    suspend fun addStageExecutionNote(
        jobId: String,
        stageId: String,
        note: String,
        actorId: String? = null,
        actorName: String? = null,
        timestamp: String
    ): DomainResult<ProductionJob>

    /**
     * Reactive stream of all stage output records.
     */
    fun observeStageOutputs(): Flow<List<com.sucharu.sucharupro.domain.model.job.ProductionStageOutput>>

    /**
     * Retrieves all output records for a specific stage.
     */
    fun getStageOutputs(jobId: String, stageId: String): Flow<List<com.sucharu.sucharupro.domain.model.job.ProductionStageOutput>>

    /**
     * Retrieves all output records for a job.
     */
    fun getStageOutputsForJob(jobId: String): Flow<List<com.sucharu.sucharupro.domain.model.job.ProductionStageOutput>>

    /**
     * Retrieves the deterministic accumulated output quantity for a stage.
     */
    fun getTotalStageOutput(jobId: String, stageId: String): Flow<Int>

    /**
     * Retrieves the remaining planned output quantity for a stage.
     */
    fun getRemainingStageQuantity(jobId: String, stageId: String): Flow<Int>

    /**
     * Records an operational production stage output quantity atomically.
     */
    suspend fun recordStageOutput(
        jobId: String,
        stageId: String,
        quantity: Int,
        unit: String,
        operatorId: String? = null,
        operatorName: String? = null,
        recordedBy: String? = null,
        recordedByName: String? = null,
        remarks: String? = null,
        timestamp: String
    ): DomainResult<com.sucharu.sucharupro.domain.model.job.ProductionStageOutput>

    /**
     * Reactive stream of overall aggregated production monitoring KPI snapshot.
     */
    fun observeProductionMonitoringSnapshot(): Flow<com.sucharu.sucharupro.domain.model.job.ProductionMonitoringSnapshot>

    /**
     * Reactive stream of currently active in-progress production stages across all jobs.
     */
    fun observeActiveProductionStages(): Flow<List<com.sucharu.sucharupro.domain.model.job.ActiveProductionStageItem>>

    /**
     * Reactive stream of operator workload metrics and task allocations.
     */
    fun observeOperatorWorkloads(): Flow<List<com.sucharu.sucharupro.domain.model.job.OperatorWorkloadItem>>

    /**
     * Reactive stream of actionable items requiring supervisor oversight.
     */
    fun observeProductionAttentionItems(): Flow<List<com.sucharu.sucharupro.domain.model.job.ProductionAttentionItem>>

    /**
     * Reactive stream of historical summaries across all production jobs.
     */
    fun observeProductionHistory(): Flow<List<com.sucharu.sucharupro.domain.model.job.ProductionHistorySummary>>

    /**
     * Reactive stream of high-level historical performance metrics and statistical KPIs.
     */
    fun observeProductionPerformanceMetrics(
        dateRange: com.sucharu.sucharupro.domain.model.job.ProductionDateRangeFilter = com.sucharu.sucharupro.domain.model.job.ProductionDateRangeFilter.ALL_TIME
    ): Flow<com.sucharu.sucharupro.domain.model.job.ProductionPerformanceMetrics>

    /**
     * Reactive stream of operator historical execution performance analytics.
     */
    fun observeOperatorPerformance(): Flow<List<com.sucharu.sucharupro.domain.model.job.ProductionOperatorPerformanceItem>>

    /**
     * Reactive stream of canonical stage type performance statistics.
     */
    fun observeStagePerformance(): Flow<List<com.sucharu.sucharupro.domain.model.job.ProductionStagePerformanceItem>>

    /**
     * Reactive stream fetching the consolidated completion summary for a specific job.
     */
    fun getProductionJobCompletionSummary(
        jobId: String
    ): Flow<DomainResult<com.sucharu.sucharupro.domain.model.job.ProductionJobCompletionSummary>>

    /**
     * Reactive stream calculating comprehensive production output reconciliation for a job card.
     */
    fun observeProductionOutputReconciliation(
        jobId: String
    ): Flow<DomainResult<com.sucharu.sucharupro.domain.model.job.ProductionOutputReconciliation>>

    /**
     * Retrieves all output records registered against a specific stage execution.
     */
    fun getProductionOutputsForExecution(
        executionId: String
    ): Flow<List<com.sucharu.sucharupro.domain.model.job.ProductionStageOutput>>

    /**
     * Reactive stream observing the real-time production completion readiness checklist.
     */
    fun observeProductionCompletionChecklist(
        jobId: String
    ): Flow<DomainResult<com.sucharu.sucharupro.domain.model.job.ProductionCompletionChecklist>>

    /**
     * Retrieves the immutable production ready handoff snapshot for a completed job card.
     */
    fun getProductionReadyHandoff(
        jobId: String
    ): Flow<DomainResult<com.sucharu.sucharupro.domain.model.job.ProductionReadyHandoff>>

    /**
     * Confirms final production completion, verifies all readiness gates atomically,
     * updates job status to READY, and records the completion activity event.
     */
    suspend fun confirmProductionCompletion(
        jobId: String,
        actorId: String,
        actorName: String,
        remarks: String? = null,
        timestamp: String
    ): DomainResult<com.sucharu.sucharupro.domain.model.job.ProductionJob>
}

