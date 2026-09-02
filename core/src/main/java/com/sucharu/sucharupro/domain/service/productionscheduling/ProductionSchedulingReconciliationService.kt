package com.sucharu.sucharupro.domain.service.productionscheduling

import com.sucharu.sucharupro.domain.model.productionexecution.ProductionJobExecution
import com.sucharu.sucharupro.domain.model.productionscheduling.ProductionDispatchQueueItem
import com.sucharu.sucharupro.domain.model.productionscheduling.ProductionSchedule
import com.sucharu.sucharupro.domain.model.productionscheduling.ProductionScheduleReconciliationResult

object ProductionSchedulingReconciliationService {

    /**
     * Executes comprehensive multi-tier integrity and reconciliation check.
     */
    fun reconcile(
        schedule: ProductionSchedule,
        job: ProductionJobExecution,
        dispatchQueue: List<ProductionDispatchQueueItem>
    ): ProductionScheduleReconciliationResult {
        val discrepancies = mutableListOf<String>()

        // 1. Check Job and Order match
        val executionJobMatch = schedule.executionJobId == job.executionJobId
        if (!executionJobMatch) discrepancies.add("Execution Job mismatch: schedule=${schedule.executionJobId}, job=${job.executionJobId}")

        val orderMatch = schedule.orderId == job.orderId
        if (!orderMatch) discrepancies.add("Order ID mismatch: schedule=${schedule.orderId}, job=${job.orderId}")

        // 2. Planning match
        val planningMatch = job.planningId.isNotBlank()
        if (!planningMatch) discrepancies.add("Missing planning provenance on job execution.")

        // 3. Work Order and Slot alignment
        val workOrderIds = job.workOrders.map { it.workOrderId }.toSet()
        val slotWorkOrderIds = schedule.slots.map { it.workOrderId }.toSet()
        val workOrdersMatched = workOrderIds.size == slotWorkOrderIds.size && workOrderIds.containsAll(slotWorkOrderIds)
        if (!workOrdersMatched) {
            discrepancies.add("Work orders count/IDs do not match schedule slots. Job WOs=${workOrderIds.size}, Slots=${slotWorkOrderIds.size}")
        }

        // 4. Slots complete
        val slotsComplete = schedule.slots.isNotEmpty() && schedule.totalEstimatedMinutes > 0
        if (!slotsComplete) discrepancies.add("Schedule has no slots or zero estimated minutes.")

        // 5. Capacity feasible
        val capacityFeasible = schedule.capacityWindows.isNotEmpty()
        if (!capacityFeasible) discrepancies.add("No capacity windows computed for schedule.")

        // 6. Zero blocking conflicts
        val zeroBlockingConflicts = !schedule.hasBlockingConflicts
        if (!zeroBlockingConflicts) discrepancies.add("Schedule contains active blocking conflicts.")

        // 7. Dispatch queue alignment
        val scheduleQueue = dispatchQueue.filter { it.scheduleId == schedule.scheduleId }
        val dispatchAligned = scheduleQueue.isEmpty() || scheduleQueue.size == schedule.slots.size
        if (!dispatchAligned) discrepancies.add("Dispatch queue item count (${scheduleQueue.size}) does not match slots (${schedule.slots.size}).")

        // 8. Tenant isolation
        val tenantIsolationVerified = schedule.tenantId.isNotBlank() && schedule.tenantId == job.tenantId
        if (!tenantIsolationVerified) discrepancies.add("Tenant isolation violation: schedule tenant=${schedule.tenantId}, job tenant=${job.tenantId}")

        val isFullyReconciled = executionJobMatch && orderMatch && planningMatch && workOrdersMatched &&
                slotsComplete && capacityFeasible && zeroBlockingConflicts && dispatchAligned && tenantIsolationVerified

        return ProductionScheduleReconciliationResult(
            scheduleId = schedule.scheduleId,
            executionJobId = schedule.executionJobId,
            orderId = schedule.orderId,
            isFullyReconciled = isFullyReconciled,
            planningMatch = planningMatch,
            executionJobMatch = executionJobMatch,
            workOrdersMatched = workOrdersMatched,
            slotsComplete = slotsComplete,
            capacityFeasible = capacityFeasible,
            zeroBlockingConflicts = zeroBlockingConflicts,
            dispatchAligned = dispatchAligned,
            tenantIsolationVerified = tenantIsolationVerified,
            discrepancies = discrepancies,
            reconciledAt = System.currentTimeMillis()
        )
    }
}
