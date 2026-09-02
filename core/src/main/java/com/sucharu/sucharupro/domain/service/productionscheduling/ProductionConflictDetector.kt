package com.sucharu.sucharupro.domain.service.productionscheduling

import com.sucharu.sucharupro.domain.model.productionexecution.ProductionJobExecution
import com.sucharu.sucharupro.domain.model.productionscheduling.*
import java.util.UUID

object ProductionConflictDetector {

    /**
     * Inspects a planned schedule and its slots for all operational conflicts.
     */
    fun detectConflicts(
        scheduleId: String,
        job: ProductionJobExecution,
        slots: List<ProductionScheduleSlot>,
        capacityWindows: List<ProductionCapacityWindow>,
        requestedDueDate: Long? = null,
        machines: List<ProductionMachineAvailability> = ProductionCapacityPlanner.getCanonicalMachines()
    ): List<ProductionScheduleConflict> {
        val conflicts = mutableListOf<ProductionScheduleConflict>()

        // 1. Active Production Job Hold
        if (job.currentHold != null && !job.currentHold.isResolved) {
            conflicts.add(
                ProductionScheduleConflict(
                    conflictId = "CONF-${UUID.randomUUID()}",
                    scheduleId = scheduleId,
                    conflictType = ScheduleConflictType.HOLD_CONFLICT,
                    severity = ConflictSeverity.CRITICAL_BLOCKING,
                    workOrderId = job.currentHold.workOrderId,
                    machineId = null,
                    operatorId = null,
                    message = "Production job is on active hold: [${job.currentHold.category.name}] ${job.currentHold.reason}",
                    isBlocking = true,
                    recommendedAction = "Resolve hold condition in shop-floor command center prior to releasing to dispatch."
                )
            )
        }

        // 2. Machine Double-Booking (internal slot overlaps on same machine)
        val slotsByMachine = slots.groupBy { it.machineId }
        slotsByMachine.forEach { (machineId, machineSlots) ->
            val sorted = machineSlots.sortedBy { it.scheduledStartTimestamp }
            for (i in 0 until sorted.size - 1) {
                val current = sorted[i]
                val next = sorted[i + 1]
                if (current.scheduledEndTimestamp > next.scheduledStartTimestamp) {
                    conflicts.add(
                        ProductionScheduleConflict(
                            conflictId = "CONF-${UUID.randomUUID()}",
                            scheduleId = scheduleId,
                            conflictType = ScheduleConflictType.MACHINE_DOUBLE_BOOKED,
                            severity = ConflictSeverity.CRITICAL_BLOCKING,
                            workOrderId = next.workOrderId,
                            machineId = machineId,
                            operatorId = null,
                            message = "Machine $machineId has overlapping slots: WO ${current.workOrderId} ends at ${current.scheduledEndTimestamp}, but WO ${next.workOrderId} starts at ${next.scheduledStartTimestamp}.",
                            isBlocking = true,
                            recommendedAction = "Resequence slots or allocate secondary compatible machine."
                        )
                    )
                }
            }
        }

        // 3. Operator Double-Booking
        val slotsByOperator = slots.filter { it.operatorId != null }.groupBy { it.operatorId!! }
        slotsByOperator.forEach { (operatorId, opSlots) ->
            val sorted = opSlots.sortedBy { it.scheduledStartTimestamp }
            for (i in 0 until sorted.size - 1) {
                val current = sorted[i]
                val next = sorted[i + 1]
                if (current.scheduledEndTimestamp > next.scheduledStartTimestamp) {
                    conflicts.add(
                        ProductionScheduleConflict(
                            conflictId = "CONF-${UUID.randomUUID()}",
                            scheduleId = scheduleId,
                            conflictType = ScheduleConflictType.OPERATOR_DOUBLE_BOOKED,
                            severity = ConflictSeverity.CRITICAL_BLOCKING,
                            workOrderId = next.workOrderId,
                            machineId = next.machineId,
                            operatorId = operatorId,
                            message = "Operator $operatorId is double-booked across overlapping operations.",
                            isBlocking = true,
                            recommendedAction = "Assign qualified backup operator or offset operation time window."
                        )
                    )
                }
            }
        }

        // 4. Maintenance Overlap
        machines.forEach { machine ->
            machine.maintenanceBlocks.filter { it.isBlocking }.forEach { block ->
                val overlappingSlots = slots.filter { it.machineId == machine.machineId && it.scheduledStartTimestamp < block.endTimestamp && it.scheduledEndTimestamp > block.startTimestamp }
                overlappingSlots.forEach { slot ->
                    conflicts.add(
                        ProductionScheduleConflict(
                            conflictId = "CONF-${UUID.randomUUID()}",
                            scheduleId = scheduleId,
                            conflictType = ScheduleConflictType.MAINTENANCE_OVERLAP,
                            severity = ConflictSeverity.CRITICAL_BLOCKING,
                            workOrderId = slot.workOrderId,
                            machineId = machine.machineId,
                            operatorId = null,
                            message = "Slot for WO ${slot.workOrderId} collides with scheduled machine maintenance: ${block.reason}.",
                            isBlocking = true,
                            recommendedAction = "Shift slot outside maintenance window or route to alternative machine."
                        )
                    )
                }
            }
        }

        // 5. Sequence / Predecessor Dependency Violations
        val sortedBySeq = slots.sortedBy { it.sequenceNumber }
        for (i in 0 until sortedBySeq.size - 1) {
            val current = sortedBySeq[i]
            val next = sortedBySeq[i + 1]
            if (current.scheduledEndTimestamp > next.scheduledStartTimestamp) {
                conflicts.add(
                    ProductionScheduleConflict(
                        conflictId = "CONF-${UUID.randomUUID()}",
                        scheduleId = scheduleId,
                        conflictType = ScheduleConflictType.DEPENDENCY_VIOLATION,
                        severity = ConflictSeverity.CRITICAL_BLOCKING,
                        workOrderId = next.workOrderId,
                        machineId = next.machineId,
                        operatorId = null,
                        message = "Predecessor stage (Seq ${current.sequenceNumber}, ${current.operationCode}) ends after successor starts (Seq ${next.sequenceNumber}, ${next.operationCode}).",
                        isBlocking = true,
                        recommendedAction = "Adjust start time of successor operation to maintain positive lead-time."
                    )
                )
            }
        }

        // 6. Capacity Exceeded Check
        capacityWindows.forEach { window ->
            if (window.utilizationRate.compareTo(java.math.BigDecimal("1.0000")) > 0) {
                conflicts.add(
                    ProductionScheduleConflict(
                        conflictId = "CONF-${UUID.randomUUID()}",
                        scheduleId = scheduleId,
                        conflictType = ScheduleConflictType.CAPACITY_EXCEEDED,
                        severity = ConflictSeverity.WARNING,
                        workOrderId = null,
                        machineId = window.machineId,
                        operatorId = null,
                        message = "Machine ${window.machineName} capacity exceeded: utilization is ${(window.utilizationRate.multiply(java.math.BigDecimal("100"))).toInt()}%.",
                        isBlocking = false,
                        recommendedAction = "Add overtime shift or split batch across parallel lines."
                    )
                )
            }
        }

        // 7. Due Date Risk
        if (requestedDueDate != null && requestedDueDate > 0L) {
            val latestSlotEnd = slots.maxOfOrNull { it.scheduledEndTimestamp } ?: 0L
            if (latestSlotEnd > requestedDueDate) {
                val delayHours = ((latestSlotEnd - requestedDueDate) / 3600000.0).toInt()
                conflicts.add(
                    ProductionScheduleConflict(
                        conflictId = "CONF-${UUID.randomUUID()}",
                        scheduleId = scheduleId,
                        conflictType = ScheduleConflictType.DUE_DATE_RISK,
                        severity = ConflictSeverity.WARNING,
                        workOrderId = slots.lastOrNull()?.workOrderId,
                        machineId = null,
                        operatorId = null,
                        message = "Scheduled completion exceeds customer due date by approximately $delayHours hours.",
                        isBlocking = false,
                        recommendedAction = "Increase production priority, expedite prepress, or authorize expedited shift."
                    )
                )
            }
        }

        return conflicts
    }
}
