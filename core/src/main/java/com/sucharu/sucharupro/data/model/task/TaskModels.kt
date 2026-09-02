package com.sucharu.sucharupro.data.model.task

/**
 * Task aggregate root entity for Sucharu Pro ERP.
 */
data class Task(
    val taskId: String,
    val taskNo: String,
    val projectId: String,
    val title: String,
    val description: String = "",
    val taskType: TaskType = TaskType.GENERAL,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val status: TaskStatus = TaskStatus.DRAFT,
    val createdBy: String,
    val assignedBy: String? = null,
    val assignedTo: String? = null,
    val departmentId: String? = null,
    val teamId: String? = null,
    val parentTaskId: String? = null,
    val relatedTaskId: String? = null,
    val referenceType: String? = null,
    val referenceId: String? = null,
    val dueDate: Long? = null,
    val startDate: Long? = null,
    val completedAt: Long? = null,
    val progressPercentage: Int = 0,
    val estimatedMinutes: Int = 0,
    val actualMinutes: Int = 0,
    val blockedReason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val assignedAt: Long? = null,
    val acknowledgedAt: Long? = null,
    val startedAt: Long? = null,
    val closedAt: Long? = null,
    val idempotencyKey: String? = null,
    val version: Long = 1L
) {
    val isOverdue: Boolean
        get() = dueDate != null && System.currentTimeMillis() > dueDate && !status.isTerminal && status != TaskStatus.COMPLETED && status != TaskStatus.VERIFIED

    val isDueToday: Boolean
        get() {
            if (dueDate == null) return false
            val now = System.currentTimeMillis()
            val dayMs = 24 * 60 * 60 * 1000L
            return dueDate in now..(now + dayMs)
        }
}

/**
 * Immutable assignment history record.
 */
data class TaskAssignment(
    val assignmentId: String,
    val projectId: String,
    val taskId: String,
    val assignedBy: String,
    val assignedTo: String,
    val previousAssigneeId: String? = null,
    val assignmentReason: String = "",
    val assignedAt: Long = System.currentTimeMillis(),
    val acceptedAt: Long? = null,
    val declinedAt: Long? = null,
    val status: String = "ASSIGNED"
)

/**
 * Task comment entity with edit/delete audit capabilities.
 */
data class TaskComment(
    val commentId: String,
    val projectId: String,
    val taskId: String,
    val authorId: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val editedAt: Long? = null,
    val deletedAt: Long? = null,
    val isDeleted: Boolean = false
)

/**
 * Task @mention tracking entry.
 */
data class TaskMention(
    val mentionId: String,
    val projectId: String,
    val taskId: String,
    val commentId: String? = null,
    val mentionedUserId: String,
    val mentionedBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val acknowledgedAt: Long? = null
)

/**
 * Task progress update tracking entry.
 */
data class TaskProgressUpdate(
    val progressUpdateId: String,
    val projectId: String,
    val taskId: String,
    val updatedBy: String,
    val previousProgress: Int,
    val newProgress: Int,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Immutable audit event types.
 */
enum class TaskActivityEventType {
    TASK_CREATED,
    TASK_UPDATED,
    TASK_ASSIGNED,
    TASK_REASSIGNED,
    TASK_ACKNOWLEDGED,
    TASK_STARTED,
    TASK_PROGRESS_UPDATED,
    TASK_BLOCKED,
    TASK_UNBLOCKED,
    TASK_ON_HOLD,
    TASK_RESUMED,
    TASK_COMMENT_ADDED,
    TASK_MENTIONED,
    TASK_COMPLETED,
    TASK_VERIFIED,
    TASK_CLOSED,
    TASK_CANCELLED,
    TASK_REJECTED,
    TASK_ESCALATED,
    TASK_OVERDUE,
    TASK_DEADLINE_CHANGED
}

/**
 * Immutable append-only activity audit event log.
 */
data class TaskActivityEvent(
    val eventId: String,
    val projectId: String,
    val taskId: String,
    val actorId: String,
    val eventType: TaskActivityEventType,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Project-scoped task metrics summary.
 */
data class TaskSummary(
    val projectId: String,
    val totalTasks: Int = 0,
    val draftTasks: Int = 0,
    val assignedTasks: Int = 0,
    val inProgressTasks: Int = 0,
    val blockedTasks: Int = 0,
    val completedTasks: Int = 0,
    val overdueTasks: Int = 0,
    val urgentTasks: Int = 0,
    val completionRate: Double = 0.0,
    val averageCompletionTimeMinutes: Long = 0L,
    val pendingCount: Int = 0
)

/**
 * Aggregated user & team task dashboard model.
 */
data class TaskDashboard(
    val projectId: String,
    val userId: String,
    val myTasks: List<Task> = emptyList(),
    val teamTasks: List<Task> = emptyList(),
    val overdueTasks: List<Task> = emptyList(),
    val dueTodayTasks: List<Task> = emptyList(),
    val dueThisWeekTasks: List<Task> = emptyList(),
    val urgentTasks: List<Task> = emptyList(),
    val blockedTasks: List<Task> = emptyList(),
    val completedTodayTasks: List<Task> = emptyList(),
    val completionRate: Double = 0.0,
    val summary: TaskSummary = TaskSummary(projectId)
)
