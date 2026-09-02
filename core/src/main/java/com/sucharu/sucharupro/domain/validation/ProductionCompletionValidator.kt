package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionCompletionChecklist
import com.sucharu.sucharupro.domain.model.job.ProductionCompletionChecklistItem
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.job.ProductionReadyHandoff
import com.sucharu.sucharupro.domain.model.job.ProductionStageExecution
import com.sucharu.sucharupro.domain.model.job.ProductionStageOutput
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageType

/**
 * Authoritative validator and calculator for Production Completion, Readiness Gate,
 * and Final Production Handoff (Module 04 Step 10).
 */
object ProductionCompletionValidator {

    /**
     * Validates whether a [job] is eligible to complete production and transition to [ProductionJobStatus.READY].
     *
     * @param job Target production job.
     * @param executions All stage execution records for the job.
     * @param outputs All recorded output records for the job.
     */
    fun validateCompletionEligibility(
        job: ProductionJob?,
        executions: List<ProductionStageExecution> = emptyList(),
        outputs: List<ProductionStageOutput> = emptyList()
    ): DomainResult<Unit> {
        // 1. Job existence check
        if (job == null) {
            return DomainResult.Error(message = "Target Production Job cannot be null.")
        }
        if (job.jobId.isBlank()) {
            return DomainResult.Error(message = "Job ID cannot be blank.")
        }

        // 2. Lifecycle state check
        when (job.status) {
            ProductionJobStatus.DRAFT -> {
                return DomainResult.Error(message = "Cannot complete a draft Job '${job.jobNumber}'.")
            }
            ProductionJobStatus.CANCELLED -> {
                return DomainResult.Error(message = "Cannot complete a cancelled Job '${job.jobNumber}'.")
            }
            ProductionJobStatus.DELIVERED -> {
                return DomainResult.Error(message = "Job '${job.jobNumber}' has already been delivered.")
            }
            ProductionJobStatus.READY -> {
                return DomainResult.Error(message = "Job '${job.jobNumber}' is already in Ready state.")
            }
            ProductionJobStatus.ON_HOLD -> {
                return DomainResult.Error(message = "Job '${job.jobNumber}' is currently on hold. Resume the job before completing.")
            }
            ProductionJobStatus.READY_FOR_PRODUCTION,
            ProductionJobStatus.IN_PROGRESS -> Unit
        }

        // 3. Stage completion gate (Stages 1..11 must be COMPLETED or SKIPPED)
        val manufacturingStages = job.stages.filter { it.sequence < ProductionStageType.READY.displayOrder }
        val incompleteStages = manufacturingStages.filter {
            it.status != ProductionStageStatus.COMPLETED && it.status != ProductionStageStatus.SKIPPED
        }

        if (incompleteStages.isNotEmpty()) {
            val pendingOrActive = incompleteStages.first()
            val stateLabel = if (pendingOrActive.status == ProductionStageStatus.IN_PROGRESS) "still IN_PROGRESS" else "has not started"
            return DomainResult.Error(
                message = "Production cannot be completed because stage '${pendingOrActive.stageType.defaultLabel}' is $stateLabel."
            )
        }

        // 4. Validate skipped stages compliance
        val invalidSkippedStages = manufacturingStages.filter {
            it.status == ProductionStageStatus.SKIPPED && !it.stageType.canBeSkipped
        }
        if (invalidSkippedStages.isNotEmpty()) {
            val invalidName = invalidSkippedStages.first().stageType.defaultLabel
            return DomainResult.Error(
                message = "Stage '$invalidName' is mandatory and cannot be skipped."
            )
        }

        // 5. Output quantity reconciliation gate
        val reconciliation = ProductionOutputReconciliationCalculator.computeJobReconciliation(job, outputs)
        if (reconciliation.remainingQuantity > 0) {
            return DomainResult.Error(
                message = "Production cannot be completed because planned quantity (${job.quantity} ${job.unit}) has not been reached. Remaining: ${reconciliation.remainingQuantity} ${job.unit} (Recorded: ${reconciliation.recordedQuantity} ${job.unit})."
            )
        }

        // 6. Multi-item reconciliation check
        if (job.items.size > 1) {
            val incompleteItem = reconciliation.itemReconciliations.find { it.remainingQuantity > 0 }
            if (incompleteItem != null) {
                return DomainResult.Error(
                    message = "Item '${incompleteItem.description}' remains incomplete. Remaining: ${incompleteItem.remainingQuantity} ${incompleteItem.unit}."
                )
            }
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Pure calculator deriving the structured [ProductionCompletionChecklist] for UI display.
     */
    fun computeCompletionChecklist(
        job: ProductionJob,
        executions: List<ProductionStageExecution> = emptyList(),
        outputs: List<ProductionStageOutput> = emptyList()
    ): ProductionCompletionChecklist {
        val blockingReasons = mutableListOf<String>()

        // 1. Job lifecycle check
        val isLifecycleValid = job.status == ProductionJobStatus.IN_PROGRESS || job.status == ProductionJobStatus.READY_FOR_PRODUCTION
        val lifecycleMessage = when {
            job.status == ProductionJobStatus.READY -> "উৎপাদন ইতিমধ্যে সম্পন্ন হয়েছে (Already Ready)"
            job.status == ProductionJobStatus.DELIVERED -> "জব ইতিমধ্যে ডেলিভারি সম্পন্ন হয়েছে (Delivered)"
            job.status == ProductionJobStatus.CANCELLED -> "জবটি বাতিল করা হয়েছে (Cancelled)"
            job.status == ProductionJobStatus.ON_HOLD -> "জবটি হোল্ড অবস্থায় আছে (On Hold)"
            isLifecycleValid -> "জবের স্ট্যাটাস সক্রিয় (${job.status.defaultLabel})"
            else -> "জবের স্ট্যাটাস অযোগ্য (${job.status.defaultLabel})"
        }
        if (!isLifecycleValid && job.status != ProductionJobStatus.READY) {
            blockingReasons.add(lifecycleMessage)
        }

        // 2. Manufacturing stages check
        val manufacturingStages = job.stages.filter { it.sequence < ProductionStageType.READY.displayOrder }
        val incompleteStages = manufacturingStages.filter {
            it.status != ProductionStageStatus.COMPLETED && it.status != ProductionStageStatus.SKIPPED
        }
        val areStagesComplete = incompleteStages.isEmpty()
        val stagesMessage = if (areStagesComplete) {
            "সকল উৎপাদন ধাপ সম্পন্ন বা স্কিপ করা হয়েছে (${manufacturingStages.size}/${manufacturingStages.size})"
        } else {
            "অসমাপ্ত ধাপ: ${incompleteStages.joinToString { it.stageType.defaultLabel }}"
        }
        if (!areStagesComplete) {
            blockingReasons.add(stagesMessage)
        }

        // 3. Execution consistency check
        val completedStages = manufacturingStages.filter { it.status == ProductionStageStatus.COMPLETED }
        val areExecutionsConsistent = completedStages.all { stage ->
            val exec = executions.find { it.jobId == job.jobId && it.stageId == stage.stageId }
            exec == null || exec.status == ProductionStageStatus.COMPLETED
        }
        val executionsMessage = if (areExecutionsConsistent) {
            "পর্যায়ভিত্তিক কাজের রেকর্ড সঠিক আছে"
        } else {
            "পর্যায়ভিত্তিক এক্সিকিউশন রেকর্ড অসম্পূর্ণ"
        }
        if (!areExecutionsConsistent) {
            blockingReasons.add(executionsMessage)
        }

        // 4. Output reconciliation check
        val reconciliation = ProductionOutputReconciliationCalculator.computeJobReconciliation(job, outputs)
        val isOutputReconciled = reconciliation.remainingQuantity == 0
        val outputMessage = when {
            reconciliation.isOverProduced -> "পরিকল্পিত লক্ষ্যমাত্রা অর্জিত (অতিরিক্ত: +${reconciliation.overProductionQuantity} ${job.unit})"
            isOutputReconciled -> "পরিকল্পিত পরিমাণ সম্পূর্ণ উৎপাদিত (${reconciliation.recordedQuantity}/${job.quantity} ${job.unit})"
            else -> "উৎপাদন ঘাটতি: অবশিষ্ট ${reconciliation.remainingQuantity} ${job.unit}"
        }
        if (!isOutputReconciled) {
            blockingReasons.add(outputMessage)
        }

        val isEligible = isLifecycleValid && areStagesComplete && areExecutionsConsistent && isOutputReconciled

        val items = listOf(
            ProductionCompletionChecklistItem(
                key = "lifecycle",
                title = "জব লাইফসাইকেল বৈধতা",
                isPassed = isLifecycleValid || job.status == ProductionJobStatus.READY,
                message = lifecycleMessage
            ),
            ProductionCompletionChecklistItem(
                key = "stages",
                title = "সকল উৎপাদন ধাপ সম্পন্ন",
                isPassed = areStagesComplete,
                message = stagesMessage
            ),
            ProductionCompletionChecklistItem(
                key = "executions",
                title = "কাজের সময় ও দায়িত্ব নির্ধারণ",
                isPassed = areExecutionsConsistent,
                message = executionsMessage
            ),
            ProductionCompletionChecklistItem(
                key = "output",
                title = "উৎপাদন পরিমাণ সামঞ্জস্য",
                isPassed = isOutputReconciled,
                message = outputMessage
            )
        )

        return ProductionCompletionChecklist(
            jobId = job.jobId,
            jobNumber = job.jobNumber,
            isEligible = isEligible,
            items = items,
            blockingReasons = blockingReasons,
            isOverProduced = reconciliation.isOverProduced,
            overProductionQuantity = reconciliation.overProductionQuantity
        )
    }

    /**
     * Builds an immutable [ProductionReadyHandoff] snapshot for downstream boundaries.
     */
    fun buildProductionReadyHandoff(
        job: ProductionJob,
        executions: List<ProductionStageExecution> = emptyList(),
        outputs: List<ProductionStageOutput> = emptyList(),
        confirmedBy: String,
        confirmedByName: String,
        remarks: String? = null,
        timestamp: String
    ): ProductionReadyHandoff {
        val reconciliation = ProductionOutputReconciliationCalculator.computeJobReconciliation(job, outputs)
        val completedStages = job.stages.count { it.status == ProductionStageStatus.COMPLETED }
        val skippedStages = job.stages.count { it.status == ProductionStageStatus.SKIPPED }
        val totalDuration = executions.filter { it.jobId == job.jobId }.sumOf { it.durationSeconds ?: 0L }
        val operatorCount = job.stages.mapNotNull { it.assignedUserId }.distinct().size

        return ProductionReadyHandoff(
            productionJobId = job.jobId,
            jobNumber = job.jobNumber,
            orderId = job.orderId,
            orderNumber = job.orderNumber,
            customerId = job.customerId,
            title = job.title,
            plannedQuantity = job.quantity,
            recordedQuantity = reconciliation.recordedQuantity,
            remainingQuantity = reconciliation.remainingQuantity,
            overProductionQuantity = reconciliation.overProductionQuantity,
            unit = job.unit,
            completionPercentage = reconciliation.completionPercentage,
            completedStageCount = completedStages,
            skippedStageCount = skippedStages,
            totalStageCount = job.stages.size,
            totalDurationSeconds = totalDuration,
            operatorCount = operatorCount,
            confirmedAt = timestamp,
            confirmedBy = confirmedBy,
            confirmedByName = confirmedByName,
            remarks = remarks,
            productionStatus = ProductionJobStatus.READY,
            items = job.items
        )
    }
}
