package com.sucharu.sucharupro.ui.features.task

import com.sucharu.sucharupro.data.model.task.*

/**
 * Immutable UI State holding Task management state.
 */
data class TaskUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val dashboard: TaskDashboard? = null,
    val tasks: List<Task> = emptyList(),
    val selectedTask: Task? = null,
    val summary: TaskSummary? = null,
    val comments: List<TaskComment> = emptyList(),
    val assignmentHistory: List<TaskAssignment> = emptyList(),
    val progressHistory: List<TaskProgressUpdate> = emptyList(),
    val activityHistory: List<TaskActivityEvent> = emptyList()
)
