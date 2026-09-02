package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.job.CompletionFilter
import com.sucharu.sucharupro.domain.model.job.ProductionActivityEvent
import com.sucharu.sucharupro.domain.model.job.ProductionDateRangeFilter
import com.sucharu.sucharupro.domain.model.job.ProductionDurationCalculator
import com.sucharu.sucharupro.domain.model.job.ProductionHistoryFilter
import com.sucharu.sucharupro.domain.model.job.ProductionHistorySortBy
import com.sucharu.sucharupro.domain.model.job.ProductionHistorySummary
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobCompletionSummary
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.job.ProductionOperator
import com.sucharu.sucharupro.domain.model.job.ProductionOperatorPerformanceItem
import com.sucharu.sucharupro.domain.model.job.ProductionPerformanceMetrics
import com.sucharu.sucharupro.domain.model.job.ProductionStageAssignment
import com.sucharu.sucharupro.domain.model.job.ProductionStageExecution
import com.sucharu.sucharupro.domain.model.job.ProductionStageHistoryItem
import com.sucharu.sucharupro.domain.model.job.ProductionStageOutput
import com.sucharu.sucharupro.domain.model.job.ProductionStagePerformanceItem
import com.sucharu.sucharupro.domain.model.job.StageAssignmentStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageType

/**
 * Pure deterministic calculation engine for historical production read models and performance analytics.
 */
object ProductionHistoryCalculator {

    /**
     * Computes the list of historical summaries across all jobs.
     */
    fun computeHistorySummaries(
        jobs: List<ProductionJob>,
        executions: List<ProductionStageExecution> = emptyList(),
        outputs: List<ProductionStageOutput> = emptyList(),
        assignments: List<ProductionStageAssignment> = emptyList()
    ): List<ProductionHistorySummary> {
        return jobs.map { job ->
            val jobExecutions = executions.filter { it.jobId == job.jobId }
            val jobOutputs = outputs.filter { it.jobId == job.jobId }
            val totalOutput = jobOutputs.sumOf { it.quantity.toLong() }.toInt()
            val remaining = (job.quantity - totalOutput).coerceAtLeast(0)

            val completedStages = job.stages.count { it.status == ProductionStageStatus.COMPLETED }
            val skippedStages = job.stages.count { it.status == ProductionStageStatus.SKIPPED }
            val totalDurationSeconds = jobExecutions.sumOf { it.durationSeconds ?: 0L }
            val operatorCount = job.stages.mapNotNull { it.assignedUserId }.distinct().size

            val completedAt = if (job.status == ProductionJobStatus.DELIVERED || job.status == ProductionJobStatus.READY) {
                job.updatedAt
            } else {
                null
            }

            ProductionHistorySummary(
                jobId = job.jobId,
                jobNumber = job.jobNumber,
                orderId = job.orderId,
                orderNumber = job.orderNumber,
                customerId = job.customerId,
                title = job.title,
                quantity = job.quantity,
                unit = job.unit,
                priority = job.priority,
                finalStatus = job.status,
                createdAt = job.createdAt,
                updatedAt = job.updatedAt,
                completedAt = completedAt,
                totalDurationSeconds = totalDurationSeconds,
                formattedDuration = ProductionDurationCalculator.formatDuration(totalDurationSeconds),
                completedStageCount = completedStages,
                skippedStageCount = skippedStages,
                totalStageCount = job.stages.size,
                totalRecordedOutput = totalOutput,
                remainingQuantity = remaining,
                overallProgressFraction = job.progressFraction,
                operatorCount = operatorCount,
                executionCount = jobExecutions.size,
                outputRecordCount = jobOutputs.size
            )
        }.sortedByDescending { it.createdAt }
    }

