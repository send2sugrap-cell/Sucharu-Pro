package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.job.ProductionItemOutputReconciliation
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobItem
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionOutputReconciliation
import com.sucharu.sucharupro.domain.model.job.ProductionStageOutput
import com.sucharu.sucharupro.domain.model.job.ProductionStageOutputReconciliation

/**
 * Pure domain calculation engine for Production Output Recording and Quantity Reconciliation (Module 04 Step 09).
 */
object ProductionOutputReconciliationCalculator {

    /**
     * Calculates comprehensive quantity reconciliation for a target [ProductionJob].
     *
     * @param job Target production job entity.
     * @param outputs All recorded valid production output records.
     */
    fun computeJobReconciliation(
        job: ProductionJob,
        outputs: List<ProductionStageOutput>
    ): ProductionOutputReconciliation {
        val jobOutputs = outputs.filter { it.jobId == job.jobId }
        val planned = job.quantity
        val recorded = jobOutputs.sumOf { it.quantity.toLong() }.toInt()
        val remaining = (planned - recorded).coerceAtLeast(0)
        val overProduction = (recorded - planned).coerceAtLeast(0)
        val underProduction = (planned - recorded).coerceAtLeast(0)
        val completionPercentage = if (planned > 0) {
            (recorded.toDouble() / planned.toDouble()) * 100.0
        } else {
            0.0
        }

        val stageReconciliations = job.stages.map { stage ->
            computeStageReconciliation(job, stage, jobOutputs)
        }

        val itemReconciliations = job.items.map { item ->
            computeItemReconciliation(job, item, jobOutputs)
        }

        return ProductionOutputReconciliation(
            jobId = job.jobId,
            jobNumber = job.jobNumber,
            plannedQuantity = planned,
            recordedQuantity = recorded,
            remainingQuantity = remaining,
            overProductionQuantity = overProduction,
            underProductionQuantity = underProduction,
            completionPercentage = completionPercentage,
            unit = job.unit,
            outputRecordCount = jobOutputs.size,
            stageReconciliations = stageReconciliations,
            itemReconciliations = itemReconciliations
        )
    }

    /**
     * Computes quantity reconciliation for a specific production stage.
     */
    fun computeStageReconciliation(
        job: ProductionJob,
        stage: ProductionJobStage,
        outputs: List<ProductionStageOutput>
    ): ProductionStageOutputReconciliation {
        val stageOutputs = outputs.filter { it.jobId == job.jobId && it.stageId == stage.stageId }
        val planned = job.quantity
        val recorded = stageOutputs.sumOf { it.quantity.toLong() }.toInt()
        val remaining = (planned - recorded).coerceAtLeast(0)
        val overProduction = (recorded - planned).coerceAtLeast(0)
        val completionPercentage = if (planned > 0) {
            (recorded.toDouble() / planned.toDouble()) * 100.0
        } else {
            0.0
        }

        return ProductionStageOutputReconciliation(
            stageId = stage.stageId,
            stageType = stage.stageType,
            plannedQuantity = planned,
            recordedQuantity = recorded,
            remainingQuantity = remaining,
            overProductionQuantity = overProduction,
            completionPercentage = completionPercentage,
            outputCount = stageOutputs.size,
            unit = job.unit
        )
    }

    /**
     * Computes quantity reconciliation for an individual line item snapshot.
     */
    fun computeItemReconciliation(
        job: ProductionJob,
        item: ProductionJobItem,
        outputs: List<ProductionStageOutput>
    ): ProductionItemOutputReconciliation {
        val planned = item.quantity
        // When multiple items exist, proportional or item-matched attribution is computed
        val totalRecordedJob = outputs.filter { it.jobId == job.jobId }.sumOf { it.quantity.toLong() }.toInt()
        val itemFraction = if (job.quantity > 0) item.quantity.toDouble() / job.quantity.toDouble() else 0.0
        val recorded = (totalRecordedJob * itemFraction).toInt()
        val remaining = (planned - recorded).coerceAtLeast(0)
        val overProduction = (recorded - planned).coerceAtLeast(0)
        val underProduction = (planned - recorded).coerceAtLeast(0)
        val completionPercentage = if (planned > 0) {
            (recorded.toDouble() / planned.toDouble()) * 100.0
        } else {
            0.0
        }

        return ProductionItemOutputReconciliation(
            itemId = item.itemId,
            description = item.description,
            plannedQuantity = planned,
            recordedQuantity = recorded,
            remainingQuantity = remaining,
            overProductionQuantity = overProduction,
            underProductionQuantity = underProduction,
            completionPercentage = completionPercentage,
            unit = item.unit
        )
    }
}
