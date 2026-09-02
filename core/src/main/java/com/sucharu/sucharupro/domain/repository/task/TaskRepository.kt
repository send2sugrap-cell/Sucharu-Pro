package com.sucharu.sucharupro.domain.repository.task

import com.sucharu.sucharupro.data.model.task.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface contract for Task Subsystem (Module 10 Step 04).
 */
interface TaskRepository {

    suspend fun createTask(
        projectId: String,
        title: String,
        description: String = "",
        taskType: TaskType = TaskType.GENERAL,
        priority: TaskPriority = TaskPriority.NORMAL,
        assignedTo: String? = null,
        departmentId: String? = null,
        teamId: String? = null,
        referenceType: String? = null,
        referenceId: String? = null,
        dueDate: Long? = null,
        startDate: Long? = null,
        estimatedMinutes: Int = 0,
        idempotencyKey: String? = null,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task>

    suspend fun getTask(
        projectId: String,
        taskId: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task>

    suspend fun getTaskByNo(
        projectId: String,
        taskNo: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task>

    suspend fun updateTask(
        task: Task,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task>

    suspend fun cancelTask(
        projectId: String,
        taskId: String,
        reason: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task>

    suspend fun listTasks(
        projectId: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<List<Task>>

    suspend fun searchTasks(
        projectId: String,
        query: String,
        status: TaskStatus? = null,
        priority: TaskPriority? = null,
        taskType: TaskType? = null,
        assigneeId: String? = null,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<List<Task>>

    suspend fun assignTask(
        projectId: String,
        taskId: String,
        assignToUserId: String,
        assignmentReason: String = "",
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task>

    suspend fun reassignTask(
        projectId: String,
        taskId: String,
        newAssigneeUserId: String,
        reassignmentReason: String = "",
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task>

    suspend fun acknowledgeTask(
        projectId: String,
        taskId: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task>

    suspend fun startTask(
        projectId: String,
        taskId: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task>

    suspend fun updateProgress(
        projectId: String,
        taskId: String,
        newProgress: Int,
        note: String = "",
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task>

    suspend fun blockTask(
        projectId: String,
        taskId: String,
        blockedReason: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task>

    suspend fun unblockTask(
        projectId: String,
        taskId: String,
        unblockNote: String = "",
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task>

    suspend fun putOnHold(
        projectId: String,
        taskId: String,
        reason: String = "",
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task>

    suspend fun resumeTask(
        projectId: String,
        taskId: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task>

    suspend fun completeTask(
        projectId: String,
        taskId: String,
        completionNote: String = "",
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task>

    suspend fun verifyTask(
        projectId: String,
        taskId: String,
        verificationNote: String = "",
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task>

    suspend fun closeTask(
        projectId: String,
        taskId: String,
        closureNote: String = "",
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task>

    suspend fun addComment(
        projectId: String,
        taskId: String,
        content: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<TaskComment>

    suspend fun addMention(
        projectId: String,
        taskId: String,
        mentionedUserId: String,
        commentId: String? = null,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<TaskMention>

    suspend fun getActivityHistory(
        projectId: String,
        taskId: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<List<TaskActivityEvent>>

    suspend fun getAssignmentHistory(
        projectId: String,
        taskId: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<List<TaskAssignment>>

    suspend fun getProgressHistory(
        projectId: String,
        taskId: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<List<TaskProgressUpdate>>

    suspend fun getDashboard(
        projectId: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<TaskDashboard>

    suspend fun getSummary(
        projectId: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<TaskSummary>

    suspend fun getOverdueTasks(
        projectId: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<List<Task>>

    suspend fun getMyTasks(
        projectId: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<List<Task>>

    fun observeTasks(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<Task>>
}