    /**
     * Computes stage-by-stage execution history for a single job.
     */
    fun computeStageHistory(
        job: ProductionJob,
        executions: List<ProductionStageExecution> = emptyList(),
        outputs: List<ProductionStageOutput> = emptyList(),
        assignments: List<ProductionStageAssignment> = emptyList()
    ): List<ProductionStageHistoryItem> {
        return job.stages.sortedBy { it.sequence }.map { stage ->
            val matchingExecution = executions.find { it.jobId == job.jobId && it.stageId == stage.stageId }
            val stageOutputs = outputs.filter { it.jobId == job.jobId && it.stageId == stage.stageId }
            val stageOutputQty = stageOutputs.sumOf { it.quantity.toLong() }.toInt()
            val durationSeconds = matchingExecution?.durationSeconds ?: 0L

            val matchingAssignment = assignments.find {
                it.jobId == job.jobId && it.stageId == stage.stageId && it.isActive
            }

            ProductionStageHistoryItem(
                jobId = job.jobId,
                jobNumber = job.jobNumber,
                stageId = stage.stageId,
                stageType = stage.stageType,
                sequence = stage.sequence,
                status = stage.status,
                operatorId = stage.assignedUserId ?: matchingAssignment?.operatorId,
                operatorName = stage.assignedUserName ?: matchingAssignment?.operatorName,
                assignmentStartedAt = matchingAssignment?.assignedAt,
                executionStartedAt = stage.startedAt ?: matchingExecution?.startedAt,
                executionCompletedAt = stage.completedAt ?: matchingExecution?.completedAt,
                durationSeconds = durationSeconds,
                formattedDuration = ProductionDurationCalculator.formatDuration(durationSeconds),
                recordedOutputQuantity = stageOutputQty,
                plannedQuantity = job.quantity,
                progressFraction = ProductionStageOutputValidator.calculateProgressFraction(stageOutputQty, job.quantity),
                remarks = matchingExecution?.completionRemarks ?: matchingExecution?.startRemarks ?: stage.notes
            )
        }
    }

    /**
     * Computes consolidated completion summary for a specific job.
     */
    fun computeJobCompletionSummary(
        job: ProductionJob,
        executions: List<ProductionStageExecution> = emptyList(),
        outputs: List<ProductionStageOutput> = emptyList(),
        assignments: List<ProductionStageAssignment> = emptyList(),
        activities: List<ProductionActivityEvent> = emptyList()
    ): ProductionJobCompletionSummary {
        val stageHistory = computeStageHistory(job, executions, outputs, assignments)
        val jobExecutions = executions.filter { it.jobId == job.jobId }
        val jobOutputs = outputs.filter { it.jobId == job.jobId }
        val totalOutput = jobOutputs.sumOf { it.quantity.toLong() }.toInt()
        val remaining = (job.quantity - totalOutput).coerceAtLeast(0)
        val totalDuration = jobExecutions.sumOf { it.durationSeconds ?: 0L }

        val completedAt = if (job.status == ProductionJobStatus.DELIVERED || job.status == ProductionJobStatus.READY) {
            job.updatedAt
        } else {
            null
        }

        return ProductionJobCompletionSummary(
            jobId = job.jobId,
            jobNumber = job.jobNumber,
            jobTitle = job.title,
            orderId = job.orderId,
            orderNumber = job.orderNumber,
            customerId = job.customerId,
            plannedQuantity = job.quantity,
            unit = job.unit,
            totalRecordedOutput = totalOutput,
            remainingQuantity = remaining,
            overallProgressFraction = job.progressFraction,
            finalStatus = job.status,
            priority = job.priority,
            createdAt = job.createdAt,
            completedAt = completedAt,
            totalDurationSeconds = totalDuration,
            formattedTotalDuration = ProductionDurationCalculator.formatDuration(totalDuration),
            operatorCount = job.stages.mapNotNull { it.assignedUserId }.distinct().size,
            completedStageCount = job.stages.count { it.status == ProductionStageStatus.COMPLETED },
            skippedStageCount = job.stages.count { it.status == ProductionStageStatus.SKIPPED },
            executionCount = jobExecutions.size,
            outputRecordCount = jobOutputs.size,
            stageHistory = stageHistory,
            recentActivities = activities.filter { it.jobId == job.jobId }.take(20)
        )
    }

