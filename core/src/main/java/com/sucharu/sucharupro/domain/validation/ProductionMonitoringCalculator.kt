package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.job.ActiveProductionStageItem
import com.sucharu.sucharupro.domain.model.job.AttentionReasonType
import com.sucharu.sucharupro.domain.model.job.OperatorWorkloadItem
import com.sucharu.sucharupro.domain.model.job.ProductionAttentionItem
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.job.ProductionOperator
import com.sucharu.sucharupro.domain.model.job.ProductionStageAssignment
import com.sucharu.sucharupro.domain.model.job.ProductionStageExecution
import com.sucharu.sucharupro.domain.model.job.ProductionMonitoringSnapshot
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus

/**
 * Pure domain calculator for aggregating production metrics, operator workloads, active stages, and attention queues.
 */
object ProductionMonitoringCalculator {

    /**
     * Computes the high-level aggregated snapshot metrics.
     */
    fun computeSnapshot(
        jobs: List<ProductionJob>,
        assignments: List<ProductionStageAssignment> = emptyList(),
        executions: List<ProductionStageExecution> = emptyList()
    ): ProductionMonitoringSnapshot {
        val totalJobs = jobs.size
        val nonTerminalJobs = jobs.filter { !it.status.isTerminal }
        val activeJobs = nonTerminalJobs.size

        val draftJobs = jobs.count { it.status == ProductionJobStatus.DRAFT }
        val readyForProductionJobs = jobs.count { it.status == ProductionJobStatus.READY_FOR_PRODUCTION }
        val inProgressJobs = jobs.count { it.status == ProductionJobStatus.IN_PROGRESS }
        val onHoldJobs = jobs.count { it.status == ProductionJobStatus.ON_HOLD }
        val readyJobs = jobs.count { it.status == ProductionJobStatus.READY }
        val deliveredJobs = jobs.count { it.status == ProductionJobStatus.DELIVERED }
        val cancelledJobs = jobs.count { it.status == ProductionJobStatus.CANCELLED }

        val allStages = jobs.flatMap { it.stages }
        val nonTerminalStages = nonTerminalJobs.flatMap { it.stages }

        val activeStageCount = nonTerminalStages.count { it.status == ProductionStageStatus.IN_PROGRESS }
        val completedStageCount = allStages.count { it.status == ProductionStageStatus.COMPLETED }
        val assignedStageCount = nonTerminalStages.count { it.assignedUserId != null }
        val unassignedPendingStageCount = nonTerminalStages.count {
            it.status == ProductionStageStatus.PENDING && it.assignedUserId == null
        }

        val overallProgressFraction = if (activeJobs > 0) {
            val totalProgress = nonTerminalJobs.sumOf { it.progressFraction.toDouble() }.toFloat()
            (totalProgress / activeJobs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        val urgentJobCount = nonTerminalJobs.count { it.priority == OrderPriority.URGENT }
        val highPriorityJobCount = nonTerminalJobs.count { it.priority == OrderPriority.HIGH }

        val attentionItems = computeAttentionItems(jobs)

        return ProductionMonitoringSnapshot(
            totalJobs = totalJobs,
            activeJobs = activeJobs,
            draftJobs = draftJobs,
            readyForProductionJobs = readyForProductionJobs,
            inProgressJobs = inProgressJobs,
            onHoldJobs = onHoldJobs,
            readyJobs = readyJobs,
            deliveredJobs = deliveredJobs,
            cancelledJobs = cancelledJobs,
            activeStageCount = activeStageCount,
            completedStageCount = completedStageCount,
            assignedStageCount = assignedStageCount,
            unassignedPendingStageCount = unassignedPendingStageCount,
            overallProgressFraction = overallProgressFraction,
            urgentJobCount = urgentJobCount,
            highPriorityJobCount = highPriorityJobCount,
            attentionRequiredCount = attentionItems.size
        )
    }

    /**
     * Computes the list of active/in-progress production stages across all jobs.
     */
    fun computeActiveStages(
        jobs: List<ProductionJob>,
        executions: List<ProductionStageExecution> = emptyList()
    ): List<ActiveProductionStageItem> {
        val activeJobs = jobs.filter { !it.status.isTerminal && it.status != ProductionJobStatus.ON_HOLD }
        val list = mutableListOf<ActiveProductionStageItem>()

        for (job in activeJobs) {
            for (stage in job.stages) {
                if (stage.status == ProductionStageStatus.IN_PROGRESS) {
                    val matchingExecution = executions.find {
                        it.jobId == job.jobId && it.stageId == stage.stageId && it.status == ProductionStageStatus.IN_PROGRESS
                    }
                    list.add(
                        ActiveProductionStageItem(
                            jobId = job.jobId,
                            jobNumber = job.jobNumber,
                            jobTitle = job.title,
                            orderNumber = job.orderNumber,
                            customerReference = job.customerId,
                            stageId = stage.stageId,
                            stageType = stage.stageType,
                            sequence = stage.sequence,
                            stageStatus = stage.status,
                            assignedOperatorId = stage.assignedUserId,
                            assignedOperatorName = stage.assignedUserName,
                            startedAt = stage.startedAt ?: matchingExecution?.startedAt,
                            durationFormatted = matchingExecution?.formattedDuration,
                            priority = job.priority,
                            progressFraction = job.progressFraction
                        )
                    )
                }
            }
        }

        return list.sortedWith(
            compareByDescending<ActiveProductionStageItem> { it.priority.ordinal }
                .thenByDescending { it.startedAt ?: "" }
        )
    }

    /**
     * Computes operator workloads and active task allocations.
     */
    fun computeOperatorWorkloads(
        jobs: List<ProductionJob>,
        availableOperators: List<ProductionOperator> = ProductionOperator.getSampleOperators()
    ): List<OperatorWorkloadItem> {
        val nonTerminalJobs = jobs.filter { !it.status.isTerminal }
        val nonTerminalStages = nonTerminalJobs.flatMap { job ->
            job.stages.map { stage -> Pair(job, stage) }
        }

        return availableOperators.map { op ->
            val assignedPairs = nonTerminalStages.filter { it.second.assignedUserId == op.operatorId }
            val inProgressCount = assignedPairs.count { it.second.status == ProductionStageStatus.IN_PROGRESS }
            val pendingAssignedCount = assignedPairs.count { it.second.status == ProductionStageStatus.PENDING }
            val completedCount = jobs.flatMap { it.stages }.count {
                it.assignedUserId == op.operatorId && it.status == ProductionStageStatus.COMPLETED
            }
            val urgentCount = assignedPairs.count {
                it.first.priority == OrderPriority.URGENT &&
                        (it.second.status == ProductionStageStatus.IN_PROGRESS || it.second.status == ProductionStageStatus.PENDING)
            }

            val firstInProgress = assignedPairs.find { it.second.status == ProductionStageStatus.IN_PROGRESS }
            val currentJobSummary = firstInProgress?.let {
                "${it.first.jobNumber} - ${it.second.stageType.defaultLabel}"
            }

            OperatorWorkloadItem(
                operatorId = op.operatorId,
                operatorName = op.operatorName,
                activeWorkCount = inProgressCount + pendingAssignedCount,
                inProgressCount = inProgressCount,
                pendingAssignedCount = pendingAssignedCount,
                completedCount = completedCount,
                urgentCount = urgentCount,
                currentJobSummary = currentJobSummary
            )
        }.sortedWith(
            compareByDescending<OperatorWorkloadItem> { it.inProgressCount }
                .thenByDescending { it.activeWorkCount }
                .thenBy { it.operatorName }
        )
    }

    /**
     * Computes the derived supervisor attention queue items.
     */
    fun computeAttentionItems(jobs: List<ProductionJob>): List<ProductionAttentionItem> {
        val items = mutableListOf<ProductionAttentionItem>()
        val nonTerminalJobs = jobs.filter { !it.status.isTerminal }

        for (job in nonTerminalJobs) {
            // 1. On-Hold Job
            if (job.status == ProductionJobStatus.ON_HOLD) {
                items.add(
                    ProductionAttentionItem(
                        itemId = "att-hold-${job.jobId}",
                        reasonType = AttentionReasonType.ON_HOLD_JOB,
                        title = "Job On Hold",
                        description = "Job '${job.jobNumber}' (${job.title}) is currently on hold.",
                        jobId = job.jobId,
                        jobNumber = job.jobNumber,
                        jobTitle = job.title,
                        priority = job.priority
                    )
                )
            }

            // 2. Urgent Active Job
            if (job.priority == OrderPriority.URGENT && job.status == ProductionJobStatus.IN_PROGRESS) {
                items.add(
                    ProductionAttentionItem(
                        itemId = "att-urg-${job.jobId}",
                        reasonType = AttentionReasonType.URGENT_ACTIVE_JOB,
                        title = "Urgent Job In Production",
                        description = "Urgent Job '${job.jobNumber}' (${job.title}) requires priority processing.",
                        jobId = job.jobId,
                        jobNumber = job.jobNumber,
                        jobTitle = job.title,
                        priority = job.priority
                    )
                )
            }

            // 3. Ready for Delivery
            if (job.status == ProductionJobStatus.READY) {
                items.add(
                    ProductionAttentionItem(
                        itemId = "att-rdy-${job.jobId}",
                        reasonType = AttentionReasonType.READY_FOR_DELIVERY,
                        title = "Ready for Delivery",
                        description = "Job '${job.jobNumber}' (${job.title}) has completed production and is ready for delivery.",
                        jobId = job.jobId,
                        jobNumber = job.jobNumber,
                        jobTitle = job.title,
                        priority = job.priority
                    )
                )
            }

            // 4. Unassigned eligible stage or Ready to Start stage
            if (job.status != ProductionJobStatus.ON_HOLD) {
                val eligibleStage = job.stages.find { stage ->
                    stage.status == ProductionStageStatus.PENDING &&
                            job.stages.filter { it.sequence < stage.sequence }.all {
                                it.status == ProductionStageStatus.COMPLETED || it.status == ProductionStageStatus.SKIPPED
                            }
                }

                if (eligibleStage != null) {
                    if (eligibleStage.assignedUserId == null) {
                        items.add(
                            ProductionAttentionItem(
                                itemId = "att-unassign-${job.jobId}-${eligibleStage.stageId}",
                                reasonType = AttentionReasonType.UNASSIGNED_ELIGIBLE_STAGE,
                                title = "Unassigned Next Stage",
                                description = "Stage '${eligibleStage.stageType.defaultLabel}' on Job '${job.jobNumber}' (${job.title}) is ready to start but has no operator assigned.",
                                jobId = job.jobId,
                                jobNumber = job.jobNumber,
                                jobTitle = job.title,
                                stageId = eligibleStage.stageId,
                                stageType = eligibleStage.stageType,
                                priority = job.priority
                            )
                        )
                    } else {
                        items.add(
                            ProductionAttentionItem(
                                itemId = "att-ready-start-${job.jobId}-${eligibleStage.stageId}",
                                reasonType = AttentionReasonType.WAITING_TO_START,
                                title = "Stage Ready to Start",
                                description = "Stage '${eligibleStage.stageType.defaultLabel}' on Job '${job.jobNumber}' (${job.title}) is assigned to ${eligibleStage.assignedUserName} and ready to start.",
                                jobId = job.jobId,
                                jobNumber = job.jobNumber,
                                jobTitle = job.title,
                                stageId = eligibleStage.stageId,
                                stageType = eligibleStage.stageType,
                                priority = job.priority,
                                operatorName = eligibleStage.assignedUserName
                            )
                        )
                    }
                }
            }
        }

        return items.sortedWith(
            compareByDescending<ProductionAttentionItem> { it.priority.ordinal }
                .thenBy { it.reasonType.ordinal }
        )
    }
}
