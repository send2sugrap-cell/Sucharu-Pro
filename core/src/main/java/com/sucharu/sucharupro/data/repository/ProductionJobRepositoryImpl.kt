package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.ProductionJobDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionActivityEvent
import com.sucharu.sucharupro.domain.model.job.ProductionActivityType
import com.sucharu.sucharupro.domain.model.job.ProductionDurationCalculator
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionOperator
import com.sucharu.sucharupro.domain.model.job.ProductionStageAssignment
import com.sucharu.sucharupro.domain.model.job.ProductionStageExecution
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.job.StageAssignmentStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.ProductionJobRepository
import com.sucharu.sucharupro.domain.validation.ProductionJobLifecycleValidator
import com.sucharu.sucharupro.domain.validation.ProductionJobValidator
import com.sucharu.sucharupro.domain.validation.ProductionStageAssignmentValidator
import com.sucharu.sucharupro.domain.validation.ProductionStageLifecycleValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.withLock
import java.util.Locale
import java.util.UUID

/**
 * Authoritative implementation of [ProductionJobRepository] enforcing domain validators,
 * state machine integrity, stage execution history, and chronological activity tracking.
 */
class ProductionJobRepositoryImpl(
    private val dataSource: ProductionJobDataSource
) : ProductionJobRepository {

    private val repositoryMutex = kotlinx.coroutines.sync.Mutex()

    private suspend fun recordActivity(
        jobId: String,
        stageId: String? = null,
        stageType: ProductionStageType? = null,
        operatorId: String? = null,
        operatorName: String? = null,
        eventType: ProductionActivityType,
        message: String? = null,
        timestamp: String,
        createdBy: String? = null
    ) {
        val event = ProductionActivityEvent(
            eventId = "act-" + UUID.randomUUID().toString(),
            jobId = jobId,
            stageId = stageId,
            stageType = stageType,
            operatorId = operatorId,
            operatorName = operatorName,
            eventType = eventType,
            message = message,
            timestamp = timestamp,
            createdBy = createdBy
        )
        dataSource.insertActivityEvent(event)
    }

    override fun observeJobs(): Flow<List<ProductionJob>> = dataSource.observeJobs()

    override fun getJobById(jobId: String): Flow<ProductionJob?> {
        return dataSource.observeJobs().map { jobs ->
            jobs.find { it.jobId == jobId }
        }
    }

    override suspend fun findJobById(jobId: String): DomainResult<ProductionJob> {
        return dataSource.fetchJobById(jobId)
    }

    override fun getJobsForOrder(orderId: String): Flow<List<ProductionJob>> {
        return dataSource.observeJobs().map { jobs ->
            jobs.filter { it.orderId == orderId }
        }
    }

    override fun getJobForHandoff(handoffId: String): Flow<ProductionJob?> {
        return dataSource.observeJobs().map { jobs ->
            jobs.find { it.handoffId == handoffId }
        }
    }

    override suspend fun createJob(job: ProductionJob): DomainResult<ProductionJob> {
        val validation = ProductionJobValidator.validateJob(job)
        if (validation is DomainResult.Error) {
            return validation
        }
        return dataSource.insertJob(job)
    }

    override suspend fun createJobFromHandoff(
        handoff: com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoff,
        title: String?,
        description: String?,
        createdBy: String?,
        timestamp: String
    ): DomainResult<ProductionJob> {
        val handoffValidation = ProductionJobValidator.validateHandoffSource(handoff)
        if (handoffValidation is DomainResult.Error) {
            return handoffValidation
        }

        val existingJobs = observeJobs().first()
        if (existingJobs.any { it.handoffId == handoff.handoffId && !it.status.isTerminal }) {
            return DomainResult.Error(message = "An active Production Job for Handoff '${handoff.handoffId}' already exists.")
        }

        val year = 2026
        val nextSeq = existingJobs.count { it.jobNumber.startsWith("JOB-$year-") } + 1
        val jobNumber = String.format(Locale.US, "JOB-%d-%04d", year, nextSeq)
        val jobId = "job-" + UUID.randomUUID().toString()

        val job = ProductionJob.fromHandoff(
            jobId = jobId,
            jobNumber = jobNumber,
            handoff = handoff,
            title = title,
            description = description,
            createdBy = createdBy,
            timestamp = timestamp
        )

        return createJob(job)
    }

    override suspend fun updateJob(job: ProductionJob): DomainResult<ProductionJob> {
        val validation = ProductionJobValidator.validateJob(job)
        if (validation is DomainResult.Error) {
            return validation
        }
        return dataSource.updateJob(job)
    }

    override suspend fun startStage(
        jobId: String,
        stageId: String,
        actorId: String?,
        actorName: String?,
        notes: String?,
        timestamp: String
    ): DomainResult<ProductionJob> {
        val fetchResult = findJobById(jobId)
        if (fetchResult !is DomainResult.Success) return fetchResult
        val currentJob = fetchResult.data

        val stageValidation = ProductionStageLifecycleValidator.validateStartStage(currentJob, stageId)
        if (stageValidation !is DomainResult.Success) return DomainResult.Error(
            message = (stageValidation as DomainResult.Error).message
        )

        val targetStage = currentJob.stages.find { it.stageId == stageId }!!
        val updatedStages = currentJob.stages.map { stage ->
            if (stage.stageId == stageId) {
                stage.copy(
                    status = ProductionStageStatus.IN_PROGRESS,
                    startedAt = timestamp,
                    assignedUserId = actorId ?: stage.assignedUserId,
                    assignedUserName = actorName ?: stage.assignedUserName,
                    notes = notes ?: stage.notes
                )
            } else {
                stage
            }
        }

        val newJobStatus = if (currentJob.status == ProductionJobStatus.READY_FOR_PRODUCTION ||
            currentJob.status == ProductionJobStatus.DRAFT
        ) {
            ProductionJobStatus.IN_PROGRESS
        } else {
            currentJob.status
        }

        val updatedJob = currentJob.copy(
            status = newJobStatus,
            stages = updatedStages,
            updatedAt = timestamp,
            updatedBy = actorId ?: currentJob.updatedBy
        )

        val updateResult = updateJob(updatedJob)
        if (updateResult is DomainResult.Success) {
            val execution = ProductionStageExecution(
                executionId = "exec-" + UUID.randomUUID().toString(),
                jobId = jobId,
                stageId = stageId,
                stageType = targetStage.stageType,
                operatorId = actorId ?: targetStage.assignedUserId,
                operatorName = actorName ?: targetStage.assignedUserName,
                startedAt = timestamp,
                startRemarks = notes,
                status = ProductionStageStatus.IN_PROGRESS,
                createdAt = timestamp
            )
            dataSource.insertExecution(execution)

            recordActivity(
                jobId = jobId,
                stageId = stageId,
                stageType = targetStage.stageType,
                operatorId = actorId ?: targetStage.assignedUserId,
                operatorName = actorName ?: targetStage.assignedUserName,
                eventType = ProductionActivityType.STAGE_STARTED,
                message = notes,
                timestamp = timestamp,
                createdBy = actorName ?: actorId
            )
        }
        return updateResult
    }

    override suspend fun completeStage(
        jobId: String,
        stageId: String,
        actorId: String?,
        notes: String?,
        timestamp: String
    ): DomainResult<ProductionJob> {
        val fetchResult = findJobById(jobId)
        if (fetchResult !is DomainResult.Success) return fetchResult
        val currentJob = fetchResult.data

        val stageValidation = ProductionStageLifecycleValidator.validateCompleteStage(currentJob, stageId)
        if (stageValidation !is DomainResult.Success) return DomainResult.Error(
            message = (stageValidation as DomainResult.Error).message
        )

        val targetStage = currentJob.stages.find { it.stageId == stageId }!!
        val updatedStages = currentJob.stages.map { stage ->
            if (stage.stageId == stageId) {
                stage.copy(
                    status = ProductionStageStatus.COMPLETED,
                    completedAt = timestamp,
                    notes = notes ?: stage.notes
                )
            } else {
                stage
            }
        }

        val allManufacturingCompleted = updatedStages.filter { it.sequence <= 12 }
            .all { it.status == ProductionStageStatus.COMPLETED || it.status == ProductionStageStatus.SKIPPED }

        val newJobStatus = when {
            updatedStages.find { it.stageType == ProductionStageType.DELIVERED }?.status == ProductionStageStatus.COMPLETED ->
                ProductionJobStatus.DELIVERED
            allManufacturingCompleted && currentJob.status != ProductionJobStatus.DELIVERED ->
                ProductionJobStatus.READY
            else ->
                currentJob.status
        }

        val updatedJob = currentJob.copy(
            status = newJobStatus,
            stages = updatedStages,
            updatedAt = timestamp,
            updatedBy = actorId ?: currentJob.updatedBy
        )

        val updateResult = updateJob(updatedJob)
        if (updateResult is DomainResult.Success) {
            val duration = ProductionDurationCalculator.calculateDurationSeconds(targetStage.startedAt, timestamp)
            val existingExecs = observeStageExecutions().first()
            val currentExec = existingExecs.find { it.jobId == jobId && it.stageId == stageId && it.status == ProductionStageStatus.IN_PROGRESS }

            if (currentExec != null) {
                dataSource.updateExecution(
                    currentExec.copy(
                        completedAt = timestamp,
                        durationSeconds = duration,
                        completionRemarks = notes,
                        status = ProductionStageStatus.COMPLETED
                    )
                )
            } else {
                val execution = ProductionStageExecution(
                    executionId = "exec-" + UUID.randomUUID().toString(),
                    jobId = jobId,
                    stageId = stageId,
                    stageType = targetStage.stageType,
                    operatorId = targetStage.assignedUserId,
                    operatorName = targetStage.assignedUserName,
                    startedAt = targetStage.startedAt,
                    completedAt = timestamp,
                    durationSeconds = duration,
                    completionRemarks = notes,
                    status = ProductionStageStatus.COMPLETED,
                    createdAt = timestamp
                )
                dataSource.insertExecution(execution)
            }

            recordActivity(
                jobId = jobId,
                stageId = stageId,
                stageType = targetStage.stageType,
                operatorId = targetStage.assignedUserId,
                operatorName = targetStage.assignedUserName,
                eventType = ProductionActivityType.STAGE_COMPLETED,
                message = notes,
                timestamp = timestamp,
                createdBy = actorId
            )
        }
        return updateResult
    }

    override suspend fun skipStage(
        jobId: String,
        stageId: String,
        actorId: String?,
        notes: String?,
        timestamp: String
    ): DomainResult<ProductionJob> {
        val fetchResult = findJobById(jobId)
        if (fetchResult !is DomainResult.Success) return fetchResult
        val currentJob = fetchResult.data

        val skipValidation = ProductionStageLifecycleValidator.validateSkipStage(currentJob, stageId)
        if (skipValidation !is DomainResult.Success) return DomainResult.Error(
            message = (skipValidation as DomainResult.Error).message
        )

        val targetStage = currentJob.stages.find { it.stageId == stageId }!!
        val updatedStages = currentJob.stages.map { stage ->
            if (stage.stageId == stageId) {
                stage.copy(
                    status = ProductionStageStatus.SKIPPED,
                    notes = notes ?: stage.notes
                )
            } else {
                stage
            }
        }

        val updatedJob = currentJob.copy(
            stages = updatedStages,
            updatedAt = timestamp,
            updatedBy = actorId ?: currentJob.updatedBy
        )

        val updateResult = updateJob(updatedJob)
        if (updateResult is DomainResult.Success) {
            recordActivity(
                jobId = jobId,
                stageId = stageId,
                stageType = targetStage.stageType,
                operatorId = null,
                operatorName = null,
                eventType = ProductionActivityType.STAGE_SKIPPED,
                message = notes,
                timestamp = timestamp,
                createdBy = actorId
            )
        }
        return updateResult
    }

    override suspend fun holdJob(
        jobId: String,
        reason: String?,
        timestamp: String
    ): DomainResult<ProductionJob> {
        val fetchResult = findJobById(jobId)
        if (fetchResult !is DomainResult.Success) return fetchResult
        val currentJob = fetchResult.data

        val holdValidation = ProductionJobLifecycleValidator.validateHold(currentJob)
        if (holdValidation !is DomainResult.Success<*>) return DomainResult.Error(
            message = (holdValidation as DomainResult.Error).message
        )

        val updatedNotes = if (reason != null) "${currentJob.notes ?: ""}\n[HOLD]: $reason".trim() else currentJob.notes
        val updatedJob = currentJob.copy(
            status = ProductionJobStatus.ON_HOLD,
            notes = updatedNotes,
            updatedAt = timestamp
        )

        val updateResult = updateJob(updatedJob)
        if (updateResult is DomainResult.Success) {
            recordActivity(
                jobId = jobId,
                eventType = ProductionActivityType.JOB_HELD,
                message = reason,
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun resumeJob(
        jobId: String,
        timestamp: String
    ): DomainResult<ProductionJob> {
        val fetchResult = findJobById(jobId)
        if (fetchResult !is DomainResult.Success) return fetchResult
        val currentJob = fetchResult.data

        val resumeValidation = ProductionJobLifecycleValidator.validateResume(currentJob)
        if (resumeValidation !is DomainResult.Success<*>) return DomainResult.Error(
            message = (resumeValidation as DomainResult.Error).message
        )

        val hasAnyInProgress = currentJob.stages.any { it.status == ProductionStageStatus.IN_PROGRESS }
        val restoredStatus = if (hasAnyInProgress) {
            ProductionJobStatus.IN_PROGRESS
        } else {
            ProductionJobStatus.READY_FOR_PRODUCTION
        }

        val updatedJob = currentJob.copy(
            status = restoredStatus,
            updatedAt = timestamp
        )

        val updateResult = updateJob(updatedJob)
        if (updateResult is DomainResult.Success) {
            recordActivity(
                jobId = jobId,
                eventType = ProductionActivityType.JOB_RESUMED,
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun cancelJob(
        jobId: String,
        reason: String,
        timestamp: String
    ): DomainResult<ProductionJob> {
        val fetchResult = findJobById(jobId)
        if (fetchResult !is DomainResult.Success) return fetchResult
        val currentJob = fetchResult.data

        val cancelValidation = ProductionJobLifecycleValidator.validateCancellation(currentJob, reason)
        if (cancelValidation !is DomainResult.Success<*>) return DomainResult.Error(
            message = (cancelValidation as DomainResult.Error).message
        )

        val updatedNotes = "${currentJob.notes ?: ""}\n[CANCELLED]: $reason".trim()
        val updatedJob = currentJob.copy(
            status = ProductionJobStatus.CANCELLED,
            notes = updatedNotes,
            updatedAt = timestamp
        )

        val updateResult = updateJob(updatedJob)
        if (updateResult is DomainResult.Success) {
            recordActivity(
                jobId = jobId,
                eventType = ProductionActivityType.JOB_CANCELLED,
                message = reason,
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun markJobReady(
        jobId: String,
        timestamp: String
    ): DomainResult<ProductionJob> {
        val fetchResult = findJobById(jobId)
        if (fetchResult !is DomainResult.Success) return fetchResult
        val currentJob = fetchResult.data

        val readyValidation = ProductionJobLifecycleValidator.validateStatusTransition(
            currentJob,
            ProductionJobStatus.READY
        )
        if (readyValidation !is DomainResult.Success<*>) return DomainResult.Error(
            message = (readyValidation as DomainResult.Error).message
        )

        val updatedStages = currentJob.stages.map { stage ->
            if (stage.stageType == ProductionStageType.READY) {
                stage.copy(
                    status = ProductionStageStatus.COMPLETED,
                    completedAt = timestamp
                )
            } else {
                stage
            }
        }

        val updatedJob = currentJob.copy(
            status = ProductionJobStatus.READY,
            stages = updatedStages,
            updatedAt = timestamp
        )

        val updateResult = updateJob(updatedJob)
        if (updateResult is DomainResult.Success) {
            recordActivity(
                jobId = jobId,
                eventType = ProductionActivityType.JOB_READY,
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun deliverJob(
        jobId: String,
        timestamp: String
    ): DomainResult<ProductionJob> {
        val fetchResult = findJobById(jobId)
        if (fetchResult !is DomainResult.Success) return fetchResult
        val currentJob = fetchResult.data

        val deliverValidation = ProductionJobLifecycleValidator.validateStatusTransition(
            currentJob,
            ProductionJobStatus.DELIVERED
        )
        if (deliverValidation !is DomainResult.Success<*>) return DomainResult.Error(
            message = (deliverValidation as DomainResult.Error).message
        )

        val updatedStages = currentJob.stages.map { stage ->
            if (stage.stageType == ProductionStageType.DELIVERED) {
                stage.copy(
                    status = ProductionStageStatus.COMPLETED,
                    completedAt = timestamp
                )
            } else {
                stage
            }
        }

        val updatedJob = currentJob.copy(
            status = ProductionJobStatus.DELIVERED,
            stages = updatedStages,
            updatedAt = timestamp
        )

        val updateResult = updateJob(updatedJob)
        if (updateResult is DomainResult.Success) {
            recordActivity(
                jobId = jobId,
                eventType = ProductionActivityType.JOB_DELIVERED,
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun assignStageOperator(
        jobId: String,
        stageId: String,
        operatorId: String,
        operatorName: String,
        assignedBy: String?,
        notes: String?,
        timestamp: String
    ): DomainResult<ProductionJob> {
        val fetchResult = findJobById(jobId)
        if (fetchResult !is DomainResult.Success) return fetchResult
        val currentJob = fetchResult.data

        val validation = ProductionStageAssignmentValidator.validateAssignment(
            job = currentJob,
            stageId = stageId,
            operatorId = operatorId,
            operatorName = operatorName
        )
        if (validation !is DomainResult.Success) {
            return DomainResult.Error(message = (validation as DomainResult.Error).message)
        }
        val stage = validation.data

        val existingAssignments = observeStageAssignments().first()
        val activeForStage = existingAssignments.find { it.jobId == jobId && it.stageId == stageId && it.isActive }
        if (activeForStage != null) {
            return DomainResult.Error(
                message = "Stage '${stage.stageType.defaultLabel}' already has an active operator assignment (${activeForStage.operatorName}). Use reassign instead."
            )
        }

        val assignment = ProductionStageAssignment(
            assignmentId = "asg-" + UUID.randomUUID().toString(),
            jobId = jobId,
            stageId = stageId,
            stageType = stage.stageType,
            operatorId = operatorId,
            operatorName = operatorName,
            assignedAt = timestamp,
            assignedBy = assignedBy,
            notes = notes,
            status = StageAssignmentStatus.ASSIGNED
        )

        val insertResult = dataSource.insertAssignment(assignment)
        if (insertResult !is DomainResult.Success) {
            return DomainResult.Error(message = (insertResult as DomainResult.Error).message)
        }

        val updatedStages = currentJob.stages.map {
            if (it.stageId == stageId) {
                it.copy(assignedUserId = operatorId, assignedUserName = operatorName)
            } else {
                it
            }
        }
        val updatedJob = currentJob.copy(stages = updatedStages, updatedAt = timestamp)
        val updateResult = updateJob(updatedJob)
        if (updateResult is DomainResult.Success) {
            recordActivity(
                jobId = jobId,
                stageId = stageId,
                stageType = stage.stageType,
                operatorId = operatorId,
                operatorName = operatorName,
                eventType = ProductionActivityType.STAGE_ASSIGNED,
                message = notes ?: "Assigned to $operatorName",
                timestamp = timestamp,
                createdBy = assignedBy
            )
        }
        return updateResult
    }

    override suspend fun reassignStageOperator(
        jobId: String,
        stageId: String,
        newOperatorId: String,
        newOperatorName: String,
        reassignedBy: String?,
        notes: String?,
        timestamp: String
    ): DomainResult<ProductionJob> {
        val fetchResult = findJobById(jobId)
        if (fetchResult !is DomainResult.Success) return fetchResult
        val currentJob = fetchResult.data

        val existingAssignments = observeStageAssignments().first()
        val activeForStage = existingAssignments.find { it.jobId == jobId && it.stageId == stageId && it.isActive }

        val validation = ProductionStageAssignmentValidator.validateReassignment(
            job = currentJob,
            stageId = stageId,
            currentAssignment = activeForStage,
            newOperatorId = newOperatorId,
            newOperatorName = newOperatorName
        )
        if (validation !is DomainResult.Success) {
            return DomainResult.Error(message = (validation as DomainResult.Error).message)
        }
        val stage = validation.data

        if (activeForStage != null) {
            val updatedOld = activeForStage.copy(
                status = StageAssignmentStatus.REASSIGNED,
                reassignedAt = timestamp,
                reassignedBy = reassignedBy
            )
            dataSource.updateAssignment(updatedOld)
        }

        val newAssignment = ProductionStageAssignment(
            assignmentId = "asg-" + UUID.randomUUID().toString(),
            jobId = jobId,
            stageId = stageId,
            stageType = stage.stageType,
            operatorId = newOperatorId,
            operatorName = newOperatorName,
            assignedAt = timestamp,
            assignedBy = reassignedBy,
            notes = notes,
            status = StageAssignmentStatus.ASSIGNED
        )
        dataSource.insertAssignment(newAssignment)

        val updatedStages = currentJob.stages.map {
            if (it.stageId == stageId) {
                it.copy(assignedUserId = newOperatorId, assignedUserName = newOperatorName)
            } else {
                it
            }
        }
        val updatedJob = currentJob.copy(stages = updatedStages, updatedAt = timestamp)
        val updateResult = updateJob(updatedJob)
        if (updateResult is DomainResult.Success) {
            recordActivity(
                jobId = jobId,
                stageId = stageId,
                stageType = stage.stageType,
                operatorId = newOperatorId,
                operatorName = newOperatorName,
                eventType = ProductionActivityType.STAGE_REASSIGNED,
                message = notes ?: "Reassigned to $newOperatorName",
                timestamp = timestamp,
                createdBy = reassignedBy
            )
        }
        return updateResult
    }

    override suspend fun unassignStageOperator(
        jobId: String,
        stageId: String,
        unassignedBy: String?,
        reason: String?,
        timestamp: String
    ): DomainResult<ProductionJob> {
        val fetchResult = findJobById(jobId)
        if (fetchResult !is DomainResult.Success) return fetchResult
        val currentJob = fetchResult.data

        val existingAssignments = observeStageAssignments().first()
        val activeForStage = existingAssignments.find { it.jobId == jobId && it.stageId == stageId && it.isActive }

        val validation = ProductionStageAssignmentValidator.validateUnassignment(
            job = currentJob,
            stageId = stageId,
            currentAssignment = activeForStage
        )
        if (validation !is DomainResult.Success) {
            return DomainResult.Error(message = (validation as DomainResult.Error).message)
        }

        if (activeForStage != null) {
            val updatedOld = activeForStage.copy(
                status = StageAssignmentStatus.UNASSIGNED,
                reassignedAt = timestamp,
                reassignedBy = unassignedBy,
                notes = if (reason != null) "${activeForStage.notes ?: ""}\nUnassigned: $reason".trim() else activeForStage.notes
            )
            dataSource.updateAssignment(updatedOld)
        }

        val updatedStages = currentJob.stages.map {
            if (it.stageId == stageId) {
                it.copy(assignedUserId = null, assignedUserName = null)
            } else {
                it
            }
        }
        val updatedJob = currentJob.copy(stages = updatedStages, updatedAt = timestamp)
        val updateResult = updateJob(updatedJob)
        if (updateResult is DomainResult.Success) {
            recordActivity(
                jobId = jobId,
                stageId = stageId,
                stageType = null,
                operatorId = null,
                operatorName = null,
                eventType = ProductionActivityType.STAGE_UNASSIGNED,
                message = reason ?: "Operator unassigned",
                timestamp = timestamp,
                createdBy = unassignedBy
            )
        }
        return updateResult
    }

    override fun getStageAssignment(jobId: String, stageId: String): Flow<ProductionStageAssignment?> {
        return dataSource.observeAssignments().map { assignments ->
            assignments.find { it.jobId == jobId && it.stageId == stageId && it.isActive }
        }
    }

    override fun getAssignmentsForJob(jobId: String): Flow<List<ProductionStageAssignment>> {
        return dataSource.observeAssignments().map { assignments ->
            assignments.filter { it.jobId == jobId }
        }
    }

    override fun getAssignmentsForOperator(operatorId: String): Flow<List<ProductionStageAssignment>> {
        return dataSource.observeAssignments().map { assignments ->
            assignments.filter { it.operatorId == operatorId }
        }
    }

    override fun observeStageAssignments(): Flow<List<ProductionStageAssignment>> {
        return dataSource.observeAssignments()
    }

    override fun getAvailableOperators(): List<ProductionOperator> {
        return listOf(
            ProductionOperator("op-01", "রহিম আহমেদ (Rahim Ahmed)", UserRole.STAFF, "+8801711001122"),
            ProductionOperator("op-02", "করিম চৌধুরী (Karim Chowdhury)", UserRole.STAFF, "+8801711002233"),
            ProductionOperator("op-03", "তানভীর হাসান (Tanveer Hassan)", UserRole.DESIGNER, "+8801711003344"),
            ProductionOperator("op-04", "মাহমুদ আলম (Mahmud Alam)", UserRole.QC_INSPECTOR, "+8801711004455"),
            ProductionOperator("op-05", "রফিকুল ইসলাম (Rafiqul Islam)", UserRole.STAFF, "+8801711005566")
        )
    }

    override fun getStageExecution(jobId: String, stageId: String): Flow<ProductionStageExecution?> {
        return dataSource.observeExecutions().map { executions ->
            executions.find { it.jobId == jobId && it.stageId == stageId }
        }
    }

    override fun getStageExecutionsForJob(jobId: String): Flow<List<ProductionStageExecution>> {
        return dataSource.observeExecutions().map { executions ->
            executions.filter { it.jobId == jobId }
        }
    }

    override fun observeStageExecutions(): Flow<List<ProductionStageExecution>> {
        return dataSource.observeExecutions()
    }

    override fun getProductionActivityEvents(jobId: String): Flow<List<ProductionActivityEvent>> {
        return dataSource.observeActivityEvents().map { events ->
            events.filter { it.jobId == jobId }.reversed().sortedByDescending { it.timestamp }
        }
    }

    override fun observeProductionActivityEvents(): Flow<List<ProductionActivityEvent>> {
        return dataSource.observeActivityEvents().map { it.reversed().sortedByDescending { event -> event.timestamp } }
    }

    override suspend fun addStageExecutionNote(
        jobId: String,
        stageId: String,
        note: String,
        actorId: String?,
        actorName: String?,
        timestamp: String
    ): DomainResult<ProductionJob> {
        val fetchResult = findJobById(jobId)
        if (fetchResult !is DomainResult.Success) return fetchResult
        val currentJob = fetchResult.data

        if (currentJob.status.isTerminal) {
            return DomainResult.Error(message = "Cannot add execution note to ${currentJob.status.defaultLabel} job.")
        }

        val stage = currentJob.stages.find { it.stageId == stageId }
            ?: return DomainResult.Error(message = "Stage '$stageId' not found on job '${currentJob.jobNumber}'.")

        val updatedStages = currentJob.stages.map {
            if (it.stageId == stageId) {
                val combinedNotes = if (it.notes.isNullOrBlank()) note else "${it.notes}\n$note"
                it.copy(notes = combinedNotes)
            } else {
                it
            }
        }

        val updatedJob = currentJob.copy(stages = updatedStages, updatedAt = timestamp)
        val updateResult = updateJob(updatedJob)
        if (updateResult is DomainResult.Success) {
            recordActivity(
                jobId = jobId,
                stageId = stageId,
                stageType = stage.stageType,
                operatorId = actorId,
                operatorName = actorName,
                eventType = ProductionActivityType.STAGE_EXECUTION_NOTE,
                message = note,
                timestamp = timestamp,
                createdBy = actorName ?: actorId
            )
        }
        return updateResult
    }

    override fun observeStageOutputs(): Flow<List<com.sucharu.sucharupro.domain.model.job.ProductionStageOutput>> = dataSource.observeOutputs()

    override fun getStageOutputs(jobId: String, stageId: String): Flow<List<com.sucharu.sucharupro.domain.model.job.ProductionStageOutput>> {
        return dataSource.observeOutputs().map { outputs ->
            outputs.filter { it.jobId == jobId && it.stageId == stageId }.reversed().sortedByDescending { it.recordedAt }
        }
    }

    override fun getStageOutputsForJob(jobId: String): Flow<List<com.sucharu.sucharupro.domain.model.job.ProductionStageOutput>> {
        return dataSource.observeOutputs().map { outputs ->
            outputs.filter { it.jobId == jobId }.reversed().sortedByDescending { it.recordedAt }
        }
    }

    override fun getTotalStageOutput(jobId: String, stageId: String): Flow<Int> {
        return dataSource.observeOutputs().map { outputs ->
            com.sucharu.sucharupro.domain.validation.ProductionStageOutputValidator.calculateTotalOutput(outputs, jobId, stageId)
        }
    }

    override fun getRemainingStageQuantity(jobId: String, stageId: String): Flow<Int> {
        return kotlinx.coroutines.flow.combine(
            getJobById(jobId),
            getTotalStageOutput(jobId, stageId)
        ) { job, totalOutput ->
            if (job == null) 0 else com.sucharu.sucharupro.domain.validation.ProductionStageOutputValidator.calculateRemainingQuantity(job.quantity, totalOutput)
        }
    }

    override suspend fun recordStageOutput(
        jobId: String,
        stageId: String,
        quantity: Int,
        unit: String,
        operatorId: String?,
        operatorName: String?,
        recordedBy: String?,
        recordedByName: String?,
        remarks: String?,
        timestamp: String
    ): DomainResult<com.sucharu.sucharupro.domain.model.job.ProductionStageOutput> {
        val fetchResult = findJobById(jobId)
        if (fetchResult !is DomainResult.Success) return DomainResult.Error(
            message = (fetchResult as? DomainResult.Error)?.message ?: "Job not found with ID: $jobId"
        )
        val currentJob = fetchResult.data

        val existingOutputs = observeStageOutputs().first()
        val validation = com.sucharu.sucharupro.domain.validation.ProductionStageOutputValidator.validateOutput(
            job = currentJob,
            stageId = stageId,
            existingOutputs = existingOutputs,
            quantity = quantity,
            unit = unit
        )
        if (validation !is DomainResult.Success) {
            return DomainResult.Error(message = (validation as DomainResult.Error).message)
        }
        val stage = validation.data

        val existingExecutions = observeStageExecutions().first()
        val currentExecution = existingExecutions.find {
            it.jobId == jobId && it.stageId == stageId && it.status == ProductionStageStatus.IN_PROGRESS
        }

        val effectiveOperatorId = operatorId ?: stage.assignedUserId
        val effectiveOperatorName = operatorName ?: stage.assignedUserName

        val output = com.sucharu.sucharupro.domain.model.job.ProductionStageOutput(
            outputId = "out-" + UUID.randomUUID().toString(),
            jobId = jobId,
            stageId = stageId,
            stageType = stage.stageType,
            quantity = quantity,
            unit = unit,
            recordedAt = timestamp,
            operatorId = effectiveOperatorId,
            operatorName = effectiveOperatorName,
            recordedBy = recordedBy,
            recordedByName = recordedByName,
            executionId = currentExecution?.executionId,
            remarks = remarks
        )

        val insertResult = dataSource.insertOutput(output)
        if (insertResult is DomainResult.Success) {
            val message = "${stage.stageType.defaultLabel} stage output recorded: $quantity $unit" +
                if (!remarks.isNullOrBlank()) " ($remarks)" else ""
            recordActivity(
                jobId = jobId,
                stageId = stageId,
                stageType = stage.stageType,
                operatorId = effectiveOperatorId,
                operatorName = effectiveOperatorName,
                eventType = com.sucharu.sucharupro.domain.model.job.ProductionActivityType.STAGE_OUTPUT_RECORDED,
                message = message,
                timestamp = timestamp,
                createdBy = recordedByName ?: recordedBy ?: effectiveOperatorName
            )
        }
        return insertResult
    }

    override fun observeProductionMonitoringSnapshot(): Flow<com.sucharu.sucharupro.domain.model.job.ProductionMonitoringSnapshot> {
        return kotlinx.coroutines.flow.combine(
            dataSource.observeJobs(),
            dataSource.observeAssignments(),
            dataSource.observeExecutions()
        ) { jobs, assignments, executions ->
            com.sucharu.sucharupro.domain.validation.ProductionMonitoringCalculator.computeSnapshot(
                jobs = jobs,
                assignments = assignments,
                executions = executions
            )
        }
    }

    override fun observeActiveProductionStages(): Flow<List<com.sucharu.sucharupro.domain.model.job.ActiveProductionStageItem>> {
        return kotlinx.coroutines.flow.combine(
            dataSource.observeJobs(),
            dataSource.observeExecutions()
        ) { jobs, executions ->
            com.sucharu.sucharupro.domain.validation.ProductionMonitoringCalculator.computeActiveStages(
                jobs = jobs,
                executions = executions
            )
        }
    }

    override fun observeOperatorWorkloads(): Flow<List<com.sucharu.sucharupro.domain.model.job.OperatorWorkloadItem>> {
        return dataSource.observeJobs().map { jobs ->
            com.sucharu.sucharupro.domain.validation.ProductionMonitoringCalculator.computeOperatorWorkloads(
                jobs = jobs
            )
        }
    }

    override fun observeProductionAttentionItems(): Flow<List<com.sucharu.sucharupro.domain.model.job.ProductionAttentionItem>> {
        return dataSource.observeJobs().map { jobs ->
            com.sucharu.sucharupro.domain.validation.ProductionMonitoringCalculator.computeAttentionItems(
                jobs = jobs
            )
        }
    }

    override fun observeProductionHistory(): Flow<List<com.sucharu.sucharupro.domain.model.job.ProductionHistorySummary>> {
        return kotlinx.coroutines.flow.combine(
            dataSource.observeJobs(),
            dataSource.observeExecutions(),
            dataSource.observeOutputs(),
            dataSource.observeAssignments()
        ) { jobs, executions, outputs, assignments ->
            com.sucharu.sucharupro.domain.validation.ProductionHistoryCalculator.computeHistorySummaries(
                jobs = jobs,
                executions = executions,
                outputs = outputs,
                assignments = assignments
            )
        }
    }

    override fun observeProductionPerformanceMetrics(
        dateRange: com.sucharu.sucharupro.domain.model.job.ProductionDateRangeFilter
    ): Flow<com.sucharu.sucharupro.domain.model.job.ProductionPerformanceMetrics> {
        return kotlinx.coroutines.flow.combine(
            dataSource.observeJobs(),
            dataSource.observeExecutions(),
            dataSource.observeOutputs(),
            dataSource.observeAssignments()
        ) { jobs, executions, outputs, assignments ->
            com.sucharu.sucharupro.domain.validation.ProductionHistoryCalculator.computePerformanceMetrics(
                jobs = jobs,
                executions = executions,
                outputs = outputs,
                assignments = assignments,
                dateRange = dateRange
            )
        }
    }

    override fun observeOperatorPerformance(): Flow<List<com.sucharu.sucharupro.domain.model.job.ProductionOperatorPerformanceItem>> {
        return kotlinx.coroutines.flow.combine(
            dataSource.observeJobs(),
            dataSource.observeExecutions(),
            dataSource.observeOutputs(),
            dataSource.observeAssignments()
        ) { jobs, executions, outputs, assignments ->
            com.sucharu.sucharupro.domain.validation.ProductionHistoryCalculator.computeOperatorPerformance(
                jobs = jobs,
                executions = executions,
                outputs = outputs,
                assignments = assignments
            )
        }
    }

    override fun observeStagePerformance(): Flow<List<com.sucharu.sucharupro.domain.model.job.ProductionStagePerformanceItem>> {
        return kotlinx.coroutines.flow.combine(
            dataSource.observeJobs(),
            dataSource.observeExecutions(),
            dataSource.observeOutputs()
        ) { jobs, executions, outputs ->
            com.sucharu.sucharupro.domain.validation.ProductionHistoryCalculator.computeStagePerformance(
                jobs = jobs,
                executions = executions,
                outputs = outputs
            )
        }
    }

    override fun getProductionJobCompletionSummary(
        jobId: String
    ): Flow<DomainResult<com.sucharu.sucharupro.domain.model.job.ProductionJobCompletionSummary>> {
        return kotlinx.coroutines.flow.combine(
            dataSource.observeJobs(),
            dataSource.observeExecutions(),
            dataSource.observeOutputs(),
            dataSource.observeAssignments(),
            dataSource.observeActivityEvents()
        ) { args: Array<Any?> ->
            @Suppress("UNCHECKED_CAST")
            val jobs = args[0] as List<com.sucharu.sucharupro.domain.model.job.ProductionJob>
            @Suppress("UNCHECKED_CAST")
            val executions = args[1] as List<com.sucharu.sucharupro.domain.model.job.ProductionStageExecution>
            @Suppress("UNCHECKED_CAST")
            val outputs = args[2] as List<com.sucharu.sucharupro.domain.model.job.ProductionStageOutput>
            @Suppress("UNCHECKED_CAST")
            val assignments = args[3] as List<com.sucharu.sucharupro.domain.model.job.ProductionStageAssignment>
            @Suppress("UNCHECKED_CAST")
            val activities = args[4] as List<com.sucharu.sucharupro.domain.model.job.ProductionActivityEvent>

            val job = jobs.find { it.jobId == jobId }
            if (job == null) {
                DomainResult.Error(message = "Production Job '$jobId' not found.")
            } else {
                val summary = com.sucharu.sucharupro.domain.validation.ProductionHistoryCalculator.computeJobCompletionSummary(
                    job = job,
                    executions = executions,
                    outputs = outputs,
                    assignments = assignments,
                    activities = activities
                )
                DomainResult.Success(summary)
            }
        }
    }

    override fun observeProductionOutputReconciliation(
        jobId: String
    ): Flow<DomainResult<com.sucharu.sucharupro.domain.model.job.ProductionOutputReconciliation>> {
        return kotlinx.coroutines.flow.combine(
            dataSource.observeJobs(),
            dataSource.observeOutputs()
        ) { jobs, outputs ->
            val job = jobs.find { it.jobId == jobId }
            if (job == null) {
                DomainResult.Error(message = "Production Job '$jobId' not found.")
            } else {
                val reconciliation = com.sucharu.sucharupro.domain.validation.ProductionOutputReconciliationCalculator.computeJobReconciliation(
                    job = job,
                    outputs = outputs
                )
                DomainResult.Success(reconciliation)
            }
        }
    }

    override fun getProductionOutputsForExecution(
        executionId: String
    ): Flow<List<com.sucharu.sucharupro.domain.model.job.ProductionStageOutput>> {
        return dataSource.observeOutputs().map { outputs ->
            outputs.filter { it.executionId == executionId }.reversed().sortedByDescending { it.recordedAt }
        }
    }

    override fun observeProductionCompletionChecklist(
        jobId: String
    ): Flow<DomainResult<com.sucharu.sucharupro.domain.model.job.ProductionCompletionChecklist>> {
        return kotlinx.coroutines.flow.combine(
            dataSource.observeJobs(),
            dataSource.observeExecutions(),
            dataSource.observeOutputs()
        ) { jobs, executions, outputs ->
            val job = jobs.find { it.jobId == jobId }
            if (job == null) {
                DomainResult.Error(message = "Production Job '$jobId' not found.")
            } else {
                val checklist = com.sucharu.sucharupro.domain.validation.ProductionCompletionValidator.computeCompletionChecklist(
                    job = job,
                    executions = executions,
                    outputs = outputs
                )
                DomainResult.Success(checklist)
            }
        }
    }

    override fun getProductionReadyHandoff(
        jobId: String
    ): Flow<DomainResult<com.sucharu.sucharupro.domain.model.job.ProductionReadyHandoff>> {
        return kotlinx.coroutines.flow.combine(
            dataSource.observeJobs(),
            dataSource.observeExecutions(),
            dataSource.observeOutputs(),
            dataSource.observeActivityEvents()
        ) { jobs, executions, outputs, activities ->
            val job = jobs.find { it.jobId == jobId }
            if (job == null) {
                DomainResult.Error(message = "Production Job '$jobId' not found.")
            } else if (job.status != com.sucharu.sucharupro.domain.model.job.ProductionJobStatus.READY && job.status != com.sucharu.sucharupro.domain.model.job.ProductionJobStatus.DELIVERED) {
                DomainResult.Error(message = "Job '${job.jobNumber}' is not yet in Ready/Delivered state (Current: ${job.status.defaultLabel}).")
            } else {
                val readyActivity = activities.find { it.jobId == jobId && it.eventType == com.sucharu.sucharupro.domain.model.job.ProductionActivityType.JOB_READY }
                val handoff = com.sucharu.sucharupro.domain.validation.ProductionCompletionValidator.buildProductionReadyHandoff(
                    job = job,
                    executions = executions,
                    outputs = outputs,
                    confirmedBy = readyActivity?.operatorId ?: "supervisor",
                    confirmedByName = readyActivity?.operatorName ?: "Production Supervisor",
                    remarks = readyActivity?.message,
                    timestamp = readyActivity?.timestamp ?: job.updatedAt
                )
                DomainResult.Success(handoff)
            }
        }
    }

    override suspend fun confirmProductionCompletion(
        jobId: String,
        actorId: String,
        actorName: String,
        remarks: String?,
        timestamp: String
    ): DomainResult<com.sucharu.sucharupro.domain.model.job.ProductionJob> = repositoryMutex.withLock {
        val fetchResult = findJobById(jobId)
        if (fetchResult !is DomainResult.Success) return@withLock fetchResult
        val currentJob = fetchResult.data

        val executions = observeStageExecutions().first()
        val outputs = observeStageOutputs().first()

        // 1. Completion eligibility gate
        val eligibilityResult = com.sucharu.sucharupro.domain.validation.ProductionCompletionValidator.validateCompletionEligibility(
            job = currentJob,
            executions = executions,
            outputs = outputs
        )
        if (eligibilityResult !is DomainResult.Success) {
            return@withLock DomainResult.Error(message = (eligibilityResult as DomainResult.Error).message)
        }

        // 2. Lifecycle transition validation
        val transitionResult = com.sucharu.sucharupro.domain.validation.ProductionJobLifecycleValidator.validateStatusTransition(
            job = currentJob,
            targetStatus = com.sucharu.sucharupro.domain.model.job.ProductionJobStatus.READY
        )
        if (transitionResult !is DomainResult.Success) {
            return@withLock DomainResult.Error(message = (transitionResult as DomainResult.Error).message)
        }

        // 3. Mark READY stage as COMPLETED if present and update Job
        val updatedStages = currentJob.stages.map { stage ->
            if (stage.stageType == com.sucharu.sucharupro.domain.model.production.ProductionStageType.READY) {
                stage.copy(
                    status = com.sucharu.sucharupro.domain.model.production.ProductionStageStatus.COMPLETED,
                    assignedUserId = actorId,
                    assignedUserName = actorName
                )
            } else {
                stage
            }
        }

        val updatedNotes = if (!remarks.isNullOrBlank()) {
            val existing = currentJob.notes ?: ""
            if (existing.isBlank()) "[COMPLETED]: $remarks" else "$existing\n[COMPLETED]: $remarks"
        } else {
            currentJob.notes
        }

        val updatedJob = currentJob.copy(
            status = com.sucharu.sucharupro.domain.model.job.ProductionJobStatus.READY,
            stages = updatedStages,
            notes = updatedNotes,
            updatedAt = timestamp
        )

        val updateResult = updateJob(updatedJob)
        if (updateResult is DomainResult.Success) {
            val message = if (!remarks.isNullOrBlank()) {
                "Production completed: $remarks"
            } else {
                "Production completed successfully. Job is ready for delivery."
            }
            recordActivity(
                jobId = jobId,
                operatorId = actorId,
                operatorName = actorName,
                eventType = com.sucharu.sucharupro.domain.model.job.ProductionActivityType.JOB_READY,
                message = message,
                timestamp = timestamp,
                createdBy = actorName
            )
        }
        updateResult
    }
}