    /**
     * Computes high-level aggregated performance metrics and statistical KPIs.
     */
    fun computePerformanceMetrics(
        jobs: List<ProductionJob>,
        executions: List<ProductionStageExecution> = emptyList(),
        outputs: List<ProductionStageOutput> = emptyList(),
        assignments: List<ProductionStageAssignment> = emptyList(),
        dateRange: ProductionDateRangeFilter = ProductionDateRangeFilter.ALL_TIME
    ): ProductionPerformanceMetrics {
        val totalHistoricalJobs = jobs.size
        val completedJobs = jobs.count { it.status == ProductionJobStatus.READY || it.status == ProductionJobStatus.DELIVERED }
        val deliveredJobs = jobs.count { it.status == ProductionJobStatus.DELIVERED }
        val cancelledJobs = jobs.count { it.status == ProductionJobStatus.CANCELLED }
        val currentlyActiveJobs = jobs.count { !it.status.isTerminal }

        val completionRate = if (totalHistoricalJobs > 0) {
            (completedJobs.toDouble() / totalHistoricalJobs.toDouble()).coerceIn(0.0, 1.0)
        } else {
            0.0
        }

        val totalStageExecutions = executions.size
        val completedStages = executions.count { it.status == ProductionStageStatus.COMPLETED }
        val skippedStages = jobs.flatMap { it.stages }.count { it.status == ProductionStageStatus.SKIPPED }

        val durationsWithValues: List<Long> = executions.mapNotNull { it.durationSeconds }.filter { it > 0L }
        val avgStageDuration = if (durationsWithValues.isNotEmpty()) {
            (durationsWithValues.sum() / durationsWithValues.size)
        } else {
            0L
        }
        val longestDuration = durationsWithValues.maxOrNull() ?: 0L
        val shortestDuration = durationsWithValues.minOrNull() ?: 0L

        val plannedQuantity = jobs.sumOf { it.quantity.toLong() }.toInt()
        val recordedOutput = outputs.sumOf { it.quantity.toLong() }.toInt()
        val remainingQuantity = (plannedQuantity - recordedOutput).coerceAtLeast(0)
        val outputCompletionRate = if (plannedQuantity > 0) {
            (recordedOutput.toDouble() / plannedQuantity.toDouble()).coerceIn(0.0, 1.0)
        } else {
            0.0
        }

        val operatorsInvolved = assignments.map { it.operatorId }.distinct().size
        val completedAssignments = assignments.count { it.status == StageAssignmentStatus.COMPLETED }
        val activeAssignments = assignments.count { it.status == StageAssignmentStatus.ASSIGNED || it.status == StageAssignmentStatus.REASSIGNED }
        val avgExecutionDuration = if (completedAssignments > 0 && durationsWithValues.isNotEmpty()) {
            (durationsWithValues.sum() / completedAssignments)
        } else {
            avgStageDuration
        }

        return ProductionPerformanceMetrics(
            totalHistoricalJobs = totalHistoricalJobs,
            completedJobs = completedJobs,
            deliveredJobs = deliveredJobs,
            cancelledJobs = cancelledJobs,
            currentlyActiveJobs = currentlyActiveJobs,
            completionRate = completionRate,
            totalStageExecutions = totalStageExecutions,
            completedStages = completedStages,
            skippedStages = skippedStages,
            averageStageDurationSeconds = avgStageDuration,
            formattedAverageStageDuration = ProductionDurationCalculator.formatDuration(avgStageDuration),
            longestStageDurationSeconds = longestDuration,
            formattedLongestStageDuration = ProductionDurationCalculator.formatDuration(longestDuration),
            shortestStageDurationSeconds = shortestDuration,
            formattedShortestStageDuration = ProductionDurationCalculator.formatDuration(shortestDuration),
            plannedQuantity = plannedQuantity,
            recordedOutput = recordedOutput,
            remainingQuantity = remainingQuantity,
            outputCompletionRate = outputCompletionRate,
            operatorsInvolvedCount = operatorsInvolved,
            completedAssignments = completedAssignments,
            activeAssignments = activeAssignments,
            averageExecutionDurationSeconds = avgExecutionDuration,
            formattedAverageExecutionDuration = ProductionDurationCalculator.formatDuration(avgExecutionDuration)
        )
    }

    /**
     * Computes performance metrics per operator.
     */
    fun computeOperatorPerformance(
        jobs: List<ProductionJob>,
        executions: List<ProductionStageExecution> = emptyList(),
        outputs: List<ProductionStageOutput> = emptyList(),
        assignments: List<ProductionStageAssignment> = emptyList(),
        availableOperators: List<ProductionOperator> = ProductionOperator.getSampleOperators()
    ): List<ProductionOperatorPerformanceItem> {
        return availableOperators.map { op ->
            val opExecutions = executions.filter { it.operatorId == op.operatorId }
            val completedStageCount = opExecutions.count { it.status == ProductionStageStatus.COMPLETED }
            val activeStageCount = opExecutions.count { it.status == ProductionStageStatus.IN_PROGRESS }
            val assignedStageCount = assignments.count { it.operatorId == op.operatorId }

            val totalDuration = opExecutions.sumOf { it.durationSeconds ?: 0L }
            val avgDuration = if (completedStageCount > 0) totalDuration / completedStageCount else 0L

            val opOutputs = outputs.filter { it.operatorId == op.operatorId }.sumOf { it.quantity.toLong() }.toInt()
            val urgentStageCount = opExecutions.count { ex ->
                jobs.find { it.jobId == ex.jobId }?.priority == OrderPriority.URGENT
            }
            val completedJobCount = jobs.count { job ->
                job.status.isTerminal && job.stages.any { it.assignedUserId == op.operatorId }
            }

            ProductionOperatorPerformanceItem(
                operatorId = op.operatorId,
                operatorName = op.operatorName,
                completedStageCount = completedStageCount,
                activeStageCount = activeStageCount,
                assignedStageCount = assignedStageCount,
                totalExecutionSeconds = totalDuration,
                formattedTotalDuration = ProductionDurationCalculator.formatDuration(totalDuration),
                averageExecutionSeconds = avgDuration,
                formattedAverageDuration = ProductionDurationCalculator.formatDuration(avgDuration),
                outputQuantity = opOutputs,
                urgentStageCount = urgentStageCount,
                completedJobCount = completedJobCount
            )
        }.sortedByDescending { it.completedStageCount }
    }

