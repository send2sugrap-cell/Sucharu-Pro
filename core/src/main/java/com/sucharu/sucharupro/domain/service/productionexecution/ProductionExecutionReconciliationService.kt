package com.sucharu.sucharupro.domain.service.productionexecution

import com.sucharu.sucharupro.domain.model.commercialcommitment.CommercialCommitment
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.printingquote.PrintingQuote
import com.sucharu.sucharupro.domain.model.printingquote.PrintingQuoteVersion
import com.sucharu.sucharupro.domain.model.productionexecution.*
import com.sucharu.sucharupro.domain.model.productionplanning.ProductionPlanningSnapshot

object ProductionExecutionReconciliationService {

    /**
     * Executes 7-way multi-tier reconciliation.
     */
    fun reconcile(
        job: ProductionJobExecution,
        order: Order,
        commitment: CommercialCommitment?,
        quote: PrintingQuote?,
        version: PrintingQuoteVersion?,
        planningSnapshot: ProductionPlanningSnapshot
    ): ProductionExecutionReconciliationResult {
        val discrepancies = mutableListOf<String>()

        // 1. Order Match
        val orderMatch = job.orderId == order.orderId && job.customerId == order.customerId
        if (!orderMatch) {
            discrepancies.add("Order or Customer ID mismatch: Job=${job.orderId}/${job.customerId}, Canonical Order=${order.orderId}/${order.customerId}")
        }

        // 2. Commitment Match
        val commitmentMatch = commitment == null || (job.commercialCommitmentId == commitment.commitmentId && job.customerId == commitment.customerId)
        if (!commitmentMatch) {
            discrepancies.add("Commercial commitment mismatch: Job=${job.commercialCommitmentId}, Commitment=${commitment?.commitmentId}")
        }

        // 3. Quotation Match
        val quoteMatch = quote == null || (job.quotationId == quote.quoteId)
        if (!quoteMatch) {
            discrepancies.add("Quotation ID mismatch: Job=${job.quotationId}, Quote=${quote?.quoteId}")
        }

        // 4. Planning Match
        val planningMatch = job.planningId == planningSnapshot.planningId && job.planningVersion == planningSnapshot.version
        if (!planningMatch) {
            discrepancies.add("Planning snapshot mismatch: Job=${job.planningId}v${job.planningVersion}, Snapshot=${planningSnapshot.planningId}v${planningSnapshot.version}")
        }

        // 5. Work Orders Completeness
        val workOrdersComplete = job.workOrders.isNotEmpty() && job.workOrders.filter { it.isMandatory }.all {
            it.status == WorkOrderStatus.COMPLETED || it.status == WorkOrderStatus.SKIPPED
        }

        // 6. Quantity Balance
        val quantityBalanced = ProductionExecutionMathUtils.isQuantityBalanced(
            planned = job.plannedQuantity,
            completed = job.completedQuantity,
            rejected = job.rejectedQuantity,
            wastage = job.wastageQuantity,
            remaining = job.remainingQuantity
        )
        if (!quantityBalanced) {
            discrepancies.add("Quantity imbalance: planned=${job.plannedQuantity.p4()}, actual sum=${(job.completedQuantity + job.rejectedQuantity + job.wastageQuantity + job.remainingQuantity).p4()}")
        }

        // 7. QC Checkpoints Passed
        val qcCheckpointsPassed = job.workOrders.filter { it.isQcCheckpoint }.all {
            it.status == WorkOrderStatus.COMPLETED
        }
        if (!qcCheckpointsPassed) {
            discrepancies.add("Mandatory QC checkpoints have not all completed successfully.")
        }

        val isFullyReconciled = orderMatch && commitmentMatch && quoteMatch && planningMatch && quantityBalanced && (discrepancies.isEmpty() || (job.status != ProductionJobExecutionStatus.COMPLETED && discrepancies.size <= 2))

        return ProductionExecutionReconciliationResult(
            executionJobId = job.executionJobId,
            orderId = job.orderId,
            isFullyReconciled = isFullyReconciled,
            quotationMatch = quoteMatch,
            commitmentMatch = commitmentMatch,
            orderMatch = orderMatch,
            planningMatch = planningMatch,
            workOrdersComplete = workOrdersComplete,
            quantityBalanced = quantityBalanced,
            qcCheckpointsPassed = qcCheckpointsPassed,
            discrepancies = discrepancies,
            reconciledAt = System.currentTimeMillis()
        )
    }
}
