package com.sucharu.sucharupro.data.datasource.task

import com.sucharu.sucharupro.data.model.task.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar

/**
 * Concurrency-safe, in-memory fake data source for Task Subsystem.
 */
class FakeTaskDataSource : TaskDataSource {

    private val mutex = Mutex()

    private val tasksStore = mutableMapOf<String, Task>() // taskId -> Task
    private val assignmentsStore = mutableListOf<TaskAssignment>()
    private val progressUpdatesStore = mutableListOf<TaskProgressUpdate>()
    private val commentsStore = mutableListOf<TaskComment>()
    private val mentionsStore = mutableListOf<TaskMention>()
    private val activityEventsStore = mutableListOf<TaskActivityEvent>()

    private val tasksFlow = MutableStateFlow<List<Task>>(emptyList())
    private var sequenceCounter = 1000

    override suspend fun createTask(task: Task): Task = mutex.withLock {
        // Idempotency check using private helper
        val existing = getTaskByIdempotencyKeyInternal(task.projectId, task.idempotencyKey)
        if (existing != null) return@withLock existing

        val taskNo = if (task.taskNo.isBlank()) generateTaskNo() else task.taskNo
        val newTask = task.copy(taskNo = taskNo)
        tasksStore[newTask.taskId] = newTask

        notifyFlowUpdatedLocked()
        newTask
    }

    override suspend fun getTask(projectId: String, taskId: String): Task? = mutex.withLock {
        val t = tasksStore[taskId]
        if (t != null && t.projectId == projectId) t else null
    }

    override suspend fun getTaskByNo(projectId: String, taskNo: String): Task? = mutex.withLock {
        tasksStore.values.firstOrNull { it.projectId == projectId && it.taskNo.equals(taskNo, ignoreCase = true) }
    }

    override suspend fun getTaskByIdempotencyKey(projectId: String, idempotencyKey: String): Task? = mutex.withLock {
        getTaskByIdempotencyKeyInternal(projectId, idempotencyKey)
    }

    private fun getTaskByIdempotencyKeyInternal(projectId: String, idempotencyKey: String?): Task? {
        if (idempotencyKey.isNull_or_blank()) return null
        return tasksStore.values.firstOrNull { it.projectId == projectId && it.idempotencyKey == idempotencyKey }
    }

    override suspend fun updateTask(task: Task): Task = mutex.withLock {
        tasksStore[task.taskId] = task
        notifyFlowUpdatedLocked()
        task
    }

    override suspend fun listTasks(projectId: String): List<Task> = mutex.withLock {
        tasksStore.values.filter { it.projectId == projectId }
    }

    override fun observeTasks(projectId: String): Flow<List<Task>> {
        return tasksFlow.asStateFlow()
    }

    override suspend fun recordAssignment(assignment: TaskAssignment): TaskAssignment = mutex.withLock {
        assignmentsStore.add(assignment)
        assignment
    }

    override suspend fun getAssignmentHistory(projectId: String, taskId: String): List<TaskAssignment> = mutex.withLock {
        assignmentsStore.filter { it.projectId == projectId && it.taskId == taskId }
    }

    override suspend fun recordProgressUpdate(update: TaskProgressUpdate): TaskProgressUpdate = mutex.withLock {
        progressUpdatesStore.add(update)
        update
    }

    override suspend fun getProgressHistory(projectId: String, taskId: String): List<TaskProgressUpdate> = mutex.withLock {
        progressUpdatesStore.filter { it.projectId == projectId && it.taskId == taskId }
    }

    override suspend fun addComment(comment: TaskComment): TaskComment = mutex.withLock {
        commentsStore.add(comment)
        comment
    }

    override suspend fun getComments(projectId: String, taskId: String): List<TaskComment> = mutex.withLock {
        commentsStore.filter { it.projectId == projectId && it.taskId == taskId && !it.isDeleted }
    }

    override suspend fun addMention(mention: TaskMention): TaskMention = mutex.withLock {
        mentionsStore.add(mention)
        mention
    }

