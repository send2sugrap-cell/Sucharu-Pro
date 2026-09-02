package com.sucharu.sucharupro.domain.validation.task

import com.sucharu.sucharupro.data.model.task.Task

/**
 * Validates task payload parameters, credential safety, and boundary checks for Sucharu Pro ERP.
 */
object TaskValidator {

    private val SUSPICIOUS_PATTERNS = listOf(
        Regex("(?i)password\\s*="),
        Regex("(?i)api_key\\s*="),
        Regex("(?i)secret\\s*="),
        Regex("(?i)authorization\\s*="),
        Regex("(?i)bearer\\s+[A-Za-z0-9\\-\\._~\\+\\/]+=*")
    )

    /**
     * Validates a Task aggregate instance before persistence.
     */
    fun validateTask(task: Task): Result<Unit> {
        if (task.taskId.isBlank()) {
            return Result.failure(IllegalArgumentException("Task ID cannot be blank."))
        }

        if (task.projectId.isBlank()) {
            return Result.failure(IllegalArgumentException("Project ID cannot be blank."))
        }
        if (task.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Task title cannot be blank."))
        }
        if (task.createdBy.isBlank()) {
            return Result.failure(IllegalArgumentException("Creator user ID cannot be blank."))
        }
        if (task.progressPercentage !in 0..100) {
            return Result.failure(IllegalArgumentException("Progress percentage must be between 0 and 100."))
        }
        if (task.estimatedMinutes < 0) {
            return Result.failure(IllegalArgumentException("Estimated minutes cannot be negative."))
        }
        if (task.actualMinutes < 0) {
            return Result.failure(IllegalArgumentException("Actual minutes cannot be negative."))
        }
        if (task.startDate != null && task.dueDate != null && task.startDate > task.dueDate) {
            return Result.failure(IllegalArgumentException("Start date cannot be after due date."))
        }

        // Credential / Secret leakage prevention check
        val textToAudit = "${task.title} ${task.description} ${task.blockedReason ?: ""}"
        for (pattern in SUSPICIOUS_PATTERNS) {
            if (pattern.containsMatchIn(textToAudit)) {
                return Result.failure(IllegalArgumentException("Security policy error: Task text contains potential sensitive credentials or tokens."))
            }
        }

        return Result.success(Unit)
    }

    /**
     * Audits raw text (e.g., comments) for sensitive token leakage.
     */
    fun validateTextContent(content: String): Result<Unit> {
        if (content.isBlank()) {
            return Result.failure(IllegalArgumentException("Content cannot be blank."))
        }
        for (pattern in SUSPICIOUS_PATTERNS) {
            if (pattern.containsMatchIn(content)) {
                return Result.failure(IllegalArgumentException("Security policy error: Text content contains potential sensitive credentials or tokens."))
            }
        }
        return Result.success(Unit)
    }
}
