package com.sucharu.sucharupro.domain.validation.task

import com.sucharu.sucharupro.data.model.task.Task
import com.sucharu.sucharupro.data.model.task.TaskStatus

/**
 * Validates task lifecycle state transitions deterministically for Sucharu Pro ERP.
 */
object TaskLifecycleValidator {

    /**
     * Determines whether a transition from [currentStatus] to [targetStatus] is allowed.
     */
    fun isValidTransition(currentStatus: TaskStatus, targetStatus: TaskStatus): Boolean {
        if (currentStatus == targetStatus) return true
        if (currentStatus.isTerminal) return false // Terminal states cannot transition further

        return when (currentStatus) {
            TaskStatus.DRAFT -> targetStatus in listOf(
                TaskStatus.ASSIGNED,
                TaskStatus.CANCELLED
            )

            TaskStatus.ASSIGNED -> targetStatus in listOf(
                TaskStatus.ACKNOWLEDGED,
                TaskStatus.IN_PROGRESS,
                TaskStatus.REJECTED,
                TaskStatus.CANCELLED
            )

            TaskStatus.ACKNOWLEDGED -> targetStatus in listOf(
                TaskStatus.IN_PROGRESS,
                TaskStatus.BLOCKED,
                TaskStatus.ON_HOLD,
                TaskStatus.CANCELLED
            )

            TaskStatus.IN_PROGRESS -> targetStatus in listOf(
                TaskStatus.BLOCKED,
                TaskStatus.ON_HOLD,
                TaskStatus.COMPLETED,
                TaskStatus.CANCELLED
            )

            TaskStatus.BLOCKED -> targetStatus in listOf(
                TaskStatus.IN_PROGRESS,
                TaskStatus.ON_HOLD,
                TaskStatus.CANCELLED
            )

            TaskStatus.ON_HOLD -> targetStatus in listOf(
                TaskStatus.IN_PROGRESS,
                TaskStatus.BLOCKED,
                TaskStatus.CANCELLED
            )

            TaskStatus.COMPLETED -> targetStatus in listOf(
                TaskStatus.VERIFIED,
                TaskStatus.CLOSED,
                TaskStatus.IN_PROGRESS // Re-open for rework
            )

            TaskStatus.VERIFIED -> targetStatus in listOf(
                TaskStatus.CLOSED,
                TaskStatus.IN_PROGRESS
            )

            TaskStatus.CLOSED,
            TaskStatus.CANCELLED,
            TaskStatus.REJECTED -> false
        }
    }

    /**
     * Validates that an update operation on a closed/terminal task does not alter protected fields.
     */
    fun validateImmutableClosedTask(existingTask: Task, updatedTask: Task): Result<Unit> {
        if (!existingTask.status.isTerminal && existingTask.status != TaskStatus.CLOSED) {
            return Result.success(Unit)
        }

        if (existingTask.assignedTo != updatedTask.assignedTo) {
            return Result.failure(IllegalArgumentException("Cannot reassign a closed or terminal task."))
        }
        if (existingTask.priority != updatedTask.priority) {
            return Result.failure(IllegalArgumentException("Cannot change priority of a closed or terminal task."))
        }
        if (existingTask.description != updatedTask.description) {
            return Result.failure(IllegalArgumentException("Cannot change description of a closed or terminal task."))
        }
        if (existingTask.progressPercentage != updatedTask.progressPercentage) {
            return Result.failure(IllegalArgumentException("Cannot change progress of a closed or terminal task."))
        }
        if (existingTask.referenceId != updatedTask.referenceId || existingTask.referenceType != updatedTask.referenceType) {
            return Result.failure(IllegalArgumentException("Cannot change business reference of a closed or terminal task."))
        }

        return Result.success(Unit)
    }
}