    override suspend fun getMentions(projectId: String, taskId: String): List<TaskMention> = mutex.withLock {
        mentionsStore.filter { it.projectId == projectId && it.taskId == taskId }
    }

    override suspend fun recordActivityEvent(event: TaskActivityEvent): TaskActivityEvent = mutex.withLock {
        activityEventsStore.add(event)
        event
    }

    override suspend fun getActivityHistory(projectId: String, taskId: String): List<TaskActivityEvent> = mutex.withLock {
        activityEventsStore.filter { it.projectId == projectId && it.taskId == taskId }
    }

    override suspend fun getSummary(projectId: String): TaskSummary = mutex.withLock {
        calculateSummaryInternal(projectId)
    }

    override suspend fun getDashboard(projectId: String, userId: String): TaskDashboard = mutex.withLock {
        val projTasks = tasksStore.values.filter { it.projectId == projectId }
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L
        val weekMs = 7 * dayMs

        val myTasks = projTasks.filter { it.assignedTo == userId }
        val teamTasks = projTasks.filter { it.assignedTo != userId }
        val overdue = projTasks.filter { it.isOverdue }
        val dueToday = projTasks.filter { it.isDueToday }
        val dueThisWeek = projTasks.filter { it.dueDate != null && it.dueDate in now..(now + weekMs) }
        val urgent = projTasks.filter { it.priority == TaskPriority.URGENT || it.priority == TaskPriority.CRITICAL }
        val blocked = projTasks.filter { it.status == TaskStatus.BLOCKED }
        val completedToday = projTasks.filter { it.status == TaskStatus.COMPLETED && it.completedAt != null && (now - it.completedAt) <= dayMs }

        val summary = calculateSummaryInternal(projectId)

        TaskDashboard(
            projectId = projectId,
            userId = userId,
            myTasks = myTasks,
            teamTasks = teamTasks,
            overdueTasks = overdue,
            dueTodayTasks = dueToday,
            dueThisWeekTasks = dueThisWeek,
            urgentTasks = urgent,
            blockedTasks = blocked,
            completedTodayTasks = completedToday,
            completionRate = summary.completionRate,
            summary = summary
        )
    }

    override suspend fun clear(): Unit = mutex.withLock {
        tasksStore.clear()
        assignmentsStore.clear()
        progressUpdatesStore.clear()
        commentsStore.clear()
        mentionsStore.clear()
        activityEventsStore.clear()
        notifyFlowUpdatedLocked()
    }

    private fun calculateSummaryInternal(projectId: String): TaskSummary {
        val projTasks = tasksStore.values.filter { it.projectId == projectId }
        val total = projTasks.size
        val draft = projTasks.count { it.status == TaskStatus.DRAFT }
        val assigned = projTasks.count { it.status == TaskStatus.ASSIGNED }
        val inProgress = projTasks.count { it.status == TaskStatus.IN_PROGRESS }
        val blocked = projTasks.count { it.status == TaskStatus.BLOCKED }
        val completed = projTasks.count { it.status == TaskStatus.COMPLETED || it.status == TaskStatus.VERIFIED || it.status == TaskStatus.CLOSED }
        val overdue = projTasks.count { it.isOverdue }
        val urgent = projTasks.count { it.priority == TaskPriority.URGENT || it.priority == TaskPriority.CRITICAL }
        val pending = projTasks.count { it.status.isActive }

        val completionRate = if (total > 0) (completed.toDouble() / total.toDouble()) * 100.0 else 0.0

        return TaskSummary(
            projectId = projectId,
            totalTasks = total,
            draftTasks = draft,
            assignedTasks = assigned,
            inProgressTasks = inProgress,
            blockedTasks = blocked,
            completedTasks = completed,
            overdueTasks = overdue,
            urgentTasks = urgent,
            completionRate = completionRate,
            averageCompletionTimeMinutes = 45L,
            pendingCount = pending
        )
    }

    private fun generateTaskNo(): String {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        return "TSK-$year-${sequenceCounter++}"
    }

    private fun notifyFlowUpdatedLocked() {
        tasksFlow.value = tasksStore.values.toList()
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
