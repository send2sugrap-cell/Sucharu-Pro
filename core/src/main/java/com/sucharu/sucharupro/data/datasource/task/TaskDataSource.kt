package com.sucharu.sucharupro.data.datasource.task

import com.sucharu.sucharupro.data.model.task.*
import kotlinx.coroutines.flow.Flow

/**
 * Data source interface for Task Subsystem (Module 10 Step 04).
 */
interface TaskDataSource {
    suspend fun createTask(task: Task): Task
    suspend fun getTask(projectId: String, taskId: String): Task?
    suspend fun getTaskByNo(projectId: String, taskNo: String): Task?
    suspend fun getTaskByIdempotencyKey(projectId: String, idempotencyKey: String): Task?
    suspend fun updateTask(task: Task): Task
    suspend fun listTasks(projectId: String): List<Task>
    fun observeTasks(projectId: String): Flow<List<Task>>

    suspend fun recordAssignment(assignment: TaskAssignment): TaskAssignment
    suspend fun getAssignmentHistory(projectId: String, taskId: String): List<TaskAssignment>

    suspend fun recordProgressUpdate(update: TaskProgressUpdate): TaskProgressUpdate
    suspend fun getProgressHistory(projectId: String, taskId: String): List<TaskProgressUpdate>

    suspend fun addComment(comment: TaskComment): TaskComment
    suspend fun getComments(projectId: String, taskId: String): List<TaskComment>

    suspend fun addMention(mention: TaskMention): TaskMention
    suspend fun getMentions(projectId: String, taskId: String): List<TaskMention>

    suspend fun recordActivityEvent(event: TaskActivityEvent): TaskActivityEvent
    suspend fun getActivityHistory(projectId: String, taskId: String): List<TaskActivityEvent>

    suspend fun getSummary(projectId: String): TaskSummary
    suspend fun getDashboard(projectId: String, userId: String): TaskDashboard
    suspend fun clear()
}
