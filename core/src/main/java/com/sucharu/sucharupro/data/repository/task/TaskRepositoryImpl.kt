package com.sucharu.sucharupro.data.repository.task

import com.sucharu.sucharupro.data.datasource.task.TaskDataSource
import com.sucharu.sucharupro.data.model.task.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.notification.NotificationCategory
import com.sucharu.sucharupro.domain.model.notification.NotificationChannel
import com.sucharu.sucharupro.domain.model.notification.NotificationPriority
import com.sucharu.sucharupro.domain.model.notification.NotificationType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.communication.internal.InternalCommunicationRepository
import com.sucharu.sucharupro.domain.repository.notification.NotificationRepository
import com.sucharu.sucharupro.domain.repository.task.TaskRepository
import com.sucharu.sucharupro.domain.validation.task.TaskAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.task.TaskLifecycleValidator
import com.sucharu.sucharupro.domain.validation.task.TaskValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID

/**
 * Repository implementation for Staff Task, Assignment & Workflow Communication Management.
 */
class TaskRepositoryImpl(
    private val dataSource: TaskDataSource,
    private val notificationRepository: NotificationRepository? = null,
    private val internalCommunicationRepository: InternalCommunicationRepository? = null
) : TaskRepository {

    override suspend fun createTask(
        projectId: String,
        title: String,
        description: String,
        taskType: TaskType,
        priority: TaskPriority,
        assignedTo: String?,
        departmentId: String?,
        teamId: String?,
        referenceType: String?,
        referenceId: String?,
        dueDate: Long?,
        startDate: Long?,
        estimatedMinutes: Int,
        idempotencyKey: String?,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task> {
        val authCheck = TaskAuthorizationValidator.validateCreateTask(callerRole)
        if (authCheck.isFailure) {
            return DomainResult.Error(authCheck.exceptionOrNull()!!)
        }

        // Check idempotency first
        if (!idempotencyKey.isNull_or_blank()) {
            val existing = dataSource.getTaskByIdempotencyKey(projectId, idempotencyKey!!)
            if (existing != null) return DomainResult.Success(existing)
        }


        val taskId = "TSK-" + UUID.randomUUID().toString().take(8).uppercase()
        val initialStatus = if (assignedTo != null) TaskStatus.ASSIGNED else TaskStatus.DRAFT

        val task = Task(
            taskId = taskId,
            taskNo = "", // Data source generates sequential task number
            projectId = projectId,
            title = title,
            description = description,
            taskType = taskType,
            priority = priority,
            status = initialStatus,
            createdBy = actorUserId,
            assignedBy = if (assignedTo != null) actorUserId else null,
            assignedTo = assignedTo,
            departmentId = departmentId,
            teamId = teamId,
            referenceType = referenceType,
            referenceId = referenceId,
            dueDate = dueDate,
            startDate = startDate,
            estimatedMinutes = estimatedMinutes,
            assignedAt = if (assignedTo != null) System.currentTimeMillis() else null,
            idempotencyKey = idempotencyKey
        )

        val validation = TaskValidator.validateTask(task)
        if (validation.isFailure) {
            return DomainResult.Error(validation.exceptionOrNull()!!)
        }

        val created = dataSource.createTask(task)

        // Record audit event
        recordAudit(projectId, created.taskId, actorUserId, TaskActivityEventType.TASK_CREATED, mapOf("title" to title))

        // Record assignment history if assigned
        if (assignedTo != null) {
            val assignment = TaskAssignment(
                assignmentId = UUID.randomUUID().toString(),
                projectId = projectId,
                taskId = created.taskId,
                assignedBy = actorUserId,
                assignedTo = assignedTo,
                assignmentReason = "Initial Task Creation Assignment"
            )
            dataSource.recordAssignment(assignment)
            recordAudit(projectId, created.taskId, actorUserId, TaskActivityEventType.TASK_ASSIGNED, mapOf("assignedTo" to assignedTo))

            // Dispatch notification
            dispatchNotification(
                projectId = projectId,
                recipientUserId = assignedTo,
                title = "New Task Assigned: ${created.taskNo}",
                message = "You have been assigned task: ${created.title}",
                referenceType = "TASK",
                referenceId = created.taskId,
                actorUserId = actorUserId,
                callerRole = callerRole
            )
        }

        return DomainResult.Success(created)
    }

    override suspend fun getTask(
        projectId: String,
        taskId: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task> {
        val task = dataSource.getTask(projectId, taskId)
            ?: return DomainResult.Error(IllegalArgumentException("Task '$taskId' not found in project '$projectId'."))

        val authCheck = TaskAuthorizationValidator.validateViewTask(task, projectId, actorUserId, callerRole)
        if (authCheck.isFailure) {
            return DomainResult.Error(authCheck.exceptionOrNull()!!)
        }

        return DomainResult.Success(task)
    }

    override suspend fun getTaskByNo(
        projectId: String,
        taskNo: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task> {
        val task = dataSource.getTaskByNo(projectId, taskNo)
            ?: return DomainResult.Error(IllegalArgumentException("Task number '$taskNo' not found in project '$projectId'."))

        val authCheck = TaskAuthorizationValidator.validateViewTask(task, projectId, actorUserId, callerRole)
        if (authCheck.isFailure) {
            return DomainResult.Error(authCheck.exceptionOrNull()!!)
        }

        return DomainResult.Success(task)
    }

    override suspend fun updateTask(
        task: Task,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task> {
        val existing = dataSource.getTask(task.projectId, task.taskId)
            ?: return DomainResult.Error(IllegalArgumentException("Task '${task.taskId}' not found."))

        val authCheck = TaskAuthorizationValidator.validateInternalUser(callerRole)
        if (authCheck.isFailure) return DomainResult.Error(authCheck.exceptionOrNull()!!)

        // Check immutability rules for closed tasks
        val immutableCheck = TaskLifecycleValidator.validateImmutableClosedTask(existing, task)
        if (immutableCheck.isFailure) return DomainResult.Error(immutableCheck.exceptionOrNull()!!)

        val valCheck = TaskValidator.validateTask(task)
        if (valCheck.isFailure) return DomainResult.Error(valCheck.exceptionOrNull()!!)

        val updated = dataSource.updateTask(task.copy(updatedAt = System.currentTimeMillis()))
        recordAudit(task.projectId, task.taskId, actorUserId, TaskActivityEventType.TASK_UPDATED)

        return DomainResult.Success(updated)
    }

    override suspend fun cancelTask(
        projectId: String,
        taskId: String,
        reason: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task> {
        val existing = dataSource.getTask(projectId, taskId)
            ?: return DomainResult.Error(IllegalArgumentException("Task '$taskId' not found."))

        val authCheck = TaskAuthorizationValidator.validateInternalUser(callerRole)
        if (authCheck.isFailure) return DomainResult.Error(authCheck.exceptionOrNull()!!)

        if (!TaskLifecycleValidator.isValidTransition(existing.status, TaskStatus.CANCELLED)) {
            return DomainResult.Error(IllegalStateException("Cannot cancel task in status '${existing.status.name}'."))
        }

        val updated = dataSource.updateTask(
            existing.copy(
                status = TaskStatus.CANCELLED,
                closedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        recordAudit(projectId, taskId, actorUserId, TaskActivityEventType.TASK_CANCELLED, mapOf("reason" to reason))

        return DomainResult.Success(updated)
    }

    override suspend fun listTasks(
        projectId: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<List<Task>> {
        val userCheck = TaskAuthorizationValidator.validateInternalUser(callerRole)
        if (userCheck.isFailure) return DomainResult.Error(userCheck.exceptionOrNull()!!)

        val tasks = dataSource.listTasks(projectId)
            .filter { task ->
                TaskAuthorizationValidator.validateViewTask(task, projectId, actorUserId, callerRole).isSuccess
            }

        return DomainResult.Success(tasks)
    }

    override suspend fun searchTasks(
        projectId: String,
        query: String,
        status: TaskStatus?,
        priority: TaskPriority?,
        taskType: TaskType?,
        assigneeId: String?,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<List<Task>> {
        val listResult = listTasks(projectId, actorUserId, callerRole)
        if (listResult.isError) return listResult

        val tasks = listResult.getOrDefault(emptyList()).filter { t ->
            val matchesQuery = query.isBlank() || t.title.contains(query, ignoreCase = true) || t.taskNo.contains(query, ignoreCase = true) || t.description.contains(query, ignoreCase = true)
            val matchesStatus = status == null || t.status == status
            val matchesPriority = priority == null || t.priority == priority
            val matchesType = taskType == null || t.taskType == taskType
            val matchesAssignee = assigneeId == null || t.assignedTo == assigneeId

            matchesQuery && matchesStatus && matchesPriority && matchesType && matchesAssignee
        }

        return DomainResult.Success(tasks)
    }

    override suspend fun assignTask(
        projectId: String,
        taskId: String,
        assignToUserId: String,
        assignmentReason: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task> {
        val authCheck = TaskAuthorizationValidator.validateAssignTask(callerRole, isReassignment = false)
        if (authCheck.isFailure) return DomainResult.Error(authCheck.exceptionOrNull()!!)

        val existing = dataSource.getTask(projectId, taskId)
            ?: return DomainResult.Error(IllegalArgumentException("Task '$taskId' not found."))

        if (existing.status.isTerminal) {
            return DomainResult.Error(IllegalStateException("Cannot assign a terminal task."))
        }

        val newStatus = if (existing.status == TaskStatus.DRAFT) TaskStatus.ASSIGNED else existing.status

        val updated = dataSource.updateTask(
            existing.copy(
                assignedBy = actorUserId,
                assignedTo = assignToUserId,
                assignedAt = System.currentTimeMillis(),
                status = newStatus,
                updatedAt = System.currentTimeMillis()
            )
        )

        val assignment = TaskAssignment(
            assignmentId = UUID.randomUUID().toString(),
            projectId = projectId,
            taskId = taskId,
            assignedBy = actorUserId,
            assignedTo = assignToUserId,
            previousAssigneeId = existing.assignedTo,
            assignmentReason = assignmentReason
        )
        dataSource.recordAssignment(assignment)
        recordAudit(projectId, taskId, actorUserId, TaskActivityEventType.TASK_ASSIGNED, mapOf("assignedTo" to assignToUserId))

        dispatchNotification(
            projectId = projectId,
            recipientUserId = assignToUserId,
            title = "Task Assigned: ${existing.taskNo}",
            message = "Task '${existing.title}' has been assigned to you.",
            referenceType = "TASK",
            referenceId = taskId,
            actorUserId = actorUserId,
            callerRole = callerRole
        )

        return DomainResult.Success(updated)
    }

    override suspend fun reassignTask(
        projectId: String,
        taskId: String,
        newAssigneeUserId: String,
        reassignmentReason: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task> {
        val authCheck = TaskAuthorizationValidator.validateAssignTask(callerRole, isReassignment = true)
        if (authCheck.isFailure) return DomainResult.Error(authCheck.exceptionOrNull()!!)

        val existing = dataSource.getTask(projectId, taskId)
            ?: return DomainResult.Error(IllegalArgumentException("Task '$taskId' not found."))

        if (existing.status.isTerminal) {
            return DomainResult.Error(IllegalStateException("Cannot reassign a terminal task."))
        }

        val previousAssignee = existing.assignedTo

        val updated = dataSource.updateTask(
            existing.copy(
                assignedBy = actorUserId,
                assignedTo = newAssigneeUserId,
                assignedAt = System.currentTimeMillis(),
                status = TaskStatus.ASSIGNED,
                acknowledgedAt = null,
                updatedAt = System.currentTimeMillis()
            )
        )

        val assignment = TaskAssignment(
            assignmentId = UUID.randomUUID().toString(),
            projectId = projectId,
            taskId = taskId,
            assignedBy = actorUserId,
            assignedTo = newAssigneeUserId,
            previousAssigneeId = previousAssignee,
            assignmentReason = reassignmentReason
        )
        dataSource.recordAssignment(assignment)
        recordAudit(projectId, taskId, actorUserId, TaskActivityEventType.TASK_REASSIGNED, mapOf("newAssignee" to newAssigneeUserId, "previousAssignee" to (previousAssignee ?: "")))

        dispatchNotification(
            projectId = projectId,
            recipientUserId = newAssigneeUserId,
            title = "Task Reassigned: ${existing.taskNo}",
            message = "Task '${existing.title}' has been reassigned to you.",
            referenceType = "TASK",
            referenceId = taskId,
            actorUserId = actorUserId,
            callerRole = callerRole
        )

        return DomainResult.Success(updated)
    }

    override suspend fun acknowledgeTask(
        projectId: String,
        taskId: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task> {
        val existing = dataSource.getTask(projectId, taskId)
            ?: return DomainResult.Error(IllegalArgumentException("Task '$taskId' not found."))

        val authCheck = TaskAuthorizationValidator.validateInternalUser(callerRole)
        if (authCheck.isFailure) return DomainResult.Error(authCheck.exceptionOrNull()!!)

        if (existing.assignedTo != actorUserId && callerRole != UserRole.ADMIN && callerRole != UserRole.MANAGER) {
            return DomainResult.Error(SecurityException("Only the assignee or Admin/Manager can acknowledge task assignment."))
        }

        if (!TaskLifecycleValidator.isValidTransition(existing.status, TaskStatus.ACKNOWLEDGED)) {
            return DomainResult.Error(IllegalStateException("Cannot acknowledge task in status '${existing.status.name}'."))
        }

        val updated = dataSource.updateTask(
            existing.copy(
                status = TaskStatus.ACKNOWLEDGED,
                acknowledgedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        recordAudit(projectId, taskId, actorUserId, TaskActivityEventType.TASK_ACKNOWLEDGED)

        return DomainResult.Success(updated)
    }

    override suspend fun startTask(
        projectId: String,
        taskId: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task> {
        val existing = dataSource.getTask(projectId, taskId)
            ?: return DomainResult.Error(IllegalArgumentException("Task '$taskId' not found."))

        val authCheck = TaskAuthorizationValidator.validateInternalUser(callerRole)
        if (authCheck.isFailure) return DomainResult.Error(authCheck.exceptionOrNull()!!)

        if (!TaskLifecycleValidator.isValidTransition(existing.status, TaskStatus.IN_PROGRESS)) {
            return DomainResult.Error(IllegalStateException("Cannot start task in status '${existing.status.name}'."))
        }

        val initialProgress = if (existing.progressPercentage == 0) 10 else existing.progressPercentage

        val updated = dataSource.updateTask(
            existing.copy(
                status = TaskStatus.IN_PROGRESS,
                startedAt = existing.startedAt ?: System.currentTimeMillis(),
                progressPercentage = initialProgress,
                updatedAt = System.currentTimeMillis()
            )
        )
        recordAudit(projectId, taskId, actorUserId, TaskActivityEventType.TASK_STARTED)

        return DomainResult.Success(updated)
    }

    override suspend fun updateProgress(
        projectId: String,
        taskId: String,
        newProgress: Int,
        note: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task> {
        val existing = dataSource.getTask(projectId, taskId)
            ?: return DomainResult.Error(IllegalArgumentException("Task '$taskId' not found."))

        val authCheck = TaskAuthorizationValidator.validateInternalUser(callerRole)
        if (authCheck.isFailure) return DomainResult.Error(authCheck.exceptionOrNull()!!)

        if (newProgress !in 0..100) {
            return DomainResult.Error(IllegalArgumentException("Progress percentage must be between 0 and 100."))
        }

        val progressUpdate = TaskProgressUpdate(
            progressUpdateId = UUID.randomUUID().toString(),
            projectId = projectId,
            taskId = taskId,
            updatedBy = actorUserId,
            previousProgress = existing.progressPercentage,
            newProgress = newProgress,
            note = note
        )
        dataSource.recordProgressUpdate(progressUpdate)

        val updated = dataSource.updateTask(
            existing.copy(
                progressPercentage = newProgress,
                updatedAt = System.currentTimeMillis()
            )
        )
        recordAudit(projectId, taskId, actorUserId, TaskActivityEventType.TASK_PROGRESS_UPDATED, mapOf("newProgress" to newProgress.toString()))

        return DomainResult.Success(updated)
    }

    override suspend fun blockTask(
        projectId: String,
        taskId: String,
        blockedReason: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task> {
        val existing = dataSource.getTask(projectId, taskId)
            ?: return DomainResult.Error(IllegalArgumentException("Task '$taskId' not found."))

        val authCheck = TaskAuthorizationValidator.validateInternalUser(callerRole)
        if (authCheck.isFailure) return DomainResult.Error(authCheck.exceptionOrNull()!!)

        if (blockedReason.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("Blocked reason cannot be blank."))
        }

        if (!TaskLifecycleValidator.isValidTransition(existing.status, TaskStatus.BLOCKED)) {
            return DomainResult.Error(IllegalStateException("Cannot block task in status '${existing.status.name}'."))
        }

        val updated = dataSource.updateTask(
            existing.copy(
                status = TaskStatus.BLOCKED,
                blockedReason = blockedReason,
                updatedAt = System.currentTimeMillis()
            )
        )
        recordAudit(projectId, taskId, actorUserId, TaskActivityEventType.TASK_BLOCKED, mapOf("reason" to blockedReason))

        // Notify manager/creator
        dispatchNotification(
            projectId = projectId,
            recipientUserId = existing.createdBy,
            title = "Task Blocked: ${existing.taskNo}",
            message = "Task '${existing.title}' is blocked. Reason: $blockedReason",
            referenceType = "TASK",
            referenceId = taskId,
            actorUserId = actorUserId,
            callerRole = callerRole
        )

        return DomainResult.Success(updated)
    }

    override suspend fun unblockTask(
        projectId: String,
        taskId: String,
        unblockNote: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task> {
        val existing = dataSource.getTask(projectId, taskId)
            ?: return DomainResult.Error(IllegalArgumentException("Task '$taskId' not found."))

        val authCheck = TaskAuthorizationValidator.validateInternalUser(callerRole)
        if (authCheck.isFailure) return DomainResult.Error(authCheck.exceptionOrNull()!!)

        if (existing.status != TaskStatus.BLOCKED) {
            return DomainResult.Error(IllegalStateException("Task is not in BLOCKED status."))
        }

        val updated = dataSource.updateTask(
            existing.copy(
                status = TaskStatus.IN_PROGRESS,
                blockedReason = null,
                updatedAt = System.currentTimeMillis()
            )
        )
        recordAudit(projectId, taskId, actorUserId, TaskActivityEventType.TASK_UNBLOCKED, mapOf("note" to unblockNote))

        return DomainResult.Success(updated)
    }

    override suspend fun putOnHold(
        projectId: String,
        taskId: String,
        reason: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task> {
        val existing = dataSource.getTask(projectId, taskId)
            ?: return DomainResult.Error(IllegalArgumentException("Task '$taskId' not found."))

        val authCheck = TaskAuthorizationValidator.validateInternalUser(callerRole)
        if (authCheck.isFailure) return DomainResult.Error(authCheck.exceptionOrNull()!!)

        if (!TaskLifecycleValidator.isValidTransition(existing.status, TaskStatus.ON_HOLD)) {
            return DomainResult.Error(IllegalStateException("Cannot put task on hold in status '${existing.status.name}'."))
        }

        val updated = dataSource.updateTask(
            existing.copy(
                status = TaskStatus.ON_HOLD,
                updatedAt = System.currentTimeMillis()
            )
        )
        recordAudit(projectId, taskId, actorUserId, TaskActivityEventType.TASK_ON_HOLD, mapOf("reason" to reason))

        return DomainResult.Success(updated)
    }

    override suspend fun resumeTask(
        projectId: String,
        taskId: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task> {
        val existing = dataSource.getTask(projectId, taskId)
            ?: return DomainResult.Error(IllegalArgumentException("Task '$taskId' not found."))

        val authCheck = TaskAuthorizationValidator.validateInternalUser(callerRole)
        if (authCheck.isFailure) return DomainResult.Error(authCheck.exceptionOrNull()!!)

        if (existing.status != TaskStatus.ON_HOLD) {
            return DomainResult.Error(IllegalStateException("Task is not ON_HOLD."))
        }

        val updated = dataSource.updateTask(
            existing.copy(
                status = TaskStatus.IN_PROGRESS,
                updatedAt = System.currentTimeMillis()
            )
        )
        recordAudit(projectId, taskId, actorUserId, TaskActivityEventType.TASK_RESUMED)

        return DomainResult.Success(updated)
    }

    override suspend fun completeTask(
        projectId: String,
        taskId: String,
        completionNote: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task> {
        val existing = dataSource.getTask(projectId, taskId)
            ?: return DomainResult.Error(IllegalArgumentException("Task '$taskId' not found."))

        val authCheck = TaskAuthorizationValidator.validateInternalUser(callerRole)
        if (authCheck.isFailure) return DomainResult.Error(authCheck.exceptionOrNull()!!)

        if (!TaskLifecycleValidator.isValidTransition(existing.status, TaskStatus.COMPLETED)) {
            return DomainResult.Error(IllegalStateException("Cannot complete task in status '${existing.status.name}'."))
        }

        val updated = dataSource.updateTask(
            existing.copy(
                status = TaskStatus.COMPLETED,
                progressPercentage = 100,
                completedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        recordAudit(projectId, taskId, actorUserId, TaskActivityEventType.TASK_COMPLETED, mapOf("note" to completionNote))

        // Notify manager/creator for verification
        dispatchNotification(
            projectId = projectId,
            recipientUserId = existing.createdBy,
            title = "Task Completed: ${existing.taskNo}",
            message = "Task '${existing.title}' has been marked completed and awaits verification.",
            referenceType = "TASK",
            referenceId = taskId,
            actorUserId = actorUserId,
            callerRole = callerRole
        )

        return DomainResult.Success(updated)
    }

    override suspend fun verifyTask(
        projectId: String,
        taskId: String,
        verificationNote: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task> {
        val existing = dataSource.getTask(projectId, taskId)
            ?: return DomainResult.Error(IllegalArgumentException("Task '$taskId' not found."))

        val authCheck = TaskAuthorizationValidator.validateVerifyOrCloseTask(existing, actorUserId, callerRole)
        if (authCheck.isFailure) return DomainResult.Error(authCheck.exceptionOrNull()!!)

        if (!TaskLifecycleValidator.isValidTransition(existing.status, TaskStatus.VERIFIED)) {
            return DomainResult.Error(IllegalStateException("Cannot verify task in status '${existing.status.name}'."))
        }

        val updated = dataSource.updateTask(
            existing.copy(
                status = TaskStatus.VERIFIED,
                updatedAt = System.currentTimeMillis()
            )
        )
        recordAudit(projectId, taskId, actorUserId, TaskActivityEventType.TASK_VERIFIED, mapOf("note" to verificationNote))

        return DomainResult.Success(updated)
    }

    override suspend fun closeTask(
        projectId: String,
        taskId: String,
        closureNote: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Task> {
        val existing = dataSource.getTask(projectId, taskId)
            ?: return DomainResult.Error(IllegalArgumentException("Task '$taskId' not found."))

        val authCheck = TaskAuthorizationValidator.validateVerifyOrCloseTask(existing, actorUserId, callerRole)
        if (authCheck.isFailure) return DomainResult.Error(authCheck.exceptionOrNull()!!)

        val updated = dataSource.updateTask(
            existing.copy(
                status = TaskStatus.CLOSED,
                closedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        recordAudit(projectId, taskId, actorUserId, TaskActivityEventType.TASK_CLOSED, mapOf("note" to closureNote))

        return DomainResult.Success(updated)
    }

    override suspend fun addComment(
        projectId: String,
        taskId: String,
        content: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<TaskComment> {
        val authCheck = TaskAuthorizationValidator.validateInternalUser(callerRole)
        if (authCheck.isFailure) return DomainResult.Error(authCheck.exceptionOrNull()!!)

        val textCheck = TaskValidator.validateTextContent(content)
        if (textCheck.isFailure) return DomainResult.Error(textCheck.exceptionOrNull()!!)

        val comment = TaskComment(
            commentId = UUID.randomUUID().toString(),
            projectId = projectId,
            taskId = taskId,
            authorId = actorUserId,
            content = content
        )

        val created = dataSource.addComment(comment)
        recordAudit(projectId, taskId, actorUserId, TaskActivityEventType.TASK_COMMENT_ADDED)

        return DomainResult.Success(created)
    }

    override suspend fun addMention(
        projectId: String,
        taskId: String,
        mentionedUserId: String,
        commentId: String?,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<TaskMention> {
        val authCheck = TaskAuthorizationValidator.validateInternalUser(callerRole)
        if (authCheck.isFailure) return DomainResult.Error(authCheck.exceptionOrNull()!!)

        val mention = TaskMention(
            mentionId = UUID.randomUUID().toString(),
            projectId = projectId,
            taskId = taskId,
            commentId = commentId,
            mentionedUserId = mentionedUserId,
            mentionedBy = actorUserId
        )

        val created = dataSource.addMention(mention)
        recordAudit(projectId, taskId, actorUserId, TaskActivityEventType.TASK_MENTIONED, mapOf("mentionedUser" to mentionedUserId))

        dispatchNotification(
            projectId = projectId,
            recipientUserId = mentionedUserId,
            title = "Mentioned in Task",
            message = "You were mentioned in a task discussion.",
            referenceType = "TASK",
            referenceId = taskId,
            actorUserId = actorUserId,
            callerRole = callerRole
        )

        return DomainResult.Success(created)
    }

    override suspend fun getActivityHistory(
        projectId: String,
        taskId: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<List<TaskActivityEvent>> {
        val authCheck = TaskAuthorizationValidator.validateInternalUser(callerRole)
        if (authCheck.isFailure) return DomainResult.Error(authCheck.exceptionOrNull()!!)

        val events = dataSource.getActivityHistory(projectId, taskId)
        return DomainResult.Success(events)
    }

    override suspend fun getAssignmentHistory(
        projectId: String,
        taskId: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<List<TaskAssignment>> {
        val authCheck = TaskAuthorizationValidator.validateInternalUser(callerRole)
        if (authCheck.isFailure) return DomainResult.Error(authCheck.exceptionOrNull()!!)

        val history = dataSource.getAssignmentHistory(projectId, taskId)
        return DomainResult.Success(history)
    }

    override suspend fun getProgressHistory(
        projectId: String,
        taskId: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<List<TaskProgressUpdate>> {
        val authCheck = TaskAuthorizationValidator.validateInternalUser(callerRole)
        if (authCheck.isFailure) return DomainResult.Error(authCheck.exceptionOrNull()!!)

        val history = dataSource.getProgressHistory(projectId, taskId)
        return DomainResult.Success(history)
    }

    override suspend fun getDashboard(
        projectId: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<TaskDashboard> {
        val authCheck = TaskAuthorizationValidator.validateInternalUser(callerRole)
        if (authCheck.isFailure) return DomainResult.Error(authCheck.exceptionOrNull()!!)

        val db = dataSource.getDashboard(projectId, actorUserId)
        return DomainResult.Success(db)
    }

    override suspend fun getSummary(
        projectId: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<TaskSummary> {
        val authCheck = TaskAuthorizationValidator.validateInternalUser(callerRole)
        if (authCheck.isFailure) return DomainResult.Error(authCheck.exceptionOrNull()!!)

        val summary = dataSource.getSummary(projectId)
        return DomainResult.Success(summary)
    }

    override suspend fun getOverdueTasks(
        projectId: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<List<Task>> {
        val listRes = listTasks(projectId, actorUserId, callerRole)
        if (listRes.isError) return listRes

        val overdue = listRes.getOrDefault(emptyList()).filter { it.isOverdue }
        return DomainResult.Success(overdue)
    }

    override suspend fun getMyTasks(
        projectId: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<List<Task>> {
        val listRes = listTasks(projectId, actorUserId, callerRole)
        if (listRes.isError) return listRes

        val myTasks = listRes.getOrDefault(emptyList()).filter { it.assignedTo == actorUserId }
        return DomainResult.Success(myTasks)
    }

    override fun observeTasks(projectId: String, callerRole: UserRole): Flow<List<Task>> {
        if (!callerRole.isInternal) return flow { emit(emptyList()) }
        return dataSource.observeTasks(projectId)
    }

    private suspend fun recordAudit(
        projectId: String,
        taskId: String,
        actorUserId: String,
        eventType: TaskActivityEventType,
        metadata: Map<String, String> = emptyMap()
    ) {
        val event = TaskActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = projectId,
            taskId = taskId,
            actorId = actorUserId,
            eventType = eventType,
            metadata = metadata
        )
        dataSource.recordActivityEvent(event)
    }

    private suspend fun dispatchNotification(
        projectId: String,
        recipientUserId: String,
        title: String,
        message: String,
        referenceType: String,
        referenceId: String,
        actorUserId: String,
        callerRole: UserRole
    ) {
        notificationRepository?.createNotification(
            projectId = projectId,
            recipientUserId = recipientUserId,
            notificationType = NotificationType.SYSTEM_ALERT,
            channel = NotificationChannel.IN_APP,
            priority = NotificationPriority.HIGH,
            title = title,
            message = message,
            referenceType = referenceType,
            referenceId = referenceId,
            actorId = actorUserId,
            callerRole = callerRole
        )
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