    /**
     * Computes performance summary per canonical production stage type.
     */
    fun computeStagePerformance(
        jobs: List<ProductionJob>,
        executions: List<ProductionStageExecution> = emptyList(),
        outputs: List<ProductionStageOutput> = emptyList()
    ): List<ProductionStagePerformanceItem> {
        return ProductionStageType.entries.map { stageType ->
            val typeExecutions = executions.filter { it.stageType == stageType }
            val completedCount = typeExecutions.count { it.status == ProductionStageStatus.COMPLETED }
            val skippedCount = jobs.flatMap { it.stages }.count {
                it.stageType == stageType && it.status == ProductionStageStatus.SKIPPED
            }

            val durations: List<Long> = typeExecutions.mapNotNull { it.durationSeconds }.filter { it > 0L }
            val avgDuration = if (durations.isNotEmpty()) (durations.sum() / durations.size) else 0L
            val typeOutputs = outputs.filter { it.stageType == stageType }.sumOf { it.quantity.toLong() }.toInt()

            ProductionStagePerformanceItem(
                stageType = stageType,
                totalExecutions = typeExecutions.size,
                completedCount = completedCount,
                skippedCount = skippedCount,
                averageDurationSeconds = avgDuration,
                formattedAverageDuration = ProductionDurationCalculator.formatDuration(avgDuration),
                totalOutputQuantity = typeOutputs
            )
        }
    }

    /**
     * Filters and sorts history summaries according to [filter] and search [query].
     */
    fun filterAndSortHistory(
        summaries: List<ProductionHistorySummary>,
        filter: ProductionHistoryFilter,
        query: String
    ): List<ProductionHistorySummary> {
        val trimmedQuery = query.trim()
        val filtered = summaries.filter { item ->
            val matchesQuery = trimmedQuery.isBlank() ||
                    item.jobNumber.contains(trimmedQuery, ignoreCase = true) ||
                    item.title.contains(trimmedQuery, ignoreCase = true) ||
                    item.orderNumber.contains(trimmedQuery, ignoreCase = true) ||
                    item.customerId.contains(trimmedQuery, ignoreCase = true)

            val matchesStatus = filter.status == null || item.finalStatus == filter.status
            val matchesPriority = filter.priority == null || item.priority == filter.priority
            val matchesCompletion = when (filter.completion) {
                CompletionFilter.ALL -> true
                CompletionFilter.COMPLETED -> item.finalStatus == ProductionJobStatus.DELIVERED || item.finalStatus == ProductionJobStatus.READY
                CompletionFilter.INCOMPLETE -> !item.finalStatus.isTerminal && item.finalStatus != ProductionJobStatus.READY
            }

            matchesQuery && matchesStatus && matchesPriority && matchesCompletion
        }

        return when (filter.sortBy) {
            ProductionHistorySortBy.DATE_DESC -> filtered.sortedByDescending { it.createdAt }
            ProductionHistorySortBy.DATE_ASC -> filtered.sortedBy { it.createdAt }
            ProductionHistorySortBy.DURATION_DESC -> filtered.sortedByDescending { it.totalDurationSeconds }
            ProductionHistorySortBy.DURATION_ASC -> filtered.sortedBy { it.totalDurationSeconds }
            ProductionHistorySortBy.PROGRESS_DESC -> filtered.sortedByDescending { it.overallProgressFraction }
            ProductionHistorySortBy.PROGRESS_ASC -> filtered.sortedBy { it.overallProgressFraction }
            ProductionHistorySortBy.PRIORITY_DESC -> filtered.sortedByDescending { it.priority.ordinal }
        }
    }
}
